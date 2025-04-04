package com.frontleaves.scheduling.logic.scheduling;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.services.scheduling.GenerateInitialPopulationService;
import com.xlf.utility.util.UuidUtil;
import enums.CourseEnuType;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
/**
 * 遗传算法初始化种群逻辑实现类
 * <p>
 * 该类实现了遗传算法的初始化种群功能，通过随机生成初始种群，为后续的遗传算法进化提供基础。
 * </p>
 * @author FLASHLACK
 * @version 1.0
 */
public class GenerateInitialPopulationLogic implements GenerateInitialPopulationService {
    /**
     * 安全随机数生成器
     * 使用 SecureRandom 而不是普通的 Random 来确保随机性的安全性和不可预测性
     * 在遗传算法中，这种不可预测性对于确保种群多样性和避免陷入局部最优解是很重要的
     */
    private final SecureRandom random = new SecureRandom();

    @Override
    public List<ScheduleDTO> generateInitialPopulation(@NotNull AutomaticClassSchedulingBaseDTO baseData) {
        // 初始化种群列表
        List<ScheduleDTO> allPopulation = new ArrayList<>();
        int populationSize = baseData.getAlgorithmParams().getPopulationSize();
        for (int i = 0; i < populationSize; i++) {
            CourseScheduleDTO schedule = new CourseScheduleDTO();
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = new HashMap<>();
            log.debug("生成第 {} 个个体", i + 1);
            List<CourseScheduleDTO> population = new ArrayList<>();
            // 为每一个课程分配时间槽
            for (CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers : baseData.getCourseList()) {
                CourseLibraryDTO course = courseAndTeachers.getCourse();
                // 按照课程和班级进行教师选择
                Map<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> teacherAssignments = new HashMap<>();
                Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> classroomAssignments =
                        this.selectClassroomsForCourse(courseAndTeachers, baseData.getClassroomList(), true);
                // 为每个班级选择随机的教师
                if (classroomAssignments != null) {
                    for (Map.Entry<List<AdministrativeClassDTO>, ClassroomInfoDTO> entry : classroomAssignments.entrySet()) {
                        List<AdministrativeClassDTO> classGroup = entry.getKey();
                        TeacherCoursePreferencesDTO teacher = this.selectTeacherForCourse(
                                course, courseAndTeachers.getTeacherList());
                        // 记录教师分配
                        if (teacher != null) {
                            teacherAssignments.put(classGroup, teacher);
                        }
                    }
                }
                // 为每门课程分配时间槽，这一步分配了时间，班级，教室，老师
                for (Map.Entry<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> entry : teacherAssignments.entrySet()) {
                    List<AdministrativeClassDTO> classGroup = entry.getKey();
                    TeacherCoursePreferencesDTO assignedTeacher = entry.getValue();
                    ClassroomInfoDTO assignedClassroom = classroomAssignments.get(classGroup);
                    // 寻找合适的时间槽
                    List<TimeSlotDTO> timeSlot = findSuitableTimeSlot(
                            null,
                            null,
                            assignedTeacher,
                            assignedClassroom,
                            courseAndTeachers,
                            baseData);
                    if (timeSlot != null) {
                        CourseScheduleItemDTO item = new CourseScheduleItemDTO(
                                course,
                                assignedTeacher,
                                assignedClassroom,
                                classGroup,
                                new CreditHourTypeEnuDTO(),
                                courseAndTeachers.getPriority()
                        );
                        //将时间槽分配到课程安排中
                        assignments.put(timeSlot, item);
                        // 更新课程表
                        schedule.setAssignments(assignments);
                    } else {
                        log.warn("无法为课程 {} 找到合适的时间槽", course.getName());
                    }
                }
            }
            population.add(schedule);
            ScheduleDTO scheduleDTO = new ScheduleDTO();
            scheduleDTO.setSchedule(population);
            allPopulation.add(scheduleDTO);
        }
        log.debug("生成初始种群完成，种群大小: {}", allPopulation.size());
        return allPopulation;
    }

    /**
     * 为课程选择合适的教室
     * 此方法从给定的教室列表中选择适合的教室进行授课
     *
     * @param courseQualification 包含课程信息及其教师资格的列表
     * @param classrooms          可用的教室信息列表
     * @return 返回一个映射，键是行政班列表，值是适合该班的教室信息；如果没有合适的教室，则返回null
     */
    Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> selectClassroomsForCourse(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            @Nonnull List<ClassroomInfoDTO> classrooms,
            Boolean divideClasses
    ) {
        List<AdministrativeClassDTO> classList = courseQualification.getClassList();
        List<CourseLibraryAndTeacherCourseQualificationListDTO> newList = new ArrayList<>();
        if (Boolean.TRUE.equals(divideClasses)) {
            if (classList == null || classList.isEmpty()) {
                // 没有行政班级则为选修课，优先创建教学班级(即分配班级)
                this.assignTeachingClasses(courseQualification, newList);
            } else {
                // 为行政班级分配教学班级
                this.assignTeachingClassesForAdministrative(courseQualification, newList);
            }
        } else {
            newList.add(courseQualification);
        }
        // 为教学班级分配教室
        return this.assignClassrooms(
                classrooms, newList);
    }

    /**
     * 随机分配教室给课程
     * 此方法接收两个参数：一个是教室信息列表，另一个是课程及其相关资格信息列表
     * 它会根据课程的需求，为每门课程随机分配一个合适的教室
     * 如果没有合适的教室，则该课程不会被分配教室
     * @param classrooms 教室信息列表，包含每个教室的详细信息
     * @param newList 课程及其相关资格信息列表，每门课程包括一系列的行政班级
     * @return 返回一个映射，键是行政班级列表，值是分配给这些班级的教室信息
     */
    @Contract(pure = true)
    private @NotNull Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> assignClassrooms(
            List<ClassroomInfoDTO> classrooms,
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> newList) {
        // 初始化结果映射
        Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> result = new HashMap<>();
        // 遍历课程列表
        for (CourseLibraryAndTeacherCourseQualificationListDTO course : newList) {
            // 查找适合当前课程的教室
            List<ClassroomInfoDTO> matchingClassrooms = this.findSuitableClassrooms(classrooms, course);
            // 如果有合适的教室，则随机选择一个教室进行分配
            if (!matchingClassrooms.isEmpty()) {
                ClassroomInfoDTO selectedClassroom = this.selectRandomClassroom(matchingClassrooms, random);
                // 将课程的班级列表和选定的教室信息存入结果映射中
                result.put(course.getClassList(), selectedClassroom);
            }
        }
        // 返回分配结果
        return result;
    }

    /**
     * 查找合适的教室
     */
    private List<ClassroomInfoDTO> findSuitableClassrooms(
            List<ClassroomInfoDTO> classrooms,
            CourseLibraryAndTeacherCourseQualificationListDTO course) {
        List<ClassroomInfoDTO> matchingClassrooms = this.findOptimalClassrooms(classrooms, course);
        if (matchingClassrooms.isEmpty()) {
            matchingClassrooms = this.findMinimumRequirementClassrooms(classrooms, course);
        }
        if (matchingClassrooms.isEmpty()) {
            matchingClassrooms = this.findClosestCapacityClassrooms(classrooms, course);
        }
        return matchingClassrooms;
    }

    /** 查找最优教室（符合专业类型和容量要求）
     */
    private List<ClassroomInfoDTO> findOptimalClassrooms(
            List<ClassroomInfoDTO> classrooms,
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO course) {
        // 首先尝试专业教室
        List<ClassroomInfoDTO> specializedClassrooms = this.findClassroomsByTypeAndCapacity(
                classrooms,
                this.getRequiredClassroomType(course.getCourse(), course.getCourseEnuType()),
                course.getNumber());
        // 如果没有合适的专业教室，尝试理论教室
        if (specializedClassrooms.isEmpty()) {
            return this.findClassroomsByTypeAndCapacity(
                    classrooms,
                    course.getCourse().getTheoryClassroomType(),
                    course.getNumber());
        }

        return specializedClassrooms;
    }

    /**
     *  按类型和容量要求筛选教室
      */
    private List<ClassroomInfoDTO> findClassroomsByTypeAndCapacity(
            @NotNull List<ClassroomInfoDTO> classrooms,
            String classroomType,
            int studentCount) {
        return classrooms.stream()
                .filter(classroom -> classroom.getType().getClassTypeUuid().equals(classroomType))
                .filter(classroom -> this.isCapacityOptimal(classroom.getClassroom().getCapacity(), studentCount))
                .toList();
    }

    /** 判断教室容量是否最优

     */
    private boolean isCapacityOptimal(int capacity, int studentCount) {
        return capacity >= studentCount
                && capacity <= studentCount * 2.5
                && (double) studentCount / capacity >= 0.4
                && (double) studentCount / capacity <= 0.9;
    }

    /** 查找满足最低要求的教室（只考虑容纳学生数）

     */
    private List<ClassroomInfoDTO> findMinimumRequirementClassrooms(
            @NotNull List<ClassroomInfoDTO> classrooms,
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO course) {
        String requiredType = this.getRequiredClassroomType(course.getCourse(), course.getCourseEnuType());
        int studentCount = course.getNumber();

        // 先尝试专业教室
        List<ClassroomInfoDTO> specializedClassrooms = classrooms.stream()
                .filter(classroom -> classroom.getType().getClassTypeUuid().equals(requiredType))
                .filter(classroom -> classroom.getClassroom().getCapacity() >= studentCount)
                .toList();

        // 如果没有合适的专业教室，尝试理论教室
        if (specializedClassrooms.isEmpty()) {
            return classrooms.stream()
                    .filter(classroom -> classroom.getType().getClassTypeUuid()
                            .equals(course.getCourse().getTheoryClassroomType()))
                    .filter(classroom -> classroom.getClassroom().getCapacity() >= studentCount)
                    .toList();
        }

        return specializedClassrooms;
    }

    /** 查找容量最接近的教室

     */
    private List<ClassroomInfoDTO> findClosestCapacityClassrooms(
            @NotNull List<ClassroomInfoDTO> classrooms,
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO course) {
        String requiredType = this.getRequiredClassroomType(course.getCourse(), course.getCourseEnuType());
        int studentCount = course.getNumber();

        // 先尝试在专业教室中找最接近的
        List<ClassroomInfoDTO> closestSpecializedClassrooms = this.sortByCapacityDifference(
                classrooms.stream()
                        .filter(classroom -> classroom.getType().getClassTypeUuid().equals(requiredType))
                        .toList(),
                studentCount);

        if (!closestSpecializedClassrooms.isEmpty()) {
            return closestSpecializedClassrooms;
        }

        // 如果没有专业教室，在所有教室中找容量最接近的
        return this.sortByCapacityDifference(classrooms, studentCount);
    }

    /** 按容量差值排序

     */
    private List<ClassroomInfoDTO> sortByCapacityDifference(
            @NotNull List<ClassroomInfoDTO> classrooms,
            int studentCount) {
        return classrooms.stream()
                .sorted((c1, c2) -> {
                    int diff1 = Math.abs(c1.getClassroom().getCapacity() - studentCount);
                    int diff2 = Math.abs(c2.getClassroom().getCapacity() - studentCount);
                    return Integer.compare(diff1, diff2);
                })
                .toList();
    }

    /**
     * 选择随机教室
     */
    private ClassroomInfoDTO selectRandomClassroom(
            @NotNull List<ClassroomInfoDTO> matchingClassrooms,
            @NotNull Random random) {
        int maxRandomRange = Math.min(3, matchingClassrooms.size());
        int randomIndex = random.nextInt(maxRandomRange);
        return matchingClassrooms.get(randomIndex);
    }

    private String getRequiredClassroomType(CourseLibraryDTO courseLibrary, @NotNull CourseEnuType courseType) {
        return switch (courseType) {
            case EXPERIMENT -> courseLibrary.getExperimentClassroomType();
            case PRACTICE -> courseLibrary.getPracticeClassroomType();
            case COMPUTER -> courseLibrary.getComputerClassroomType();
            case OTHER ->
                // 其他类型默认使用理论教室
                    courseLibrary.getTheoryClassroomType();
            default -> courseLibrary.getTheoryClassroomType();
        };
    }

    /**
     * 为行政班级分配课程
     * 此方法根据课程资格和班级列表，为行政班级分配课程
     * 它通过排序班级并按组处理来确保班级组合符合课程要求
     *
     * @param courseQualification 课程资格信息，包括可教授课程和班级列表
     * @param newList             用于存储处理后的班级列表
     */
    private void assignTeachingClassesForAdministrative(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> newList) {
        // 获取行政班级列表并按专业排序
        List<AdministrativeClassDTO> sortedClassList = sortClassesByMajor(courseQualification.getClassList());
        // 临时存储当前正在组合的班级
        List<AdministrativeClassDTO> currentGroup = new ArrayList<>();
        // 当前组合班级的大小
        int currentGroupSize = 0;
        // 遍历排序后的班级列表
        for (int i = 0; i < sortedClassList.size(); i++) {
            // 当前正在处理的班级
            AdministrativeClassDTO currentClass = sortedClassList.get(i);
            // 处理当前班级的分配
            this.processClassAssignment(currentClass, currentGroup, currentGroupSize, courseQualification, newList);
            // 重新计算当前组合班级的大小
            currentGroupSize = this.calculateCurrentGroupSize(currentGroup);
            // 如果是最后一个班级，则处理最后一组
            if (i == sortedClassList.size() - 1) {
                this.handleLastGroup(currentGroup, currentGroupSize, courseQualification, newList);
            }
        }
    }

    /**
     * 处理班级分配
     */
    private void processClassAssignment(
            AdministrativeClassDTO currentClass,
            @NotNull List<AdministrativeClassDTO> currentGroup,
            int currentGroupSize,
            CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> newList) {
        if (currentGroup.isEmpty()) {
            currentGroup.add(currentClass);
            return;
        }
        int newGroupSize = currentGroupSize + currentClass.getStudentCount();
        this.handleGroupSizeChange(currentClass, currentGroup, currentGroupSize, newGroupSize, courseQualification, newList);
    }

    /**
     * 处理组大小变化
     */
    private void handleGroupSizeChange(
            AdministrativeClassDTO currentClass,
            List<AdministrativeClassDTO> currentGroup,
            int currentGroupSize,
            int newGroupSize,
            CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> newList) {
        if (newGroupSize > 180 || (currentGroupSize >= 30 && random.nextBoolean())) {
            if (currentGroupSize >= 30) {
                this.createNewTeachingClass(courseQualification, newList, currentGroup, currentGroupSize);
                currentGroup.clear();
                currentGroup.add(currentClass);
            } else {
                currentGroup.add(currentClass);
            }
        } else {
            currentGroup.add(currentClass);
        }
    }

    /**
     * 计算当前组的总人数
     */
    private int calculateCurrentGroupSize(@NotNull List<AdministrativeClassDTO> currentGroup) {
        return currentGroup.stream()
                .mapToInt(AdministrativeClassDTO::getStudentCount)
                .sum();
    }

    /**
     * 处理最后一组
     */
    private void handleLastGroup(
            List<AdministrativeClassDTO> currentGroup,
            int currentGroupSize,
            CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> newList) {

        if (currentGroupSize < 30) {
            this.redistributeLastGroup(currentGroup, newList);
        } else {
            this.createNewTeachingClass(courseQualification, newList, currentGroup, currentGroupSize);
        }
    }

    /**
     * 重新分配最后一组不足30人的班级
     */
    private void redistributeLastGroup(
            List<AdministrativeClassDTO> lastGroup,
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> newList) {
        if (!newList.isEmpty()) {
            CourseLibraryAndTeacherCourseQualificationListDTO lastTeachingClass = newList.get(newList.size() - 1);
            List<AdministrativeClassDTO> existingClasses = lastTeachingClass.getClassList();
            existingClasses.addAll(lastGroup);
            lastTeachingClass.setNumber(lastTeachingClass.getNumber() +
                    lastGroup.stream().mapToInt(AdministrativeClassDTO::getStudentCount).sum());
        }
    }

    /**
     * 创建新的教学班
     */
    private void createNewTeachingClass(
            CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> newList,
            List<AdministrativeClassDTO> classGroup,
            int totalStudents) {

        CourseLibraryAndTeacherCourseQualificationListDTO newClass =
                BeanUtil.copyProperties(courseQualification, CourseLibraryAndTeacherCourseQualificationListDTO.class);

        newClass.setNumber(totalStudents);
        newClass.setClassList(new ArrayList<>(classGroup));
        newClass.setTeachingClassUuid(UuidUtil.generateUuidNoDash());

        newList.add(newClass);
    }

    /**
     * 按专业对行政班级列表进行排序，使相同专业的班级在列表中位置相连
     *
     * @param classList 行政班级列表
     * @return 排序后的行政班级列表，相同专业的班级位置相连
     */
    private List<AdministrativeClassDTO> sortClassesByMajor(List<AdministrativeClassDTO> classList) {
        if (classList == null || classList.isEmpty()) {
            return Collections.emptyList();
        }
        // 按专业分组
        Map<String, List<AdministrativeClassDTO>> groupedByMajor = classList.stream()
                .collect(Collectors.groupingBy(
                        AdministrativeClassDTO::getMajorUuid,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
        // 将分组后的结果展平为一个列表，保持相同专业的班级位置相连
        return groupedByMajor.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 分配教学班级
     * 根据课程资格信息和当前课程资格列表，决定如何分配学生到不同的班级
     * 如果总人数小于30，只能开一个班；否则，随机决定要分几个班，并随机分配每个班的人数
     *
     * @param courseQualification        当前课程的资格信息，包括总人数等
     * @param newCourseQualificationList 新的课程资格列表，用于存储分配班级后的课程资格信息
     */
    private void assignTeachingClasses(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> newCourseQualificationList) {
        // 如果总人数小于30，只能开一个班
        Integer number = courseQualification.getNumber();
        if (number <= 30) {
            newCourseQualificationList.add(courseQualification);
        }
        int maxClasses = number / 30 + 1;
        int numClasses = 1 + random.nextInt(maxClasses);
        // 如果只分一个班，直接返回
        if (numClasses == 1) {
            newCourseQualificationList.add(courseQualification);
        }
        // 随机分配每个班的人数
        int remainingStudents = number;
        int remainingClasses = numClasses;
        while (remainingClasses > 0) {
            CourseLibraryAndTeacherCourseQualificationListDTO newClass =
                    BeanUtil.copyProperties(courseQualification, CourseLibraryAndTeacherCourseQualificationListDTO.class);
            if (remainingClasses == 1) {
                newClass.setNumber(remainingStudents);
            } else {
                // 确保剩余人数足够分配给剩下的班
                int maxPossible = Math.min(180, remainingStudents - (remainingClasses - 1) * 30);
                int minPossible = 30;
                int classSize = minPossible + random.nextInt(maxPossible - minPossible + 1);
                newClass.setNumber(classSize);
                remainingStudents -= classSize;
            }
            //设置教学班级UUID
            newClass.setTeachingClassUuid(UuidUtil.generateUuidNoDash());
            newCourseQualificationList.add(newClass);
            remainingClasses--;
        }
    }

}
