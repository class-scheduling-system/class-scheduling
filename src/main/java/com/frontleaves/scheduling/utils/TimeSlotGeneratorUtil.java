package com.frontleaves.scheduling.utils;

import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.TimeSlotDTO;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 时间槽生成工具类
 */
public class TimeSlotGeneratorUtil {

    /**
     * 时间槽生成上下文
     */
    private record TimeSlotContext(
            int maxPeriodsPerDay,
            int targetTotalHours,
            int startWeek,
            int endWeek,
            int totalScheduledHours
    ) {}

    /**
     * 生成课程的时间槽
     *
     * @param course 课程信息
     * @param eveningCoursesEnabled 是否开启晚课
     * @return 生成的时间槽列表
     */
    public static @NotNull List<TimeSlotDTO> generateTimeSlots(
            CourseLibraryAndTeacherCourseQualificationListDTO course,
            boolean eveningCoursesEnabled) {

        // 1. 初始化上下文
        TimeSlotContext context = initializeContext(course, eveningCoursesEnabled);

        // 2. 生成每天可用的时间块
        Map<Integer, List<List<Integer>>> availableBlocks = generateAvailableBlocks(context.maxPeriodsPerDay());

        // 3. 确定课时分配
        List<Integer> distribution = determineDistribution(course.getWeeklyHours());

        // 4. 分配时间块到具体天数
        Map<Integer, List<Integer>> dayTimeSlots = assignTimeBlocksToDays(distribution, availableBlocks);

        // 5. 生成最终时间槽
        return generateFinalTimeSlots(dayTimeSlots, context);
    }

    /**
     * 初始化上下文
     */
    private static TimeSlotContext initializeContext(
            CourseLibraryAndTeacherCourseQualificationListDTO course,
            boolean eveningCoursesEnabled) {
        return new TimeSlotContext(
                eveningCoursesEnabled ? 8 : 12,
                course.getExpectedTotalHours().intValue(),
                course.getStartWeek(),
                course.getEndWeek(),
                0
        );
    }

    /**
     * 生成每天可用的时间块
     */
    private static @NotNull Map<Integer, List<List<Integer>>> generateAvailableBlocks(int maxPeriodsPerDay) {
        Map<Integer, List<List<Integer>>> blocks = new HashMap<>();
        for (int day = 1; day <= 5; day++) {
            List<List<Integer>> dayBlocks = new ArrayList<>();
            // 上午
            dayBlocks.add(Arrays.asList(1, 2, 3, 4));
            // 下午
            dayBlocks.add(Arrays.asList(5, 6, 7, 8));
            // 晚上
            if (maxPeriodsPerDay >= 9) {
                dayBlocks.add(Arrays.asList(9, 10, 11, 12));
            }
            blocks.put(day, dayBlocks);
        }
        return blocks;
    }

    /**
     * 确定课时分配策略
     */
    private static List<Integer> determineDistribution(int weeklyHours) {
        List<Integer> distribution = new ArrayList<>();

        // 优先分配2节连续课
        while (weeklyHours >= 2) {
            distribution.add(2);
            weeklyHours -= 2;
        }

        // 处理剩余1节课
        if (weeklyHours == 1) {
            distribution.add(1);
        }

        Collections.shuffle(distribution);
        return distribution;
    }

    /**
     * 分配时间块到具体天数
     */
    private static Map<Integer, List<Integer>> assignTimeBlocksToDays(
            List<Integer> distribution,
            Map<Integer, List<List<Integer>>> availableBlocks) {
        Map<Integer, List<Integer>> dayTimeSlots = new HashMap<>();
        List<Integer> days = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Collections.shuffle(days);

        for (Integer blockSize : distribution) {
            if (days.isEmpty()) break;

            int day = days.remove(0);
            List<List<Integer>> possibleSlots = generatePossibleTimeSlots(
                    availableBlocks.get(day),
                    blockSize);

            if (!possibleSlots.isEmpty()) {
                Collections.shuffle(possibleSlots);
                dayTimeSlots.put(day, possibleSlots.get(0));
            }
        }
        return dayTimeSlots;
    }

    /**
     * 生成可能的时间段
     */
    private static List<List<Integer>> generatePossibleTimeSlots(
            List<List<Integer>> dayBlocks,
            int blockSize) {
        List<List<Integer>> possibleSlots = new ArrayList<>();

        for (List<Integer> block : dayBlocks) {
            for (int i = 0; i <= block.size() - blockSize; i++) {
                List<Integer> timeSlot = new ArrayList<>();
                for (int j = 0; j < blockSize; j++) {
                    timeSlot.add(block.get(i + j));
                }
                possibleSlots.add(timeSlot);
            }
        }

        return possibleSlots;
    }

    /**
     * 生成最终时间槽
     */
    private static List<TimeSlotDTO> generateFinalTimeSlots(
            Map<Integer, List<Integer>> dayTimeSlots,
            TimeSlotContext context) {
        List<TimeSlotDTO> slots = new ArrayList<>();
        int totalHours = 0;

        for (int week = context.startWeek();
             week <= context.endWeek() && totalHours < context.targetTotalHours();
             week++) {
            for (Map.Entry<Integer, List<Integer>> entry : dayTimeSlots.entrySet()) {
                int day = entry.getKey();
                for (Integer period : entry.getValue()) {
                    slots.add(new TimeSlotDTO(week, day, period));
                    totalHours++;
                }
            }
        }

        return slots;
    }
}