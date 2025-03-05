package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
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
import org.redisson.api.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 教学楼数据访问对象
 * <p>
 * 该类是用于教学楼相关数据操作的数据访问对象（DAO）。它继承自 MyBatis-Plus 的 {@code ServiceImpl}，
 * 并实现了 {@code IService} 接口，提供了基本的 CRUD 操作。此外，该类还利用 Redisson 客户端进行缓存管理，
 * 以提高查询性能并减少数据库负载。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class BuildingDAO extends ServiceImpl<BuildingMapper, BuildingDO> implements IService<BuildingDO> {
    private final RedissonClient redisson;

    /**
     * 从 Redis 中删除教学楼相关缓存
     * <p>
     * 该方法用于在执行教学楼信息删除操作时，清除 Redis 中与该教学楼相关的所有缓存数据。通过 Redisson 提供的事务功能来确保缓存删除操作的一致性。
     * 具体来说，该方法会删除以下三个缓存：
     * 1. 教学楼 UUID 对应的缓存数据。
     * 2. 教学楼名称对应的缓存数据。
     * 3. 教学楼所在校区 UUID 对应的缓存数据。
     * </p>
     *
     * @param transaction Redisson 事务对象，用于保证缓存删除操作的原子性
     * @param buildingDO  教学楼实体对象，包含需要删除的教学楼信息
     */
    private void deleteBuildingRedis(@NotNull RTransaction transaction, @NotNull BuildingDO buildingDO) {
        // 使用 Redisson 事务处理
        RMap<String, String> buildingMap = transaction.getMap(StringConstant.Redis.BUILDING_UUID + buildingDO.getBuildingUuid());
        buildingMap.delete();

        RMap<String, String> buildingNameMap = transaction.getMap(StringConstant.Redis.BUILDING_NAME + buildingDO.getBuildingName());
        buildingNameMap.delete();

        RMap<String, String> buildingCampusMap = transaction.getMap(StringConstant.Redis.BUILDING_CAMPUS + buildingDO.getCampusUuid());
        buildingCampusMap.delete();
    }

    /**
     * 获取包含关键字的教学楼列表
     * <p>
     * 该方法用于根据给定的关键字从数据库中查询教学楼列表，并支持分页和排序。首先尝试从 Redis 缓存中读取数据，如果缓存中没有数据，则从数据库查询并缓存结果。
     * </p>
     *
     * @param page    分页的页码
     * @param size    每页的大小
     * @param isDesc  是否降序排列，默认为升序
     * @param keyword 查询关键字，用于匹配教学楼名称
     * @return 返回包含教学楼信息的分页对象
     */
    public Page<BuildingDO> getBuildingList(int page, int size, boolean isDesc, String keyword) {
        String cacheKey = StringConstant.Redis.BUILDING_LIST + ":" + page + ":" + size + ":" + isDesc + ":" + keyword;
        RMap<String, String> map = redisson.getMap(cacheKey);
        if (!map.isExists()) {
            LambdaQueryChainWrapper<BuildingDO> queryWrapper = this.lambdaQuery();
            if (isDesc) {
                queryWrapper.orderByDesc(BuildingDO::getCreatedAt);
            } else {
                queryWrapper.orderByAsc(BuildingDO::getCreatedAt);
            }
            if (keyword != null) {
                queryWrapper.like(BuildingDO::getBuildingName, keyword);
            }
            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            return ProjectUtil.getPageForMap(map, BuildingDO.class);
        }
    }

    /**
     * 根据教学楼 UUID 获取教学楼信息
     * <p>
     * 该方法通过给定的教学楼 UUID 从 Redis 缓存或数据库中获取对应的 {@code BuildingDO} 对象。
     * 首先尝试从 Redis 缓存中读取数据，如果缓存中没有数据，则从数据库查询并缓存到 Redis 中。
     * 如果在 Redis 或数据库中都未找到对应的教学楼信息，则返回 {@code null}。
     * </p>
     *
     * @param building 教学楼的 UUID
     * @return 返回与给定 UUID 对应的 {@code BuildingDO} 对象，如果没有找到则返回 {@code null}
     */
    @Nullable
    public BuildingDO getBuildingByUuid(@NotNull String building) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.BUILDING_UUID + building);
        if (!map.isExists()) {
            BuildingDO buildingDO = this.lambdaQuery().eq(BuildingDO::getBuildingUuid, building).one();
            if (buildingDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(buildingDO));
                map.expire(Duration.ofSeconds(86400));
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
     * 该方法根据传入的教学楼名称从数据库中查询对应的 {@code BuildingDO} 对象。如果找到匹配的记录，则将其相关信息缓存到 Redis 中，并设置过期时间为 24 小时。
     * 如果 Redis 中已经存在对应的缓存数据，则直接返回缓存中的数据。如果未找到匹配的记录，则返回 {@code null}。
     * </p>
     *
     * @param building 教学楼名称
     * @return 返回与指定名称匹配的 {@code BuildingDO} 对象，如果没有找到则返回 {@code null}
     */
    @Nullable
    public BuildingDO getBuildingByName(@NotNull String building) {
        RBucket<String> value = redisson.getBucket(StringConstant.Redis.BUILDING_NAME + building);
        if (!value.isExists()) {
            BuildingDO buildingDO = this.lambdaQuery().eq(BuildingDO::getBuildingName, building).one();
            if (buildingDO != null) {
                value.set(buildingDO.getBuildingUuid());
                value.expire(Duration.ofSeconds(86400));
                RMap<Object, Object> map = redisson.getMap(StringConstant.Redis.BUILDING_UUID + buildingDO.getBuildingUuid());
                map.putAll(ConvertUtil.convertObjectToMapString(buildingDO));
                map.expire(Duration.ofSeconds(86400));
                return buildingDO;
            }
        } else {
            return this.getBuildingByUuid(value.get());
        }
        return null;
    }

    /**
     * 根据校区获取教学楼列表
     * <p>
     * 该方法根据给定的校区 UUID 获取该校区下的教学楼列表，并支持分页和排序。首先尝试从 Redis 缓存中读取数据，如果缓存中没有数据，则从数据库查询并缓存结果。
     * </p>
     *
     * @param campusUuid 校区的 UUID
     * @param page       分页的页码
     * @param size       每页的大小
     * @param isDesc     是否降序排列，默认为升序
     * @return 返回包含教学楼信息的分页对象
     */
    public Page<BuildingDO> getBuildingByCampus(String campusUuid, int page, int size, boolean isDesc) {
        String cacheKey = StringConstant.Redis.BUILDING_CAMPUS + campusUuid + ":" + page + ":" + size + ":" + isDesc;
        RMap<String, String> map = redisson.getMap(cacheKey);
        if (!map.isExists()) {
            LambdaQueryChainWrapper<BuildingDO> queryWrapper = this.lambdaQuery().eq(BuildingDO::getCampusUuid, campusUuid);
            if (isDesc) {
                queryWrapper.orderByDesc(BuildingDO::getCreatedAt);
            } else {
                queryWrapper.orderByAsc(BuildingDO::getCreatedAt);
            }
            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            return ProjectUtil.getPageForMap(map, BuildingDO.class);
        }
    }

    /**
     * 更新教学楼信息
     * <p>
     * 该方法用于更新指定的教学楼信息。它首先在 Redis 中删除与该教学楼相关的缓存数据，然后更新数据库中的记录。
     * 整个过程在一个事务中进行，确保数据的一致性。如果在操作过程中发生任何异常，将抛出 {@code ServerInternalErrorException} 异常，并回滚事务。
     * </p>
     *
     * @param buildingDO 待更新的教学楼实体对象，包含需要更新的信息
     * @throws ServerInternalErrorException 如果在更新过程中发生异常，则抛出此异常
     */
    @Transactional
    public void updateBuilding(BuildingDO buildingDO) throws ServerInternalErrorException {
        redisson.getKeys().deleteByPattern(StringConstant.Redis.BUILDING_LIST);
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            this.deleteBuildingRedis(transaction, buildingDO);
            this.updateById(buildingDO);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            log.error(StringConstant.DATABASE_OPERATION_FAILED, e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 删除教学楼信息
     * <p>
     * 该方法用于从数据库和 Redis 缓存中删除指定的教学楼信息。操作通过 Redisson 事务处理，确保数据的一致性。
     * 如果在删除过程中发生异常，将抛出 {@code ServerInternalErrorException} 异常，并附带错误信息 {@code DATABASE_OPERATION_FAILED}。
     * </p>
     *
     * @param buildingDO 教学楼实体对象，包含需要删除的教学楼信息
     * @throws ServerInternalErrorException 如果在删除过程中发生异常
     */
    @Transactional
    public void deleteBuilding(BuildingDO buildingDO) {
        redisson.getKeys().deleteByPattern(StringConstant.Redis.BUILDING_LIST + "*");
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            this.deleteBuildingRedis(transaction, buildingDO);
            this.removeById(buildingDO.getBuildingUuid());
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            log.error(StringConstant.DATABASE_OPERATION_FAILED, e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 根据校区UUID删除所有属于该校区的建筑信息
     * 此方法首先查询与给定校区UUID关联的所有建筑对象，然后删除与这些建筑相关的缓存，
     * 最后从数据库中删除这些建筑信息
     *
     * @param campusUuid 校区的唯一标识符UUID
     */
    public void deleteBuildingByCampusUuid(String campusUuid) {
        RKeys keys = redisson.getKeys();
        keys.deleteByPattern(StringConstant.Redis.BUILDING_UUID + "*");
        keys.deleteByPattern(StringConstant.Redis.BUILDING_NAME + "*");
        // 删除列表缓存
        keys.deleteByPattern(StringConstant.Redis.BUILDING_CAMPUS + campusUuid + "*");
        keys.deleteByPattern(StringConstant.Redis.BUILDING_LIST + "*");
        // 从数据库中删除与校区UUID关联的所有建筑信息
        this.lambdaUpdate().eq(BuildingDO::getCampusUuid, campusUuid).remove();
    }
}
