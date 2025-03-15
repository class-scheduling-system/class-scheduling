package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;

/**
 * 部门信息传输对象
 *
 * @author FLASHLACK
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DepartmentDTO {
    /**
     * 部门主键
     */
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
    @Nullable
    private String departmentEnglishName;

    /**
     * 部门简称
     */
    @Nullable
    private String departmentShortName;

    /**
     * 部门地址
     */
    @Nullable
    private String departmentAddress;

    /**
     * 是否实体部门
     */
    private Boolean isEntity;

    /**
     * 行政负责人
     */
    @Nullable
    private String administrativeHead;

    /**
     * 党委负责人
     */
    @Nullable
    private String partyCommitteeHead;

    /**
     * 成立日期
     */
    private LocalDate establishmentDate;

    /**
     * 失效日期
     */
    @Nullable
    private LocalDate expirationDate;

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
    @Nullable
    private String parentDepartment;

    /**
     * 分配教学楼
     */
    private List<String> assignedTeachingBuilding;

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
    @Nullable
    private String fixedPhone;

    /**
     * 备注
     */
    @Nullable
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
