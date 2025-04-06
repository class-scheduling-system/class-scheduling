package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.SemesterMapper;
import com.frontleaves.scheduling.models.entity.base.SemesterDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 学期数据访问对象
 * <p>
 * 该类提供了对学期数据的操作方法，包括从 Redis 或数据库中获取学期信息、更新学期信息等。
 * 通过继承 {@code ServiceImpl} 类并实现 {@code IService} 接口，提供了基础的 CRUD 操作，并扩展了特定的业务逻辑。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SemesterDAO extends ServiceImpl<SemesterMapper, SemesterDO> {
    private final RedissonClient redisson;

    /**
     * 根据学期的唯一标识获取学期信息
     * <p>
     * 该方法首先尝试从 Redis 缓存中获取学期数据。如果缓存中没有找到对应的学期信息，则会从数据库中查询。
     * 查询到的数据会被存入 Redis 缓存中，并设置过期时间为一天（86400秒）。如果在缓存和数据库中都没有找到对应的学期信息，则返回 {@code null}。
     * </p>
     *
     * @param semesterUuid 学期的唯一标识符
     * @return 返回与给定 UUID 对应的学期对象，如果没有找到则返回 {@code null}
     */
    @Nullable
    public SemesterDO getSemesterByUuid(String semesterUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.SEMESTER_UUID + semesterUuid);
        if (map.isEmpty()) {
            SemesterDO entity = this.getById(semesterUuid);
            if (entity != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(entity));
                map.expire(Duration.ofSeconds(86400));
                return entity;
            }
        } else {
            return BeanUtil.toBean(map, SemesterDO.class);
        }
        return null;
    }

    /**
     * 获取分页学期列表
     * <p>
     * 该方法用于获取学期的分页列表，支持按关键字搜索和排序。
     * 结果会被缓存在 Redis 中，缓存时间为一小时。
     * </p>
     *
     * @param page    页码
     * @param size    每页大小
     * @param isDesc  是否降序排序
     * @param keyword 搜索关键字（可选）
     * @return 返回分页的学期列表
     */
    public Page<SemesterDO> getSemesterPage(Integer page, Integer size, Boolean isDesc, String keyword) {
        // 构建缓存键
        String cacheKey = StringConstant.Redis.SEMESTER_PAGE +
                page + ":" + size + ":" + isDesc + ":" +
                (keyword != null ? keyword : "all");

        RMap<String, String> cacheMap = redisson.getMap(cacheKey);
        if (cacheMap.isEmpty()) {
            // 构建查询条件
            LambdaQueryChainWrapper<SemesterDO> wrapper = this.lambdaQuery();
            if (keyword != null && !keyword.isBlank()) {
                wrapper.like(SemesterDO::getName, keyword);
            }
            wrapper.orderBy(true, isDesc, SemesterDO::getCreatedAt);

            return ProjectUtil.queryAndCache(wrapper, page, size, cacheMap);
        } else {
            return ProjectUtil.convertMapToPage(cacheMap, SemesterDO.class);
        }
    }

    /**
     * 获取启用的学期列表
     * <p>
     * 该方法用于获取所有启用状态的学期列表。
     * 结果会被缓存在 Redis 中，缓存时间为一小时。
     * </p>
     *
     * @return 返回启用的学期列表
     */
    public List<SemesterDO> getEnabledSemesters() {
        RList<SemesterDO> cacheList = redisson.getList(StringConstant.Redis.SEMESTER_LIST);
        if (!cacheList.isExists()) {
            // 构建查询条件
            List<SemesterDO> entityList = this.lambdaQuery()
                    .eq(SemesterDO::getIsEnabled, true)
                    .orderByDesc(SemesterDO::getCreatedAt)
                    .list();

            // 如果查询结果为空，返回空列表
            if (entityList.isEmpty()) {
                return new ArrayList<>();
            }

            // 缓存结果
            cacheList.addAll(entityList);
            cacheList.expire(Duration.ofHours(1));

            return entityList;
        }
        return cacheList.readAll();
    }

    /**
     * 更新学期信息
     * <p>
     * 该方法用于更新学期信息，同时会清除相关的 Redis 缓存。
     * </p>
     *
     * @param entity 要更新的学期信息
     */
    public void updateSemester(SemesterDO entity) {
        RKeys keys = redisson.getKeys();
        this.updateById(entity);
        keys.delete(StringConstant.Redis.SEMESTER_UUID + entity.getSemesterUuid());
        keys.delete(StringConstant.Redis.SEMESTER_LIST);
        keys.deleteByPattern(StringConstant.Redis.SEMESTER_PAGE + "*");
    }

    /**
     * 删除学期
     * <p>
     * 该方法用于删除学期信息，同时会清除相关的 Redis 缓存。
     * 如果学期下存在关联数据，将抛出异常。
     * </p>
     *
     * @param semesterUuid 要删除的学期的UUID
     * @throws BusinessException 如果学期下存在关联数据
     */
    @Transactional
    public void removeSemester(String semesterUuid) {
        try {
            RKeys keys = redisson.getKeys();
            this.removeById(semesterUuid);
            keys.delete(StringConstant.Redis.SEMESTER_UUID + semesterUuid);
            keys.delete(StringConstant.Redis.SEMESTER_LIST);
            keys.deleteByPattern(StringConstant.Redis.SEMESTER_PAGE + "*");
        } catch (DataIntegrityViolationException e) {
            if (e.getMessage().contains("cs_class_assignment")) {
                throw new BusinessException("删除学期失败，学期下存在排课信息", ErrorCode.EXISTED);
            } else if (e.getMessage().contains("cs_teacher_preferences")) {
                throw new BusinessException("删除学期失败，学期下存在教师偏好设置", ErrorCode.EXISTED);
            } else {
                log.error("删除学期失败", e);
                throw new BusinessException("删除学期失败", ErrorCode.EXISTED);
            }
        }
    }
}
