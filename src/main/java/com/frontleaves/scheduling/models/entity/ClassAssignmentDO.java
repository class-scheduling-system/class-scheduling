package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 排课分配实体类
 * <p>
 * 对应数据库表：`cs_class_assignment`
 * 本类用于封装排课分配的详细信息，主键为 class_assignment_uuid，采用 UUID 自动生成。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("cs_class_assignment")
@NoArgsConstructor
@Accessors(chain = true)
public class ClassAssignmentDO {

    /**
     * 排课主键，采用 UUID 自动生成
     */
    @TableId(value = "class_assignment_uuid", type = IdType.ASSIGN_UUID)
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
     * 教室主键
     */
    private String classroomUuid;

    /**
     * 教学班组成
     */
    private String teachingClassComposition;

    /**
     * 课程归属
     */
    private String courseOwnership;

    /**
     * 教学班名称
     */
    private String teachingClassName;

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
     * 班级规模
     */
    private Integer classSize;

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

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
