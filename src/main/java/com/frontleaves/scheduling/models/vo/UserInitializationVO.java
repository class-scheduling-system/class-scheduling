package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户初始化VO
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserInitializationVO {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[0-9A-Za-z_-]{4,32}$",
            message = "用户名格式不正确，应为4-32位的字母、数字、下划线或短横线")
    private String name;
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-zA-Z])[A-Za-z0-9]{6,20}$",
            message = "密码格式不正确，应为6-20位，且必须包含数字和字母")
    @NotBlank(message = "密码不能为空")
    private String password;
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$",
            message = "邮箱格式不正确")
    private String email;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3456789]\\d{9}$",
            message = "手机号格式不正确")
    private String phone;

}
