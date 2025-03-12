package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.Date;

/**
 * 部门信息实体类
 *
 * @author FLASHLACK
 */
@TableName(value = "cs_department")
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentDO {
    /**
     * 部门主键，采用 UUID 自动生成
     */
    @TableId(value = "department_uuid", type = IdType.ASSIGN_UUID)
    private String departmentUuid;

    /**
     * 部门编码
     */
    private String departmentCode;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 部门排序 默认100
     */
    private Integer departmentOrder;

    /**
     * 部门英文名称
     */
    private String departmentEnglishName;

    /**
     * 部门简称
     */
    private String departmentShortName;

    /**
     * 部门地址
     */
    private String departmentAddress;

    /**
     * 是否实体部门
     */
    private Boolean isEntity;

    /**
     * 行政负责人
     */
    private String administrativeHead;

    /**
     * 党委负责人
     */
    private String partyCommitteeHead;

    /**
     * 成立日期，默认当前日期
     */
    @TableField(value = "establishment_date")
    private Date establishmentDate;

    /**
     * 失效日期
     */
    @TableField(value = "expiration_date")
    private Date expirationDate;

    /**
     * 单位类别
     */
    private String unitCategory;

    /**
     * 单位办别
     */
    private String unitType;

    /**
     * 上级部门
     */
    private String parentDepartment;

    /**
     * 分配教学楼
     */
    private String assignedTeachingBuilding;

    /**
     * 是否为开课院系
     */
    private Boolean isTeachingCollege;

    /**
     * 是否为上课院系
     */
    private Boolean isAttendingCollege;

    /**
     * 固定电话
     */
    private String fixedPhone;

    /**
     * 备注
     */
    @TableField(value = "remark")
    private String remark;

    /**
     * 是否为开课教研室
     */
    private Boolean isTeachingOffice;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 创建时间，自动填充
     */
    private Timestamp createdAt;

    /**
     * 更新时间，自动填充
     */
    private Timestamp updatedAt;

}
