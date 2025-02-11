package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户初始化VO
 *
 * @author FLASHLACK
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInitializationVO {
    @NotBlank(message = "用户名不能为空")
    private String user;
    @NotBlank(message = "密码不能为空")
    private String password;
    @Pattern(regexp = "^[0-9A-Za-z_-]{4,32}$",
            message = "用户名格式不正确，应为4-32位的字母、数字、下划线或短横线")
    private String name;
    @Pattern(regexp = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{6,}$",
            message = "强密码(必须包含大小写字母和数字的组合，可以使用特殊字符，至少6位）")
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
            message = "邮箱格式不正确")
    private String email;
    @Pattern(regexp = "^1[3456789]\\d{9}$",
            message = "手机号格式不正确")
    private String phone;

}
