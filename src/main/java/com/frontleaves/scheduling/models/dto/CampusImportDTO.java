package com.frontleaves.scheduling.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 校区导入数据传输对象
 * <p>
 * 用于Excel导入校区时的数据传输，包含校区的基本信息
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CampusImportDTO {

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 校区描述
     */
    private String campusDesc;

    /**
     * 校区状态 0:禁用 1:启用
     */
    private Boolean campusStatus;

    /**
     * 校区地址
     */
    private String campusAddress;
    
    /**
     * 纬度
     */
    private Double latitude;
    
    /**
     * 经度
     */
    private Double longitude;
} 