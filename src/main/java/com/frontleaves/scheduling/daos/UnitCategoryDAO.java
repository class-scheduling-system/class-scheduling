package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.UnitCategoryMapper;
import com.frontleaves.scheduling.models.dto.UnitCategoryLiteDTO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.*;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 单位类别数据访问对象
 * <p>
 * 该类实现了对单位类别数据的增删改查操作，并提供了通过单位类别UUID和名称获取单位类别信息的方法。
 * 同时，利用Redis进行数据缓存，以提高查询效率。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 */
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
                    return unitCategoryDO;
                }
                transaction.commit();
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

    /**
     * 根据单位类别名称获取单位类别，排除指定的UUID
     * <p>
     * 该方法用于检查更新单位类别时名称是否与其他单位类别重复
     * </p>
     *
     * @param name 单位类别名称
     * @param uuid 要排除的UUID
     * @return 单位类别对象，如果找不到则返回null
     */
    public UnitCategoryDO getUnitCategoryByNameExceptUuid(String name, String uuid) {
        return this.lambdaQuery()
                .eq(UnitCategoryDO::getName, name)
                .ne(UnitCategoryDO::getUnitCategoryUuid, uuid)
                .one();
    }

    /**
     * 删除单位类别相关缓存
     * <p>
     * 该方法在删除或更新单位类别信息时，清除相关的Redis缓存
     * </p>
     *
     * @param unitCategoryDO 单位类别DO
     */
    public void deleteUnitCategoryCache(UnitCategoryDO unitCategoryDO) {
        RKeys keys = redisson.getKeys();
        // 删除通过UUID存储的缓存
        keys.delete(StringConstant.Redis.UNIT_CATEGORY_UUID + unitCategoryDO.getUnitCategoryUuid());
        // 删除通过名称存储的缓存
        keys.delete(StringConstant.Redis.UNIT_CATEGORY_NAME + unitCategoryDO.getName());
        // 删除单位类别列表缓存
        keys.delete(StringConstant.Redis.UNIT_CATEGORY_LIST);
        // 删除单位类别分页缓存
        keys.deleteByPattern(StringConstant.Redis.UNIT_CATEGORY_PAGE + "*");
    }

    /**
     * 获取单位类别分页数据
     * <p>
     * 该方法根据给定的分页参数和查询条件，获取单位类别的分页数据
     * </p>
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param isDesc   是否降序
     * @param keyword  关键词
     * @return 分页数据
     */
    public Page<UnitCategoryDO> getPageOfUnitCategory(Integer pageNum, Integer pageSize, Boolean isDesc, String keyword) {
        String cacheKey = StringConstant.Redis.UNIT_CATEGORY_PAGE + pageNum + ":" + pageSize + ":" + isDesc + ":" + keyword;
        RMap<String, String> map = redisson.getMap(cacheKey);

        if (!map.isExists()) {
            LambdaQueryChainWrapper<UnitCategoryDO> queryWrapper = this.lambdaQuery();

            // 添加关键词搜索条件
            if (keyword != null && !keyword.isBlank()) {
                queryWrapper.like(UnitCategoryDO::getName, keyword)
                        .or(i -> i.like(UnitCategoryDO::getEnglishName, keyword))
                        .or(i -> i.like(UnitCategoryDO::getShortName, keyword));
            }

            // 添加排序
            queryWrapper.orderBy(true, isDesc, UnitCategoryDO::getCreatedAt);

            return ProjectUtil.queryAndCache(queryWrapper, pageNum, pageSize, map);
        } else {
            return ProjectUtil.convertMapToPage(map, UnitCategoryDO.class);
        }
    }

    /**
     * 获取单位类别列表
     * <p>
     * 该方法用于获取所有单位类别的精简列表信息，优先从缓存获取
     * </p>
     *
     * @return 单位类别列表
     */
    public List<UnitCategoryLiteDTO> getUnitCategoryList() {
        RList<UnitCategoryLiteDTO> categoryList = redisson.getList(StringConstant.Redis.UNIT_CATEGORY_LIST);

        if (!categoryList.isExists()) {
            this.lambdaQuery()
                    .orderByAsc(UnitCategoryDO::getOrder)
                    .list()
                    .stream()
                    .map(unitCategoryDO -> {
                        UnitCategoryLiteDTO dto = new UnitCategoryLiteDTO();
                        dto.setUnitCategoryUuid(unitCategoryDO.getUnitCategoryUuid());
                        dto.setName(unitCategoryDO.getName());
                        dto.setShortName(unitCategoryDO.getShortName());
                        dto.setOrder(unitCategoryDO.getOrder());
                        dto.setIsEntity(unitCategoryDO.getIsEntity());
                        return dto;
                    })
                    .forEach(categoryList::add);
            categoryList.expire(Duration.ofSeconds(86400));

            return categoryList.readAll();
        }

        return categoryList.readAll();
    }

    public UnitCategoryDO updateUnitCategory(UnitCategoryDO unitCategoryDO) {
        this.deleteUnitCategoryCache(unitCategoryDO);
        if (!this.updateById(unitCategoryDO)) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
        return this.getUnitCategoryByUuid(unitCategoryDO.getUnitCategoryUuid());
    }

    public UnitCategoryDO saveUnitCategory(@NotNull UnitCategoryDO unitCategoryDO) {
        Optional.ofNullable(redisson.getKeys())
                .ifPresent(keys -> {
                    keys.delete(StringConstant.Redis.UNIT_CATEGORY_LIST);
                    keys.deleteByPattern(StringConstant.Redis.UNIT_CATEGORY_PAGE + "*");
                });
        String newNoDashUuid = UuidUtil.generateUuidNoDash();
        unitCategoryDO.setUnitCategoryUuid(newNoDashUuid);
        if (!this.save(unitCategoryDO)) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }

        return this.getUnitCategoryByUuid(newNoDashUuid);
    }
}
