package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 课程库表的 DO 对象
 * @author qiyu
 */
@Data
@TableName("cs_course_library")
public class CourseLibraryDO {

    @TableId(value = "course_library_uuid", type = IdType.ASSIGN_UUID)
    private String courseLibraryUuid;

    @TableField("id")
    private String id;

    @TableField("name")
    private String name;

    @TableField("english_name")
    private String englishName;

    @TableField("category")
    private String category;

    @TableField("property")
    private String property;

    @TableField("type")
    private String type;

    @TableField("nature")
    private String nature;

    @TableField("department")
    private String department;

    @TableField("is_enabled")
    private Boolean isEnabled;

    @TableField("total_hours")
    private BigDecimal totalHours;

    @TableField("week_hours")
    private BigDecimal weekHours;

    @TableField("theory_hours")
    private BigDecimal theoryHours;

    @TableField("experiment_hours")
    private BigDecimal experimentHours;

    @TableField("practice_hours")
    private BigDecimal practiceHours;

    @TableField("computer_hours")
    private BigDecimal computerHours;

    @TableField("other_hours")
    private BigDecimal otherHours;

    @TableField("credit")
    private BigDecimal credit;

    @TableField("theory_classroom_type")
    private String theoryClassroomType;

    @TableField("experiment_classroom_type")
    private String experimentClassroomType;

    @TableField("practice_classroom_type")
    private String practiceClassroomType;

    @TableField("computer_classroom_type")
    private String computerClassroomType;

    @TableField("description")
    private String description;

    @TableField("edit_user")
    private String editUser;

    @TableField("created_at")
    private Timestamp createdAt;

    @TableField("updated_at")
    private Timestamp updatedAt;
}