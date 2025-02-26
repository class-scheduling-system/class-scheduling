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

import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.UserInfoDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserAddVO;
import com.frontleaves.scheduling.models.vo.UserEditVO;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
     * 获取当前登录用户信息接口
     * <p>
     * 该接口用于获取当前登录的用户的三方信息;
     * 并针对不同身份用户返回对应的DTO
     *
     * @return 用户信息(DTO)
     */
    @RequestLogin
    @GetMapping("/current")
    public ResponseEntity<BaseResponse<UserInfoDTO>> getCurrentUserInfo(HttpServletRequest request) {
        // 从请求中获取当前用户
        UserDO getCurrentUser = userService.getUserByRequest(request);
        if (getCurrentUser == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_LOGIN, request);
        }
        // 用户信息是存在的
        UserInfoDTO userInfo = userService.getUserInfoWithRole(getCurrentUser);
        return ResultUtil.success("用户信息获取成功", userInfo);
    }

    /**
     * 获取用户信息接口
     *
     * @param userUuid 用户唯一标识符
     * @param request  HTTP请求对象，用于从中提取用户Token
     * @return UserDO 用户信息对象，包含用户的详细信息
     */
    @GetMapping("/{user_uuid}")
    public ResponseEntity<BaseResponse<UserInfoDTO>> userGetInfo(
            @PathVariable("user_uuid") String userUuid,
            HttpServletRequest request
    ) {
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
    @PostMapping("/")
    public ResponseEntity<BaseResponse<UserInfoDTO>> addUser(
            @RequestBody UserAddVO userAddVO
    ) {
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
    @DeleteMapping("/{user_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteUser(
            @PathVariable("user_uuid") String userUuid,
            HttpServletRequest request
    ) {
        userService.checkUuid(userUuid);
        userService.deleteUser(userUuid, request);
        return ResultUtil.success("删除用户成功");
    }

    /**
     * 更新用户
     *
     * @param userUuid   用户唯一标识符
     * @param userEditVO 用户编辑视图对象
     * @param request    HTTP请求对象
     * @return 用户信息数据传输对象
     */
    @PutMapping("/{user_uuid}")
    public ResponseEntity<BaseResponse<UserInfoDTO>> updateUser(
            @PathVariable("user_uuid") String userUuid,
            @Validated @RequestBody UserEditVO userEditVO,
            HttpServletRequest request
    ) {
        userService.checkUuid(userUuid);
        UserInfoDTO userInfoDTO = userService.updateUser(userUuid, userEditVO, request);
        if (userInfoDTO == null) {
            throw new BusinessException("更新用户失败", ErrorCode.OPERATION_ERROR);
        }
        return ResultUtil.success("更新用户成功", userInfoDTO);
    }

    /**
     * 获取用户列表
     *
     * @param page    页数
     * @param size    每页大小
     * @param keyWord 关键字
     * @param isDesc  是否降序
     * @param request HTTP请求对象
     * @return 用户信息数据传输对象分页列表
     */
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<PageDTO<UserInfoDTO>>> getUserList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "key_word", required = false) String keyWord,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            HttpServletRequest request
    ) {
        userService.checkPageAndSize(page, size);
        PageDTO<UserInfoDTO> userInfoDTOPage = userService.getUserList(page, size, keyWord, isDesc, request);
        return ResultUtil.success("获取用户列表成功", userInfoDTOPage);
    }
}
