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

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.PermissionDTO;
import com.frontleaves.scheduling.models.dto.lite.PermissionLiteDTO;
import com.frontleaves.scheduling.services.PermissionService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 权限控制器
 * <p>
 * 该控制器类负责处理与权限相关的 HTTP 请求。通过 {@code /api/v1/permission} 路径，
 * 提供了对权限资源的访问和管理功能。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/permission")
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 获取权限分页列表
     * <p>
     * 该方法用于获取权限的分页列表。通过指定页码、每页大小、关键词和排序方式来查询权限数据。
     * 返回的数据包含在 {@code PageDTO<PermissionDTO>} 中，并封装在 {@code BaseResponse} 中返回给客户端。
     *
     * @param page 页码，默认值为 1，必须大于 0
     * @param size 每页大小，默认值为 20，必须大于 0 且不超过 200
     * @param keyword 关键词，可选参数，用于模糊搜索权限名称或描述
     * @param isDesc 是否降序排列，默认值为 true
     * @return 包含权限分页数据的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<PermissionDTO>>> getPermissionPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc
    ) {
        AtomicReference<String> makeKeyword = new AtomicReference<>();
        Optional.ofNullable(size)
                .filter(s -> s > 0)
                .orElseThrow(() -> new IllegalArgumentException("分页大小必须大于 0"));
        Optional.of(size)
                .filter(s -> s <= 200)
                .orElseThrow(() -> new IllegalArgumentException("分页大小不能超过 200"));
        Optional.ofNullable(page)
                .filter(p -> p > 0)
                .orElseThrow(() -> new IllegalArgumentException("页码必须大于 0"));
        Optional.ofNullable(keyword)
                .ifPresent(k -> makeKeyword.set(k.isBlank() ? null : k));
        PageDTO<PermissionDTO> getPermissionPage = permissionService.getPermissionPage(page, size, makeKeyword.get(), isDesc);
        return ResultUtil.success("获取权限列表成功", getPermissionPage);
    }

    /**
     * 获取权限列表
     * <p>
     * 该方法用于从服务中获取所有权限的简要信息列表，并将其封装在 {@code ResponseEntity} 中返回。
     * 返回的响应包含一个消息 "获取权限列表成功" 和权限列表数据。此方法仅限具有 "管理员" 角色的用户访问。
     *
     * @return 包含权限列表和操作状态的 {@code ResponseEntity<BaseResponse<List<PermissionLiteDTO>>>}
     */
    @RequestRole({"管理员"})
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<PermissionLiteDTO>>> getPermissionList() {
        List<PermissionLiteDTO> permissionList = permissionService.getPermissionList();
        return ResultUtil.success("获取权限列表成功", permissionList);
    }
}
