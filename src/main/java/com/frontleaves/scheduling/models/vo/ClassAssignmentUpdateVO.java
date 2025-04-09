package com.frontleaves.scheduling.models.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;


/**
 * 排课分配更新请求对象
 *
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ClassAssignmentUpdateVO {
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
     * 教室主键
     */
    private String classroomUuid;

    /**
     * 课程归属
     */
    private String courseOwnership;

    /**
     * 教学班名称
     */
    private String teachingClassName;

    /**
     * 教学班编号
     */
    private String teachingClassCode;

    /**
     * 班级规模
     */
    private Integer classSize;
    /**
     * 行政班UUID链表
     */
    private List<String> administrativeClassUuids;
    /**
     * 教学班描述
     */
    private String description;
    /**
     * 学生人数
     */
    private Integer studentCount;

    /**
     * 学时类型
     */
    private String creditHourType;

    /**
     * 教学学时
     */
    private BigDecimal teachingHours;

    /**
     * 排课学时
     */
    private BigDecimal scheduledHours;

    /**
     * 总学时
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
    private List<ClassTimeVO> classTime;

    /**
     * 连堂节数
     */
    private Integer consecutiveSessions;

}
