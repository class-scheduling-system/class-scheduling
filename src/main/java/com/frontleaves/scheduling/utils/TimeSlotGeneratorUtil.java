package com.frontleaves.scheduling.utils;

import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.TimeSlotDTO;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 时间槽生成工具类
 * @author  FLASHLACK
 */
public class TimeSlotGeneratorUtil {
    // 类级别的Random对象，用于整个类的随机操作
    private static final Random RANDOM = new Random();

    /**
     * 时间槽生成上下文
     */
    private record TimeSlotContext(
            int maxPeriodsPerDay,
            int targetTotalHours,
            int startWeek,
            int endWeek,
            int totalScheduledHours,
            Boolean isOddWeekCourse,  // true=单周上课，false=双周上课，null=不区分单双周
            Integer weeklyHours,     // 每周课时数
            Integer extraSlotDay     // 存放额外课时的天数（奇数课时情况下）
    ) {
        /**
         * 检查是否有额外的时间槽（奇数课时）
         */
        public boolean hasExtraSlot() {
            return weeklyHours != null && weeklyHours % 2 == 1;
        }
    }

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

        // 5. 预留额外天数（不添加时间槽）
        if (course.getWeeklyHours() % 2 == 1 && course.getIsOddWeek() != null) {
            context = new TimeSlotContext(
                    context.maxPeriodsPerDay(),
                    context.targetTotalHours(),
                    context.startWeek(),
                    context.endWeek(),
                    context.totalScheduledHours(),
                    context.isOddWeekCourse(),
                    context.weeklyHours(),
                    RANDOM.nextInt(5) + 1
            );
        }

        // 6. 生成最终时间槽
        return generateFinalTimeSlots(dayTimeSlots, context);
    }

    /**
     * 初始化上下文
     */
    @Contract("_, _ -> new")
    private static @NotNull TimeSlotContext initializeContext(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO course,
            boolean eveningCoursesEnabled) {
        // 仅当weeklyHours为奇数时，才考虑单双周属性
        Boolean isOddWeekCourse = null;
        if (course.getWeeklyHours() % 2 == 1 && course.getIsOddWeek() != null) {
            isOddWeekCourse = course.getIsOddWeek();
        }

        // 计算额外时间槽的天数
        Integer extraSlotDay = null;
        if (course.getWeeklyHours() % 2 == 1) {
            // 默认周五
            extraSlotDay = RANDOM.nextInt(5) + 1;
        }
        return new TimeSlotContext(
                eveningCoursesEnabled ? 12 : 8,
                course.getExpectedTotalHours().intValue(),
                course.getStartWeek(),
                course.getEndWeek(),
                0,
                isOddWeekCourse,
                course.getWeeklyHours(),
                extraSlotDay
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
    private static @NotNull List<Integer> determineDistribution(int weeklyHours) {
        List<Integer> distribution = new ArrayList<>();

        // 对于奇数课时，按照(n-1)课时固定分配
        int baseHours = weeklyHours;
        if (weeklyHours % 2 == 1) {
            baseHours = weeklyHours - 1;
        }

        // 优先分配2节连续课
        while (baseHours >= 2) {
            distribution.add(2);
            baseHours -= 2;
        }

        // 处理剩余1节课
        if (baseHours == 1) {
            distribution.add(1);
        }

        Collections.shuffle(distribution, RANDOM);
        return distribution;
    }

    /**
     * 分配时间块到具体天数
     */
    private static @NotNull Map<Integer, List<Integer>> assignTimeBlocksToDays(
            @NotNull List<Integer> distribution,
            Map<Integer, List<List<Integer>>> availableBlocks) {
        Map<Integer, List<Integer>> dayTimeSlots = new HashMap<>();
        List<Integer> days = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Collections.shuffle(days, RANDOM);

        for (Integer blockSize : distribution) {
            if (days.isEmpty()) {
                break;
            }
            int day = days.remove(0);
            List<List<Integer>> possibleSlots = generatePossibleTimeSlots(
                    availableBlocks.get(day),
                    blockSize);

            if (!possibleSlots.isEmpty()) {
                Collections.shuffle(possibleSlots, RANDOM);
                dayTimeSlots.put(day, possibleSlots.get(0));
            }
        }
        return dayTimeSlots;
    }

    /**
     * 生成可能的时间段
     */
    private static @NotNull List<List<Integer>> generatePossibleTimeSlots(
            @NotNull List<List<Integer>> dayBlocks,
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
     * 查找未被占用的日期
     */
    private static List<Integer> findAvailableDays(Map<Integer, List<Integer>> weekTimeSlots) {
        List<Integer> availableDays = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            if (!weekTimeSlots.containsKey(day)) {
                availableDays.add(day);
            }
        }
        return availableDays;
    }

    /**
     * 查找课时最少的天
     */
    private static int findDayWithMinPeriods(Map<Integer, List<Integer>> weekTimeSlots) {
        int minPeriods = Integer.MAX_VALUE;
        int selectedDay = -1;

        for (Map.Entry<Integer, List<Integer>> entry : weekTimeSlots.entrySet()) {
            int day = entry.getKey();
            List<Integer> periods = entry.getValue();

            if (periods.size() < minPeriods) {
                minPeriods = periods.size();
                selectedDay = day;
            }
        }

        return selectedDay;
    }

    /**
     * 寻找可用的连续2节课时间段
     */
    private static int @Nullable [] findAvailableTimeSlot(List<Integer> existingPeriods) {
        Set<Integer> occupied = new HashSet<>(existingPeriods);
        int[][] possibleSlots = {{1, 2}, {3, 4}, {5, 6}, {7, 8}};

        for (int[] slot : possibleSlots) {
            if (!occupied.contains(slot[0]) && !occupied.contains(slot[1])) {
                return slot;
            }
        }

        return null;
    }

    /**
     * 向未排课的天数添加2课时
     */
    private static void addExtraClassToNewDay(Map<Integer, List<Integer>> weekTimeSlots, List<Integer> availableDays) {
        int extraDay = availableDays.get(RANDOM.nextInt(availableDays.size()));
        weekTimeSlots.put(extraDay, Arrays.asList(1, 2));
    }

    /**
     * 向已有课时的天添加额外课时
     */
    private static void addExtraClassToExistingDay(Map<Integer, List<Integer>> weekTimeSlots, int selectedDay) {
        List<Integer> existingPeriods = weekTimeSlots.get(selectedDay);
        int[] extraPeriods = findAvailableTimeSlot(existingPeriods);

        if (extraPeriods != null) {
            List<Integer> updatedPeriods = new ArrayList<>(existingPeriods);
            updatedPeriods.add(extraPeriods[0]);
            updatedPeriods.add(extraPeriods[1]);
            weekTimeSlots.put(selectedDay, updatedPeriods);
        }
    }

    /**
     * 为指定周次添加额外的2课时
     */
    private static void addExtraClassHours(Map<Integer, List<Integer>> weekTimeSlots) {
        // 找出未被占用的日期
        List<Integer> availableDays = findAvailableDays(weekTimeSlots);

        // 如果有未被占用的日期，随机选择一天
        if (!availableDays.isEmpty()) {
            addExtraClassToNewDay(weekTimeSlots, availableDays);
            return;
        }

        // 如果所有日期都已被占用，寻找课时最少的天
        int selectedDay = findDayWithMinPeriods(weekTimeSlots);
        if (selectedDay != -1) {
            addExtraClassToExistingDay(weekTimeSlots, selectedDay);
        }
    }

    /**
     * 处理单周和双周的排课逻辑
     */
    private static boolean handleWeekScheduling(
            boolean isOddWeek,
            @NotNull TimeSlotContext context,
            Map<Integer, List<Integer>> currentWeekTimeSlots) {

        if (context.isOddWeekCourse() == null) {
            // 不需要特殊处理
            return true;
        }

        boolean isMatchingWeek = (isOddWeek == context.isOddWeekCourse());

        if (context.weeklyHours() % 2 == 0) {
            // 偶数课时：只在匹配的周次排课
            return isMatchingWeek;
        } else if (isMatchingWeek) {
            // 奇数课时：在匹配的周次增加2课时补课
            addExtraClassHours(currentWeekTimeSlots);
        }

        return true;
    }

    /**
     * 将时间槽添加到结果列表中
     */
    private static int addTimeSlots(
            List<TimeSlotDTO> slots,
            @NotNull Map<Integer, List<Integer>> currentWeekTimeSlots,
            int week,
            int totalHours,
            int targetHours) {

        for (Map.Entry<Integer, List<Integer>> entry : currentWeekTimeSlots.entrySet()) {
            int day = entry.getKey();
            for (Integer period : entry.getValue()) {
                slots.add(new TimeSlotDTO(week, day, period));
                totalHours++;

                if (totalHours >= targetHours) {
                    return totalHours;
                }
            }
        }

        return totalHours;
    }

    /**
     * 生成最终时间槽
     */
    private static @NotNull List<TimeSlotDTO> generateFinalTimeSlots(
            Map<Integer, List<Integer>> dayTimeSlots,
            @NotNull TimeSlotContext context) {
        List<TimeSlotDTO> slots = new ArrayList<>();
        int totalHours = 0;

        // 遍历周次范围
        for (int week = context.startWeek();
             week <= context.endWeek() && totalHours < context.targetTotalHours();
             week++) {
            boolean isOddWeek = (week % 2 == 1);
            Map<Integer, List<Integer>> currentWeekTimeSlots = new HashMap<>(dayTimeSlots);

            // 只在满足条件的周次进行排课
            boolean shouldSchedule = handleWeekScheduling(isOddWeek, context, currentWeekTimeSlots);
            if (shouldSchedule) {
                // 添加课时并更新总数
                totalHours = addTimeSlots(slots, currentWeekTimeSlots, week, totalHours, context.targetTotalHours());
            }
        }

        return slots;
    }
}