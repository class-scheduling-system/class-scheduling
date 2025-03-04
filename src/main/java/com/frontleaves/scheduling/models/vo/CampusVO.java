package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

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
    @NotNull
    private String campusName;
    /**
     * 校区编码
     */
    @NotNull
    private String campusCode;
    /**
     * 校区描述
     */
    @NotNull
    private String campusDesc;
    /**
     * 校区状态 0:禁用，1:启用
     */
    @Pattern(regexp = "^[01]$",
            message = "校区状态只能为0或1")
    @NotNull
    private Integer campusStatus;
    /**
     * 校区地址
     */
    @NotNull
    private String campusAddress;
}
