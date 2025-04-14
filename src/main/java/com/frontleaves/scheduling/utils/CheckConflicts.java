package com.frontleaves.scheduling.utils;

import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.models.dto.base.ClassAssignmentDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.dto.scheduling.TimeSlotDTO;
import com.xlf.utility.util.UuidUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 检查冲突的工具类
 *
 * @author FLASHLACK
 */
public class CheckConflicts {

    private CheckConflicts() {
        // 私有构造函数，防止实例化
    }

    /**
     * 检查给定课程安排与其他所有课程安排之间的时间冲突
     *
     * @param allAssignments   所有的课程安排列表，用于比较是否存在时间冲突
     * @param classAssignment1 特定的课程安排，需要检查其与其他课程安排的时间冲突情况
     * @return 返回一个包含时间冲突信息的列表如果不存在时间冲突，返回空列表
     */
    public static @NotNull List<SchedulingConflictDTO> checkConflicts(
            @NotNull List<ClassAssignmentDTO> allAssignments,
            @NotNull ClassAssignmentDTO classAssignment1) {
        // 检查时间槽
        List<SchedulingConflictDTO> conflicts = new ArrayList<>();
        List<TimeSlotDTO> timeSlots = JSONUtil.toList(classAssignment1.getClassTime(), TimeSlotDTO.class);
        for (ClassAssignmentDTO classAssignment : allAssignments) {
            List<TimeSlotDTO> classTime = JSONUtil.toList(classAssignment.getClassTime(), TimeSlotDTO.class);
            for (TimeSlotDTO classTimeSlot : classTime) {
                for (TimeSlotDTO timeSlot : timeSlots) {
                    // 检查时间槽是否冲突
                    if (Boolean.TRUE.equals(checkTime(classTimeSlot, timeSlot))) {
                        checkSpecificConflicts(timeSlot,
                                classAssignment, classAssignment1, conflicts);
                    }
                }
            }
        }
        return conflicts;
    }

    public static @NotNull Boolean booleanConflicts(
            @NotNull ClassAssignmentDTO allAssignments,
            @NotNull ClassAssignmentDTO classAssignment1) {
        List<TimeSlotDTO> timeSlots = JSONUtil.toList(classAssignment1.getClassTime(), TimeSlotDTO.class);
        List<TimeSlotDTO> classTime = JSONUtil.toList(allAssignments.getClassTime(), TimeSlotDTO.class);
        for (TimeSlotDTO classTimeSlot : classTime) {
            for (TimeSlotDTO timeSlot : timeSlots) {
                // 检查时间槽是否冲突
                if (Boolean.TRUE.equals(checkTime(classTimeSlot, timeSlot))
                        && booleanSpecificConflicts(timeSlot, allAssignments, classAssignment1)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static @NotNull Boolean booleanSpecificConflicts(
            TimeSlotDTO timeSlot,
            @NotNull ClassAssignmentDTO classAssignment,
            @NotNull ClassAssignmentDTO classAssignment1) {
        //检测老师是否冲突
        if (Objects.equals(classAssignment.getTeacherUuid(), classAssignment1.getTeacherUuid())) {
            return true;
        }
        //检测教室是否冲突
        if (Objects.equals(classAssignment.getClassroomUuid(), classAssignment1.getClassroomUuid())) {
            return true;
        }
        //检测教学班级是否冲突
        return Objects.equals(classAssignment.getTeachingClassUuid(), classAssignment1.getTeachingClassUuid());
    }


    public static void checkSpecificConflicts(
            TimeSlotDTO timeSlot,
            @NotNull ClassAssignmentDTO classAssignment,
            @NotNull ClassAssignmentDTO classAssignment1,
            List<SchedulingConflictDTO> conflicts) {
        //检测老师是否冲突
        if (Objects.equals(classAssignment.getTeacherUuid(), classAssignment1.getTeacherUuid())) {
            conflicts.add(new SchedulingConflictDTO()
                    .setConflictUuid(UuidUtil.generateUuidNoDash())
                    .setConflictType(1)
                    .setConflictTime(timeSlot)
                    .setDescription(String.format(
                            "教师在第%d周星期%d第%d节课被安排了多个课程，无法同时上课。建议：1.调整其中一个课程到其他时间；2.更换其中一个课程的授课教师；3.检查教师工作量分配是否合理。",
                            timeSlot.getWeek(), timeSlot.getDay(), timeSlot.getPeriod()))
                    .setSemesterUuid(classAssignment.getSemesterUuid())
                    .setResolutionStatus(0)
                    .setFirstAssignmentUuid(classAssignment.getClassAssignmentUuid())
                    .setSecondAssignmentUuid(classAssignment1.getClassAssignmentUuid()));
        }
        //检测教室是否冲突
        if (Objects.equals(classAssignment.getClassroomUuid(), classAssignment1.getClassroomUuid())) {
            conflicts.add(new SchedulingConflictDTO()
                    .setConflictUuid(UuidUtil.generateUuidNoDash())
                    .setConflictType(2)
                    .setConflictTime(timeSlot)
                    .setDescription(String.format(
                            "教室在第%d周星期%d第%d节课被多个课程同时占用。建议：1.将其中一个课程调整到空闲教室；2.调整其中一个课程的上课时间；3.检查是否有容量更大的教室可用。", 
                            timeSlot.getWeek(), timeSlot.getDay(), timeSlot.getPeriod()))
                    .setSemesterUuid(classAssignment.getSemesterUuid())
                    .setResolutionStatus(0)
                    .setFirstAssignmentUuid(classAssignment.getClassAssignmentUuid())
                    .setSecondAssignmentUuid(classAssignment1.getClassAssignmentUuid()));
        }
        //检测教学班级是否冲突
        if (Objects.equals(classAssignment.getTeachingClassUuid(), classAssignment1.getTeachingClassUuid())) {
            conflicts.add(new SchedulingConflictDTO()
                    .setConflictUuid(UuidUtil.generateUuidNoDash())
                    .setConflictType(3)
                    .setConflictTime(timeSlot)
                    .setDescription(String.format(
                            "班级在第%d周星期%d第%d节课被安排了多个课程，学生无法同时上课。建议：1.调整其中一个课程到该班级的空闲时间；2.检查课程是否重复安排；3.平衡班级课程分布。", 
                            timeSlot.getWeek(), timeSlot.getDay(), timeSlot.getPeriod()))
                    .setSemesterUuid(classAssignment.getSemesterUuid())
                    .setResolutionStatus(0)
                    .setFirstAssignmentUuid(classAssignment.getClassAssignmentUuid())
                    .setSecondAssignmentUuid(classAssignment1.getClassAssignmentUuid()));
        }
    }

    public static @NotNull Boolean checkTime(@NotNull TimeSlotDTO classTimeSlot, @NotNull TimeSlotDTO timeSlot) {
        // 检查时间槽是否冲突
        if (!Objects.equals(classTimeSlot.getWeek(), timeSlot.getWeek())) {
            return false;
        }
        if (!Objects.equals(classTimeSlot.getDay(), timeSlot.getDay())) {
            return false;
        }
        if (!Objects.equals(classTimeSlot.getPeriod(), timeSlot.getPeriod())) {
            return false;
        }
        return Objects.equals(classTimeSlot.getPeriod(), timeSlot.getPeriod());
    }
}
