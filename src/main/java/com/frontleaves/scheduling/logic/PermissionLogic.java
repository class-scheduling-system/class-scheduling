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

package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.dto.UserDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.PermissionService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * 权限逻辑处理类
 * <p>
 * 该类实现了 {@code PermissionService} 接口，提供了权限相关的逻辑处理方法。主要功能包括将权限字符串拆分为权限列表和检查用户是否具有指定的权限。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see PermissionService
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionLogic implements PermissionService {

    private final RoleDAO roleDAO;

    /**
     * 检查用户权限是否包含指定的父级权限
     * <p>
     * 该方法用于验证用户的权限列表是否包含给定的权限列表作为其前缀。
     * 如果 {@code userPermission} 包含 {@code permissionList} 的所有层级，则认为用户具有相应的父级权限。
     *
     * @param userPermission 用户的权限列表，每一项表示一个层级
     * @param permissionList 需要检查的权限列表，每一项表示一个层级
     * @return 如果用户权限包含指定的父级权限则返回 true，否则返回 false
     */
    private boolean hasParentPermission(List<String> userPermission, @NotNull List<String> permissionList) {
        // 比较用户权限的层级和要检查的权限层级
        for (int i = 0; i < permissionList.size(); i++) {
            if (i >= userPermission.size() || !userPermission.get(i).equals(permissionList.get(i))) {
                return false;
            }
        }
        // 如果 userPermission 包含 permissionList 的前缀，则认为是拥有权限
        return true;
    }

    /**
     * 将权限字符串拆分为权限列表
     * <p>
     * 该方法接收一个权限字符串，并将其按照点号（{@code .}）进行拆分，返回一个包含各个权限部分的列表。如果输入的权限字符串为空或为 {@code null}，则返回一个空列表。
     * </p>
     *
     * @param permissions 权限字符串
     * @return 拆分后的权限列表
     */
    @Override
    public List<String> dismantlingPermissions(String permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return List.of();
        } else {
            return List.of(permissions.split("\\."));
        }
    }

    /**
     * 检查用户是否具有指定的权限
     * <p>
     * 该方法接收一个权限字符串和用户对象，首先将传入的权限字符串拆解成权限列表。接着，通过转换用户信息获取用户的权限列表，并从数据库中获取用户角色的权限信息。最后，结合用户的直接权限和角色赋予的权限，检查这些权限是否包含或部分包含所需的权限。
     *
     * @param permission 权限字符串，表示需要验证的权限，例如 {@code "admin:write"}
     * @param userDO     用户数据对象，包含用户的详细信息如角色标识等
     * @return 如果用户拥有给定的权限（包括直接拥有或者通过角色间接拥有），则返回 {@code true}；否则返回 {@code false}
     */
    @Override
    public boolean checkPermission(@NotNull String permission, @NotNull UserDO userDO) {
        // 拆解权限字符串为一个权限列表
        List<String> permissionList = this.dismantlingPermissions(permission);

        // 转换用户信息，获取用户的权限列表
        UserDTO userDTO = ProjectUtil.convertUserDoToUserDTO(userDO);
        List<List<String>> userPermissionList = userDTO.getPermission().stream()
                .map(this::dismantlingPermissions)
                .toList();

        // 获取用户角色的权限信息
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userDO.getRoleUuid());
        List<List<String>> rolePermissionList = roleDTO.getPermission().stream()
                .map(this::dismantlingPermissions)
                .toList();

        // 将角色权限和用户权限结合起来
        List<List<String>> allPermissions = new ArrayList<>();
        allPermissions.addAll(userPermissionList);
        allPermissions.addAll(rolePermissionList);

        // 遍历所有权限进行匹配
        for (List<String> userPermission : allPermissions) {
            // 权限匹配：直接匹配权限，或者匹配权限的父级
            if (new HashSet<>(userPermission).containsAll(permissionList)
                    || this.hasParentPermission(userPermission, permissionList)) {
                return true;
            }
        }
        return false;
    }
}
