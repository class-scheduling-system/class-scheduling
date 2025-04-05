package com.frontleaves.scheduling.utils;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排课适应度计算工具类
 */
public final class ScheduleFitnessCalculator {

    // 私有构造函数，防止实例化
    private ScheduleFitnessCalculator() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * 计算排课表的适应度值
     *
     * @param schedule 待评估的排课表
     * @param baseDTO 基础数据
     * @return 适应度值
     */
    public static double calculateFitness(ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        if (schedule == null || schedule.getSchedule() == null || schedule.getSchedule().isEmpty() || baseDTO == null) {
            return 0.0;
        }
        double totalFitness = 0.0;
        int courseCount = schedule.getSchedule().size();
        // 遍历每个课程计算适应度
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 基础分数
            double courseFitness = 100.0;
            // 减去冲突惩罚
            courseFitness -= calculateConflictPenalty(courseSchedule, baseDTO.getDataCourseScheduleList());
            // 连续课程适应度
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getConsecutiveCoursesPreferred())) {
                courseFitness += calculateConsecutiveCoursesFitness(courseSchedule);
            }
            // 时间偏好适应度
            courseFitness += calculateTimePreferenceFitness(courseSchedule, baseDTO.getTimePreferences());
            // 教室优化适应度
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getRoomOptimization())) {
                courseFitness += calculateRoomOptimizationFitness(courseSchedule);
            }
            // 确保单个课程的适应度不为负
            courseFitness = Math.max(0.0, courseFitness);
            // 将课程适应度添加到总适应度
            totalFitness += courseFitness;
            // 更新单个课程的适应度
            courseSchedule.setFitness(courseFitness);
        }
        // 计算平均适应度
        double averageFitness = totalFitness / courseCount;
        // 更新整个课程表的适应度
        schedule.setFitness(averageFitness);
        return averageFitness;
    }

    /**
     * 计算教室优化适应度
     */
    public static double calculateRoomOptimizationFitness(CourseScheduleDTO schedule) {
        if (schedule == null || schedule.getAssignments() == null) {
            return 0.0;
        }

        return schedule.getAssignments().entrySet().stream()
                .map(Map.Entry::getValue)
                .filter(Objects::nonNull)
                .mapToDouble(ScheduleFitnessCalculator::calculateItemRoomFitness)
                .sum();
    }

    /**
     * 计算单个课程项的教室适应度
     */
    private static double calculateItemRoomFitness(CourseScheduleItemDTO item) {
        // 验证对象有效性
        if (item == null || item.getClassroom() == null || item.getClassroom().getClassroom() == null
                || item.getCourse() == null) {
            return 0.0;
        }

        double fitness = 0.0;

        // 计算容量适应度
        fitness += calculateCapacityFitness(
                item.getClassroom().getClassroom().getCapacity(),
                getStudentCount(item.getCourse())
        );

        // 计算类型匹配适应度
        fitness += calculateTypeMatchingFitness(
                item.getCourse().getType(),
                item.getClassroom().getType().getClassTypeUuid()
        );

        return fitness;
    }

    /**
     * 获取学生数量
     */
    private static int getStudentCount(CourseLibraryDTO course) {
        return course.getTotalHours() != null ? course.getTotalHours().intValue() : 30;
    }

    /**
     * 计算容量适应度
     */
    private static double calculateCapacityFitness(int capacity, int studentCount) {
        // 容量不足
        if (capacity < studentCount) {
            return -50.0;
        }

        // 计算利用率
        double utilizationRate = (double) studentCount / capacity;

        // 利用率达到70%以上给予奖励
        return utilizationRate >= 0.7 ? 5.0 : 0.0;
    }

    /**
     * 计算类型匹配适应度
     */
    private static double calculateTypeMatchingFitness(String courseType, String classroomType) {
        if (classroomType == null) {
            return 0.0;
        }

        if (classroomType.equals(courseType)) {
            return 10.0;  // 类型匹配，奖励
        }

        return (courseType != null) ? -5.0 : 0.0;  // 类型不匹配且课程类型不为空，惩罚
    }

    /**
     * 计算时间偏好适应度值
     */
    public static double calculateTimePreferenceFitness(
            CourseScheduleDTO schedule,
            AutomaticClassSchedulingBaseDTO.TimePreferences preferences) {

        if (schedule == null || schedule.getAssignments() == null || preferences == null) {
            return 0.0;
        }

        return schedule.getAssignments().entrySet().stream()
                .map(Map.Entry::getKey)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .mapToDouble(slot -> calculateSlotPreferenceFitness(slot, preferences))
                .sum();
    }

    /**
     * 计算单个时间槽的偏好适应度
     */
    public static double calculateSlotPreferenceFitness(
            TimeSlotDTO slot,
            AutomaticClassSchedulingBaseDTO.TimePreferences preferences) {

        double fitness = 0.0;

        // 检查是否在偏好时间段
        boolean inPreferredSlot = preferences.getPreferredTimeSlots().stream()
                .anyMatch(preferred -> isSlotInPreferredTime(slot, preferred));

        if (inPreferredSlot) {
            fitness += 10.0;
        }

        // 如果不喜欢晚课但安排在晚上
        if (Boolean.TRUE.equals(preferences.getEveningCourses()) && slot.getPeriod() >= 5) {
            fitness -= 5.0;
        }

        return fitness;
    }

    /**
     * 检查时间槽是否在偏好时间范围内
     */
    public static boolean isSlotInPreferredTime(
            TimeSlotDTO slot,
            AutomaticClassSchedulingBaseDTO.TimePreferences.PreferredTimeSlot preferred) {
        if (slot == null || preferred == null) {
            return false;
        }
        return Objects.equals(preferred.getDay(), slot.getDay()) &&
                preferred.getPeriodStart() <= slot.getPeriod() &&
                preferred.getPeriodEnd() >= slot.getPeriod();
    }

    /**
     * 计算连续课程的适应度值
     */
    public static double calculateConsecutiveCoursesFitness(CourseScheduleDTO schedule) {
        if (schedule == null || schedule.getAssignments() == null) {
            return 0.0;
        }

        // 按课程分组，获取每个课程的所有时间槽
        Map<String, List<List<TimeSlotDTO>>> courseSlots = groupSlotsByCourse(schedule);
        return courseSlots.values().stream()
                .mapToDouble(ScheduleFitnessCalculator::calculateCourseFitness)
                .sum();
    }

    /**
     * 按课程分组获取时间槽
     */
    public static Map<String, List<List<TimeSlotDTO>>> groupSlotsByCourse(CourseScheduleDTO schedule) {
        if (schedule == null || schedule.getAssignments() == null || schedule.getAssignments().isEmpty()) {
            return Collections.emptyMap();
        }

        return schedule.getAssignments().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().getCourse() != null)
                .filter(e -> e.getValue().getCourse().getCourseLibraryUuid() != null)
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue().getCourse().getCourseLibraryUuid(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
    }

    /**
     * 计算单个课程的适应度值
     */
    public static double calculateCourseFitness(List<List<TimeSlotDTO>> courseSlotsLists) {
        if (courseSlotsLists == null || courseSlotsLists.isEmpty()) {
            return 0.0;
        }

        // 展平所有时间槽
        List<TimeSlotDTO> allSlots = courseSlotsLists.stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .toList();

        if (allSlots.isEmpty()) {
            return 0.0;
        }

        // 按周和天分组
        Map<Integer, Map<Integer, List<TimeSlotDTO>>> weekDaySlots = groupSlotsByWeekAndDay(allSlots);

        // 计算每周的适应度值
        return weekDaySlots.values().stream()
                .mapToDouble(ScheduleFitnessCalculator::calculateWeekFitness)
                .sum();
    }

    /**
     * 按周和天分组时间槽
     */
    public static Map<Integer, Map<Integer, List<TimeSlotDTO>>> groupSlotsByWeekAndDay(List<TimeSlotDTO> slots) {
        if (slots == null || slots.isEmpty()) {
            return Collections.emptyMap();
        }

        return slots.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(
                        TimeSlotDTO::getWeek,
                        Collectors.groupingBy(TimeSlotDTO::getDay)
                ));
    }

    /**
     * 计算一周内的适应度值
     */
    public static double calculateWeekFitness(Map<Integer, List<TimeSlotDTO>> daySlots) {
        if (daySlots == null || daySlots.isEmpty()) {
            return 0.0;
        }

        return daySlots.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(ScheduleFitnessCalculator::calculateDayFitness)
                .sum();
    }

    /**
     * 计算一天内的适应度值
     */
    public static double calculateDayFitness(List<TimeSlotDTO> dailySlots) {
        if (dailySlots == null || dailySlots.size() <= 1) {
            return 0.0;
        }

        List<TimeSlotDTO> sortedSlots = dailySlots.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(TimeSlotDTO::getPeriod))
                .toList();

        return calculateConsecutiveSlotsFitness(sortedSlots);
    }

    /**
     * 计算连续时间槽的适应度值
     */
    public static double calculateConsecutiveSlotsFitness(List<TimeSlotDTO> sortedSlots) {
        if (sortedSlots == null || sortedSlots.size() <= 1) {
            return 0.0;
        }

        double fitness = 0.0;
        int consecutiveCount = 1;

        // 遍历计算连续课程
        for (int i = 0; i < sortedSlots.size() - 1; i++) {
            if (isConsecutiveSlots(sortedSlots.get(i), sortedSlots.get(i + 1))) {
                consecutiveCount++;
            } else {
                fitness += calculateConsecutiveBonus(consecutiveCount);
                consecutiveCount = 1;
            }
        }

        // 处理最后一组连续课程
        fitness += calculateConsecutiveBonus(consecutiveCount);

        return fitness;
    }

    /**
     * 检查两个时间槽是否连续
     */
    public static boolean isConsecutiveSlots(TimeSlotDTO slot1, TimeSlotDTO slot2) {
        if (slot1 == null || slot2 == null) {
            return false;
        }
        return slot2.getPeriod() - slot1.getPeriod() == 1;
    }

    /**
     * 计算连续课程的奖励分数
     */
    public static double calculateConsecutiveBonus(int consecutiveCount) {
        return consecutiveCount > 1 ? (consecutiveCount - 1) * 5.0 : 0.0;
    }

    /**
     * 计算课程安排的冲突罚分
     */
    public static double calculateConflictPenalty(
            CourseScheduleDTO schedule,
            List<CourseScheduleDTO> dataCourseScheduleList) {

        if (schedule == null || schedule.getAssignments() == null) {
            return 0.0;
        }

        double totalPenalty = 0.0;

        // 获取当前课程表的所有安排
        var currentEntries = schedule.getAssignments().entrySet().stream().toList();

        // 1. 检查当前课程表内部的冲突
        totalPenalty += calculateInternalConflicts(currentEntries);

        // 2. 检查与已排课程的冲突
        totalPenalty += calculateExistingScheduleConflicts(currentEntries, dataCourseScheduleList);

        return totalPenalty;
    }

    /**
     * 计算当前课程表内部的冲突
     */
    public static double calculateInternalConflicts(
            List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries) {

        if (entries == null || entries.isEmpty()) {
            return 0.0;
        }

        double penalty = 0.0;

        for (int i = 0; i < entries.size(); i++) {
            var entry1 = entries.get(i);
            for (int j = i + 1; j < entries.size(); j++) {
                var entry2 = entries.get(j);
                penalty += checkTimeSlotConflicts(
                        entry1.getKey(),
                        entry2.getKey(),
                        entry1.getValue(),
                        entry2.getValue()
                );
            }
        }

        return penalty;
    }

    /**
     * 计算与已排课程的冲突
     */
    public static double calculateExistingScheduleConflicts(
            List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> currentEntries,
            List<CourseScheduleDTO> dataCourseScheduleList) {

        double penalty = 0.0;

        if (currentEntries == null || dataCourseScheduleList == null ||
                currentEntries.isEmpty() || dataCourseScheduleList.isEmpty()) {
            return penalty;
        }

        // 遍历所有已排课程表
        for (CourseScheduleDTO existingSchedule : dataCourseScheduleList) {
            if (existingSchedule == null || existingSchedule.getAssignments() == null) {
                continue;
            }

            var existingEntries = existingSchedule.getAssignments().entrySet();

            // 检查当前课程与已排课程的冲突
            for (var currentEntry : currentEntries) {
                for (var existingEntry : existingEntries) {
                    penalty += checkTimeSlotConflicts(
                            currentEntry.getKey(),
                            existingEntry.getKey(),
                            currentEntry.getValue(),
                            existingEntry.getValue()
                    );
                }
            }
        }

        return penalty;
    }

    /**
     * 检查时间槽冲突并计算惩罚值
     */
    public static double checkTimeSlotConflicts(
            List<TimeSlotDTO> slots1,
            List<TimeSlotDTO> slots2,
            CourseScheduleItemDTO item1,
            CourseScheduleItemDTO item2) {

        if (slots1 == null || slots2 == null || item1 == null || item2 == null) {
            return 0.0;
        }

        // 如果没有时间重叠，直接返回0
        if (!hasTimeOverlap(slots1, slots2)) {
            return 0.0;
        }

        return calculateResourceConflicts(item1, item2);
    }

    /**
     * 检查是否存在时间重叠
     */
    public static boolean hasTimeOverlap(List<TimeSlotDTO> slots1, List<TimeSlotDTO> slots2) {
        if (slots1 == null || slots2 == null || slots1.isEmpty() || slots2.isEmpty()) {
            return false;
        }

        return slots1.stream()
                .filter(Objects::nonNull)
                .anyMatch(slot1 -> slots2.stream()
                        .filter(Objects::nonNull)
                        .anyMatch(slot2 -> isSameTimeSlot(slot1, slot2)));
    }

    /**
     * 检查是否为同一时间槽
     */
    public static boolean isSameTimeSlot(TimeSlotDTO slot1, TimeSlotDTO slot2) {
        if (slot1 == null || slot2 == null) {
            return false;
        }
        return Objects.equals(slot1.getWeek(), slot2.getWeek())
                && Objects.equals(slot1.getDay(), slot2.getDay())
                && Objects.equals(slot1.getPeriod(), slot2.getPeriod());
    }

    /**
     * 计算资源冲突的惩罚值
     */
    public static double calculateResourceConflicts(
            CourseScheduleItemDTO item1,
            CourseScheduleItemDTO item2) {

        if (item1 == null || item2 == null) {
            return 0.0;
        }

        // 使用Stream API检查所有冲突
        List<Boolean> conflicts = Arrays.asList(
                isTeacherConflict(item1, item2),
                isClassroomConflict(item1, item2),
                isClassConflict(item1, item2)
        );

        // 计算总惩罚值
        return conflicts.stream()
                .filter(conflict -> conflict)
                .count() * 100.0;
    }

    /**
     * 检查教师冲突
     */
    public static boolean isTeacherConflict(
            CourseScheduleItemDTO item1,
            CourseScheduleItemDTO item2) {

        if (item1 == null || item2 == null ||
                item1.getTeacher() == null || item2.getTeacher() == null ||
                item1.getTeacher().getTeacher() == null || item2.getTeacher().getTeacher() == null) {
            return false;
        }

        String teacher1Uuid = item1.getTeacher().getTeacher().getTeacherUuid();
        String teacher2Uuid = item2.getTeacher().getTeacher().getTeacherUuid();
        return teacher1Uuid != null && teacher1Uuid.equals(teacher2Uuid);
    }

    /**
     * 检查教室冲突
     */
    public static boolean isClassroomConflict(
            CourseScheduleItemDTO item1,
            CourseScheduleItemDTO item2) {

        if (item1 == null || item2 == null ||
                item1.getClassroom() == null || item2.getClassroom() == null ||
                item1.getClassroom().getClassroom() == null || item2.getClassroom().getClassroom() == null) {
            return false;
        }

        String room1Uuid = item1.getClassroom().getClassroom().getClassroomUuid();
        String room2Uuid = item2.getClassroom().getClassroom().getClassroomUuid();
        return room1Uuid != null && room1Uuid.equals(room2Uuid);
    }

    /**
     * 检查行政班级冲突
     */
    public static boolean isClassConflict(
            CourseScheduleItemDTO item1,
            CourseScheduleItemDTO item2) {

        if (item1 == null || item2 == null ||
                item1.getClassGroup() == null || item2.getClassGroup() == null) {
            return false;
        }

        // 获取两个课程的行政班级列表
        List<AdministrativeClassDTO> classList1 = item1.getClassGroup();
        List<AdministrativeClassDTO> classList2 = item2.getClassGroup();

        // 检查是否有重叠的班级
        return classList1.stream()
                .filter(Objects::nonNull)
                .anyMatch(class1 -> class1.getAdministrativeClassUuid() != null &&
                        classList2.stream()
                                .filter(Objects::nonNull)
                                .anyMatch(class2 -> class2.getAdministrativeClassUuid() != null &&
                                        class1.getAdministrativeClassUuid().equals(class2.getAdministrativeClassUuid())));
    }
}