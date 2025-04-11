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

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.PermissionDTO;
import com.frontleaves.scheduling.models.dto.lite.PermissionLiteDTO;
import com.frontleaves.scheduling.models.entity.base.UserDO;

import java.util.List;

/**
 * 权限服务接口
 * <p>
 * 该接口定义了权限管理相关的操作方法，包括权限字符串的拆分、权限检查、获取权限分页数据以及获取所有权限的轻量级列表。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface PermissionService {

    /**
     * 将权限字符串拆分为权限列表
     * <p>
     * 该方法接收一个权限字符串，并将其按照点号（{@code .}）进行拆分，返回一个包含各个权限部分的列表。如果输入的权限字符串为空或为 {@code null}，则返回一个空列表。
     * </p>
     *
     * @param permissions 权限字符串
     * @return 拆分后的权限列表
     */
    List<String> dismantlingPermissions(String permissions);

    /**
     * 检查用户是否具有指定权限
     * <p>
     * 该方法用于验证给定的用户是否拥有特定的权限。如果用户拥有该权限，则返回 {@code true}；否则返回 {@code false}。
     * </p>
     *
     * @param permission 要检查的权限字符串
     * @param userDO     用户对象，包含用户的详细信息
     * @return 如果用户拥有指定的权限则返回 {@code true}，否则返回 {@code false}
     */
    boolean checkPermission(String permission, UserDO userDO);

    /**
     * 获取权限分页数据
     * <p>
     * 该方法用于获取权限信息的分页数据。根据提供的参数，返回指定页码和每页大小的权限列表，并支持按指定字段排序。
     * </p>
     *
     * @param page   当前页码，从1开始
     * @param size   每页显示的记录数
     * @param s      排序字段，例如 {@code "name"} 或 {@code "permissionKey"}
     * @param isDesc 是否降序排列，如果为 {@code true} 则降序，否则升序
     * @return 返回一个包含权限分页数据的 {@code PageDTO<PermissionDTO>} 对象
     */
    PageDTO<PermissionDTO> getPermissionPage(int page, int size, String s, boolean isDesc);

    /**
     * 获取所有权限的轻量级列表
     * <p>
     * 该方法用于获取系统中所有权限的基本信息，并以 {@code PermissionLiteDTO} 对象的形式返回。每个 {@code PermissionLiteDTO} 包含权限的唯一标识符、权限键和权限名称。
     * 适用于需要快速获取或传递权限基本信息的场景，可以减少不必要的数据传输，提高系统性能。
     * </p>
     *
     * @return 返回一个包含所有权限基本信息的 {@code List<PermissionLiteDTO>} 列表
     */
    List<PermissionLiteDTO> getPermissionList();
}
