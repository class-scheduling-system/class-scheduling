package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 校区视图对象
 * <p>
 * 用于在系统中表示校区的相关信息，包括校区的名称、编码、描述、状态和地址
 * 主要用于数据传输和展示
 *
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CampusVO {
    /**
     * 校区名称
     */
    @NotNull(message = "校区名称不能为空")
    private String campusName;
    /**
     * 校区编码
     */
    @NotNull(message = "校区编码不能为空")
    private String campusCode;
    /**
     * 校区描述
     */
    @NotNull(message = "校区描述不能为空")
    private String campusDesc;
    /**
     * 校区状态 0:禁用，1:启用
     */
    @Min(value = 0, message = "校区状态只能为 0（禁用）或 1（启用）")
    @Max(value = 1, message = "校区状态只能为 0（禁用）或 1（启用）")
    @NotNull(message = "校区状态不能为空")
    private Integer campusStatus;
    /**
     * 校区地址
     */
    @NotNull(message = "校区地址不能为空")
    private String campusAddress;
}
