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

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.frontleaves.scheduling.models.dto.UserInfoDTO;
import com.frontleaves.scheduling.models.dto.UserLoginDTO;
import com.frontleaves.scheduling.models.vo.UserAddVO;
import com.frontleaves.scheduling.models.vo.UserEditVO;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.frontleaves.scheduling.services.UserService;
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
 * 用户控制器
 * <p>
 * 该类用于定义用户控制器;
 * 用于定义用户相关的控制器接口。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;

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
            HttpServletRequest request) {
        UserLoginDTO userLoginDTO = userService.checkLoginForUser(userLoginVO, request);
        if (userLoginDTO != null) {
            return ResultUtil.success("登录成功", userLoginDTO);
        } else {
            UserLoginDTO newUserLoginDTO = userService.checkLoginForNewUser(userLoginVO, request);
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
            @NotNull HttpServletRequest request) {
        userService.checkUserNotUseDefaultPassword(userInitializationVO.getUser(),
                userInitializationVO.getNewPassword());
        userService.userRegistered(userInitializationVO, request);
        return ResultUtil.success("注册成功");
    }

    /**
     * 获取用户信息接口
     *
     * @param userUuid 用户唯一标识符
     * @param request  HTTP请求对象，用于从中提取用户Token
     * @return UserDO 用户信息对象，包含用户的详细信息
     */
    @GetMapping("/getUserInfo")
    public ResponseEntity<BaseResponse<UserInfoDTO>> userGetInfo(
            @RequestParam("user_uuid") String userUuid,
            HttpServletRequest request) {
        userService.checkUuid(userUuid);
        UserInfoDTO userInfoDTO = userService.getUserInfo(userUuid, request);
        return ResultUtil.success("获取用户信息成功", userInfoDTO);
    }

    /**
     * 添加用户
     *
     * @param userAddVO 用户添加视图对象
     * @return 用户信息数据传输对象
     */
    @PostMapping("/addUser")
    public ResponseEntity<BaseResponse<UserInfoDTO>> addUser(
            @RequestBody UserAddVO userAddVO) {
        userService.checkAddUser(userAddVO);
        UserInfoDTO userInfoDTO = userService.addUser(userAddVO);
        return ResultUtil.success("添加用户成功", userInfoDTO);
    }

    /**
     * 删除用户
     *
     * @param userUuid 用户唯一标识符
     * @return 空数据的响应实体，表示删除操作已成功处理
     */
    @DeleteMapping("/deleteUser")
    public ResponseEntity<BaseResponse<Void>> deleteUser(
            @RequestParam("user_uuid") String userUuid,
            HttpServletRequest request) {
        userService.checkUuid(userUuid);
        userService.deleteUser(userUuid, request);
        return ResultUtil.success("删除用户成功");
    }

    /**
     * 更新用户
     * @param userUuid 用户唯一标识符
     * @param userEditVO 用户编辑视图对象
     * @param request HTTP请求对象
     * @return 用户信息数据传输对象
     */
    @PutMapping("/updateUser")
    public ResponseEntity<BaseResponse<UserInfoDTO>> updateUser(
            @RequestParam("user_uuid")String userUuid,
            @Validated @RequestBody UserEditVO userEditVO,
            HttpServletRequest request) {
        userService.checkUuid(userUuid);
        UserInfoDTO userInfoDTO = userService.updateUser(userUuid,userEditVO,request);
        if (userInfoDTO == null) {
            throw new BusinessException("更新用户失败", ErrorCode.OPERATION_ERROR);
        }
        return ResultUtil.success("更新用户成功",userInfoDTO);
    }
    @GetMapping("/getUserList")
    public ResponseEntity<BaseResponse<PageDTO<UserInfoDTO>>> getUserList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "is_desc",defaultValue = "true")Boolean isDesc,
            HttpServletRequest request) {
        PageDTO<UserInfoDTO> userInfoDTOPage = userService.getUserList(page, size, request);
        return ResultUtil.success("获取用户列表成功", userInfoDTOPage);
    }
}

