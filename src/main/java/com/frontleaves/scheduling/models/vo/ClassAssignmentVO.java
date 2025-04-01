package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 排课分配请求对象
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
public class ClassAssignmentVO {

    /**
     * 学期主键
     */
    @NotBlank(message = "学期主键不能为空")
    private String semesterUuid;

    /**
     * 课程主键
     */
    @NotBlank(message = "课程主键不能为空")
    private String courseUuid;

    /**
     * 教师主键
     */
    @NotBlank(message = "教师主键不能为空")
    private String teacherUuid;

    /**
     * 教室主键
     */
    @NotBlank(message = "教室主键不能为空")
    private String classroomUuid;

    /**
     * 教学班组成
     */
    @NotBlank(message = "教学班组成不能为空")
    private String teachingClassComposition;

    /**
     * 课程归属
     */
    @NotBlank(message = "课程归属不能为空")
    private String courseOwnership;

    /**
     * 教学班名称
     */
    @NotBlank(message = "教学班名称不能为空")
    private String teachingClassName;

    /**
     * 学时类型
     */
    @NotBlank(message = "学时类型不能为空")
    private String creditHourType;

    /**
     * 教学学时
     */
    @NotNull(message = "教学学时不能为空")
    private BigDecimal teachingHours;

    /**
     * 排课学时
     */
    @NotNull(message = "排课学时不能为空")
    private BigDecimal scheduledHours;

    /**
     * 总学时
     */
    @NotNull(message = "总学时不能为空")
    private BigDecimal totalHours;

    /**
     * 排课优先级
     */
    @NotNull(message = "排课优先级不能为空")
    private Integer schedulingPriority;

    /**
     * 班级规模
     */
    @NotNull(message = "班级规模不能为空")
    private Integer classSize;

    /**
     * 教学校区
     */
    @NotBlank(message = "教学校区不能为空")
    private String teachingCampus;

    /**
     * 上课时间
     */
    @NotBlank(message = "上课时间不能为空")
    private String classTime;

    /**
     * 连堂节数
     */
    @NotNull(message = "连堂节数不能为空")
    private Integer consecutiveSessions;

    /**
     * 教室类型
     */
    @NotBlank(message = "教室类型不能为空")
    private String classroomType;

    /**
     * 指定教室
     */
    private String designatedClassroom;

    /**
     * 指定教学楼
     */
    private String designatedTeachingBuilding;

    /**
     * 指定时间
     */
    private String specifiedTime;
}
