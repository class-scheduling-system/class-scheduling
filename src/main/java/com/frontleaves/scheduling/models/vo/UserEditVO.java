package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
    @Pattern(regexp = StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "用户名格式不正确，应为4-32位的字母、数字、下划线或短横线")
    private String name;
    @Pattern(regexp = StringConstant.Regular.PASSWORD_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "强密码(必须包含大小写字母和数字的组合，可以使用特殊字符，至少6位）")
    private String password;
    @Pattern(regexp = StringConstant.Regular.EMAIL_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "邮箱格式不正确")
    private String email;
    @Pattern(regexp = StringConstant.Regular.PHONE_REGULAR_EXPRESSION_ABLE_EMPTY,
            message = "手机号格式不正确")
    private String phone;
    private Boolean status;
    private Boolean ban;
    private String roleUuid;
    private String permission;
}
