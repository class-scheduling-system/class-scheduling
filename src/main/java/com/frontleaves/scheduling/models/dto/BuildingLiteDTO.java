package com.frontleaves.scheduling.models.dto;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 教学楼数据传输对象(简略版)
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class BuildingLiteDTO {
    /**
     * 教学楼主键，UUID
     */
    private String buildingUuid;

    /**
     * 教学楼名称
     */
    private String buildingName;
}
