package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 学期视图对象
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class SemesterVO {
    /**
     * 学期名称
     */
    @NotBlank(message = "学期名称不能为空")
    private String name;

    /**
     * 学期编码
     */
    @NotBlank(message = "学期编码不能为空")
    private String code;

    /**
     * 开始日期
     */
    @NotNull(message = "开始日期不能为空")
    private Timestamp startDate;

    /**
     * 结束日期
     */
    @NotNull(message = "结束日期不能为空")
    private Timestamp endDate;

    /**
     * 是否启用
     */
    @NotNull(message = "是否启用不能为空")
    private Boolean isEnabled;
} 