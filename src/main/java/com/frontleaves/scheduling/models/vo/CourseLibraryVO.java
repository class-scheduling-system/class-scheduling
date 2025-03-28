package com.frontleaves.scheduling.models.vo;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CourseLibraryVO {
    /**
     * 课程ID
     */
    private String id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程英文名称
     */
    @Nullable
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
     * 所属院系
     */
    private String department;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 总课时
     */
    private BigDecimal totalHours;

    /**
     * 周课时
     */
    private BigDecimal weekHours;

    /**
     * 理论课时
     */
    private BigDecimal theoryHours;

    /**
     * 实验课时
     */
    private BigDecimal experimentHours;

    /**
     * 实践课时
     */
    private BigDecimal practiceHours;

    /**
     * 上机课时
     */
    private BigDecimal computerHours;

    /**
     * 其他课时
     */
    private BigDecimal otherHours;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 课程描述
     */
    @Nullable
    private String description;

    /**
     * 编辑用户
     */
    private String editUser;
}
