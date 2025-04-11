package com.frontleaves.scheduling.models.dto.scheduling;

import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.List;

/**
 * DTO类，用于表示调整课程安排的请求和响应
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class BackAdjustCourseScheduleDTO {
    /**
     * 教学安排的唯一标识符
     */
    private String assignmentId;

    /**
     * 课程代码
     */
    private String courseCode;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 调整前的课程安排详情
     */
    private CourseDetailsDTO before;

    /**
     * 调整后的课程安排详情
     */
    private CourseDetailsDTO after;



    /**
     * 调整操作发生的时间
     */
    private Timestamp adjustedAt;

    /**
     * 执行调整操作的用户ID
     */
    private String adjustedBy;

    /**
     * 执行调整操作的用户姓名
     */
    private String adjustedByName;

    /**
     * 调整导致的新冲突列表。如果为空，表示没有新冲突。
     * 列表元素的具体类型需要根据实际冲突信息的结构确定，这里暂时使用 Object。
     */
    private List<SchedulingConflictDTO> newConflicts;

    /**
     * 调整原因
     */
    private String reason;



}