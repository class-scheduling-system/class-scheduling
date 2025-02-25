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

package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.RoleMapper;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 角色表数据访问对象
 * <p>
 * 该类用于定义角色表数据访问对象。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RoleDAO extends ServiceImpl<RoleMapper, RoleDO> implements IService<RoleDO> {

    private final RedissonClient redisson;

    /**
     * 通过角色 UUID 获取角色信息。
     * <p>
     * 该方法首先尝试从 Redis 中获取角色信息，如果 Redis 中不存在，则从数据库中查询角色信息并将其存入 Redis。
     * 如果在 Redis 和数据库中都未找到角色信息，则返回 null。
     *
     * @param roleUuid 角色的 UUID
     * @return 返回角色信息，如果未找到则返回 null
     * @throws ServerInternalErrorException 如果数据库操作失败
     */
    public RoleDTO getRoleByUuid(String roleUuid) throws ServerInternalErrorException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.ROLE_UUID + roleUuid);
        if (!map.isExists()) {
            RoleDO roleDO = this.lambdaQuery().eq(RoleDO::getRoleUuid, roleUuid).one();
            if (roleDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(roleDO));
                return reorganizePermissions(roleDO);
            }
        } else {
            return reorganizePermissionsRedis(map);
        }
        return null;
    }

    /**
     * 通过角色名称获取角色信息
     * <p>
     * 该方法首先尝试从 Redis 中获取角色信息，如果 Redis 中不存在，则从数据库中查询角色信息并将其存入 Redis。
     * 如果在 Redis 和数据库中都未找到角色信息，则返回 null。如果在 Redis 或数据库中找到了角色信息，则将其转换为 {@code RoleDTO} 对象并返回。
     *
     * @param roleName 角色名称
     * @return 返回角色信息的 DTO 对象，如果未找到则返回 null
     * @throws ServerInternalErrorException 如果在操作数据库或 Redis 时发生异常
     */
    public RoleDTO getRoleByName(String roleName) throws ServerInternalErrorException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.ROLE_NAME + roleName);
        if (!map.isExists()) {
            RoleDO roleDO = this.lambdaQuery().eq(RoleDO::getRoleName, roleName).one();
            if (roleDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(roleDO));
                return reorganizePermissions(roleDO);
            }
        } else {
            return reorganizePermissionsRedis(map);
        }
        return null;
    }

    /**
     * 重新组织权限信息并转换为 RoleDTO 对象
     * <p>
     * 该方法接收一个包含权限信息的 Map，并将其转换为 {@code RoleDTO} 对象。如果 Map 中包含 "permission" 键且其值不为空，
     * 则将该键对应的值解析为字符串列表，并设置到返回的 {@code RoleDTO} 对象中。如果 "permission" 键不存在或其值为空，则直接将 Map 转换为 {@code RoleDTO} 对象。
     * </p>
     *
     * @param map 包含角色和权限信息的映射，其中 "permission" 键用于存储权限信息
     * @return 返回包含角色和权限信息的 RoleDTO 对象
     */
    private RoleDTO reorganizePermissionsRedis(@NotNull Map<String, String> map) {
        if (map.get("permission").isEmpty()) {
            return BeanUtil.toBean(map, RoleDTO.class);
        } else {
            List<String> permissionDOList = JSONUtil.parseArray(map.get("permission")).toList(String.class);
            return BeanUtil.toBean(map, RoleDTO.class).setPermission(permissionDOList);
        }
    }

    /**
     * 重新组织权限信息
     * <p>
     * 该方法用于将 {@code RoleDO} 对象转换为 {@code RoleDTO} 对象，并处理权限信息。
     * 如果 {@code RoleDO} 中的权限信息为空或不存在，则直接进行对象转换；否则，将权限信息解析为字符串列表并设置到 {@code RoleDTO} 中。
     * </p>
     *
     * @param roleDO 角色数据对象，包含角色的基本信息和权限信息
     * @return 转换后的角色传输对象，包含重新组织的权限信息
     */
    private RoleDTO reorganizePermissions(@NotNull RoleDO roleDO) {
        if (roleDO.getPermission() == null || roleDO.getPermission().isEmpty()) {
            return BeanUtil.toBean(roleDO, RoleDTO.class);
        } else {
            List<String> permissionList = JSONUtil.parseArray(roleDO.getPermission()).toList(String.class);
            return BeanUtil.toBean(roleDO, RoleDTO.class).setPermission(permissionList);
        }
    }
}
