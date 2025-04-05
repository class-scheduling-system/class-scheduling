/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@RequiredArgsConstructor
class BaseGeneticSchedulingLogic {
    /**
     * Redis客户端，用于缓存和分布式锁
     */
    final RedissonClient redisson;


    /**
     * 获取任务进度对应的Redis键
     *
     * @param taskId 任务ID
     * @return Redis键名
     */
    protected String getProgressKey(String taskId) {
        return StringConstant.Redis.SCHEDULE_EXECUTE_PROGRESS + taskId;
    }

    /**
     * 获取任务状态对应的Redis键
     *
     * @param taskId 任务ID
     * @return Redis键名
     */
    protected String getStatusKey(String taskId) {
        return StringConstant.Redis.SCHEDULE_EXECUTE_STATUS + taskId;
    }


    /**
     * 查找课程表中的冲突
     * 检查教师冲突和教室冲突
     *
     * @param schedule 待检查的课程表
     * @return 冲突列表
     */
    List<SchedulingConflictDTO> findConflicts(@NotNull ScheduleDTO schedule) {
        List<SchedulingConflictDTO> conflicts = new ArrayList<>();
        // 检查courseSchedules的冲突
        this.checkCourseSchedulesConflicts(schedule.getSchedule(), schedule.getData(), conflicts);
        return conflicts;
    }

    /**
     * 检查课程安排列表中的冲突
     */
    private void checkCourseSchedulesConflicts(@NotNull List<CourseScheduleDTO> courseSchedules,
                                               List<CourseScheduleDTO> dataSchedules, List<SchedulingConflictDTO> conflicts) {
        for (int i = 0; i < courseSchedules.size(); i++) {
            CourseScheduleDTO courseSchedule = courseSchedules.get(i);
            this.checkSingleCourseScheduleConflicts(courseSchedule, courseSchedules.subList(i + 1, courseSchedules.size()),
                    dataSchedules, conflicts);
        }
    }

    /**
     * 检查单个课程安排的冲突
     */
    private void checkSingleCourseScheduleConflicts(@NotNull CourseScheduleDTO courseSchedule,
                                                    List<CourseScheduleDTO> remainingSchedules, List<CourseScheduleDTO> dataSchedules,
                                                    List<SchedulingConflictDTO> conflicts) {
        List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries =
                new ArrayList<>(courseSchedule.getAssignments().entrySet());

        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry = entries.get(i);
            this.checkTimeSlotConflicts(entry, entries.subList(i + 1, entries.size()),
                    remainingSchedules, dataSchedules, conflicts);
        }
    }

    /**
     * 检查时间槽的冲突
     */
    private void checkTimeSlotConflicts(Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry,
                                        @NotNull List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> remainingEntries,
                                        List<CourseScheduleDTO> remainingSchedules, List<CourseScheduleDTO> dataSchedules,
                                        List<SchedulingConflictDTO> conflicts) {
        List<TimeSlotDTO> slots = entry.getKey();
        CourseScheduleItemDTO item = entry.getValue();
        // 检查同一课程内的其他时间槽
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> otherEntry : remainingEntries) {
            checkConflicts(conflicts, slots, item, otherEntry);
        }
        // 检查其他课程安排
        this.checkConflictsWithOtherSchedules(slots, item, remainingSchedules, conflicts);
        // 检查与dataSchedules的冲突
        if (dataSchedules != null) {
            checkConflictsWithDataSchedules(slots, item, dataSchedules, conflicts);
        }
    }

    /**
     * 检查与其他课程安排的冲突
     */
    private void checkConflictsWithOtherSchedules(List<TimeSlotDTO> slots, CourseScheduleItemDTO item,
                                                  @NotNull List<CourseScheduleDTO> schedules, List<SchedulingConflictDTO> conflicts) {
        for (CourseScheduleDTO schedule : schedules) {
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry :
                    schedule.getAssignments().entrySet()) {
                this.checkConflicts(conflicts, slots, item, entry);
            }
        }
    }

    /**
     * 检查与数据课程安排的冲突
     */
    private void checkConflictsWithDataSchedules(List<TimeSlotDTO> slots, CourseScheduleItemDTO item,
                                                 @NotNull List<CourseScheduleDTO> dataSchedules, List<SchedulingConflictDTO> conflicts) {
        for (CourseScheduleDTO dataSchedule : dataSchedules) {
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry :
                    dataSchedule.getAssignments().entrySet()) {
                checkConflicts(conflicts, slots, item, entry);
            }
        }
    }

    /**
     * 检查两个课程安排之间的冲突
     */
    private void checkConflicts(
            List<SchedulingConflictDTO> conflicts,
            @NotNull List<TimeSlotDTO> slots1,
            CourseScheduleItemDTO item1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2
    ) {
        List<TimeSlotDTO> slots2 = entry2.getKey();
        CourseScheduleItemDTO item2 = entry2.getValue();
        // 检查时间冲突
        for (TimeSlotDTO slot1 : slots1) {
            if (hasTimeOverlap(slot1, slots2)) {
                // 如果时间有重叠，检查资源冲突
                checkResourceConflicts(conflicts, item1, item2, slot1);
            }
        }
    }
    /**
     * 检查时间是否重叠
     */
    private boolean hasTimeOverlap(TimeSlotDTO slot1, @NotNull List<TimeSlotDTO> slots2) {
        for (TimeSlotDTO slot2 : slots2) {
            if (this.isTimeSlotConflict(slot1, slot2)) {
                return true;
            }
        }
        return false;
    }
    /**
     * 检查资源冲突（教师、教室、班级）
     */
    private void checkResourceConflicts(
            List<SchedulingConflictDTO> conflicts,
            CourseScheduleItemDTO item1,
            CourseScheduleItemDTO item2,
            TimeSlotDTO slot1
    ) {
        // 检查教师冲突
        if (this.isTeacherConflict(item1, item2)) {
            conflicts.add(this.createTeacherConflict(item1, slot1));
        }
        // 检查教室冲突
        if (this.isClassroomConflict(item1, item2)) {
            conflicts.add(this.createClassroomConflict(item1, slot1));
        }
        // 检查班级冲突
        if (this.isClassConflict(item1, item2)) {
            conflicts.add(this.createClassConflict(item1, slot1));
        }
    }
    /**
     * 创建班级冲突记录
     */
    private SchedulingConflictDTO createClassConflict(@NotNull CourseScheduleItemDTO item, @NotNull TimeSlotDTO slot) {
        // 获取班级组名称列表
        String classGroupNames = item.getClassGroup().stream()
                .map(AdministrativeClassDTO::getClassName)
                .collect(Collectors.joining("、"));

        return new SchedulingConflictDTO()
                .setConflictType(3)
                .setDescription(String.format(
                        "班级 %s 在第%d周星期%d第%d节课有重复安排",
                        classGroupNames,
                        slot.getWeek(),
                        slot.getDay(),
                        slot.getPeriod()
                ));
    }

    /**
     * 检查班级组冲突

     */
    private boolean isClassConflict(CourseScheduleItemDTO item1, CourseScheduleItemDTO item2) {
        // 如果任一项为空或其班级组链表为空，则不存在冲突
        if (item1 == null || item2 == null ||
                item1.getClassGroup() == null || item2.getClassGroup() == null) {
            return false;
        }
        List<AdministrativeClassDTO> classGroups1 = item1.getClassGroup();
        List<AdministrativeClassDTO> classGroups2 = item2.getClassGroup();
        // 如果任一链表为空，则不存在冲突
        if (classGroups1.isEmpty() || classGroups2.isEmpty()) {
            return false;
        }
        // 检查两个链表是否有重叠的班级组
        return classGroups1.stream()
                .map(AdministrativeClassDTO::getAdministrativeClassUuid)
                .filter(Objects::nonNull)
                .anyMatch(uuid1 ->
                        classGroups2.stream()
                                .map(AdministrativeClassDTO::getAdministrativeClassUuid)
                                .filter(Objects::nonNull)
                                .anyMatch(uuid1::equals)
                );
    }

    /**
     * 检查时间槽是否冲突
     */
    private boolean isTimeSlotConflict(@NotNull TimeSlotDTO slot1, @NotNull TimeSlotDTO slot2) {
        return slot1.getWeek().equals(slot2.getWeek()) &&
                slot1.getDay().equals(slot2.getDay()) &&
                slot1.getPeriod().equals(slot2.getPeriod());
    }

    /**
     * 检查教师是否冲突
     */
    private boolean isTeacherConflict(@NotNull CourseScheduleItemDTO item1, @NotNull CourseScheduleItemDTO item2) {
        return item1.getTeacher().getTeacher().getTeacherUuid()
                .equals(item2.getTeacher().getTeacher().getTeacherUuid());
    }

    /**
     * 检查教室是否冲突
     */
    private boolean isClassroomConflict(@NotNull CourseScheduleItemDTO item1, @NotNull CourseScheduleItemDTO item2) {
        return item1.getClassroom().getClassroom().getClassroomUuid()
                .equals(item2.getClassroom().getClassroom().getClassroomUuid());
    }

    /**
     * 创建教师冲突记录
     */
    private SchedulingConflictDTO createTeacherConflict(@NotNull CourseScheduleItemDTO item, @NotNull TimeSlotDTO slot) {
        return new SchedulingConflictDTO()
                .setConflictType(1)
                .setDescription(String.format(
                        "教师 %s 在第%d周星期%d第%d节课有重复安排",
                        item.getTeacher().getTeacher().getName(),
                        slot.getWeek(),
                        slot.getDay(),
                        slot.getPeriod()
                ));
    }

    /**
     * 创建教室冲突记录
     */
    private SchedulingConflictDTO createClassroomConflict(@NotNull CourseScheduleItemDTO item, @NotNull TimeSlotDTO slot) {
        return new SchedulingConflictDTO()
                .setConflictType(2)
                .setDescription(String.format(
                        "教室 %s 在第%d周星期%d第%d节课有重复安排",
                        item.getClassroom().getClassroom().getName(),
                        slot.getWeek(),
                        slot.getDay(),
                        slot.getPeriod()
                ));
    }

    /**
     * 计算资源利用率
     * <p>
     * 评估课程表中各种资源的利用情况，包括：
     * - 教室利用率：衡量教室容量与实际使用情况的匹配度
     * - 教师工作量：评估教师课程分配的均衡性
     * - 时间槽使用率：评估时间资源的利用效率
     * - 总体利用率：综合上述三项指标的平均值
     *
     * @param schedule 待评估的课程表
     * @return 资源利用率指标
     */
    ScheduleResultDTO.ResourceUtilization calculateResourceUtilization(@NotNull ScheduleDTO schedule) {
        // 教室利用率
        Map<String, Integer> roomUsage = new HashMap<>();
        Map<String, Integer> roomCapacity = new HashMap<>();
        // 教师工作量
        Map<String, Integer> teacherWorkload = new HashMap<>();
        // 时间槽使用情况
        Set<String> usedTimeSlots = new HashSet<>();
        int totalTimeSlots = 0;
        // 遍历所有课程安排
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 遍历每个课程的时间槽和排课项
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : courseSchedule.getAssignments().entrySet()) {
                List<TimeSlotDTO> slots = entry.getKey();
                CourseScheduleItemDTO item = entry.getValue();
                // 教室使用统计
                String roomId = item.getClassroom().getClassroom().getClassroomUuid();
                roomUsage.merge(roomId, slots.size(), Integer::sum);
                roomCapacity.putIfAbsent(roomId, item.getClassroom().getClassroom().getCapacity());
                // 教师工作量统计
                String teacherId = item.getTeacher().getTeacher().getTeacherUuid();
                teacherWorkload.merge(teacherId, slots.size(), Integer::sum);
                // 时间槽使用统计
                for (TimeSlotDTO slot : slots) {
                    String timeSlotKey = String.format("%d-%d-%d",
                            slot.getWeek(), slot.getDay(), slot.getPeriod());
                    usedTimeSlots.add(timeSlotKey);
                    totalTimeSlots++;
                }
            }
        }
        // 计算教室平均利用率
        double classroomUtilization = roomUsage.entrySet().stream()
                .mapToDouble(entry -> {
                    int usage = entry.getValue();
                    int capacity = roomCapacity.get(entry.getKey());
                    return (double) usage / capacity;
                })
                .average()
                .orElse(0.0);
        // 计算教师工作量平均值
        double teacherUtilization = teacherWorkload.values().stream()
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0) / (totalTimeSlots > 0 ? totalTimeSlots : 1);
        // 计算时间槽利用率
        double timeSlotUtilization = totalTimeSlots > 0 ?
                (double) usedTimeSlots.size() / totalTimeSlots : 0.0;
        // 计算总体利用率
        double overallUtilization = (classroomUtilization + teacherUtilization + timeSlotUtilization) / 3;
        return new ScheduleResultDTO.ResourceUtilization()
                .setOverall(overallUtilization)
                .setClassroom(classroomUtilization)
                .setTeacher(teacherUtilization)
                .setTimeSlot(timeSlotUtilization);
    }

    /**
     * 转换课程表为课程安排列表
     * 将内部使用的课程表模型转换为前端展示所需的课程安排格式。
     *
     * @param schedule 内部课程表模型
     * @return 课程安排列表
     */
    List<ScheduleResultDTO.ClassAssignmentDTO> convertScheduleToAssignments(@NotNull ScheduleDTO schedule) {
        List<ScheduleResultDTO.ClassAssignmentDTO> assignments = new ArrayList<>();
        // 遍历所有课程安排
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 遍历每个课程的时间槽和排课项
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : courseSchedule.getAssignments().entrySet()) {
                List<TimeSlotDTO> slots = entry.getKey();
                CourseScheduleItemDTO item = entry.getValue();
                // 为每个时间槽创建课程安排
                for (TimeSlotDTO slot : slots) {
                    ScheduleResultDTO.TimeSlot timeSlot = new ScheduleResultDTO.TimeSlot()
                            .setWeek(slot.getWeek())
                            .setDayOfWeek(slot.getDay())
                            .setPeriod(slot.getPeriod());
                    ScheduleResultDTO.ClassAssignmentDTO assignment = new ScheduleResultDTO.ClassAssignmentDTO()
                            .setCourse(item.getCourse())
                            .setTeacher(item.getTeacher())
                            .setClassroom(item.getClassroom())
                            .setClassGroup(item.getClassGroup())
                            .setTimeSlot(timeSlot)
                            .setPriority(item.getPriority());

                    assignments.add(assignment);
                }
            }
        }

        return assignments;
    }

    /**
     * 更新排课任务的进度
     *
     * @param taskId   任务ID
     * @param progress 进度值（0-100）
     */
    void updateProgress(String taskId, int progress) {
        String key = getProgressKey(taskId);
        RBucket<Integer> processList = redisson.getBucket(key);
        processList.set(progress);
    }

    /**
     * 更新排课任务的状态
     *
     * @param taskId 任务ID
     * @param status 状态描述
     */
    void updateStatus(String taskId, String status) {
        String key = getStatusKey(taskId);
        RList<String> getBuket = redisson.getList(key);
        getBuket.add(status);
    }

    /**
     * 深拷贝课程表
     * 创建课程表对象的深拷贝，包括所有课程安排的完整复制，
     * 确保原对象和复制对象完全独立，避免引用共享导致的意外修改。
     *
     * @param schedule 源课程表对象
     * @return 深拷贝后的新课程表对象
     */
    @NotNull ScheduleDTO deepCopySchedule(@NotNull ScheduleDTO schedule) {
        ScheduleDTO copy = new ScheduleDTO();
        List<CourseScheduleDTO> courseSchedules = new ArrayList<>();
        // 复制每个课程安排
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            CourseScheduleDTO courseScheduleCopy = new CourseScheduleDTO();
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
        return copy;
    }

}

