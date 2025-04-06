package com.frontleaves.scheduling.models.dto.scheduling;

import com.frontleaves.scheduling.models.dto.base.*;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 排课结果数据传输对象
 *
 * @author AI Assistant
 */
@Data
@Accessors(chain = true)
public class ScheduleResultDTO {
    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 学期ID
     */
    private String semesterId;

    /**
     * 部门ID
     */
    private String departmentId;

    /**
     * 排课状态
     * processing - 进行中
     * completed - 已完成
     * failed - 失败
     */
    private String status;

    /**
     * 完成进度（0-100）
     */
    private int progress;

    /**
     * 适应度得分
     */
    private double fitness;

    /**
     * 课程安排列表
     */
    private List<ClassAssignmentDTO> assignments;



    /**
     * 冲突信息
     */
    private List<SchedulingConflictDTO> conflicts;

    /**
     * 资源利用统计
     */
    private ResourceUtilization resourceUtilization;

    @Data
    @Accessors(chain = true)
    public static class ClassAssignmentDTO {
        /**
         * 课程安排ID
         */
        private String courseScheduleItemUuid;

        /**
         * 课程信息
         */
        private CourseLibraryDTO course;

        /**
         * 教师信息
         */
        private TeacherCoursePreferencesDTO teacher;

        /**
         * 教室信息
         */
        private ClassroomInfoDTO classroom;

        /**
         * 班级分组信息
         */
        private List<AdministrativeClassDTO> classGroup;
        /**
         * 教学班级只有UUID数据
         */
        private TeachingClassDTO teachingClass;
        /**
         * 时间安排
         */
        private List<TimeSlotDTO> timeSlot;
        /**
         * 课程类型
         */
        private CreditHourTypeEnuDTO courseType;
        /**
         * 教学班人数
         */
        private Integer number;

        /**
         * 优先级
         */
        private Integer priority;
    }


    @Data
    @Accessors(chain = true)
    public static class ResourceUtilization {
        /**
         * 总体资源利用率
         */
        private double overall;

        /**
         * 教室利用率
         */
        private double classroom;

        /**
         * 教师资源利用率
         */
        private double teacher;

        /**
         * 时间段利用率
         */
        private double timeSlot;
    }
}
