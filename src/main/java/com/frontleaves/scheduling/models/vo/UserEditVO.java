package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 用户编辑 VO
 *
 * @author FLASHLACK
 */
@Getter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserEditVO {
    /**
     * 用户名
     * <p>
     * 该字段用于存储用户的名称。用户名必须符合正则表达式 {@code StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION_ABLE_EMPTY}，
     * 允许为空或为4到32位的字母、数字、下划线或短横线的组合。
     */
    @Pattern(regexp = StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "用户名格式不正确，应为4-32位的字母、数字、下划线或短横线")
    private String name;

    @Pattern(regexp = StringConstant.Regular.PASSWORD_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "强密码(必须包含大小写字母和数字的组合，可以使用特殊字符，至少6位）")
    private String password;
    /**
     * 用户邮箱
     * <p>
     * 该字段用于存储用户的电子邮箱地址。邮箱地址必须符合正则表达式 {@code StringConstant.Regular.EMAIL_REGULAR_EXPRESSION_ABLE_EMPTY}，
     * 允许为空或为标准的电子邮件格式，例如 "example@example.com"。
     */
    @Pattern(regexp = StringConstant.Regular.EMAIL_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "邮箱格式不正确")
    private String email;
    /**
     * 用户手机号
     * <p>
     * 该字段用于存储用户的手机号码。手机号码必须符合正则表达式 {@code StringConstant.Regular.PHONE_REGULAR_EXPRESSION_ABLE_EMPTY}，
     * 允许为空或为标准的手机号格式，例如 "13800138000"。
     */
    @Pattern(regexp = StringConstant.Regular.PHONE_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "手机号格式不正确")
    private String phone;

    /**
     * 账号状态 0: 禁用 1: 启用
     */
    private Boolean status;

    /**
     * 账号是否被封禁 0: 未封禁 1: 已封禁
     */
    private Boolean ban;

    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "角色 UUID 格式不正确")
    private String roleUuid;

    /**
     * 用户权限列表
     */
    private List<String> permission;
}
