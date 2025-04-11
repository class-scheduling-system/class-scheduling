package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.BuildingMapper;
import com.frontleaves.scheduling.models.dto.excel.BackAddBuildingDTO;
import com.frontleaves.scheduling.models.entity.base.BuildingDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
@Slf4j
@Repository
@RequiredArgsConstructor
public class BuildingDAO extends ServiceImpl<BuildingMapper, BuildingDO> {
    private final RedissonClient redisson;

    /**
     * 从 Redis 中删除指定教学楼的相关缓存数据
     * <p>
     * 该方法根据传入的教学楼信息，删除与之相关的所有 Redis 缓存条目。具体来说，它会删除以下几类缓存：
     * <ul>
     *     <li>以 {@code StringConstant.Redis.BUILDING_LIST + "*"} 为模式的所有缓存条目</li>
     *     <li>以 {@code StringConstant.Redis.CLASSROOM_LIST + "*"} 为模式的所有缓存条目</li>
     *     <li>以 {@code StringConstant.Redis.BUILDING_UUID + buildingDO.getBuildingUuid()} 为键的缓存条目</li>
     *     <li>以 {@code StringConstant.Redis.BUILDING_NAME + buildingDO.getBuildingName()} 为键的缓存条目</li>
     *     <li>以 {@code StringConstant.Redis.BUILDING_CAMPUS + buildingDO.getCampusUuid()} 为键的缓存条目</li>
     * </ul>
     * 删除完成后，会记录删除的总条数。
     *
     * @param buildingDO 教学楼的数据对象，包含教学楼的 UUID、名称和校区 UUID 等信息
     */
    private void deleteBuildingRedis(@NotNull BuildingDO buildingDO) {
        RKeys keys = redisson.getKeys();
        long checkTotal = 0;
        checkTotal += keys.deleteByPattern(StringConstant.Redis.BUILDING_LIST + "*");
        checkTotal += keys.deleteByPattern(StringConstant.Redis.CLASSROOM_PAGE + "*");
        checkTotal += keys.delete(StringConstant.Redis.BUILDING_UUID + buildingDO.getBuildingUuid());
        checkTotal += keys.delete(StringConstant.Redis.BUILDING_NAME + buildingDO.getBuildingName());
        checkTotal += keys.delete(StringConstant.Redis.BUILDING_CAMPUS + buildingDO.getCampusUuid());
        log.debug(LogConstant.DAO + "删除教学楼缓存数据，共删除 {} 条数据", checkTotal);
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
    public Page<BuildingDO> getBuildingPage(int page, int size, boolean isDesc, String keyword) {
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
            return ProjectUtil.convertMapToPage(map, BuildingDO.class);
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
            return ProjectUtil.convertMapToPage(map, BuildingDO.class);
        }
    }

    /**
     * 更新教学楼信息
     * <p>
     * 该方法用于更新指定的教学楼信息。首先从 Redis 中删除与该教学楼相关的所有缓存数据，然后在数据库中更新教学楼信息。
     * 通过事务管理确保操作的一致性。
     * </p>
     *
     * @param buildingDO 教学楼实体对象，包含需要更新的教学楼信息
     */
    public void updateBuilding(BuildingDO buildingDO) {
        this.deleteBuildingRedis(buildingDO);
        this.updateById(buildingDO);
    }

    /**
     * 删除教学楼信息
     * <p>
     * 该方法用于删除指定的教学楼信息。首先从 Redis 中删除与该教学楼相关的所有缓存数据，然后从数据库中删除对应的记录。
     * 整个过程在一个事务中进行，确保数据的一致性。如果在操作过程中发生任何异常，将抛出 {@code ServerInternalErrorException} 异常，并回滚事务。
     * </p>
     *
     * @param buildingDO 待删除的教学楼实体对象，包含需要删除的信息
     * @throws ServerInternalErrorException 如果在删除过程中发生异常，则抛出此异常
     */
    public void deleteBuilding(BuildingDO buildingDO) {
        this.deleteBuildingRedis(buildingDO);
        this.removeById(buildingDO.getBuildingUuid());
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

    /**
     * 添加建筑信息
     * <p>
     * 该方法用于向系统中添加一个新的建筑信息。在添加新的建筑之前，会先清除 Redis 中所有与建筑列表相关的缓存数据，以确保数据的一致性。
     * 随后，将新的建筑信息保存到数据库中。
     *
     * @param buildingDO 包含建筑详细信息的对象 {@code BuildingDO}
     */
    public void addBuilding(BuildingDO buildingDO) {
        Optional.ofNullable(redisson.getKeys())
                .ifPresent(keys -> keys.deleteByPattern(StringConstant.Redis.BUILDING_LIST + "*"));
        this.save(buildingDO);
    }


    /**
     * 根据关键字获取建筑列表
     * 首先尝试从Redis缓存中获取数据，如果缓存不存在，则从数据库中查询，并将结果缓存到Redis中
     *
     * @param keyword 搜索关键字，用于模糊查询建筑名称
     * @return 返回建筑列表，如果找不到则返回null
     */
    @Nullable
    public List<BuildingDO> getBuildingListByKey(@Nullable String keyword) {
        RList<BuildingDO> rList;
        if (keyword == null || keyword.isBlank()) {
            rList = redisson.getList(StringConstant.Redis.BUILDING_LIST);
        } else {
            rList = redisson.getList(StringConstant.Redis.BUILDING_KEY_LIST + keyword);
        }
        if (!rList.isExists()) {
            // 构建查询条件
            LambdaQueryChainWrapper<BuildingDO> queryWrapper = this.lambdaQuery()
                    .eq(BuildingDO::getStatus, 1);
            if (keyword != null && !keyword.isBlank()) {
                queryWrapper.like(BuildingDO::getBuildingName, keyword);
            }
            if (queryWrapper.exists()) {
                rList.addAll(queryWrapper.list());
                rList.expire(Duration.ofSeconds(3600));
                return rList.readAll();
            }
        } else {
            return rList.readAll();
        }
        return null;
    }

    /**
     * 保存教学楼信息，遇到错误时抛出异常
     * <p>
     * 该方法尝试保存教学楼信息，并处理可能出现的各种异常，将其转换为业务异常抛出。
     * </p>
     *
     * @param buildingDO 教学楼实体对象
     * @param i 行号索引
     * @throws BusinessException 当保存过程中发生异常时抛出，并包含详细的错误信息
     */
    public void saveBuildingBackError(BuildingDO buildingDO, int i) {
        try {
            this.addBuilding(buildingDO);
        } catch (DuplicateKeyException e) {
            // 教学楼名称重复异常
            log.error("教学楼名称重复", e);
            throw new BusinessException("第" + (i + 3) + "行教学楼名称重复，请检查", ErrorCode.BODY_ERROR);
        } catch (DataIntegrityViolationException e) {
            // 分析数据完整性异常的具体原因
            String errorMessage = e.getMessage();
            String detailedReason = analyzeBuildingDataError(errorMessage);
            log.error("数据完整性错误", e);
            throw new BusinessException("第" + (i + 3) + "行" + detailedReason, ErrorCode.BODY_ERROR);
        } catch (Exception e) {
            // 其他未预期的异常
            throw new BusinessException("第" + (i + 3) + "行保存失败：" + e.getMessage(), ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 分析教学楼数据完整性错误的详细原因
     *
     * @param errorMessage 错误信息
     * @return 具体的错误原因
     */
    private String analyzeBuildingDataError(String errorMessage) {
        // 外键错误映射
        Map<String, String> foreignKeyErrors = Map.of(
                "fk_cs_building_cs_campus", "校区信息错误"
        );

        // 检查外键错误
        for (Map.Entry<String, String> entry : foreignKeyErrors.entrySet()) {
            if (errorMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 长度错误检查
        if (errorMessage.contains("Data too long")) {
            if (errorMessage.contains("building_name")) {
                return "教学楼名称长度超出限制，最大64个字符";
            }
            return "数据长度超出限制";
        }

        // 默认错误信息
        return "数据错误：可能包含错误的值（意料之外的报错）";
    }

    /**
     * 保存教学楼信息，忽略错误并返回失败详情
     * <p>
     * 该方法尝试保存教学楼信息，发生异常时不抛出，而是收集错误信息并返回。
     * </p>
     *
     * @param buildingDO 教学楼实体对象
     * @param i 当前处理的行索引
     * @return 失败详情列表，如果成功则返回空列表
     */
    public List<BackAddBuildingDTO.FailedDetail> saveBuildingIgnoreError(BuildingDO buildingDO, int i) {
        try {
            this.save(buildingDO);
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.singletonList(createBuildingFailedDetail(e, i));
        }
    }

    /**
     * 根据异常创建教学楼失败详情
     *
     * @param e 异常对象
     * @param i 行索引
     * @return 失败详情对象
     */
    private BackAddBuildingDTO.FailedDetail createBuildingFailedDetail(Exception e, int i) {
        BackAddBuildingDTO.FailedDetail failedDetail = new BackAddBuildingDTO.FailedDetail();
        failedDetail.setRow(i + 3);

        if (e instanceof DuplicateKeyException) {
            failedDetail.setReason("教学楼名称重复");
        } else if (e instanceof DataIntegrityViolationException) {
            String errorMessage = e.getMessage();
            failedDetail.setReason(analyzeBuildingDataIntegrityError(errorMessage));
        } else {
            failedDetail.setReason("保存失败：" + e.getMessage());
        }

        return failedDetail;
    }

    /**
     * 分析教学楼数据完整性错误（详细版本）
     *
     * @param errorMessage 错误信息
     * @return 格式化的错误原因
     */
    private String analyzeBuildingDataIntegrityError(String errorMessage) {
        // 外键错误映射
        Map<String, String> foreignKeyErrors = Map.of(
                "fk_cs_building_cs_campus", "校区信息错误"
        );

        // 检查外键错误
        String foreignKeyMatch = foreignKeyErrors.entrySet().stream()
                .filter(entry -> errorMessage.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);

        if (foreignKeyMatch != null) {
            return foreignKeyMatch;
        }

        // 长度错误检查
        if (errorMessage.contains("Data too long")) {
            Map<String, String> lengthErrors = Map.of(
                    "building_name", "教学楼名称长度超出限制，最大64个字符",
                    "campus_uuid", "校区ID格式错误"
            );

            return lengthErrors.entrySet().stream()
                    .filter(entry -> errorMessage.contains(entry.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse("数据长度超出限制");
        }

        // 默认错误信息
        return "数据错误：可能包含空值、超出长度限制或不符合外键约束";
    }

    /**
     * 删除所有教学楼缓存数据
     * <p>
     * 该方法用于删除 Redis 中所有与教学楼相关的缓存数据，包括教学楼列表、教学楼 UUID、名称和校区等信息。
     * </p>
     */
    public void deleteBuildingCache() {
        RKeys keys = redisson.getKeys();
        keys.deleteByPattern(StringConstant.Redis.BUILDING_LIST + "*");
        keys.deleteByPattern(StringConstant.Redis.CLASSROOM_PAGE + "*");
        keys.deleteByPattern(StringConstant.Redis.BUILDING_UUID + "*");
        keys.deleteByPattern(StringConstant.Redis.BUILDING_NAME + "*");
        keys.deleteByPattern(StringConstant.Redis.BUILDING_CAMPUS + "*");
        log.debug(LogConstant.DAO + "删除教学楼缓存数据成功");
    }
}
