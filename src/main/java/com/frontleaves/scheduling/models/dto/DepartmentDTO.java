/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.models.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DepartmentDTO {
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
    private Date establishmentDate;

    /**
     * 失效日期
     */
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
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间，自动填充
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}
