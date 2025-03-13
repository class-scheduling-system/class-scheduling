package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 学生视图对象
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StudentVO {
    @NotBlank(message = "学生UUID不能为空")
    private String studentUuid;

    @NotBlank(message = "学号不能为空")
    private String id;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "性别不能为空")
    private Boolean gender;

    @NotBlank(message = "年级不能为空")
    private String gradeUuid;

    @NotBlank(message = "学院不能为空")
    private String department;

    @NotBlank(message = "专业不能为空")
    private String major;

    private String clazz;

    private String userUuid;

    @NotNull(message = "毕业状态不能为空")
    private Boolean graduated;
}
