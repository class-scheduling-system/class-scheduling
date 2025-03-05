package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CampusMapper;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.*;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 校区数据访问对象
 * <p>
 * 该类实现了对校区数据的增删改查操作，并提供了通过校区UUID、名称和编码获取校区信息的方法。
 * 同时，利用Redis进行数据缓存，以提高查询效率。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CampusDAO extends ServiceImpl<CampusMapper, CampusDO> implements IService<CampusDO> {
    private final RedissonClient redisson;

    /**
     * 通过校区UUID获取校区信息
     * <p>
     * 该方法根据给定的校区唯一标识 {@code campusUuid} 从Redis缓存或数据库中查询对应的校区信息。如果在Redis缓存中没有找到相应的数据，则从数据库中查询，并将结果缓存到Redis中以提高后续查询效率。
     * 如果缓存中存在，则直接从缓存中读取校区信息。
     * </p>
     *
     * @param campusUuid 校区的唯一标识
     * @return 返回与给定校区唯一标识匹配的校区信息，如果未找到则返回 {@code null}
     */
    public CampusDO getCampusByUuid(String campusUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.CAMPUS_UUID + campusUuid);
        if (!map.isExists()) {
            CampusDO campusDO = this.getById(campusUuid);
            if (campusDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(campusDO));
                map.expire(Duration.ofSeconds(86400));
                return campusDO;
            }
        } else {
            return BeanUtil.toBean(map, CampusDO.class);
        }
        return null;
    }

    /**
     * 通过校区名称获取校区信息
     * <p>
     * 该方法根据给定的校区名称 {@code campusName} 从Redis缓存或数据库中查询对应的校区信息。如果在Redis缓存中没有找到相应的数据，则从数据库中查询，并将结果缓存到Redis中以提高后续查询效率。
     * 如果缓存中存在，则直接从缓存中读取校区信息。
     * </p>
     *
     * @param campusName 校区名称
     * @return 返回与给定校区名称匹配的校区信息，如果未找到则返回 {@code null}
     */
    public CampusDO getCampusByName(String campusName) {
        return getCampusAndCache(
                redisson.getBucket(StringConstant.Redis.CAMPUS_NAME + campusName),
                this.lambdaQuery().eq(CampusDO::getCampusName, campusName)
        );
    }

    /**
     * 通过校区编码获取校区信息
     * <p>
     * 该方法根据给定的校区编码 {@code campusCode} 从Redis缓存或数据库中查询对应的校区信息。如果在Redis缓存中没有找到相应的数据，则从数据库中查询，并将结果缓存到Redis中以提高后续查询效率。
     * 如果缓存中存在，则直接从缓存中读取校区信息。
     * </p>
     *
     * @param campusCode 校区编码
     * @return 返回与给定校区编码匹配的校区信息，如果未找到则返回 {@code null}
     */
    public CampusDO getCampusByCode(String campusCode) {
        return getCampusAndCache(
                redisson.getBucket(StringConstant.Redis.CAMPUS_CODE + campusCode),
                this.lambdaQuery().eq(CampusDO::getCampusCode, campusCode)
        );
    }

    /**
     * 从缓存或数据库中获取校区信息
     * <p>
     * 该方法首先检查Redis缓存中是否存在指定的校区信息。如果不存在，则从数据库中查询校区信息，并将查询结果存入Redis缓存中。
     * 如果缓存中存在，则直接从缓存中读取校区信息。
     * </p>
     *
     * @param rBucket Redis缓存桶，用于存储和获取校区UUID
     * @param eq      查询条件链，用于构建查询条件
     * @return 返回查询到的校区信息，如果未找到则返回null
     */
    @Nullable
    private CampusDO getCampusAndCache(@NotNull RBucket<String> rBucket, LambdaQueryChainWrapper<CampusDO> eq) {
        if (!rBucket.isExists()) {
            CampusDO campusDO = eq.one();
            if (campusDO != null) {
                // 插入 Redis 数据并设置过期时间
                rBucket.set(campusDO.getCampusUuid());
                rBucket.expire(Duration.ofSeconds(86400));
                RMap<String, String> campusMap = redisson.getMap(StringConstant.Redis.CAMPUS_UUID + campusDO.getCampusUuid());
                campusMap.putAll(ConvertUtil.convertObjectToMapString(campusDO));
                campusMap.expire(Duration.ofSeconds(86400));
                return campusDO;
            }
        } else {
            return this.getCampusByUuid(rBucket.get());
        }
        return null;
    }

    /**
     * 更新校园信息，并在Redis中删除相关缓存
     *
     * @param campusDO 校园信息对象，包含要更新的校园信息
     * @return 返回更新后的校园信息对象
     */
    public CampusDO updateCampus(CampusDO campusDO) {
        // 创建Redis事务，用于删除与校园相关的缓存
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            // 更新数据库中的校园信息
            this.updateById(campusDO);
            this.deleteUserRedis(campusDO, transaction);
        } catch (Exception e) {
            // 如果发生异常，回滚事务，确保数据一致性
            transaction.rollback();
            throw new BusinessException("更新校园信息失败", ErrorCode.OPERATION_ERROR);
        }
        return this.lambdaQuery().eq(
                CampusDO::getCampusUuid, campusDO.getCampusUuid()).one();
    }

    /**
     * 删除与校园相关的Redis缓存
     *
     * @param campusDO    校园实体对象，包含校园的UUID、代码和名称
     * @param transaction Redis事务对象，用于执行删除操作
     */
    private void deleteUserRedis(@NotNull CampusDO campusDO, @NotNull RTransaction transaction) {
        transaction.getMap(StringConstant.Redis.CAMPUS_UUID + campusDO.getCampusUuid()).delete();
        transaction.getBucket(StringConstant.Redis.CAMPUS_CODE + campusDO.getCampusCode()).delete();
        transaction.getBucket(StringConstant.Redis.CAMPUS_NAME + campusDO.getCampusName()).delete();
        transaction.commit();
    }

    public void deleteCampus(CampusDO campusDO) {
        RTransaction rTransaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            this.removeById(campusDO.getCampusUuid());
            this.deleteUserRedis(campusDO, rTransaction);
        } catch (Exception e) {
            rTransaction.rollback();
            throw new BusinessException("删除校园信息失败", ErrorCode.OPERATION_ERROR);
        }

    }
}
