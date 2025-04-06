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

package com.frontleaves.scheduling.models.dto.merge;

import com.frontleaves.scheduling.models.dto.base.StudentDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherDTO;
import com.frontleaves.scheduling.models.dto.base.TokenDTO;
import com.frontleaves.scheduling.models.dto.base.UserDTO;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户登录数据传输对象
 * <p>
 * 用于返回用户登录相关信息，包括用户基本信息、Token 信息及账户状态。
 * 当用户为学生时，{@code student} 字段有效，教师登录则此字段为空；
 * 若用户尚未注册账号，则 {@code user} 字段不存在，通过 {@code initialization} 标记区分。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserLoginDTO {
    /**
     * 用户数据传输对象，若用户未注册则为 {@code null}
     */
    @Nullable
    private UserDTO user;

    /**
     * 学生数据传输对象，仅在学生登录时有效，其他身份登录则为 {@code null}
     */
    @Nullable
    private StudentDTO student;

    /**
     * 教师数据传输对象，仅在教师登录时有效，其他身份登录则为 {@code null}
     */
    @Nullable
    private TeacherDTO teacher;

    /**
     * 令牌对象，包含认证信息
     */
    @Nullable
    private TokenDTO token;

    /**
     * 是否是初始化用户，指示用户是否首次使用或未完全注册
     */
    @NotNull
    private Boolean initialization;
}
