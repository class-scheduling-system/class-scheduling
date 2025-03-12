package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 教学楼数据传输对象
 * <p>
 * 用于返回教学楼数据相关信息，传输的是教学楼的基本信息；
 * 包含教学楼主键、名称、校区主键、状态、创建时间和更新时间等信息。
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
public class BuildingDTO {

    /**
     * 教学楼主键，UUID
     */
    private String buildingUuid;

    /**
     * 教学楼名称
     */
    private String buildingName;

    /**
     * 所属校区信息
     */
    private CampusDTO campus;

    /**
     * 教学楼状态
     */
    private Boolean status;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}
