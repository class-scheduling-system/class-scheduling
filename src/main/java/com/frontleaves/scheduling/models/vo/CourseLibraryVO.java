package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
    @NotBlank(message = "课程ID不能为空")
    @Pattern(regexp = StringConstant.Regular.SERIAL_NUMBER_REGULAR_EXPRESSION, message = "课程工号格式不正确")
    private String id;

    /**
     * 课程名称
     */
    @NotBlank(message = "课程名称不能为空")
    private String name;

    /**
     * 课程英文名称
     */
    private String englishName;

    /**
     * 课程类别
     */
    @NotBlank(message = "课程类别不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "课程类别主键格式不正确")
    private String category;

    /**
     * 课程属性
     */
    @NotBlank(message = "课程属性不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "课程属性主键格式不正确")
    private String property;

    /**
     * 课程类型
     */
    @NotBlank(message = "课程类型不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "课程类型主键格式不正确")
    private String type;

    /**
     * 课程性质
     */
    @NotBlank(message = "课程性质不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "课程性质主键格式不正确")
    private String nature;

    /**
     * 所属院系
     */
    @NotBlank(message = "所属院系不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "课程院系主键格式不正确")
    private String department;

    /**
     * 是否启用
     */
    @NotNull(message = "启用状态不能为空")
    private Boolean isEnabled;

    /**
     * 总课时
     */
    @NotNull(message = "总课时不能为空")
    private BigDecimal totalHours;

    /**
     * 周课时
     */
    @NotNull(message = "周课时不能为空")
    private BigDecimal weekHours;

    /**
     * 理论课时
     */
    @NotNull(message = "理论课时不能为空")
    private BigDecimal theoryHours;

    /**
     * 实验课时
     */
    @NotNull(message = "实验课时不能为空")
    private BigDecimal experimentHours;

    /**
     * 实践课时
     */
    @NotNull(message = "实践课时不能为空")
    private BigDecimal practiceHours;

    /**
     * 上机课时
     */
    @NotNull(message = "上机课时不能为空")
    private BigDecimal computerHours;

    /**
     * 其他课时
     */
    @NotNull(message = "其他课时不能为空")
    private BigDecimal otherHours;

    /**
     * 学分
     */
    @NotNull(message = "学分不能为空")
    private BigDecimal credit;

    /**
     * 课程描述
     */

    private String description;

}
