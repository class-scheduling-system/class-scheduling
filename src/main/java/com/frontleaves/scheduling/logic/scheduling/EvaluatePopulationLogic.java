package com.frontleaves.scheduling.logic.scheduling;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.services.scheduling.EvaluatePopulationService;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 遗传算法评估种群逻辑实现类
 * @author FLASHLACK
 */
@Service
@Slf4j
public class EvaluatePopulationLogic implements EvaluatePopulationService {

    @Override
    public void evaluatePopulation(@NotNull List<ScheduleDTO> allPopulation, AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO population : allPopulation) {
            double fitness = calculateFitness(population, baseDTO);
            population.setFitness(fitness);
        }
    }

    public  double calculateFitness(@NotNull ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        if (schedule.getSchedule() == null || schedule.getSchedule().isEmpty()) {
            return 0.0;
        }
        double totalFitness = 0.0;
        int courseCount = schedule.getSchedule().size();
        // 遍历每个课程计算适应度
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 基础分数
            double courseFitness = 100.0;
            // 减去冲突惩罚
            courseFitness -= this.calculateConflictPenalty(courseSchedule,baseDTO.getDataCourseScheduleList());
            // 连续课程适应度
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getConsecutiveCoursesPreferred())) {
                courseFitness += this.calculateConsecutiveCoursesFitness(courseSchedule);
            }
            // 时间偏好适应度
            courseFitness += this.calculateTimePreferenceFitness(courseSchedule, baseDTO.getTimePreferences());
            // 教室优化适应度
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getRoomOptimization())) {
                courseFitness += this.calculateRoomOptimizationFitness(courseSchedule);
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
     * <p>
     * 评估教室分配的合理性，包括容量匹配度和教室类型匹配度。
     * 理想情况下，教室容量应略大于学生数量，教室类型应符合课程需求。
     * </p>
     *
     * @param schedule 待评估的课程表
     * @return 教室优化适应度得分
     */
    private double calculateRoomOptimizationFitness(@NotNull CourseScheduleDTO schedule) {
        double fitness = 0.0;
        // 遍历所有课程安排
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            CourseScheduleItemDTO item = entry.getValue();
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
                // 利用率过高（超过90%）给予惩罚
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
        return fitness;
    }

    /**
     * 计算时间偏好适应度值
     * @param schedule    课程表
     * @param preferences 时间偏好设置
     * @return 时间偏好适应度值
     */
    private double calculateTimePreferenceFitness(
            @NotNull CourseScheduleDTO schedule,
            AutomaticClassSchedulingBaseDTO.TimePreferences preferences
    ) {
        double fitness = 0.0;
        // 遍历所有课程安排
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            List<TimeSlotDTO> slots = entry.getKey();
            // 遍历每个时间槽
            for (TimeSlotDTO slot : slots) {
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
        return fitness;
    }

    /**
     * 计算连续课程的适应度值
     */
    private double calculateConsecutiveCoursesFitness(@NotNull CourseScheduleDTO schedule) {
        // 按课程分组，获取每个课程的所有时间槽
        Map<String, List<List<TimeSlotDTO>>> courseSlots = this.groupSlotsByCourse(schedule);
        return courseSlots.values().stream()
                .mapToDouble(this::calculateCourseFitness)
                .sum();
    }
    /**
     * 按课程分组获取时间槽
     */
    private Map<String, List<List<TimeSlotDTO>>> groupSlotsByCourse(@NotNull CourseScheduleDTO schedule) {
        return schedule.getAssignments().entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue().getCourse().getCourseLibraryUuid(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));
    }
    /**
     * 计算单个课程的适应度值
     */
    private double calculateCourseFitness(@NotNull List<List<TimeSlotDTO>> courseSlotsLists) {
        // 展平所有时间槽
        List<TimeSlotDTO> allSlots = courseSlotsLists.stream()
                .flatMap(List::stream)
                .toList();

        // 按周和天分组
        Map<Integer, Map<Integer, List<TimeSlotDTO>>> weekDaySlots = this.groupSlotsByWeekAndDay(allSlots);

        // 计算每周的适应度值
        return weekDaySlots.values().stream()
                .mapToDouble(this::calculateWeekFitness)
                .sum();
    }

    /**
     * 按周和天分组时间槽
     */
    private Map<Integer, Map<Integer, List<TimeSlotDTO>>> groupSlotsByWeekAndDay(@NotNull List<TimeSlotDTO> slots) {
        return slots.stream()
                .collect(Collectors.groupingBy(
                        TimeSlotDTO::getWeek,
                        Collectors.groupingBy(TimeSlotDTO::getDay)
                ));
    }

    /**
     * 计算一周内的适应度值
     */
    private double calculateWeekFitness(@NotNull Map<Integer, List<TimeSlotDTO>> daySlots) {
        return daySlots.values().stream()
                .mapToDouble(this::calculateDayFitness)
                .sum();
    }

    /**
     * 计算一天内的适应度值
     */
    private double calculateDayFitness(@NotNull List<TimeSlotDTO> dailySlots) {
        if (dailySlots.size() <= 1) {
            return 0.0;
        }

        List<TimeSlotDTO> sortedSlots = dailySlots.stream()
                .sorted(Comparator.comparingInt(TimeSlotDTO::getPeriod))
                .toList();

        return this.calculateConsecutiveSlotsFitness(sortedSlots);
    }
    /**
     * 计算连续时间槽的适应度值
     */
    private double calculateConsecutiveSlotsFitness(@NotNull List<TimeSlotDTO> sortedSlots) {
        double fitness = 0.0;
        int consecutiveCount = 1;
        // 遍历计算连续课程
        for (int i = 0; i < sortedSlots.size() - 1; i++) {
            if (this.isConsecutiveSlots(sortedSlots.get(i), sortedSlots.get(i + 1))) {
                consecutiveCount++;
            } else {
                fitness += this.calculateConsecutiveBonus(consecutiveCount);
                consecutiveCount = 1;
            }
        }

        // 处理最后一组连续课程
        fitness += this.calculateConsecutiveBonus(consecutiveCount);

        return fitness;
    }
    /**
     * 检查两个时间槽是否连续
     */
    private boolean isConsecutiveSlots(@NotNull TimeSlotDTO slot1, @NotNull TimeSlotDTO slot2) {
        return slot2.getPeriod() - slot1.getPeriod() == 1;
    }
    /**
     * 计算连续课程的奖励分数
     */
    private double calculateConsecutiveBonus(int consecutiveCount) {
        return consecutiveCount > 1 ? (consecutiveCount - 1) * 5.0 : 0.0;
    }


    /**
     * 计算课程安排的冲突罚分
     * 该方法旨在评估给定课程安排与其他安排之间的冲突程度，通过计算冲突罚分来实现
     * 冲突包括课程安排内部的冲突以及与已有课程安排的冲突罚分越高，表示冲突越严重
     * @param schedule            当前课程安排，用于计算冲突罚分
     * @param dataCourseScheduleList 所有已有的课程安排列表，用于检查与当前安排的冲突情况
     * @return 返回总冲突罚分，作为评估冲突程度的指标
     */
    private double calculateConflictPenalty(
            @NotNull CourseScheduleDTO schedule,
            List<CourseScheduleDTO> dataCourseScheduleList) {
        double totalPenalty = 0.0;
        // 获取当前课程表的所有安排
        var currentEntries = schedule.getAssignments().entrySet().stream().toList();
        // 1. 检查当前课程表内部的冲突
        totalPenalty += this.calculateInternalConflicts(currentEntries);
        // 2. 检查与已排课程的冲突
        totalPenalty += this.calculateExistingScheduleConflicts(currentEntries, dataCourseScheduleList);
        return totalPenalty;
    }
    /**
     * 计算当前课程表内部的冲突
     */
    private double calculateInternalConflicts(
            @NotNull List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries) {
        double penalty = 0.0;
        for (int i = 0; i < entries.size(); i++) {
            var entry1 = entries.get(i);
            for (int j = i + 1; j < entries.size(); j++) {
                var entry2 = entries.get(j);
                penalty += this.checkTimeSlotConflicts(
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
    private double calculateExistingScheduleConflicts(
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
                    penalty += this.checkTimeSlotConflicts(
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
    private double checkTimeSlotConflicts(
            @NotNull List<TimeSlotDTO> slots1,
            @NotNull List<TimeSlotDTO> slots2,
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        // 如果没有时间重叠，直接返回0
        if (!this.hasTimeOverlap(slots1, slots2)) {
            return 0.0;
        }
        return this.calculateResourceConflicts(item1, item2);
    }
    /**
     * 检查是否存在时间重叠
     */
    private boolean hasTimeOverlap(
            @NotNull List<TimeSlotDTO> slots1,
            @NotNull List<TimeSlotDTO> slots2) {
        return slots1.stream()
                .anyMatch(slot1 -> slots2.stream()
                        .anyMatch(slot2 -> this.isSameTimeSlot(slot1, slot2)));
    }
    /**
     * 计算资源冲突的惩罚值
     */
    private double calculateResourceConflicts(
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        // 使用Stream API检查所有冲突
        List<Boolean> conflicts = Arrays.asList(
                this.isTeacherConflict(item1, item2),
                this.isClassroomConflict(item1, item2),
                this.isClassConflict(item1, item2)
        );
        // 计算总惩罚值
        return conflicts.stream()
                .filter(conflict -> conflict)
                .count() * 100.0;
    }
    /**
     * 检查是否为同一时间槽
     */
    private boolean isSameTimeSlot(@NotNull TimeSlotDTO slot1, @NotNull TimeSlotDTO slot2) {
        return Objects.equals(slot1.getWeek(), slot2.getWeek())
                && Objects.equals(slot1.getDay(), slot2.getDay())
                && Objects.equals(slot1.getPeriod(), slot2.getPeriod());
    }
    /**
     * 检查教师冲突
     */
    private boolean isTeacherConflict(
            @NotNull CourseScheduleItemDTO item1,
            @NotNull CourseScheduleItemDTO item2) {
        String teacher1Uuid = item1.getTeacher().getTeacher().getTeacherUuid();
        String teacher2Uuid = item2.getTeacher().getTeacher().getTeacherUuid();
        return teacher1Uuid.equals(teacher2Uuid);
    }
    /**
     * 检查教室冲突
     */
    private boolean isClassroomConflict(
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
    private boolean isClassConflict(
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
