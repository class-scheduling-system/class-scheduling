package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 课程库DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseLibraryDTO {
    /**
     * 课程库主键
     */
    private String courseLibraryUuid;

    /**
     * 课程编号
     */
    private String id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程英文名称
     */
    private String englishName;

    /**
     * 课程类别
     */
    private String category;

    /**
     * 课程属性
     */
    private String property;

    /**
     * 课程类型
     */
    private String type;

    /**
     * 课程性质
     */
    private String nature;

    /**
     * 开课院系
     */
    private String department;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 总学时
     */
    private BigDecimal totalHours;

    /**
     * 周学时
     */
    private BigDecimal weekHours;

    /**
     * 理论学时
     */
    private BigDecimal theoryHours;

    /**
     * 实验学时
     */
    private BigDecimal experimentHours;

    /**
     * 实践学时
     */
    private BigDecimal practiceHours;

    /**
     * 上机学时
     */
    private BigDecimal computerHours;

    /**
     * 其他学时
     */
    private BigDecimal otherHours;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 课程描述
     */
    private String description;

    /**
     * 编辑用户
     */
    private String editUser;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}