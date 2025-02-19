package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 用户添加视图对象
 *
 * @author FLASHLACK
 */
@Getter
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserAddVO {
    /**
     * 用户角色除了学生和老师
     */
    private String roleName;
    @Pattern(regexp = StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION,
            message = "用户名格式不正确，应为4-32位的字母、数字、下划线或短横线")
    @NotNull
    private String name;
    @Pattern(regexp = StringConstant.Regular.PASSWORD_REGULAR_EXPRESSION,
            message = "强密码(必须包含大小写字母和数字的组合，可以使用特殊字符，至少6位）")
    private String password;
    @Email(message = "邮箱格式不正确")
    @NotNull
    private String email;
    @Pattern(regexp = StringConstant.Regular.PHONE_REGULAR_EXPRESSION,
            message = "手机号格式不正确")
    @NotNull
    private String phone;
    /**
     * 用户权限
     */
    private List<String> permission;


}
