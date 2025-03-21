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

package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.BackProfileDTO;
import com.frontleaves.scheduling.models.dto.ForgetPasswordResponseDTO;
import com.frontleaves.scheduling.models.dto.UserLoginDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.frontleaves.scheduling.services.AuthService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 用户认证控制器
 * <p>
 * 该控制器负责处理与用户认证相关的所有请求，包括登录和初始化注册。通过此控制器，
 * 用户可以使用多种方式登录系统，并且在需要时完成信息补全以正式注册。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService authService;

    /**
     * 用户登录接口
     * <p>
     * 此接口用于处理用户登录逻辑，支持多种登录方式：
     * <ol>
     *   <li>当用户已存在时，可通过用户名、手机或邮箱登录。</li>
     *   <li>
     *     若用户不存在，但输入的凭据为学号或工号且密码为默认格式（{@code stu+学号} 或 {@code te+工号}），
     *     则系统将检查学生或教师表。若在相应表中存在该记录，则自动创建用户，并分配默认角色（学生或教师）。
     *   </li>
     *   <li>
     *     如果用户既不存在于用户表，也不在学生/教师表中，则响应中将包含 {@code initialization} 字段，
     *     用于通知前端该用户为未注册状态，需要进一步完善用户信息。
     *   </li>
     * </ol>
     *
     * @param userLoginVO 包含用户登录请求信息的视图对象，已通过验证
     * @return 返回包含用户登录信息的响应实体。若响应数据中含有 {@code initialization} 字段，则表示该用户尚未完成正式注册，
     * 前端应引导用户进入补全信息页面
     * @see UserLoginDTO
     */
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<UserLoginDTO>> userLogin(
            @RequestBody @Validated UserLoginVO userLoginVO,
            HttpServletRequest request
    ) {
        UserLoginDTO userLoginDTO = authService.checkLoginForUser(userLoginVO, request);
        if (userLoginDTO != null) {
            return ResultUtil.success("登录成功", userLoginDTO);
        } else {
            UserLoginDTO newUserLoginDTO = authService.checkLoginForNewUser(userLoginVO, request);
            return ResultUtil.success("登录成功", newUserLoginDTO);
        }
    }

    /**
     * 初始化用户注册接口
     * <p>
     * 当登录接口返回的 {@code initialization} 字段表明用户为初始化状态时，
     * 前端应引导用户进入此页面，补全其用户信息（如用户名、密码、邮箱等），以完成正式注册。
     * 此接口负责接收用户补全信息后的注册请求，并将用户信息更新至数据库。
     *
     * @param userInitializationVO 包含用户初始化信息的视图对象，已通过验证
     * @return 返回空数据的响应实体，表示注册操作已成功处理
     */
    @PostMapping("/registered")
    public ResponseEntity<BaseResponse<Void>> userRegistered(
            @RequestBody @Validated UserInitializationVO userInitializationVO,
            @NotNull HttpServletRequest request
    ) {
        authService.checkUserNotUseDefaultPassword(
                userInitializationVO.getUser(),
                userInitializationVO.getNewPassword()
        );
        authService.userRegistered(userInitializationVO, request);
        return ResultUtil.success("注册成功");
    }

    /**
     * 忘记密码发送邮件接口
     *
     * @param email   邮箱
     * @return 返回包含忘记密码响应信息的响应实体
     */
    @PostMapping("/forget-password")
    public ResponseEntity<BaseResponse<ForgetPasswordResponseDTO>> forgetPassword(
            @RequestParam String email
    ) {
        ForgetPasswordResponseDTO forgetPasswordResponseDTO = authService.forgetPassword(email);
        return ResultUtil.success("重置密码链接已发送", forgetPasswordResponseDTO);
    }

    /**
     * 重置密码接口
     *
     * @param token           令牌
     * @param newPassword     新密码
     * @param confirmPassword 确认密码
     * @return 返回空数据的响应实体，表示密码重置操作已成功处理
     */
    @PostMapping("/reset-password")
    public ResponseEntity<BaseResponse<Void>> resetPassword(
            @RequestParam String token,
            @RequestParam (value = "new_password")String newPassword,
            @RequestParam (value = "confirm_password")String confirmPassword
    ) {
        // 校验密码是否符合要求
        if (newPassword.isEmpty()) {
            throw new BusinessException("密码不能为空", ErrorCode.BODY_ERROR);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次密码不一致", ErrorCode.BODY_ERROR);
        }
        // 使用正则表达式校验密码
        if (!newPassword.matches(StringConstant.Regular.PASSWORD_REGULAR_EXPRESSION)) {
            throw new BusinessException("密码格式错误", ErrorCode.BODY_ERROR);
        }
        UserDO userDO = authService.checkResetPassword(token, newPassword);
        authService.resetPassword(userDO, newPassword);
        return ResultUtil.success("密码重置成功");
    }

    /**
     * 个人信息更新接口
     * @param name 用户名
     * @param email 邮箱
     * @param phone 手机号
     * @param request 请求
     * @return 返回包含个人信息更新响应信息的响应实体
     */
    @PutMapping("/profile")
    public ResponseEntity<BaseResponse<BackProfileDTO>> profile(
            @RequestParam (required = false) String name,
            @RequestParam (required = false) String email,
            @RequestParam (required = false) String phone,
            HttpServletRequest request
    ){
        //检查格式是否正确
        if (name != null && !name.matches(StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION)) {
            throw new BusinessException("用户名格式错误", ErrorCode.BODY_ERROR);
        }
        if (email != null && !email.matches(StringConstant.Regular.EMAIL_REGULAR_EXPRESSION)) {
            throw new BusinessException("邮箱格式错误", ErrorCode.BODY_ERROR);
        }
        if (phone != null && !phone.matches(StringConstant.Regular.PHONE_REGULAR_EXPRESSION)) {
            throw new BusinessException("手机号格式错误", ErrorCode.BODY_ERROR);
        }
        //检查修改内容是否合规
        UserDO userDO = authService.checkProfile(name, email, phone,request);
        BackProfileDTO backProfileDTO = authService.profile(userDO);
        return ResultUtil.success("个人信息更新成功", backProfileDTO);
    }


    /**
     * 修改用户密码接口
     *
     * @param currentPassword 当前密码，用于验证用户身份
     * @param newPassword 新密码，用户希望设置的新密码
     * @param confirmPassword 确认新密码，确保用户两次输入的新密码一致
     * @param request HTTP请求对象，可能用于获取用户信息或会话详情
     * @return 返回一个包含成功消息的响应实体
     *
     * 此接口允许已登录的用户提交当前密码和新密码，以修改其账户密码
     * 它首先验证当前密码的正确性，然后确保新密码和确认密码匹配，最后更新密码
     */
    @PutMapping("/change-password")
    public ResponseEntity<BaseResponse<Void>> changePassword(
            @RequestParam (value = "current_password")String currentPassword,
            @RequestParam (value = "new_password")String newPassword,
            @RequestParam (value = "confirm_password") String confirmPassword,
            HttpServletRequest request
    ){
        // 校验密码是否符合要求
        if (newPassword.isEmpty() || currentPassword.isEmpty() || confirmPassword.isEmpty()) {
            throw new BusinessException("密码不能为空", ErrorCode.BODY_ERROR);
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new BusinessException("两次密码不一致", ErrorCode.BODY_ERROR);
        }
        // 使用正则表达式校验密码
        if (!newPassword.matches(StringConstant.Regular.PASSWORD_REGULAR_EXPRESSION)) {
            throw new BusinessException("密码格式错误", ErrorCode.BODY_ERROR);
        }
        if (currentPassword.equals(newPassword)){
            throw new BusinessException("新密码不能与当前密码相同", ErrorCode.BODY_ERROR);
        }
        // 调用认证服务的修改密码方法，传入当前密码、新密码、确认密码和请求对象
        authService.changePassword(currentPassword, newPassword, request);
        // 使用ResultUtil工具类返回一个表示操作成功的响应实体
        return ResultUtil.success("密码修改成功");
    }
}
