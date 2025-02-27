package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 权限表实体类
 * <p>
 * 对应数据库表：`cs_permission`
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName(value = "cs_permission")
@Accessors(chain = true)
public class PermissionDO implements Serializable {

    /**
     * 权限主键，采用 UUID 自动生成
     */
    @TableId(value = "permission_uuid", type = IdType.ASSIGN_UUID)
    private String permissionUuid;

    /**
     * 权限键
     */
    private String permissionKey;

    /**
     * 权限名称
     */
    private String name;

    /**
     * 权限描述
     */
    @TableField(value = "`desc`")
    private String desc;
}
