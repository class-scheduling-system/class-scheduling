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

package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.annotations.IgnoreLog;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.RoleMapper;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RList;
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
public class RoleDAO extends ServiceImpl<RoleMapper, RoleDO> {

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
    @IgnoreLog
    public RoleDTO getRoleByUuid(String roleUuid) throws ServerInternalErrorException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.ROLE_UUID + roleUuid);
        if (!map.isExists()) {
            RoleDO roleDO = this.lambdaQuery().eq(RoleDO::getRoleUuid, roleUuid).one();
            if (roleDO != null) {
                return this.reorganizePermissions(roleDO, map);
            }
        } else {
            log.debug(LogConstant.DAO + "通过 redis 的 uuid 获取角色");
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
                return this.reorganizePermissions(roleDO, map);
            }
        } else {
            log.debug(LogConstant.DAO + "通过 redis 的 name 获取角色");
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
    @NotNull
    private RoleDTO reorganizePermissionsRedis(@NotNull Map<String, String> map) {
        if (map.get("permission").isEmpty()) {
            return BeanUtil.toBean(map, RoleDTO.class)
                    .setPermission(List.of());
        } else {
            List<String> permissionDOList = JSONUtil.parseArray(map.get("permission")).toList(String.class);
            return BeanUtil.toBean(map, RoleDTO.class)
                    .setPermission(permissionDOList);
        }
    }

    /**
     * 重组权限信息
     * <p>
     * 该方法接收一个 {@code RoleDO} 对象和一个 {@code RMap<String, String>} 对象作为参数。它首先将 {@code RoleDO} 对象的属性转换为键值对并存入传入的映射中。
     * 如果 {@code RoleDO} 中的权限信息为空或不存在，则返回一个新的 {@code RoleDTO} 对象，其权限列表为空。
     * 否则，将 {@code RoleDO} 的权限信息解析为字符串列表，并设置到新创建的 {@code RoleDTO} 对象中，然后返回该对象。
     *
     * @param roleDO 角色数据对象，不能为空
     * @param map    用于存储角色属性的映射，不能为空
     * @return 角色数据传输对象，包含处理后的权限信息(可能为空)
     */
    @NotNull
    private RoleDTO reorganizePermissions(@NotNull RoleDO roleDO, @NotNull RMap<String, String> map) {
        map.putAll(ConvertUtil.convertObjectToMapString(roleDO));
        if (roleDO.getPermission() == null || roleDO.getPermission().isEmpty()) {
            return BeanUtil.toBean(roleDO, RoleDTO.class)
                    .setPermission(List.of());
        } else {
            List<String> permissionList = JSONUtil.parseArray(roleDO.getPermission()).toList(String.class);
            return BeanUtil.toBean(roleDO, RoleDTO.class)
                    .setPermission(permissionList);
        }
    }

    /**
     * 获取角色分页列表
     * @param page 当前页数
     * @param size 每页显示数量
     * @param isDesc 是否降序
     * @param search 搜索关键字
     * @return 角色分页列表
     */
    public PageDTO<RoleDTO> getRoleDtoPageDTO(
            @NotNull Integer page, @NotNull Integer size, Boolean isDesc, String search) {
        if (page <= 0 || size <= 0) {
            throw new BusinessException("RoleDAO内page和size为空", ErrorCode.OPERATION_ERROR);
        }
        LambdaQueryChainWrapper<RoleDO> query = this.lambdaQuery();
        if (search != null && !search.isEmpty()) {
            query
                    .like(RoleDO::getRoleName, search);
        }
        if (Boolean.TRUE.equals(isDesc)) {
            query.orderByDesc(RoleDO::getCreatedAt);
        } else {
            query.orderByAsc(RoleDO::getCreatedAt);
        }
        Page<RoleDO> roleDoPage =  query.page(new Page<>(page, size));
        List<RoleDTO> roleDTOList = roleDoPage.getRecords().stream().map(
                roleDO -> {
                    if (roleDO.getPermission() == null || roleDO.getPermission().isEmpty()) {
                        return BeanUtil.toBean(roleDO, RoleDTO.class)
                                .setPermission(List.of());
                    } else {
                        List<String> permissionList = JSONUtil.parseArray(roleDO.getPermission()).toList(String.class);
                        return BeanUtil.toBean(roleDO, RoleDTO.class)
                                .setPermission(permissionList);
                    }
                }).toList();
        return ProjectUtil.convertPageToPageDTO(roleDoPage, RoleDTO.class)
                .setRecords(roleDTOList);
    }

    /**
     * 获取所有有效角色
     * 只返回 roleStatus 为 1 的角色
     *
     * @return 有效角色列表
     */
    @Nullable
    public List<RoleDO> getActiveRoles() {
        RList<RoleDO> getList = redisson.getList(StringConstant.Redis.ROLE_LIST);
        if (!getList.isExists()) {
            List<RoleDO> getRoleList = this.lambdaQuery()
                    .eq(RoleDO::getRoleStatus, 1)
                    .orderByAsc(RoleDO::getCreatedAt)
                    .list();
            if (!getRoleList.isEmpty()) {
                getList.addAll(getRoleList);
                return getRoleList;
            }
        } else {
            return getList;
        }
        return null;
    }
}
