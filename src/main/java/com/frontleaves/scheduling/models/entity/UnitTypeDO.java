package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 单位办别 DO
 * @author qiyu
 */
@Data
@TableName("cs_unit_type")
public class UnitTypeDO  {

    /**
     * 单位办别主键
     */
    @TableId(value = "unit_type_uuid", type = IdType.ASSIGN_UUID)
    private String unitTypeUuid;

    /**
     * 单位名称
     */
    @TableField("name")
    private String name;

    /**
     * 单位英文名称
     */
    @TableField("english_name")
    private String englishName;

    /**
     * 单位简称
     */
    @TableField("short_name")
    private String shortName;

    /**
     * 单位排序
     */
    @TableField("`order`")
    private Integer order;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private Timestamp updatedAt;
}
