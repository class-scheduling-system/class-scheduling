package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 排课分配数据传输对象
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class ClassAssignmentDTO {
    /**
     * 排课主键
     */
    private String classAssignmentUuid;
    /**
     * 学期主键
     */
    private String semesterUuid;
    /**
     * 课程主键
     */
    private String courseUuid;
    /**
     * 教师主键
     */
    private String teacherUuid;
    /**
     * 校区主键
     */
    private String campusUuid;
    /**
     * 教学楼主键
     */
    private String buildingUuid;
    /**
     * 教室主键
     */
    private String classroomUuid;
    /**
     * 教学班主键
     */
    private String teachingClassUuid;
    /**
     * 课程归属
     */
    private String courseOwnership;
    /**
     * 学时类型
     */
    private String creditHourType;
    /**
     * 教学学时（指教师实际授课的学时）
     */
    private BigDecimal teachingHours;
    /**
     * 排课学时（指课程安排的学时）
     */
    private BigDecimal scheduledHours;
    /**
     * 总需学时
     */
    private BigDecimal totalHours;
    /**
     * 排课优先级
     */
    private Integer schedulingPriority;
    /**
     * 教学校区
     */
    private String teachingCampus;
    /**
     * 上课时间
     */
    private String classTime;
    /**
     * 连堂节数
     */
    private Integer consecutiveSessions;

    /**
     * 教室类型
     */
    private String classroomType;

    /**
     * 指定时间
     */
    private String specifiedTime;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}