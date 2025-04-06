package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 学时类型实体
 * @author FLASHLACK
 */
@Data
@Accessors
@TableName("cs_credit_hour_type")
public class CreditHourTypeDO {
    /**
     * 学时类型主键
     */
    @TableId(value = "credit_hour_type_uuid", type = IdType.ASSIGN_UUID)
    private String creditHourTypeUuid;

    /**
     * 学时类型名称
     */
    private String name;

    /**
     * 学时类型描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}

