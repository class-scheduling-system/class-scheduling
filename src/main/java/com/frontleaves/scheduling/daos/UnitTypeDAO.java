package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.UnitTypeMapper;
import com.frontleaves.scheduling.models.dto.UnitTypeLiteDTO;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.*;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * UnitTypeDAO
 *
 * @author xiao_lfeng
 * @version 1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class UnitTypeDAO extends ServiceImpl<UnitTypeMapper, UnitTypeDO> {
    private final RedissonClient redisson;

    /**
     * 根据UUID获取单位类型信息
     * 首先尝试从Redis中获取单位类型信息，如果不存在，则从数据库中获取，并存入Redis中以缓存
     *
     * @param unitTypeUuid 单位类型UUID，用于唯一标识一个单位类型
     * @return 返回单位类型对象，如果未找到则返回null
     */
    public UnitTypeDO getUnitTypeByUuid(String unitTypeUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.UNIT_TYPE_UUID + unitTypeUuid);
        if (!map.isExists()) {
            UnitTypeDO unitTypeDO = this.getById(unitTypeUuid);
            if (unitTypeDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(unitTypeDO));
                map.expire(java.time.Duration.ofSeconds(86400));
                return unitTypeDO;
            }
        } else {
            return BeanUtil.toBean(map, UnitTypeDO.class);
        }
        return null;
    }

    public UnitTypeDO getUnitTypeByName(String unitTypeName) {
        return getUnitTypeAndCache(
                redisson.getBucket(StringConstant.Redis.UNIT_TYPE_NAME + unitTypeName),
                this.lambdaQuery().eq(UnitTypeDO::getName, unitTypeName));
    }

    @Nullable
    private UnitTypeDO getUnitTypeAndCache(@NotNull RBucket<String> rBucket, LambdaQueryChainWrapper<UnitTypeDO> eq) {
        if (!rBucket.isExists()) {
            UnitTypeDO unitTypeDO = eq.one();
            if (unitTypeDO != null) {
                rBucket.set(unitTypeDO.getUnitTypeUuid());
                rBucket.expire(Duration.ofSeconds(86400));
                RMap<String, String> unitTypeMap = redisson
                        .getMap(StringConstant.Redis.UNIT_TYPE_UUID + unitTypeDO.getUnitTypeUuid());
                unitTypeMap.putAll(ConvertUtil.convertObjectToMapString(unitTypeDO));
                unitTypeMap.expire(Duration.ofSeconds(86400));
                return unitTypeDO;
            }
        } else {
            return this.getUnitTypeByUuid(rBucket.get());
        }
        return null;
    }

    public void deleteUnitTypeCache(UnitTypeDO unitTypeDO) {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            transaction.getMap(StringConstant.Redis.UNIT_TYPE_UUID + unitTypeDO.getUnitTypeUuid()).delete();
            transaction.getBucket(StringConstant.Redis.UNIT_TYPE_NAME + unitTypeDO.getName()).delete();
            this.removeById(unitTypeDO);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new BusinessException("缓存清理失败", ErrorCode.OPERATION_ERROR);
        }
    }

    public Page<UnitTypeDO> getPageOfUnitType(int page, int size, boolean isDesc, @Nullable String keyword) {
        RMap<String, String> map = redisson
                .getMap(StringConstant.Redis.UNIT_TYPE_PAGE_OF_LIST + page + ":" + size + ":" + isDesc + ":" + keyword);
        if (!map.isExists()) {
            LambdaQueryChainWrapper<UnitTypeDO> queryWrapper = this.lambdaQuery();
            if (keyword != null && !keyword.isBlank()) {
                queryWrapper
                        .or(i -> i.like(UnitTypeDO::getName, keyword))
                        .or(i -> i.like(UnitTypeDO::getUnitTypeUuid, keyword));
            }
            queryWrapper.orderBy(true, !isDesc, UnitTypeDO::getOrder);
            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            return ProjectUtil.convertMapToPage(map, UnitTypeDO.class);
        }
    }

    /**
     * 获取单位办别列表
     *
     * @return 单位办别列表
     */
    public List<UnitTypeLiteDTO> getUnitTypeList() {
        RList<UnitTypeLiteDTO> typeList = redisson.getList(StringConstant.Redis.UNIT_TYPE_LIST);
        if (!typeList.isExists()) {
            this.lambdaQuery().list().stream()
                    .map(unitTypeDO -> BeanUtil.toBean(unitTypeDO, UnitTypeLiteDTO.class,
                            ProjectOption.stringBlankToNull()))
                    .forEach(typeList::add);
            typeList.expire(Duration.ofSeconds(43200));
        }
        return typeList.readAll();
    }

    /**
     * 添加单位办别
     * <p>
     * 添加单位办别信息，并清除缓存
     *
     * @param unitTypeDO 单位办别DO
     * @return 添加成功返回true，否则返回false
     */
    public boolean addUnitType(UnitTypeDO unitTypeDO) {
        if (this.save(unitTypeDO)) {
            Optional.ofNullable(redisson.getKeys())
                    .ifPresent(keys -> {
                        keys.delete(StringConstant.Redis.UNIT_TYPE_LIST);
                        keys.deleteByPattern(StringConstant.Redis.UNIT_TYPE_PAGE_OF_LIST + "*");
                    });
        }
        return false;
    }
}
