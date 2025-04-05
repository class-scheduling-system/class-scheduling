package com.frontleaves.scheduling.logic.scheduling;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.services.scheduling.IterateService;
import com.frontleaves.scheduling.utils.ClassroomSelectionUtil;
import com.frontleaves.scheduling.utils.TimeSlotGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.*;

import static com.frontleaves.scheduling.utils.ScheduleFitnessCalculator.calculateFitness;


/**
 * 选择逻辑实现类
 *
 * @author FLASHLACK
 */
@Service
@Slf4j
public class IterateLogic implements IterateService {
    /**
     * 安全随机数生成器
     * 使用 SecureRandom 而不是普通的 Random 来确保随机性的安全性和不可预测性
     * 在遗传算法中，这种不可预测性对于确保种群多样性和避免陷入局部最优解是很重要的
     */
    private final SecureRandom random = new SecureRandom();

    @Override
    public List<ScheduleDTO> selection(@NotNull List<ScheduleDTO> allPopulation) {
        List<ScheduleDTO> selected = new ArrayList<>();
        // 计算总适应度
        double totalFitness = allPopulation.stream()
                .mapToDouble(ScheduleDTO::getFitness)
                .sum();
        // 使用轮盘赌选择算法选择个体
        while (selected.size() < allPopulation.size()) {
            // 生成随机点
            double point = random.nextDouble() * totalFitness;
            double sum = 0;
            // 轮盘赌选择
            for (ScheduleDTO schedule : allPopulation) {
                sum += schedule.getFitness();
                if (sum >= point) {
                    // 选中当前个体，进行深拷贝
                    selected.add(this.deepCopySchedule(schedule));
                    break;
                }
            }
        }
        return selected;
    }

    /**
     * 执行交叉操作，生成新一代时间表
     *
     * @param selected      经过选择的操作父代时间表
     * @param crossoverRate 交叉概率，决定是否执行交叉操作
     * @param baseDTO       基础数据传输对象，包含自动排课所需的基础信息
     * @return 返回通过交叉操作生成的新一代时间表列表
     */
    @Override
    public List<ScheduleDTO> crossover(@NotNull List<ScheduleDTO> selected, Double crossoverRate, AutomaticClassSchedulingBaseDTO baseDTO) {
        // 初始化后代时间表列表
        List<ScheduleDTO> offspring = new ArrayList<>();
        // 两两配对进行交叉
        for (int i = 0; i < selected.size() - 1; i += 2) {
            // 获取一对父代时间表
            ScheduleDTO parent1 = selected.get(i);
            ScheduleDTO parent2 = selected.get(i + 1);
            // 根据交叉概率决定是否执行交叉操作
            if (random.nextDouble() < crossoverRate) {
                // 执行交叉
                List<ScheduleDTO> children = this.crossoverSchedules(parent1, parent2, baseDTO);
                offspring.addAll(children);
            } else {
                // 直接复制父代
                offspring.add(this.deepCopySchedule(parent1));
                offspring.add(this.deepCopySchedule(parent2));
            }
        }
        // 如果是奇数，保留最后一个
        if (selected.size() % 2 != 0) {
            offspring.add(this.deepCopySchedule(selected.get(selected.size() - 1)));
        }
        return offspring;
    }

    @Override
    public void mutation(
            @NotNull List<ScheduleDTO> offspring, Double mutationRate,
            AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO schedule : offspring) {
            List<CourseScheduleDTO> schedules = schedule.getSchedule();
            if (schedules == null || schedules.isEmpty()) {
                // 跳过空的课程表
                continue;
            }
            // 随机选择一门课程进行变异
            CourseScheduleDTO courseSchedule = schedules.get(random.nextInt(schedules.size()));
            if (random.nextDouble() < mutationRate) {
                // 选择变异策略
                int strategy = random.nextInt(3);
                try {
                    switch (strategy) {
                        case 0:
                            // 时间槽变异
                            this.timeSlotMutation(
                                    courseSchedule, baseDTO.getCourseList(), baseDTO, schedules);
                            break;
                        case 1:
                            // 教室变异
                            this.classroomMutation(
                                    courseSchedule, baseDTO);
                            break;
                        case 2:
                            // 教师变异
                            this.teacherMutation(
                                    courseSchedule, baseDTO);
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    log.error("变异操作失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 执行教师变异操作
     * 随机选择新的合格教师替换当前教师
     * @param courseSchedule 课程安排
     * @param baseDTO 基础数据
     */
    private void teacherMutation(
            CourseScheduleDTO courseSchedule,
            AutomaticClassSchedulingBaseDTO baseDTO) {
        // 参数校验
        if (courseSchedule == null || courseSchedule.getAssignments().isEmpty() || baseDTO == null) {
            return;
        }
        // 获取当前课程安排
        Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = courseSchedule.getAssignments();
        Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry = assignments.entrySet().iterator().next();
        List<TimeSlotDTO> timeSlots = entry.getKey();
        CourseScheduleItemDTO currentItem = entry.getValue();
        // 获取当前课程ID
        String courseId = currentItem.getCourse().getCourseLibraryUuid();
        // 从基础数据中查找当前课程
        CourseLibraryAndTeacherCourseQualificationListDTO courseInfo =
                baseDTO.getCourseList().stream()
                        .filter(c -> courseId.equals(c.getCourse().getCourseLibraryUuid()))
                        .findFirst()
                        .orElse(null);
        // 如果找不到课程信息或教师列表为空，则返回
        if (courseInfo == null || courseInfo.getTeacherList() == null) {
            log.warn("找不到课程信息或教师列表为空: {}", courseId);
            return;
        }
        // 筛选出能够教授该课程的教师列表
        List<TeacherCoursePreferencesDTO> suitableTeachers = courseInfo.getTeacherList().stream()
                .filter(teacher -> teacher.getQualification() != null
                        && teacher.getQualification().getCourseUuid().equals(courseId))
                .toList();
        // 如果没有找到符合条件的教师，直接返回
        if (suitableTeachers.isEmpty()) {
            log.warn("没有找到合适的教师来教授课程: {}", currentItem.getCourse().getName());
            return;
        }
        // 获取当前教师ID
        String currentTeacherId = currentItem.getTeacher().getTeacher().getTeacherUuid();
        // 过滤掉当前教师，只保留其他教师
        List<TeacherCoursePreferencesDTO> otherTeachers = suitableTeachers.stream()
                .filter(teacher -> !teacher.getTeacher().getTeacherUuid().equals(currentTeacherId))
                .toList();
        // 如果存在其他可选教师，则随机选择一个替换当前教师
        if (!otherTeachers.isEmpty()) {
            // 随机选择一个教师
            TeacherCoursePreferencesDTO newTeacher =
                    otherTeachers.get(random.nextInt(otherTeachers.size()));
            // 移除原来的课程安排
            assignments.remove(timeSlots);
            // 创建新的排课项，保持其他属性不变，只替换教师
            CourseScheduleItemDTO newItem = new CourseScheduleItemDTO(
                    currentItem.getCourse(),
                    newTeacher,  // 替换为新教师
                    currentItem.getClassroom(),
                    currentItem.getClassGroup(),
                    currentItem.getCourseType(),
                    currentItem.getPriority()
            );
            // 添加新的课程安排
            assignments.put(timeSlots, newItem);
            log.debug("课程 {} 的教师从 {} 变更为 {}",
                    currentItem.getCourse().getName(),
                    currentTeacherId,
                    newTeacher.getTeacher().getTeacherUuid());
        } else {
            log.debug("课程 {} 没有其他合格的教师可替换", currentItem.getCourse().getName());
        }
    }
    private void classroomMutation(
            @NotNull CourseScheduleDTO courseSchedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        // 获取所有课程安排
        List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries =
                new ArrayList<>(courseSchedule.getAssignments().entrySet());
        if (!entries.isEmpty()) {
            // 随机选择一个课程安排
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseScheduleItemDTO currentItem = entry.getValue();
            CourseLibraryDTO course = currentItem.getCourse();
            // 查找可以教授这门课程的教室列表
            CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeacher = this.findCourseById(
                    course.getCourseLibraryUuid(),
                    baseDTO.getCourseList()
            );
            if (courseAndTeacher != null) {
                // 获取可用的教室列表
                Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> classroomList =
                        this.selectClassroomsForCourse(courseAndTeacher, baseDTO.getClassroomList());
                if (!classroomList.isEmpty()) {
                    // 过滤掉当前教室
                    List<Map.Entry<List<AdministrativeClassDTO>, ClassroomInfoDTO>> availableClassrooms =
                            classroomList.entrySet().stream()
                                    .filter(classroomEntry ->
                                            !classroomEntry.getValue().getClassroom().getClassroomUuid()
                                                    .equals(currentItem.getClassroom().getClassroom().getClassroomUuid()))
                                    .toList();
                    if (!availableClassrooms.isEmpty()) {
                        // 随机选择一个新教室
                        Map.Entry<List<AdministrativeClassDTO>, ClassroomInfoDTO> newClassroomEntry =
                                availableClassrooms.get(random.nextInt(availableClassrooms.size()));
                        // 创建新的排课项
                        CourseScheduleItemDTO newItem = new CourseScheduleItemDTO(
                                course,
                                currentItem.getTeacher(),
                                newClassroomEntry.getValue(),
                                //使用原来的班级
                                currentItem.getClassGroup(),
                                currentItem.getCourseType(),
                                currentItem.getPriority()
                        );
                        // 更新课程安排
                        courseSchedule.getAssignments().put(entry.getKey(), newItem);
                    }
                }
            }
        }
    }

    private @NotNull Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> selectClassroomsForCourse(
            CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeacher,
            List<ClassroomInfoDTO> classroomList) {
        // 初始化结果
        Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> result = new HashMap<>();
        // 参数校验
        if (courseAndTeacher == null || classroomList == null || classroomList.isEmpty()) {
            return result;
        }
        // 使用工具类查找合适的教室
        List<ClassroomInfoDTO> suitableClassrooms =
                ClassroomSelectionUtil.findSuitableClassrooms(classroomList, courseAndTeacher);
        // 如果找到合适的教室，随机选择一个并添加到结果中
        if (!suitableClassrooms.isEmpty()) {
            // 使用随机数生成器选择教室
            ClassroomInfoDTO selectedClassroom =
                    ClassroomSelectionUtil.selectRandomClassroom(suitableClassrooms, random);
            // 将教室与班级关联
            result.put(courseAndTeacher.getClassList(), selectedClassroom);
        }
        return result;
    }

    /**
     * 根据课程库UUID在课程列表中查找匹配的课程
     * @param courseLibraryUuid 课程库的唯一标识符
     * @param courseList 课程列表，包含课程库和教师课程资格信息
     * @return 如果找到匹配的课程，则返回该课程的对象；否则返回null
     */
    private CourseLibraryAndTeacherCourseQualificationListDTO findCourseById(
            String courseLibraryUuid,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList) {
        // 检查输入参数是否有效，如果无效则直接返回null
        if (courseLibraryUuid == null || courseList == null || courseList.isEmpty()) {
            return null;
        }
        // 使用流处理课程列表，过滤出课程库UUID匹配的课程
        return courseList.stream()
                .filter(course -> courseLibraryUuid
                        .equals(course.getCourse().getCourseLibraryUuid()))
                .findFirst()
                .orElse(null);
    }

    private void timeSlotMutation(
            @NotNull CourseScheduleDTO courseSchedule,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList,
            AutomaticClassSchedulingBaseDTO baseDTO,
            List<CourseScheduleDTO> schedules) {
        // 获取当前课程安排
        Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry =
                courseSchedule.getAssignments().entrySet().iterator().next();
        // 获取对应的课程信息
        CourseLibraryAndTeacherCourseQualificationListDTO course =
                findCourseByScheduleItem(entry.getValue(), courseList);
        if (course != null) {
            // 尝试找到新的合适时间槽
            List<TimeSlotDTO> newTimeSlot = this.findSuitableTimeSlot(
                    schedules,
                    entry,
                    entry.getValue().getTeacher(),
                    entry.getValue().getClassroom(),
                    course,
                    baseDTO
            );
            if (newTimeSlot != null) {
                // 找到新时间槽才进行替换
                courseSchedule.getAssignments().remove(entry.getKey());
                courseSchedule.getAssignments().put(newTimeSlot, entry.getValue());
            } else {
                // 如果找不到合适的新时间槽，尝试与其他课程交换时间
                for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> other : courseSchedule.getAssignments().entrySet()) {
                    if (!other.equals(entry) &&
                            this.isSwapValid(entry, other, courseSchedule, schedules)) {
                        this.swapTimeSlots(courseSchedule, entry, other);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 交换两个课程的时间槽
     */
    private void swapTimeSlots(
            @NotNull CourseScheduleDTO courseSchedule,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2) {
        // 保存原始数据
        List<TimeSlotDTO> slots1 = entry1.getKey();
        List<TimeSlotDTO> slots2 = entry2.getKey();
        CourseScheduleItemDTO item1 = entry1.getValue();
        CourseScheduleItemDTO item2 = entry2.getValue();
        // 从原始Map中移除条目
        courseSchedule.getAssignments().remove(slots1);
        courseSchedule.getAssignments().remove(slots2);
        // 创建新的时间槽列表（深拷贝）
        List<TimeSlotDTO> newSlots1 = deepCopyTimeSlots(slots2);
        List<TimeSlotDTO> newSlots2 = deepCopyTimeSlots(slots1);
        // 重新添加到Map中，交换时间槽
        courseSchedule.getAssignments().put(newSlots1, item1);
        courseSchedule.getAssignments().put(newSlots2, item2);

    }

    /**
     * 深拷贝时间槽列表
     */
    private List<TimeSlotDTO> deepCopyTimeSlots(@NotNull List<TimeSlotDTO> slots) {
        return slots.stream()
                .map(slot -> new TimeSlotDTO(
                        slot.getWeek(),
                        slot.getDay(),
                        slot.getPeriod()))
                .toList();
    }

    /**
     * 检查两个课程时间槽交换是否有效
     */
    private boolean isSwapValid(
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2,
            CourseScheduleDTO courseSchedule,
            List<CourseScheduleDTO> population) {

        // 1. 检查教师冲突
        if (hasTeacherConflict(entry1, entry2)) {
            return false;
        }
        // 2. 检查教室冲突
        if (hasClassroomConflict(entry1, entry2)) {
            return false;
        }
        // 3. 检查班级冲突
        if (hasClassConflict(entry1, entry2)) {
            return false;
        }
        // 4. 检查与其他课程的冲突
        return !hasConflictWithOtherCourses(entry1, entry2, courseSchedule, population);
    }

    /**
     * 检查教师冲突
     */
    private boolean hasTeacherConflict(
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2) {

        String teacher1Uuid = entry1.getValue().getTeacher().getTeacher().getTeacherUuid();
        String teacher2Uuid = entry2.getValue().getTeacher().getTeacher().getTeacherUuid();

        // 如果是同一个教师，检查交换后的时间是否冲突
        if (teacher1Uuid.equals(teacher2Uuid)) {
            return hasTimeOverlap(entry1.getKey(), entry2.getKey());
        }

        return false;
    }

    /**
     * 检查教室冲突
     */
    private boolean hasClassroomConflict(
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2) {
        // 获取教室信息，如果任一环节为空则返回false
        if (entry1.getValue().getClassroom() == null ||
                entry2.getValue().getClassroom() == null ||
                entry1.getValue().getClassroom().getClassroom() == null ||
                entry2.getValue().getClassroom().getClassroom() == null) {
            return false;
        }
        String room1Uuid = entry1.getValue().getClassroom().getClassroom().getClassroomUuid();
        String room2Uuid = entry2.getValue().getClassroom().getClassroom().getClassroomUuid();
        // 如果教室UUID为空，则不检测冲突
        if (room1Uuid == null || room2Uuid == null) {
            return false;
        }
        // 如果是同一个教室，检查交换后的时间是否冲突
        if (room1Uuid.equals(room2Uuid)) {
            return hasTimeOverlap(entry1.getKey(), entry2.getKey());
        }
        return false;
    }

    /**
     * 检查班级冲突
     * 如果任一课程没有班级信息，则认为不冲突，返回false
     */
    private boolean hasClassConflict(
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2) {
        // 首先检查entry的value是否为空
        if (entry1.getValue() == null || entry2.getValue() == null) {
            return false;
        }
        // 检查getClassGroup()的返回值是否为空
        List<AdministrativeClassDTO> classes1 = entry1.getValue().getClassGroup();
        List<AdministrativeClassDTO> classes2 = entry2.getValue().getClassGroup();
        // 如果任一班级列表为空，则认为不冲突
        if (classes1 == null || classes2 == null ||
                classes1.isEmpty() || classes2.isEmpty()) {
            return false;
        }
        // 检查是否有相同的班级
        boolean hasCommonClass = classes1.stream()
                .anyMatch(class1 -> classes2.stream()
                        .anyMatch(class2 -> {
                            // 增加对getAdministrativeClassUuid的空值检查
                            String uuid1 = class1.getAdministrativeClassUuid();
                            String uuid2 = class2.getAdministrativeClassUuid();
                            return uuid1 != null && uuid1.equals(uuid2);
                        }));

        // 如果有相同的班级，检查交换后的时间是否冲突
        if (hasCommonClass) {
            return hasTimeOverlap(entry1.getKey(), entry2.getKey());
        }

        return false;
    }

    /**
     * 检查与其他课程的冲突
     */
    private boolean hasConflictWithOtherCourses(
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2,
            @NotNull CourseScheduleDTO currentSchedule,
            List<CourseScheduleDTO> population) {

        // 检查当前课程表中的其他课程
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> other :
                currentSchedule.getAssignments().entrySet()) {
            // 跳过要交换的两个课程
            if (other.equals(entry1) || other.equals(entry2)) {
                continue;
            }

            // 检查交换后是否与其他课程冲突
            if (wouldCauseConflict(entry1, entry2, other)) {
                return true;
            }
        }

        // 检查其他课程表中的课程
        for (CourseScheduleDTO schedule : population) {
            if (schedule.equals(currentSchedule)) {
                continue;
            }

            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> other :
                    schedule.getAssignments().entrySet()) {
                if (wouldCauseConflict(entry1, entry2, other)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 检查交换后是否会导致冲突
     */
    private boolean wouldCauseConflict(
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> other) {
        // 检查时间重叠
        boolean timeConflict1 = hasTimeOverlap(entry2.getKey(), other.getKey());
        boolean timeConflict2 = hasTimeOverlap(entry1.getKey(), other.getKey());
        if (!timeConflict1 && !timeConflict2) {
            return false;
        }
        // 如果有时间重叠，检查资源冲突
        return hasResourceConflict(entry1.getValue(), other.getValue()) ||
                hasResourceConflict(entry2.getValue(), other.getValue());
    }

    /**
     * 检查时间是否重叠
     */
    private boolean hasTimeOverlap(@NotNull List<TimeSlotDTO> slots1, List<TimeSlotDTO> slots2) {
        return checkTime(slots1, slots2);
    }

    /**
     * 检查资源冲突
     */
    private boolean hasResourceConflict(
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        // 检查教师冲突
        if (item1.getTeacher().getTeacher().getTeacherUuid()
                .equals(item2.getTeacher().getTeacher().getTeacherUuid())) {
            return true;
        }
        // 检查教室冲突
        if (item1.getClassroom().getClassroom().getClassroomUuid()
                .equals(item2.getClassroom().getClassroom().getClassroomUuid())) {
            return true;
        }
        // 如果任一班级列表为空，则不检查班级冲突，直接返回false
        List<AdministrativeClassDTO> classes1 = item1.getClassGroup();
        List<AdministrativeClassDTO> classes2 = item2.getClassGroup();
        if (classes1 == null || classes2 == null ||
                classes1.isEmpty() || classes2.isEmpty()) {
            return false;
        }
        // 检查班级冲突
        return classes1.stream()
                .anyMatch(class1 -> classes2.stream()
                        .anyMatch(class2 -> class1.getAdministrativeClassUuid()
                                .equals(class2.getAdministrativeClassUuid())));
    }

    /**
     * 查找合适的时间槽
     *
     * @param schedules 课程表列表
     * @param entry     课程时间表项
     * @param teacher   教师信息
     * @param classroom 教室信息
     * @param course    课程信息
     * @param baseDTO   基础数据传输对象
     * @return 合适的时间槽列表，如果没有找到则返回null
     */
    @Contract(pure = true)
    private @Nullable List<TimeSlotDTO> findSuitableTimeSlot(
            List<CourseScheduleDTO> schedules,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry,
            TeacherCoursePreferencesDTO teacher,
            ClassroomInfoDTO classroom,
            CourseLibraryAndTeacherCourseQualificationListDTO course,
            @NotNull AutomaticClassSchedulingBaseDTO baseDTO) {
        // 生成新的时间槽
        List<TimeSlotDTO> newTimeSlots = TimeSlotGeneratorUtil.generateTimeSlots(
                course,
                Boolean.TRUE.equals(baseDTO.getTimePreferences().getEveningCourses())
        );
        // 检查新时间槽是否有冲突
        if (this.isTimeSlotValid(newTimeSlots, schedules, teacher, classroom, entry.getValue())) {
            return newTimeSlots;
        }
        return null;
    }

    /**
     * 检查时间槽是否有效（无冲突）
     */
    private boolean isTimeSlotValid(
            List<TimeSlotDTO> newTimeSlots,
            List<CourseScheduleDTO> schedules,
            TeacherCoursePreferencesDTO teacher,
            ClassroomInfoDTO classroom,
            CourseScheduleItemDTO currentItem) {
        // 如果没有其他课程表，则时间槽有效
        if (schedules == null || schedules.isEmpty()) {
            return true;
        }
        // 检查与其他课程的冲突
        for (CourseScheduleDTO schedule : schedules) {
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> existingEntry :
                    schedule.getAssignments().entrySet()) {
                // 跳过当前项
                if (existingEntry.getValue().equals(currentItem)) {
                    continue;
                }
                // 检查时间冲突
                if (hasTimeConflict(newTimeSlots, existingEntry.getKey()) &&
                        hasResourceConflict(
                                teacher,
                                classroom,
                                currentItem,
                                existingEntry.getValue())) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 检查时间冲突
     */
    private boolean hasTimeConflict(@NotNull List<TimeSlotDTO> slots1, List<TimeSlotDTO> slots2) {
        return checkTime(slots1, slots2);
    }

    /**
     * 检查两个时间槽列表是否有时间上的冲突
     * 该方法通过比较两个列表中的时间槽，检查它们是否在同一周的同一天的同一时间段有重叠
     * @param slots1 第一个时间槽列表，不能为空
     * @param slots2 第二个时间槽列表，可以为空
     * @return 如果存在时间冲突，则返回true；否则返回false
     */
    private boolean checkTime(@NotNull List<TimeSlotDTO> slots1, List<TimeSlotDTO> slots2) {
        // 遍历第一个时间槽列表
        for (TimeSlotDTO slot1 : slots1) {
            // 遍历第二个时间槽列表
            for (TimeSlotDTO slot2 : slots2) {
                // 检查两个时间槽是否在同一周、同一天、同一时间段
                if (slot1.getWeek().equals(slot2.getWeek()) &&
                        slot1.getDay().equals(slot2.getDay()) &&
                        slot1.getPeriod().equals(slot2.getPeriod())) {
                    // 如果找到时间冲突，立即返回true
                    return true;
                }
            }
        }
        // 如果没有找到任何时间冲突，返回false
        return false;
    }

    /**
     * 检查资源冲突（教师、教室、班级）
     */
    private boolean hasResourceConflict(
            @NotNull TeacherCoursePreferencesDTO teacher,
            ClassroomInfoDTO classroom,
            CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        // 检查教师冲突
        if (teacher.getTeacher().getTeacherUuid()
                .equals(item2.getTeacher().getTeacher().getTeacherUuid())) {
            return true;
        }
        // 检查教室冲突
        if (classroom.getClassroom().getClassroomUuid()
                .equals(item2.getClassroom().getClassroom().getClassroomUuid())) {
            return true;
        }
        // 检查班级冲突（如果任一为空则跳过检查）
        List<AdministrativeClassDTO> classes1 = item1 != null ? item1.getClassGroup() : null;
        List<AdministrativeClassDTO> classes2 = item2.getClassGroup();
        if (classes1 == null || classes2 == null ||
                classes1.isEmpty() || classes2.isEmpty()) {
            return false;
        }
        return hasClassConflict(classes1, classes2);
    }


    /**
     * 检查班级冲突
     */
    private boolean hasClassConflict(
            @NotNull List<AdministrativeClassDTO> classes1,
            List<AdministrativeClassDTO> classes2) {

        return classes1.stream()
                .anyMatch(class1 -> classes2.stream()
                        .anyMatch(class2 -> class1.getAdministrativeClassUuid()
                                .equals(class2.getAdministrativeClassUuid())));
    }

    /**
     * 根据课程时间表项在课程列表中查找匹配的课程
     * 此方法用于在给定的课程列表中，根据课程时间表项中的课程库UUID，
     * 查找并返回匹配的课程信息如果找不到匹配的课程，则返回null
     *
     * @param value      课程时间表项，包含需要查找的课程的信息
     * @param courseList 课程列表，用于搜索匹配的课程
     * @return 匹配的课程信息，如果没有找到则返回null
     */
    private CourseLibraryAndTeacherCourseQualificationListDTO findCourseByScheduleItem(
            CourseScheduleItemDTO value, @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList) {
        // 使用流处理，过滤出课程库UUID匹配的课程
        return courseList.stream()
                .filter(course -> course.getCourse().getCourseLibraryUuid()
                        .equals(value.getCourse().getCourseLibraryUuid()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 交叉两个日程安排以生成新的日程安排
     * 此方法通过结合两个父日程安排的特征来创建两个新的子日程安排
     * 它首先将每个父日程安排转换为课程安排的映射，然后通过交叉操作生成两个子日程安排
     *
     * @param parent1 第一个父日程安排，不能为空
     * @param parent2 第二个父日程安排，不能为空
     * @param baseDTO 基础DTO，包含自动排课的通用信息
     * @return 可能为空的子日程安排列表，包含两个子日程安排
     */
    @Contract(pure = true)
    private @NotNull List<ScheduleDTO> crossoverSchedules(
            @NotNull ScheduleDTO parent1,
            @NotNull ScheduleDTO parent2,
            AutomaticClassSchedulingBaseDTO baseDTO) {
        // 1. 准备父代课程映射
        Map<String, CourseScheduleDTO> courseSchedules1 = this.createCourseMap(parent1.getSchedule());
        Map<String, CourseScheduleDTO> courseSchedules2 = this.createCourseMap(parent2.getSchedule());
        // 2. 创建子代
        SchedulePair children = this.initializeChildren();
        // 3. 执行交叉操作
        this.performCrossover(
                courseSchedules1,
                courseSchedules2,
                children.child1Schedules(),
                children.child2Schedules()
        );
        // 4. 构建并返回结果
        return this.buildChildrenSchedules(children, baseDTO);
    }

    /**
     * 创建课程ID到课程表的映射
     */
    private @NotNull Map<String, CourseScheduleDTO> createCourseMap(@NotNull List<CourseScheduleDTO> schedules) {
        Map<String, CourseScheduleDTO> courseMap = new HashMap<>();
        for (CourseScheduleDTO schedule : schedules) {
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry :
                    schedule.getAssignments().entrySet()) {
                String courseId = entry.getValue().getCourse().getCourseLibraryUuid();
                courseMap.put(courseId, schedule);
            }
        }
        return courseMap;
    }

    /**
     * 初始化子代
     */
    private @NotNull SchedulePair initializeChildren() {
        ScheduleDTO child1 = new ScheduleDTO();
        ScheduleDTO child2 = new ScheduleDTO();
        List<CourseScheduleDTO> child1Schedules = new ArrayList<>();
        List<CourseScheduleDTO> child2Schedules = new ArrayList<>();

        return new SchedulePair(child1, child2, child1Schedules, child2Schedules);
    }

    /**
     * 执行交叉操作
     */
    private void performCrossover(
            @NotNull Map<String, CourseScheduleDTO> courseSchedules1,
            @NotNull Map<String, CourseScheduleDTO> courseSchedules2,
            List<CourseScheduleDTO> child1Schedules,
            List<CourseScheduleDTO> child2Schedules) {

        Set<String> allCourses = new HashSet<>();
        allCourses.addAll(courseSchedules1.keySet());
        allCourses.addAll(courseSchedules2.keySet());

        int crossoverPoint = random.nextInt(!allCourses.isEmpty() ? allCourses.size() : 1);

        this.crossoverCourses(
                allCourses,
                crossoverPoint,
                courseSchedules1,
                courseSchedules2,
                child1Schedules,
                child2Schedules
        );
    }

    /**
     * 交叉课程
     */
    private void crossoverCourses(
            @NotNull Set<String> allCourses,
            int crossoverPoint,
            Map<String, CourseScheduleDTO> courseSchedules1,
            Map<String, CourseScheduleDTO> courseSchedules2,
            List<CourseScheduleDTO> child1Schedules,
            List<CourseScheduleDTO> child2Schedules) {

        int count = 0;
        for (String courseId : allCourses) {
            if (count < crossoverPoint) {
                this.copyToChildren(
                        courseSchedules1,
                        courseSchedules2,
                        courseId,
                        child1Schedules,
                        child2Schedules
                );
            } else {
                this.copyToChildren(
                        courseSchedules2,
                        courseSchedules1,
                        courseId,
                        child1Schedules,
                        child2Schedules
                );
            }
            count++;
        }
    }

    /**
     * 复制到子代
     */
    private void copyToChildren(
            @NotNull Map<String, CourseScheduleDTO> sourceMap1,
            Map<String, CourseScheduleDTO> sourceMap2,
            String courseId,
            List<CourseScheduleDTO> child1Schedules,
            List<CourseScheduleDTO> child2Schedules) {
        if (sourceMap1.containsKey(courseId)) {
            child1Schedules.add(this.deepCopyCourseSchedule(sourceMap1.get(courseId)));
        }
        if (sourceMap2.containsKey(courseId)) {
            child2Schedules.add(this.deepCopyCourseSchedule(sourceMap2.get(courseId)));
        }
    }

    /**
     * 构建子代课程表
     */
    private @NotNull List<ScheduleDTO> buildChildrenSchedules(
            @NotNull SchedulePair children,
            AutomaticClassSchedulingBaseDTO baseDTO) {
        List<ScheduleDTO> result = new ArrayList<>();
        // 设置并计算子代1
        children.child1().setSchedule(children.child1Schedules());
        children.child1().setFitness(calculateFitness(children.child1(), baseDTO));
        // 设置并计算子代2
        children.child2().setSchedule(children.child2Schedules());
        children.child2().setFitness(calculateFitness(children.child2(), baseDTO));
        result.add(children.child1());
        result.add(children.child2());
        return result;
    }

    /**
     * 深拷贝一个课程安排对象
     * 此方法用于创建一个课程安排的深拷贝，包括其所有时间槽和排课项
     * 深拷贝意味着所有嵌套的对象都会被递归复制，而不是仅仅复制引用
     *
     * @param courseScheduleDTO 要深拷贝的课程安排对象，不能为空
     * @return 返回一个深拷贝的课程安排对象
     */
    private @NotNull CourseScheduleDTO deepCopyCourseSchedule(
            @NotNull CourseScheduleDTO courseScheduleDTO) {
        // 创建一个新的课程安排对象
        CourseScheduleDTO targetSchedule = new CourseScheduleDTO();
        // 初始化一个新的哈希映射来存储时间槽列表和排课项的映射
        Map<List<TimeSlotDTO>, CourseScheduleItemDTO> targetAssignments = new HashMap<>();
        // 复制时间槽列表和排课项
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : courseScheduleDTO.getAssignments().entrySet()) {
            // 复制时间槽列表
            List<TimeSlotDTO> timeSlotsCopy = entry.getKey().stream()
                    .map(slot -> new TimeSlotDTO(
                            slot.getWeek(),
                            slot.getDay(),
                            slot.getPeriod()
                    ))
                    .toList();
            // 复制排课项
            CourseScheduleItemDTO itemCopy = new CourseScheduleItemDTO(entry.getValue());
            // 将复制的时间槽列表和排课项放入新的哈希映射中
            targetAssignments.put(timeSlotsCopy, itemCopy);
        }
        // 设置复制的课程安排
        targetSchedule.setAssignments(targetAssignments);
        // 复制适应度值
        targetSchedule.setFitness(courseScheduleDTO.getFitness());
        // 返回深拷贝的课程安排对象
        return targetSchedule;
    }

    /**
     * 深拷贝一个ScheduleDTO对象
     * 该方法创建一个新对象，其中包含与原始对象相同的数据，但不共享任何引用
     * 这确保了修改新对象不会影响原始对象
     *
     * @param schedule 要深拷贝的ScheduleDTO对象，不能为null
     * @return 新创建的ScheduleDTO对象，其内容与输入参数相同
     */
    @NotNull
    private ScheduleDTO deepCopySchedule(@NotNull ScheduleDTO schedule) {
        // 创建一个新的ScheduleDTO对象
        ScheduleDTO copy = new ScheduleDTO();
        // 初始化一个新的课程安排列表
        List<CourseScheduleDTO> courseSchedules = new ArrayList<>();
        // 复制每个课程安排
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 创建一个新的CourseScheduleDTO对象
            CourseScheduleDTO courseScheduleCopy = new CourseScheduleDTO();
            // 初始化一个新的时间槽列表和排课项的映射
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = new HashMap<>();
            // 复制每个时间槽列表和排课项
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : courseSchedule.getAssignments().entrySet()) {
                // 复制时间槽列表
                List<TimeSlotDTO> timeSlotsCopy = entry.getKey().stream()
                        .map(slot -> new TimeSlotDTO(
                                slot.getWeek(),
                                slot.getDay(),
                                slot.getPeriod()
                        ))
                        .toList();
                // 复制排课项
                CourseScheduleItemDTO itemCopy = new CourseScheduleItemDTO(entry.getValue());
                // 将复制的对象添加到新的映射中
                assignments.put(timeSlotsCopy, itemCopy);
            }
            // 设置课程安排的属性
            courseScheduleCopy.setAssignments(assignments);
            courseScheduleCopy.setFitness(courseSchedule.getFitness());
            // 添加到课程安排列表
            courseSchedules.add(courseScheduleCopy);
        }
        // 设置课程表的属性
        copy.setSchedule(courseSchedules);
        copy.setFitness(schedule.getFitness());
        // 返回深拷贝后的对象
        return copy;
    }

    /**
     * 课程表配对记录类
     */
    private record SchedulePair(
            ScheduleDTO child1,
            ScheduleDTO child2,
            List<CourseScheduleDTO> child1Schedules,
            List<CourseScheduleDTO> child2Schedules
    ) {
    }
}
