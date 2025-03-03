package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 单位类别 DO
 */
@Data
@TableName("cs_unit_category")
public class UnitCategoryDO {

    /**
     * 单位类别主键
     */
    @TableId(value = "unit_category_uuid", type = IdType.ASSIGN_UUID)
    private String unitCategoryUuid;

    /**
     * 单位类别名称
     */
    @TableField("name")
    private String name;

    /**
     * 单位类别排序
     */
    @TableField("`order`")
    private Integer order;

    /**
     * 单位类别英文名称
     */
    @TableField("english_name")
    private String englishName;

    /**
     * 单位类别简称
     */
    @TableField("short_name")
    private String shortName;

    /**
     * 是否实体单位类别
     */
    @TableField("is_entity")
    private Boolean isEntity;

    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private LocalDateTime updatedAt;
}

