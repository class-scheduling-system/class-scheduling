package com.frontleaves.scheduling.logic.scheduling;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.services.scheduling.GenerateInitialPopulationService;
import com.frontleaves.scheduling.utils.ClassroomSelectionUtil;
import com.frontleaves.scheduling.utils.TimeSlotGeneratorUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 遗传算法生成初始种群逻辑实现类
 *
 * @author FLASHLACK
 */
@Service
@Slf4j
public class GenerateInitialPopulationLogic implements GenerateInitialPopulationService {
    /**
     * 安全随机数生成器
     * 使用 SecureRandom 而不是普通的 Random 来确保随机性的安全性和不可预测性
     * 在遗传算法中，这种不可预测性对于确保种群多样性和避免陷入局部最优解是很重要的
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * 重写生成初始种群的方法
     * 该方法根据提供的基础数据生成一个包含多个时间表的列表，作为遗传算法的初始种群
     *
     * @param baseData 不为空的自动课程调度基础数据对象，包含算法参数和种群大小等信息
     * @return 返回一个包含多个时间表的列表，表示初始种群
     */
    @Override
    public List<ScheduleDTO> generateInitialPopulation(@NotNull AutomaticClassSchedulingBaseDTO baseData) {
        // 初始化一个空列表来存储所有个体
        List<ScheduleDTO> allPopulation = new ArrayList<>();
        // 获取种群大小
        int populationSize = baseData.getAlgorithmParams().getPopulationSize();
        // 生成种群
        for (int i = 0; i < populationSize; i++) {
            // 记录每个个体的生成过程
            log.debug("生成第 {} 个个体", i + 1);
            // 调用生成个体的方法，并将个体添加到种群中
            ScheduleDTO scheduleDTO = this.generateIndividual(baseData);
            allPopulation.add(scheduleDTO);
        }
        // 种群生成完成后，记录种群大小
        log.debug("生成初始种群完成，种群大小: {}", allPopulation.size());
        // 返回生成的种群
        return allPopulation;
    }

    /**
     * 生成单个个体
     */
    private @NotNull ScheduleDTO generateIndividual(
            @NotNull AutomaticClassSchedulingBaseDTO baseData) {
        CourseScheduleDTO schedule = new CourseScheduleDTO();
        Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = new HashMap<>();

        // 处理每个课程
        for (CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers : baseData.getCourseList()) {
            this.processCourseAssignment(courseAndTeachers, baseData, assignments);
        }

        schedule.setAssignments(assignments);

        return this.createScheduleDTO(schedule, baseData);
    }

    /**
     * 处理单个课程的分配
     */
    private void processCourseAssignment(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers,
            @NotNull AutomaticClassSchedulingBaseDTO baseData,
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments) {

        CourseLibraryDTO course = courseAndTeachers.getCourse();

        // 获取教室和教师分配
        Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> classroomAssignments =
                this.selectClassroomsForCourse(courseAndTeachers, baseData.getClassroomList());
        log.debug("输出课程 {} 的教室分配: {}", course.getName(), JSONUtil.toJsonStr(classroomAssignments));
        if (classroomAssignments == null) {
            return;
        }

        // 为每个班级分配教师和时间槽
        Map<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> teacherAssignments =
                this.assignTeachersToClasses(course, courseAndTeachers, classroomAssignments);

        // 分配时间槽
        this.assignTimeSlots(courseAndTeachers, baseData, assignments, teacherAssignments, classroomAssignments);
    }

    /**
     * 为班级分配教师
     */
    private @NotNull Map<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> assignTeachersToClasses(
            CourseLibraryDTO course,
            CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers,
            @NotNull Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> classroomAssignments) {
        Map<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> teacherAssignments = new HashMap<>();
        for (List<AdministrativeClassDTO> classGroup : classroomAssignments.keySet()) {
            TeacherCoursePreferencesDTO teacher = this.selectTeacherForCourse(
                    course, courseAndTeachers.getTeacherList());
            if (teacher != null) {
                teacherAssignments.put(classGroup, teacher);
            }
        }

        return teacherAssignments;
    }

    /**
     * 分配时间槽
     */
    private void assignTimeSlots(
            CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers,
            AutomaticClassSchedulingBaseDTO baseData,
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments,
            @NotNull Map<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> teacherAssignments,
            Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> classroomAssignments) {

        for (Map.Entry<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> entry : teacherAssignments.entrySet()) {
            List<AdministrativeClassDTO> classGroup = entry.getKey();
            TeacherCoursePreferencesDTO assignedTeacher = entry.getValue();
            ClassroomInfoDTO assignedClassroom = classroomAssignments.get(classGroup);

            this.createAndAssignTimeSlot(
                    courseAndTeachers,
                    baseData,
                    assignments,
                    classGroup,
                    assignedTeacher,
                    assignedClassroom
            );
        }
    }

    /**
     * 创建并分配时间槽
     */
    private void createAndAssignTimeSlot(
            CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers,
            AutomaticClassSchedulingBaseDTO baseData,
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments,
            List<AdministrativeClassDTO> classGroup,
            TeacherCoursePreferencesDTO assignedTeacher,
            ClassroomInfoDTO assignedClassroom) {

        List<TimeSlotDTO> timeSlot = this.findSuitableTimeSlot(
                courseAndTeachers,
                baseData
        );

        if (timeSlot != null) {
            CourseScheduleItemDTO item = new CourseScheduleItemDTO(
                    courseAndTeachers.getCourse(),
                    assignedTeacher,
                    assignedClassroom,
                    classGroup,
                    new CreditHourTypeEnuDTO(),
                    courseAndTeachers.getPriority()
            );
            assignments.put(timeSlot, item);
        } else {
            log.warn("无法为课程 {} 找到合适的时间槽", courseAndTeachers.getCourse().getName());
        }
    }

    /**
     * 创建ScheduleDTO
     */
    private @NotNull ScheduleDTO createScheduleDTO(CourseScheduleDTO schedule, @NotNull AutomaticClassSchedulingBaseDTO baseData) {
        List<CourseScheduleDTO> population = new ArrayList<>();
        population.add(schedule);

        ScheduleDTO scheduleDTO = new ScheduleDTO();
        scheduleDTO.setSchedule(population);
        scheduleDTO.setData(baseData.getDataCourseScheduleList());

        return scheduleDTO;
    }

    /**
     * 查找合适的时间槽
     */
    private @NotNull List<TimeSlotDTO> findSuitableTimeSlot(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO course,
            @NotNull AutomaticClassSchedulingBaseDTO baseDTO
    ) {
        // 生成时间槽
        List<TimeSlotDTO> timeSlots = TimeSlotGeneratorUtil.generateTimeSlots(
                course,
                Boolean.TRUE.equals(baseDTO.getTimePreferences().getEveningCourses())
        );
        log.debug("初始化种群，直接返回随机生成的时间槽");
        return timeSlots;
    }

    /**
     * 为课程选择合适的教师
     * <p>
     * 本方法根据课程的学科要求，从候选教师列表中选择一名合适的教师进行课程教学。
     * 选择过程仅考虑教师对课程学科的适应性，确保教师具备教授该课程的资格。
     * </p>
     *
     * @param course   课程信息对象，包含课程学科、课程类型等基本属性
     * @param teachers 候选教师列表，包含所有可能分配给该课程的教师
     * @return 选择的教师对象；如果没有合适的教师，则返回null
     */
    @Nullable
    private TeacherCoursePreferencesDTO selectTeacherForCourse(
            CourseLibraryDTO course,
            @NotNull List<TeacherCoursePreferencesDTO> teachers
    ) {
        // 筛选出能够教授该课程学科的教师列表
        List<TeacherCoursePreferencesDTO> suitableTeachers = teachers.stream()
                .filter(teacher -> teacher.getQualification() != null
                        && teacher.getQualification().getCourseUuid().equals(course.getCourseLibraryUuid()))
                .toList();
        // 如果没有找到符合条件的教师，返回null
        if (suitableTeachers.isEmpty()) {
            log.warn("没有找到合适的教师来教授课程: {}", course.getName());
            return null;
        }
        // 随机选择一个符合条件的教师
        return suitableTeachers.get(random.nextInt(suitableTeachers.size()));
    }

    /**
     * 为课程选择合适的教室
     */
    Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> selectClassroomsForCourse(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualification,
            @Nonnull List<ClassroomInfoDTO> classrooms) {

        List<AdministrativeClassDTO> classList = courseQualification.getClassList();
        List<CourseLibraryAndTeacherCourseQualificationListDTO> newList = new ArrayList<>();
        if (classList == null || classList.isEmpty()) {
            // 没有行政班级则为选修课，优先创建教学班级(即分配班级)
            this.assignTeachingClasses(courseQualification, newList);
        } else {
            // 为行政班级分配教学班级
            this.assignTeachingClassesForAdministrative(courseQualification, newList);
        }
        newList.add(courseQualification);
        // 为教学班级分配教室
        return this.assignClassrooms(classrooms, newList);
    }

    /**
     * 随机分配教室给课程
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
            List<ClassroomInfoDTO> matchingClassrooms =
                    ClassroomSelectionUtil.findSuitableClassrooms(classrooms, course);
            // 如果有合适的教室，则随机选择一个教室进行分配
            if (!matchingClassrooms.isEmpty()) {
                ClassroomInfoDTO selectedClassroom =
                        ClassroomSelectionUtil.selectRandomClassroom(matchingClassrooms, random);
                // 将课程的班级列表和选定的教室信息存入结果映射中
                result.put(course.getClassList(), selectedClassroom);
            }
        }
        // 返回分配结果
        return result;
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
            courseQualification.setTeachingClassUuid(UuidUtil.generateUuidNoDash());
            newCourseQualificationList.add(courseQualification);
            return;
        }
        // 计算最大可能的班级数，确保至少为1
        int maxClasses = Math.max(1, number / 30);
        int numClasses = 1 + random.nextInt(maxClasses);
        // 如果只分一个班，直接返回
        if (numClasses == 1) {
            courseQualification.setTeachingClassUuid(UuidUtil.generateUuidNoDash());
            newCourseQualificationList.add(courseQualification);
            return;
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
