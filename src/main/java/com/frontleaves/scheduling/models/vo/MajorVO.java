package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * 专业视图对象
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MajorVO {

    /**
     * 专业UUID(主键,长度固定:32)
     */
    @NotBlank(message = "专业UUID不能为空")
    @Size(min = 32, max = 32, message = "专业UUID长度必须为32个字符")
    private String majorUuid;

    /**
     * 专业名称(长度限制:1-12,唯一)
     */
    @NotBlank(message = "专业名称不能为空")
    @Size(max = 32, message = "专业长度不能超过32个字符")
    private String majorName;

    /**
     * 专业描述(长度限制:0-255)
     */
    @Size(max = 32, message = "专业描述长度不能超过255个字符")
    private String majorDescription;

    /**
     * 专业代码(长度限制:1-32,唯一)
     */
    @NotBlank(message = "专业代码不能为空")
    @Size(max = 32, message = "专业代码长度不能超过32个字符")
    private String majorCode;

    /**
     * 专业状态(0:禁用; 1:启用)
     */
    @NotBlank(message = "专业状态不能为空")
    @Min(value = 0, message = "专业状态只能是0或1")
    @Max(value = 1, message = "专业状态只能是0或1")
    private Integer majorStatus;

    /**
     * 所属学院UUID(必填,长度固定:32)
     */
    @NotBlank(message = "所属学院UUID不能为空")
    @Size(min = 32, max = 32, message = "所属学院UUID长度必须为32个字符")
    private String departmentUuid;

    /**
     * 学制(年)(限制: 1-65535年, smallint unsigned 类型最大值65535)
     */
    @NotNull
    @Min(value = 1, message = "学制必须大于等于1年")
    @Max(value = 65535, message = "学制不能超过65535年")
    private Integer educationYears;

    /**
     * 培养层析(长度限制:1-32)
     */
    @NotBlank(message = "培养层次不能为空")
    @Size(max = 32, message = "培养层次长度不能超过32个字符")
    private String trainingLevel;
}
