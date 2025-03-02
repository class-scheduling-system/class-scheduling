package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 教务权限实体类
 * @author FLASHLACK
 */
@TableName("cs_academic_affairs_permission")
@Data
@Accessors(chain = true)
public class AcademicAffairsPermissionDO {
    /**
     * 教务权限主键
     */
    @TableId(value = "academic_affairs_permission_uuid", type = IdType.ASSIGN_UUID)
    private String academicAffairsPermissionUuid;


    /**
     * 授权用户 UUID
     */
    @TableField("authorized_user")
    private String authorizedUser;

    /**
     * 部门（要求该部门为院系）
     */
    @TableField("department")
    private String department;

    /**
     * 类型 0:所有权限, 1:教务权限...
     */
    @TableField("type")
    private Integer type;

    /**
     * 创建时间（默认 `CURRENT_TIMESTAMP`）
     */
    @TableField(value = "created_at")
    private Timestamp createdAt;

    /**
     * 更新时间（默认 `CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP`）
     */
    @TableField(value = "updated_at")
    private Timestamp updatedAt;
}
