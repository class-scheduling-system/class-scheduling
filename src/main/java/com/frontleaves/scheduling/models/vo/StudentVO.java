package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 学生视图对象
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public class StudentVO {
    @NotBlank(message = "学号不能为空")
    private String id;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull
    @Min(value = 0, message = "性别0:女")
    @Max(value = 1, message = "性别1:男")
    private Integer gender;

    @NotBlank(message = "学院不能为空")
    private String department;

    @NotBlank(message = "专业不能为空")
    private String major;

    @NotBlank(message = "班级不能为空")
    private String clazz;

}
