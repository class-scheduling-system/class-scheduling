package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

/**
 * 部门信息实体类
 *
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentVO {
    /**
     * 部门编码
     */
    @NotBlank(message = "部门编码不能为空")
    @Pattern(regexp = StringConstant.Regular.SERIAL_NUMBER_REGULAR_EXPRESSION, message = "部门编码格式不正确")
    private String departmentCode;

    /**
     * 部门名称
     */
    @NotBlank(message = "部门名称不能为空")
    private String departmentName;

    /**
     * 部门排序 默认100
     */
    @NotNull(message = "部门排序不能为空")
    @Min(value = 0, message = "部门排序不能小于0")
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
    @NotNull(message = "是否实体部门不能为空")
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
    @NotNull(message = "成立日期不能为空")
    private Date establishmentDate;

    /**
     * 失效日期
     */
    private Date expirationDate;

    /**
     * 单位类别
     */
    @NotBlank(message = "单位类别不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "单位类别格式不正确")
    private String unitCategory;

    /**
     * 单位办别
     */
    @NotBlank(message = "单位办别不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "单位办别格式不正确")
    private String unitType;

    /**
     * 上级部门
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY, message = "上级部门格式不正确")
    private String parentDepartment;

    /**
     * 分配教学楼
     */
    private List<@Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "分配教学楼格式不正确") String> assignedTeachingBuilding;

    /**
     * 是否为开课院系
     */
    @NotNull(message = "是否为开课院系不能为空")
    private Boolean isTeachingCollege;

    /**
     * 是否为上课院系
     */
    @NotNull(message = "是否为上课院系不能为空")
    private Boolean isAttendingCollege;

    /**
     * 固定电话
     */
    @Pattern(regexp = StringConstant.Regular.FIXED_PHONE_REGULAR_EXPRESSION_ABLE_EMPTY, message = "固定电话格式不正确")
    private String fixedPhone;

    /**
     * 备注
     */
    private String remark;

    /**
     * 是否为开课教研室
     */
    @NotNull(message = "是否为开课教研室不能为空")
    private Boolean isTeachingOffice;

    /**
     * 是否启用
     */
    @NotNull(message = "是否启用不能为空")
    private Boolean isEnabled;
}
