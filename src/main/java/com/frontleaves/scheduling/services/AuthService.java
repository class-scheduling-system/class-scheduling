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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.BackProfileDTO;
import com.frontleaves.scheduling.models.dto.ForgetPasswordResponseDTO;
import com.frontleaves.scheduling.models.dto.merge.UserLoginDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Email;
import org.jetbrains.annotations.NotNull;

/**
 * 用户认证服务接口，定义了用户登录、注册及密码验证等相关操作。
 * <p>
 * 该接口提供了用户认证相关的基础方法，包括用户登录检查、新用户登录检查、
 * 密码验证以及用户注册等。具体实现细节由实现类决定。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface AuthService {

    /**
     * 检查登录数据
     *
     * @param userLoginVO 用户登录数据
     * @return 用户登录数据传输对象
     */
    UserLoginDTO checkLoginForUser(
            UserLoginVO userLoginVO,
            HttpServletRequest request
    );

    /**
     * 检查学生或教师
     *
     * @param userLoginVO 用户登录数据
     * @return 用户登录数据传输对象
     */
    UserLoginDTO checkLoginForNewUser(
            UserLoginVO userLoginVO,
            HttpServletRequest request
    );

    /**
     * 检查用户是否使用了默认密码。
     * <p>
     * 该方法用于验证用户提供的新密码是否为系统默认生成的初始密码。如果用户的新密码与系统生成的默认密码相同，则抛出异常。
     *
     * @param stuOrTeId   学生或教师的唯一标识符，用于生成默认密码
     * @param newPassword 用户提供的新密码
     * @throws BusinessException 如果新密码与默认密码相同，则抛出此异常
     */
    void checkUserNotUseDefaultPassword(
            @NotNull String stuOrTeId,
            @NotNull String newPassword
    ) throws BusinessException;

    /**
     * 用户注册
     *
     * @param userInitializationVO 用户初始化数据
     */
    void userRegistered(
            UserInitializationVO userInitializationVO,
            HttpServletRequest request
    );

    /**
     * 忘记密码
     * @param email 邮箱
     * @return 忘记密码响应DTO
     */
    ForgetPasswordResponseDTO forgetPassword(
            @Email String email
    );

    /**
     * 检查重置密码
     * @param token 令牌
     * @param newPassword 新密码
     * @return 用户数据对象
     */
    UserDO checkResetPassword(
            String token,
            String newPassword);

    /**
     * 重置密码
     * @param userDO 用户数据对象
     * @param newPassword 新密码
     */
    void resetPassword(UserDO userDO
            , String newPassword);

    /**
     * 检查用户信息
     * @param name 用户名
     * @param email 邮箱
     * @param phone 电话
     * @param request HTTP请求对象
     */
    UserDO checkProfile(
            String name,
            String email,
            String phone,
            HttpServletRequest request);

    /**
     * 用户信息
     * @param userDO 用户数据对象
     * @return 后台用户信息数据传输对象
     */
    BackProfileDTO profile(
            UserDO userDO);

    /**
     * 修改密码
     * @param currentPassword 当前密码
     * @param newPassword 新密码
     * @param request HTTP请求对象
     */
    void changePassword(
            String currentPassword,
            String newPassword,
            HttpServletRequest request);
}
