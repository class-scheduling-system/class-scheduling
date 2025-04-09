package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;


/**
 * 排课详细信息数据传输对象
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class BackDetailedAssignmentDTO {
    /**
     * 排课主键
     */
    private String classAssignmentUuid;
    /**
     * 学期主键
     */
    private String semesterUuid;
    /**
     * 学期名称
     */
    private String semesterName;
    /**
     * 课程主键
     */
    private String courseUuid;
    /**
     * 课程名称
     */
    private String courseName;
    /**
     * 教师主键
     */
    private String teacherUuid;
    /**
     * 教师名称
     */
    private String teacherName;
    /**
     * 校区主键
     */
    private String campusUuid;
    /**
     * 校区名称
     */
    private String campusName;
    /**
     * 教学楼主键
     */
    private String buildingUuid;
    /**
     * 教学楼名称
     */
    private String buildingName;
    /**
     * 教室主键
     */
    private String classroomUuid;
    /**
     * 教室名称
     */
    private String classroomName;
    /**
     * 教学班主键
     */
    private String teachingClassUuid;
    /**
     * 教学班名称
     */
    private String teachingClassName;
    /**
     * 学时类型
     */
    private String creditHourType;
    /**
     * 学时类型名称
     */
    private String creditHourTypeName;
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
     * 教学校区
     */
    private String teachingCampus;
    /**
     * 教学校区名称
     */
    private String teachingCampusName;
    /**
     * 教室类型
     */
    private String classroomType;
    /**
     * 教室类型名称
     */
    private String classroomTypeName;
}
