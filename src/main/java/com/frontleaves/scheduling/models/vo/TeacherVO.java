package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 教师数据传输对象
 * <p>
 * 用于在不同层之间传输教师基本信息。
 * 该 DTO 包含单位主键、用户主键、教师工号、教师姓名、教师英文名、教师民族、教师性别、
 * 教师类型、教师电话、教师邮箱、教师职称、教师描述等字段。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherVO {
    /**
     * 单位主键
     */
    @NotBlank(message = "单位主键不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "单位主键格式不正确")
    private String unitUuid;
    /**
     * 用户主键
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY, message = "用户主键格式不正确")
    private String userUuid;
    /**
     * 教师工号
     */
    @NotBlank(message = "教师工号不能为空")
    @Pattern(regexp = StringConstant.Regular.SERIAL_NUMBER_REGULAR_EXPRESSION, message = "教师工号格式不正确")
    private String id;
    /**
     * 教师姓名
     */
    @NotBlank(message = "教师姓名不能为空")
    private String name;
    /**
     * 教师英文名
     */
    @NotBlank(message = "教师英文名不能为空")
    private String englishName;
    /**
     * 教师民族
     */
    @NotBlank(message = "教师民族不能为空")
    private String ethnic;
    /**
     * 教师性别 0：女 1：男
     */
    @NotNull(message = "教师性别不能为空")
    private Boolean sex;
    /**
     * 教师类型
     */
    @NotBlank(message = "教师类型不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "教师类型格式不正确")
    private String type;
    /**
     * 教师电话
     */
    @Pattern(regexp = StringConstant.Regular.PHONE_REGULAR_EXPRESSION_ABLE_EMPTY, message = "电话格式不正确")
    private String phone;
    /**
     * 教师邮箱
     */
    @Pattern(regexp = StringConstant.Regular.EMAIL_REGULAR_EXPRESSION_ABLE_EMPTY, message = "邮箱格式不正确")
    private String email;
    /**
     * 教师职称
     */
    private String jobTitle;
    /**
     * 教师描述
     */
    private String desc;
}
