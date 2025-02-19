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

package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户初始化视图对象
 * <p>
 * 此类用于封装用户在初始化时所需提供的信息，
 * 包括用户名、密码以及验证相关的信息如新密码、
 * 姓名、邮箱和手机号。数据校验规则通过注解实现，
 * 确保接收到的数据符合系统要求。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @see InitVO 初始化视图对象参考
 * @see UserLoginVO 用户登录视图对象参考
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInitializationVO {
    @NotNull(message = "类型只能为学生「TRUE」或老师「FALSE」")
    private boolean type;
    @NotBlank(message = "学号/工号不能为空")
    @NotNull
    private String user;
    @Pattern(regexp = StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION,
            message = "用户名格式不正确，应为4-32位的字母、数字、下划线或短横线")
    @NotNull
    private String name;
    @Pattern(regexp = StringConstant.Regular.PASSWORD_REGULAR_EXPRESSION,
            message = "强密码(必须包含大小写字母和数字的组合，可以使用特殊字符，至少6位）")
    @NotNull
    private String newPassword;
    @Email(message = "邮箱格式不正确")
    @NotNull
    private String email;
    @NotNull
    @Pattern(regexp = StringConstant.Regular.PHONE_REGULAR_EXPRESSION,
            message = "手机号格式不正确")
    private String phone;

}
