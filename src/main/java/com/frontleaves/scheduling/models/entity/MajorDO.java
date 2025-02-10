package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 专业DO
 *
 * @author FLASHLACK
 */
@Data
@TableName("cs_major")
@Accessors(chain = true)
public class MajorDO {
    /**
     * 专业主键，自增
     */
    @TableId(value = "major_uuid", type = IdType.ASSIGN_UUID)
    private String majorUuid;
    /**
     * 专业名词
     */
    @TableField(value = "major_name")
    private String majorName;
    /**
     * 专业描述
     */
    @TableField(value = "major_description")
    private String majorDescription;
    /**
     * 专业代码
     */
    @TableField(value = "major_code")
    private String majorCode;
    /**
     * 专业状态，0：禁用，1：启用
     */
    @TableField(value = "majorStatus")
    private Integer majorStatus;
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
