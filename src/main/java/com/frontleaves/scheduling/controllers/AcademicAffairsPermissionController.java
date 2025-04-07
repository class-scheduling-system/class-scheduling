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
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

import com.frontleaves.scheduling.models.dto.base.AcademicAffairsPermissionDTO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.services.AcademicAffairsPermissionService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教务权限控制器
 * <p>
 * 该控制器类负责处理与教务权限相关的 HTTP 请求。通过 {@code /api/v1/academic-affairs} 路径，
 * 提供了对教务权限资源的访问和管理功能。
 *
 * @author Claude
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/academic-affairs")
public class AcademicAffairsPermissionController {

    private final AcademicAffairsPermissionService academicAffairsPermissionService;
    private final UserService userService;

    /**
     * 获取当前登录用户的教务权限信息
     * <p>
     * 该方法用于获取当前登录用户的教务权限信息。如果用户具有教务权限，
     * 则返回对应的权限信息；如果用户没有教务权限，则返回空数据。
     * </p>
     *
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @return 包含教务权限信息的响应实体
     */
    @GetMapping("/current")
    public ResponseEntity<BaseResponse<AcademicAffairsPermissionDTO>> getCurrentUserAcademicPermission(
            HttpServletRequest request
    ) {
        UserDO userDO = userService.getUserByRequest(request);
        AcademicAffairsPermissionDTO permissionDTO = academicAffairsPermissionService.getCurrentUserAcademicPermission(userDO);
        return ResultUtil.success("获取当前用户教务权限信息成功", permissionDTO);
    }
}
