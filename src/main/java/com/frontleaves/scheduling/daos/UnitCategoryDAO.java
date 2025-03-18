package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.UnitCategoryMapper;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Repository
@RequiredArgsConstructor
public class UnitCategoryDAO extends ServiceImpl<UnitCategoryMapper, UnitCategoryDO> {

    // Redis 缓存客户端
    private final RedissonClient redisson;

    /**
     * 根据唯一标识符获取单位类别信息
     * 该方法首先尝试从Redis中获取单位类别信息，如果未找到，则从数据库中获取，并将其缓存到Redis中
     * 使用缓存的目的是提高相同数据的获取效率，减少数据库查询次数
     *
     * @param unitCategoryUuid 单位类别的唯一标识符
     * @return 返回单位类别对象，如果未找到则返回null
     */
    public UnitCategoryDO getUnitCategoryByUuid(String unitCategoryUuid) {
        // 从Redis中获取单位类别信息
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.UNIT_CATEGORY_UUID + unitCategoryUuid);
        if (!map.isExists()) {
            // 如果Redis中不存在该单位类别信息，则从数据库中获取
            UnitCategoryDO unitCategoryDO = this.getById(unitCategoryUuid);
            if (unitCategoryDO != null) {
                // 将获取到的单位类别信息存入Redis，并设置过期时间
                map.putAll(ConvertUtil.convertObjectToMapString(unitCategoryDO));
                map.expire(Duration.ofSeconds(86400));
                return unitCategoryDO;
            }
        } else {
            // 如果Redis中存在该单位类别信息，则直接转换并返回单位类别对象
            return BeanUtil.toBean(map, UnitCategoryDO.class);
        }
        return null;
    }

    /**
     * 根据单位类别名称获取单位类别信息
     * 首先尝试从Redis中获取单位类别UUID，如果不存在，则从数据库中查询并更新到Redis中
     * 使用事务确保数据一致性
     *
     * @param unitCategoryName 单位类别名称
     * @return 单位类别对象，如果找不到则返回null
     * @throws ServerInternalErrorException 如果数据库操作失败，则抛出此异常
     */
    public UnitCategoryDO getUnitCategoryByName(String unitCategoryName) {
        // 创建Redis事务
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            // 尝试从Redis中获取单位类别UUID
            RBucket<String> getCategoryUuid = transaction.getBucket(StringConstant.Redis.UNIT_CATEGORY_NAME + unitCategoryName);
            if (!getCategoryUuid.isExists()) {
                // 如果Redis中不存在，从数据库中查询
                UnitCategoryDO unitCategoryDO = this.lambdaQuery()
                        .eq(UnitCategoryDO::getName, unitCategoryName)
                        .one();
                if (unitCategoryDO != null) {
                    // 查询到的单位类别信息更新到Redis中
                    getCategoryUuid.set(unitCategoryDO.getUnitCategoryUuid());
                    RMap<String, String> map = transaction.getMap(StringConstant.Redis.UNIT_CATEGORY_UUID + unitCategoryDO.getUnitCategoryUuid());
                    map.putAll(ConvertUtil.convertObjectToMapString(unitCategoryDO));
                    map.expire(Duration.ofSeconds(86400));
                    transaction.commit();
                }
            } else {
                // 如果Redis中存在，直接返回单位类别信息
                return this.getUnitCategoryByUuid(getCategoryUuid.get());
            }
            return null;
        } catch (Exception e) {
            // 发生异常时回滚事务
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }
}
