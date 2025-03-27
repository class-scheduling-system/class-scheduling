package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;


/**
 * 教务权限数据传输对象
 * @author FLASHLACK
 */
@Data
@Accessors
public class AcademicAffairsPermissionDTO {
    /**
     * 教务权限主键
     */
    private String academicAffairsPermissionUuid;
    /**
     * 授权用户 UUID
     */
    private String authorizedUser;

    /**
     * 部门（要求该部门为院系）
     */
    private String department;

    /**
     * 类型 0:所有权限, 1:教务权限...
     */
    private Byte type;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}

