package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
    /**
     *
     */
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
     * 账号状态
     * <p>
     * 该字段用于表示用户的账号状态。取值范围为0或1，其中0表示禁用，1表示启用。
     * 通过 {@code @Min} 和 {@code @Max} 注解进行限制，确保该字段的值只能是0或1。
     */
    @Min(value = 0, message = "账号状态只能为 0（禁用）或 1（启用）")
    @Max(value = 1, message = "账号状态只能为 0（禁用）或 1（启用）")
    private Short status;
    /**
     * 账号封禁状态
     * <p>
     * 该字段用于表示用户的账号是否被封禁。取值范围为0或1，其中0表示未封禁，1表示已封禁。
     * 通过此字段可以控制用户是否能够正常使用系统功能。
     */
    @Min(value = 0, message = "账号是否被封禁只能为 0（未封禁）或 1（已封禁）")
    @Max(value = 1, message = "账号是否被封禁只能为 0（未封禁）或 1（已封禁）")
    private Integer ban;
    /**
     * 角色 UUID
     * <p>
     * 该字段用于存储与用户关联的角色的唯一标识符。角色 UUID 必须符合正则表达式 {@code StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY}，
     * 允许为空或为标准的 UUID 格式（不包含短横线），例如 "123e4567e89b12d3a456426655440000"。
     */
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "角色 UUID 格式不正确")
    private String roleUuid;
    /**
     * 用户权限列表
     * <p>
     * 该字段用于存储用户的权限信息。每个元素代表一个权限字符串，表示用户所拥有的某项具体权限。
     * 权限字符串的具体格式和含义由系统定义，通常用于控制用户对系统的访问和操作权限。
     * 例如，权限字符串可能包括 "READ_USER", "WRITE_USER", "DELETE_USER" 等。
     */
    private List<String> permission;
}
