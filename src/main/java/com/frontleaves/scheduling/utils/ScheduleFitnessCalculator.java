package com.frontleaves.scheduling.utils;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排课适应度计算工具类
 *
 * @author FLASHLACK
 */
@Slf4j
public final class ScheduleFitnessCalculator {

    // 私有构造函数，防止实例化
    private ScheduleFitnessCalculator() {

    }

    public static double calculateFitness(@NotNull ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        if (schedule.getSchedule() == null || schedule.getSchedule().isEmpty()) {
            return 0.0;
        }
        double totalFitness = 0.0;
        int courseCount = schedule.getSchedule().size();

        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            double courseFitness = 0;
            // 冲突惩罚
            courseFitness -= ScheduleFitnessCalculator.calculateConflictPenalty(courseSchedule, baseDTO.getDataCourseScheduleList());
            // 连续课程
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getConsecutiveCoursesPreferred())) {
                courseFitness += ScheduleFitnessCalculator.calculateConsecutiveCoursesFitness(courseSchedule);
            }
            // 时间偏好
            courseFitness += ScheduleFitnessCalculator.calculateTimePreferenceFitness(courseSchedule, baseDTO.getTimePreferences());
            // 教室优化
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getRoomOptimization())) {
                courseFitness += ScheduleFitnessCalculator.calculateRoomOptimizationFitness(courseSchedule);
            }
            // 非负限制
            courseFitness = Math.max(0.0, courseFitness);
            totalFitness += courseFitness;
            courseSchedule.setFitness(courseFitness);
        }
        // 平均适应度
        double averageFitness = totalFitness / courseCount;
        // === 等比缩放 ===
        double maxExpectedFitness = 600.0;
        double scaledFitness = (averageFitness / maxExpectedFitness) * 100.0;
        scaledFitness = Math.min(scaledFitness, 100.0);

        schedule.setFitness(scaledFitness);
        return scaledFitness;
    }



    /**
     * 计算教室优化适应度
     * <p>
     * 评估教室分配的合理性，包括容量匹配度和教室类型匹配度。
     * 理想情况下，教室容量应略大于学生数量，教室类型应符合课程需求。
     * </p>
     *
     * @param schedule 待评估的课程表
     * @return 教室优化适应度得分
     */
    public static double calculateRoomOptimizationFitness(@NotNull CourseScheduleDTO schedule) {
        double fitness = 0.0;
        int itemCount = 0;

        // 遍历所有课程安排
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            CourseScheduleItemDTO item = entry.getValue();
            itemCount++;

            // 获取教室容量
            int capacity = item.getClassroom().getClassroom().getCapacity();
            // 获取课程所需学生数，使用totalHours作为替代指标
            int studentCount = item.getCourse().getTotalHours() != null ?
                    item.getCourse().getTotalHours().intValue() : 30;

            // 计算容量匹配度
            if (capacity >= studentCount) {
                // 容量足够，计算利用率
                double utilizationRate = (double) studentCount / capacity;
                // 利用率达到70%以上给予奖励
                if (utilizationRate >= 0.7) {
                    fitness += 5.0;
                }
                // 利用率超过90%给予额外奖励
                if (utilizationRate > 0.9) {
                    fitness -= 2.0;
                }
            } else {
                // 容量不足，严重惩罚
                fitness -= 50.0;
            }

            // 教室类型匹配度
            String courseType = item.getCourse().getType();
            String classroomType = item.getClassroom().getType().getClassTypeUuid();
            if (classroomType != null && classroomType.equals(courseType)) {
                fitness += 10.0;
            } else if (classroomType != null && courseType != null) {
                // 类型不匹配，给予惩罚
                fitness -= 5.0;
            }
        }

        return itemCount > 0 ? fitness / itemCount : 0.0;
    }


    /**
     * 计算时间偏好适应度值
     *
     * @param schedule    课程表
     * @param preferences 时间偏好设置
     * @return 时间偏好适应度值
     */
    public static double calculateTimePreferenceFitness(
            @NotNull CourseScheduleDTO schedule,
            AutomaticClassSchedulingBaseDTO.TimePreferences preferences
    ) {
        double fitness = 0.0;
        int slotCount = 0;

        // 检查 preferences 是否为空或其 preferredTimeSlots 是否为空
        if (preferences == null || preferences.getPreferredTimeSlots() == null || preferences.getPreferredTimeSlots().isEmpty()) {
            // 如果为空，直接返回 0.0
            return 0.0;
        }
        // 遍历所有课程安排
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            List<TimeSlotDTO> slots = entry.getKey();
            // 遍历每个时间槽
            for (TimeSlotDTO slot : slots) {
                slotCount++;
                // 检查是否在偏好时间段
                boolean inPreferredSlot = preferences.getPreferredTimeSlots().stream()
                        .anyMatch(preferred ->
                                Objects.equals(preferred.getDay(), slot.getDay()) &&
                                        preferred.getPeriodStart() <= slot.getPeriod() &&
                                        preferred.getPeriodEnd() >= slot.getPeriod());
                if (inPreferredSlot) {
                    fitness += 10.0;
                }
                // 如果不喜欢晚课但安排在晚上
                if (Boolean.TRUE.equals(preferences.getEveningCourses()) && slot.getPeriod() >= 5) {
                    fitness -= 5.0;
                }
            }
        }
        // 避免除以 0
        return slotCount > 0 ? fitness / slotCount : 0.0;
    }


    /**
     * 计算连续课程的适应度值
     */
    public static double calculateConsecutiveCoursesFitness(@NotNull CourseScheduleDTO schedule) {
        // 按课程分组，获取每个课程的所有时间槽
        Map<String, List<List<TimeSlotDTO>>> courseSlots = groupSlotsByCourse(schedule);
        // 总课程数
        int courseCount = courseSlots.size();
        // 所有课程的总适应度
        double totalFitness = courseSlots.values().stream()
                .mapToDouble(ScheduleFitnessCalculator::calculateCourseFitness)
                .sum();
        // 计算平均值
        return courseCount > 0 ? totalFitness / courseCount : 0.0;
    }


    /**
     * 按课程分组获取时间槽
     */
    public static Map<String, List<List<TimeSlotDTO>>> groupSlotsByCourse(@NotNull CourseScheduleDTO schedule) {
        return schedule.getAssignments().entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue().getCourse().getCourseLibraryUuid(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
    }

    /**
     * 计算单个课程的适应度值
     */
    public static double calculateCourseFitness(@NotNull List<List<TimeSlotDTO>> courseSlotsLists) {
        // 展平所有时间槽
        List<TimeSlotDTO> allSlots = courseSlotsLists.stream()
                .flatMap(List::stream)
                .toList();
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
    public static Map<Integer, Map<Integer, List<TimeSlotDTO>>> groupSlotsByWeekAndDay(@NotNull List<TimeSlotDTO> slots) {
        return slots.stream()
                .collect(Collectors.groupingBy(
                        TimeSlotDTO::getWeek,
                        Collectors.groupingBy(TimeSlotDTO::getDay)
                ));
    }

    /**
     * 计算一周内的适应度值
     */
    public static double calculateWeekFitness(@NotNull Map<Integer, List<TimeSlotDTO>> daySlots) {
        return daySlots.values().stream()
                .mapToDouble(ScheduleFitnessCalculator::calculateDayFitness)
                .sum();
    }

    /**
     * 计算一天内的适应度值
     */
    public static double calculateDayFitness(@NotNull List<TimeSlotDTO> dailySlots) {
        if (dailySlots.size() <= 1) {
            return 0.0;
        }

        List<TimeSlotDTO> sortedSlots = dailySlots.stream()
                .sorted(Comparator.comparingInt(TimeSlotDTO::getPeriod))
                .toList();

        return calculateConsecutiveSlotsFitness(sortedSlots);
    }

    /**
     * 计算连续时间槽的适应度值
     */
    public static double calculateConsecutiveSlotsFitness(@NotNull List<TimeSlotDTO> sortedSlots) {
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
    public static boolean isConsecutiveSlots(@NotNull TimeSlotDTO slot1, @NotNull TimeSlotDTO slot2) {
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
     * 该方法旨在评估给定课程安排与其他安排之间的冲突程度，通过计算冲突罚分来实现
     * 冲突包括课程安排内部的冲突以及与已有课程安排的冲突罚分越高，表示冲突越严重
     *
     * @param schedule               当前课程安排，用于计算冲突罚分
     * @param dataCourseScheduleList 所有已有的课程安排列表，用于检查与当前安排的冲突情况
     * @return 返回总冲突罚分，作为评估冲突程度的指标
     */
    public static double calculateConflictPenalty(
            @NotNull CourseScheduleDTO schedule,
            List<CourseScheduleDTO> dataCourseScheduleList) {
        double totalPenalty = 0.0;
        // 获取当前课程表的所有安排
        var currentEntries = schedule.getAssignments().entrySet().stream().toList();
        totalPenalty += calculateExistingScheduleConflicts(currentEntries, dataCourseScheduleList);
        return totalPenalty;
    }



    /**
     * 计算与已排课程的冲突
     */
    public static double calculateExistingScheduleConflicts(
            List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> currentEntries,
            List<CourseScheduleDTO> dataCourseScheduleList) {
        double penalty = 0.0;
        if (dataCourseScheduleList == null || dataCourseScheduleList.isEmpty()) {
            return penalty;
        }
        // 遍历所有已排课程表
        for (CourseScheduleDTO existingSchedule : dataCourseScheduleList) {
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
            @NotNull List<TimeSlotDTO> slots1,
            @NotNull List<TimeSlotDTO> slots2,
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        // 如果没有时间重叠，直接返回0
        if (!hasTimeOverlap(slots1, slots2)) {
            return 0.0;
        }
        return calculateResourceConflicts(item1, item2);
    }

    /**
     * 检查是否存在时间重叠
     */
    public static boolean hasTimeOverlap(
            @NotNull List<TimeSlotDTO> slots1,
            @NotNull List<TimeSlotDTO> slots2) {
        return slots1.stream()
                .anyMatch(slot1 -> slots2.stream()
                        .anyMatch(slot2 -> isSameTimeSlot(slot1, slot2)));
    }

    /**
     * 计算资源冲突的惩罚值
     */
    public static double calculateResourceConflicts(
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        double penalty = 0.0;
        // 检查教师冲突
        boolean teacherConflict = isTeacherConflict(item1, item2);
        if (teacherConflict) {
            log.info("发现教师冲突 - 教师: {}, 课程1: {}, 课程2: {}",
                    item1.getTeacher().getTeacher().getName(),
                    item1.getCourse().getName(),
                    item2.getCourse().getName());
            penalty += 100.0;
        }
        // 检查教室冲突
        boolean classroomConflict = isClassroomConflict(item1, item2);
        if (classroomConflict) {
            log.info("发现教室冲突 - 教室: {}, 课程1: {}, 课程2: {}",
                    item1.getClassroom().getClassroom().getName(),
                    item1.getCourse().getName(),
                    item2.getCourse().getName());
            penalty += 100.0;
        }
        // 检查班级冲突
        boolean classConflict = isClassConflict(item1, item2);
        if (classConflict) {
            // 获取冲突的班级名称
            List<String> conflictingClasses = getConflictingClassNames(item1, item2);
            log.info("发现班级冲突 - 冲突班级: {}, 课程1: {}, 课程2: {}",
                    String.join("、", conflictingClasses),
                    item1.getCourse().getName(),
                    item2.getCourse().getName());
            penalty += 100.0;
        }
        return penalty;
    }

    /**
     * 获取冲突的班级名称列表
     */
    private static @NotNull List<String> getConflictingClassNames(
            @NotNull CourseScheduleItemDTO item1,
            CourseScheduleItemDTO item2) {
        List<String> conflictingClasses = new ArrayList<>();

        if (item1.getClassGroup() == null || item2.getClassGroup() == null) {
            return conflictingClasses;
        }

        for (AdministrativeClassDTO class1 : item1.getClassGroup()) {
            for (AdministrativeClassDTO class2 : item2.getClassGroup()) {
                if (class1.getAdministrativeClassUuid()
                        .equals(class2.getAdministrativeClassUuid())) {
                    conflictingClasses.add(class1.getClassName());
                }
            }
        }

        return conflictingClasses;
    }

    /**
     * 检查是否为同一时间槽
     */
    public static boolean isSameTimeSlot(@NotNull TimeSlotDTO slot1, @NotNull TimeSlotDTO slot2) {
        return Objects.equals(slot1.getWeek(), slot2.getWeek())
                && Objects.equals(slot1.getDay(), slot2.getDay())
                && Objects.equals(slot1.getPeriod(), slot2.getPeriod());
    }

    /**
     * 检查教师冲突
     */
    public static boolean isTeacherConflict(
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        String teacher1Uuid = item1.getTeacher().getTeacher().getTeacherUuid();
        String teacher2Uuid = item2.getTeacher().getTeacher().getTeacherUuid();
        return teacher1Uuid.equals(teacher2Uuid);
    }

    /**
     * 检查教室冲突
     */
    public static boolean isClassroomConflict(
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        String room1Uuid = item1.getClassroom().getClassroom().getClassroomUuid();
        String room2Uuid = item2.getClassroom().getClassroom().getClassroomUuid();
        return room1Uuid.equals(room2Uuid);
    }

    /**
     * 检查行政班级冲突
     * 如果任一课程没有行政班级信息，则认为不冲突，返回false
     */
    public static boolean isClassConflict(
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        // 获取两个课程的行政班级列表
        List<AdministrativeClassDTO> classList1 = item1.getClassGroup();
        List<AdministrativeClassDTO> classList2 = item2.getClassGroup();
        // 如果任一班级列表为空，则认为不冲突
        if (classList1 == null || classList2 == null ||
                classList1.isEmpty() || classList2.isEmpty()) {
            return false;
        }
        // 检查是否有重叠的班级
        return classList1.stream()
                .anyMatch(class1 -> classList2.stream()
                        .anyMatch(class2 -> class1.getAdministrativeClassUuid()
                                .equals(class2.getAdministrativeClassUuid())));
    }
}