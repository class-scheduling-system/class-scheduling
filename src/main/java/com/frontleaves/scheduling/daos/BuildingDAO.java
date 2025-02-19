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
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.BuildingMapper;
import com.frontleaves.scheduling.models.entity.BuildingDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Map;

/**
 * 教学楼数据访问对象
 * <p>
 * 该类是 {@code BuildingDO} 实体的数据访问实现，继承自 MyBatis-Plus 的 {@code ServiceImpl} 类，
 * 并实现了 {@code IService<BuildingDO>} 接口。通过该类可以对教学楼信息进行增删改查等操作。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class BuildingDAO extends ServiceImpl<BuildingMapper, BuildingDO> implements IService<BuildingDO> {
    private final Jedis jedis;

    /**
     * 获取教学楼列表
     * <p>
     * 该方法用于从 Redis 或数据库中获取分页的教学楼列表。如果 Redis 中存在对应的数据，则直接返回；
     * 否则，从数据库中查询数据，并将结果存入 Redis 中以便后续快速访问。
     * </p>
     *
     * @param page   页码，表示请求的页数
     * @param size   每页大小，表示每页显示的记录数
     * @param isDesc 是否降序排序，true 表示按创建时间降序排序，false 表示按校区 UUID 升序排序
     * @return 返回一个包含教学楼信息的分页对象 {@code Page<BuildingDO>}，如果查询失败则返回 null
     */
    public Page<BuildingDO> getBuildingList(int page, int size, boolean isDesc) {
        String cacheKey = StringConstant.Redis.BUILDING_LIST + ":" + page + ":" + size + ":" + isDesc;
        Map<String, String> map = jedis.hgetAll(cacheKey);
        try (Transaction transaction = jedis.multi()) {
            if (map.isEmpty()) {
                LambdaQueryChainWrapper<BuildingDO> queryWrapper = this.lambdaQuery();
                if (isDesc) {
                    queryWrapper.orderByDesc(BuildingDO::getCreatedAt);
                } else {
                    queryWrapper.orderByAsc(BuildingDO::getCampusUuid);
                }
                return this.queryAndCache(queryWrapper, page, size, transaction, cacheKey);
            } else {
                return ProjectUtil.getPageForMap(map, BuildingDO.class);
            }
        } catch (Exception e) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 获取包含关键词的教学楼列表
     * <p>
     * 该方法用于从 Redis 或数据库中获取分页的教学楼列表，其中教学楼名称包含指定的关键词。
     * 如果 Redis 中存在对应的数据，则直接返回；否则，从数据库中查询数据，并将结果存入 Redis 中以便后续快速访问。
     * </p>
     *
     * @param page    页码，表示请求的页数
     * @param size    每页大小，表示每页显示的记录数
     * @param isDesc  是否降序排序，true 表示按创建时间降序排序，false 表示按校区 UUID 升序排序
     * @param keyword 关键词，用于模糊匹配教学楼名称
     * @return 返回一个包含教学楼信息的分页对象 {@code Page<BuildingDO>}，如果查询失败则返回 null
     */
    public Page<BuildingDO> getBuildingListHasKeyword(int page, int size, boolean isDesc, String keyword) {
        String cacheKey = StringConstant.Redis.BUILDING_LIST + ":" + page + ":" + size + ":" + isDesc + ":" + keyword;
        Map<String, String> map = jedis.hgetAll(cacheKey);
        try (Transaction transaction = jedis.multi()) {
            if (map.isEmpty()) {
                LambdaQueryChainWrapper<BuildingDO> queryWrapper = this.lambdaQuery();
                if (isDesc) {
                    queryWrapper.orderByDesc(BuildingDO::getCreatedAt);
                } else {
                    queryWrapper.orderByAsc(BuildingDO::getCampusUuid);
                }
                queryWrapper.like(BuildingDO::getBuildingName, keyword);
                this.queryAndCache(queryWrapper, page, size, transaction, cacheKey);
            } else {
                return ProjectUtil.getPageForMap(map, BuildingDO.class);
            }
            return null;
        } catch (Exception e) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 根据 UUID 获取教学楼信息
     * <p>
     * 该方法用于根据给定的 {@code buildingUuid} 从 Redis 或数据库中获取教学楼信息。
     * 如果 Redis 中存在对应的数据，则直接返回；否则，从数据库中查询数据，并将结果存入 Redis 中以便后续快速访问。
     * </p>
     *
     * @param building 教学楼的 UUID
     * @return 返回一个包含教学楼信息的对象 {@code BuildingDO}，如果未找到则返回 null
     */
    @Nullable
    public BuildingDO getBuildingByUuid(@NotNull String building) {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.BUILDING_UUID + building);
        if (map.isEmpty()) {
            BuildingDO buildingDO = this.lambdaQuery().eq(BuildingDO::getBuildingUuid, building).one();
            if (buildingDO != null) {
                try (Transaction transaction = jedis.multi()) {
                    transaction.hset(StringConstant.Redis.BUILDING_UUID + building, ConvertUtil.convertObjectToMapString(buildingDO));
                    transaction.expire(StringConstant.Redis.BUILDING_UUID + building, 86400);
                    transaction.exec();
                } catch (Exception e) {
                    throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
                }
                return buildingDO;
            }
        } else {
            return BeanUtil.toBean(map, BuildingDO.class);
        }
        return null;
    }

    /**
     * 根据教学楼名称获取教学楼信息
     * <p>
     * 该方法首先尝试从 Redis 中获取指定名称的教学楼 UUID。如果 Redis 中存在对应的值，则直接调用 {@code getBuildingByUuid} 方法返回教学楼信息。
     * 如果 Redis 中不存在对应的值，则从数据库中查询教学楼信息，并将查询结果存入 Redis 中以便后续快速访问。如果在数据库中也未找到对应的教学楼信息，则返回 null。
     * </p>
     *
     * @param building 教学楼名称
     * @return 返回与给定名称匹配的 {@code BuildingDO} 对象，如果未找到则返回 null
     */
    @Nullable
    public BuildingDO getBuildingByName(@NotNull String building) {
        String value = jedis.get(StringConstant.Redis.BUILDING_NAME + building);
        if (value == null) {
            BuildingDO buildingDO = this.lambdaQuery().eq(BuildingDO::getBuildingName, building).one();
            if (buildingDO != null) {
                try (Transaction transaction = jedis.multi()) {
                    transaction.set(StringConstant.Redis.BUILDING_NAME + building, buildingDO.getBuildingUuid());
                    transaction.expire(StringConstant.Redis.BUILDING_NAME + building, 86400);
                    transaction.hset(StringConstant.Redis.BUILDING_UUID + buildingDO.getBuildingUuid(), ConvertUtil.convertObjectToMapString(buildingDO));
                    transaction.expire(StringConstant.Redis.BUILDING_UUID + building, 86400);
                    transaction.exec();
                } catch (Exception e) {
                    throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
                }
                return buildingDO;
            }
        } else {
            return this.getBuildingByUuid(value);
        }
        return null;
    }

    /**
     * 根据校区获取教学楼列表
     * <p>
     * 该方法用于从 Redis 或数据库中获取指定校区的教学楼分页列表。如果 Redis 中存在对应的数据，则直接返回；
     * 否则，从数据库中查询数据，并将结果存入 Redis 中以便后续快速访问。
     * </p>
     *
     * @param campusUuid 校区的 UUID
     * @param page       页码，表示请求的页数
     * @param size       每页大小，表示每页显示的记录数
     * @param isDesc     是否降序排序，true 表示按创建时间降序排序，false 表示按校区 UUID 升序排序
     * @return 返回一个包含教学楼信息的分页对象 {@code Page<BuildingDO>}，如果查询失败则返回 null
     */
    public Page<BuildingDO> getBuildingByCampus(String campusUuid, int page, int size, boolean isDesc) {
        String cacheKey = StringConstant.Redis.BUILDING_CAMPUS + campusUuid + ":" + page + ":" + size + ":" + isDesc;
        Map<String, String> map = jedis.hgetAll(cacheKey);
        try (Transaction transaction = jedis.multi()) {
            if (map.isEmpty()) {
                LambdaQueryChainWrapper<BuildingDO> queryWrapper = this.lambdaQuery().eq(BuildingDO::getCampusUuid, campusUuid);
                if (isDesc) {
                    queryWrapper.orderByDesc(BuildingDO::getCreatedAt);
                } else {
                    queryWrapper.orderByAsc(BuildingDO::getBuildingUuid);
                }
                return this.queryAndCache(queryWrapper, page, size, transaction, cacheKey);
            } else {
                return ProjectUtil.getPageForMap(map, BuildingDO.class);
            }
        } catch (Exception e) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 执行查询并将结果缓存到 Redis
     * <p>
     * 该方法用于执行分页查询，并将查询结果缓存到 Redis 中。如果查询有结果，则将分页数据存储到指定的 Redis 缓存键中。
     * </p>
     *
     * @param queryWrapper 查询包装器，用于构建和执行查询
     * @param page         页码，表示请求的页数
     * @param size         每页大小，表示每页显示的记录数
     * @param transaction  Redis 事务对象，用于执行缓存操作
     * @param cacheKey     缓存键，用于在 Redis 中存储和获取缓存数据
     * @return 返回一个包含查询结果的分页对象 {@code Page<T>}
     */
    @Nullable
    private <T> Page<T> queryAndCache(@NotNull LambdaQueryChainWrapper<T> queryWrapper, int page, int size, Transaction transaction, String cacheKey) {
        // 执行分页查询
        Page<T> buildingPage = queryWrapper.page(new Page<>(page, size));

        // 如果查询有结果，则将分页数据存入 Redis
        if (buildingPage.getCurrent() != 0) {
            transaction.hset(cacheKey, "records", JSONUtil.toJsonStr(buildingPage.getRecords()));
            transaction.hset(cacheKey, "current", String.valueOf(buildingPage.getCurrent()));
            transaction.hset(cacheKey, "size", String.valueOf(buildingPage.getSize()));
            transaction.hset(cacheKey, "total", String.valueOf(buildingPage.getTotal()));
            transaction.hset(cacheKey, "pages", String.valueOf(buildingPage.getPages()));
            transaction.expire(cacheKey, 3600);
            transaction.exec();
            return buildingPage;
        }
        return null;
    }
}
