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
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.ClassroomMapper;
import com.frontleaves.scheduling.models.entity.ClassroomDO;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 教室标签数据访问对象
 * <p>
 * 该类扩展了 {@code ServiceImpl}，并实现了 {@code IService<ClassroomTagDO>} 接口，
 * 用于对教室标签实体 {@code ClassroomTagDO} 进行数据库操作。通过继承自定义的
 * {@code ClassroomTagMapper}，提供了对教室标签表的基本 CRUD 操作以及其他业务逻辑方法。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see ClassroomDO
 * @see ClassroomMapper
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class ClassroomDAO extends ServiceImpl<ClassroomMapper, ClassroomDO> {
    private final RedissonClient redisson;

    /**
     * 获取教室分页数据
     * <p>
     * 该方法用于从数据库中获取符合条件的教室信息，并返回分页结果。支持根据关键字、标签和类型进行过滤，同时可以指定排序方式。
     * 如果 Redis 中存在缓存，则直接从缓存中读取数据并返回；否则，从数据库查询并将结果缓存到 Redis 中。
     * </p>
     *
     * @param page    分页页码，从1开始
     * @param size    每页显示的记录数
     * @param isDesc  是否降序排列，true表示降序，false表示升序
     * @param keyword 搜索关键字，用于匹配教室名称或编号，可为空
     * @param tag     教室标签，用于精确匹配，可为空
     * @param type    教室类型，用于精确匹配，可为空
     * @return 返回包含教室信息的分页对象 {@code Page<ClassroomDO>}
     */
    public Page<ClassroomDO> getClassroomPage(
            int page,
            int size,
            boolean isDesc,
            @Nullable String keyword,
            @Nullable String tag,
            @Nullable String type
    ) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.CLASSROOM_PAGE + page + ":" + size + ":" + isDesc + ":" + keyword + ":" + tag + ":" + type);
        if (!map.isExists()) {
            LambdaQueryChainWrapper<ClassroomDO> queryWrapper = this.lambdaQuery();
            if (tag != null) {
                // tag 存储 JSON 字符串，使用 like 进行模糊匹配
                queryWrapper.like(ClassroomDO::getTag, tag);
            }
            if (type != null) {
                queryWrapper.eq(ClassroomDO::getType, type);
            }
            if (keyword != null) {
                queryWrapper
                        .or(i -> i.like(ClassroomDO::getName, keyword))
                        .or(i -> i.like(ClassroomDO::getNumber, keyword));
            }
            if (isDesc) {
                queryWrapper.orderByDesc(ClassroomDO::getCreatedAt);
            } else {
                queryWrapper.orderByAsc(ClassroomDO::getCreatedAt);
            }
            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            return ProjectUtil.convertMapToPage(map, ClassroomDO.class);
        }
    }

    /**
     * 根据教室 UUID 获取教室信息
     * <p>
     * 该方法通过传入的教室 UUID 从 Redis 缓存中查找教室信息。如果缓存中不存在，则从数据库中查询，并将结果缓存到 Redis 中。
     * 如果数据库中也不存在对应的教室信息，则返回 null。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符 {@code String}
     * @return 返回教室信息，如果找不到则返回 null {@code ClassroomDO}
     */
    @Nullable
    public ClassroomDO getClassroomByUuid(String classroomUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.CLASSROOM_UUID + classroomUuid);
        if (!map.isExists()) {
            ClassroomDO classroomDO = this.getById(classroomUuid);
            if (classroomDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(classroomDO));
                map.expire(Duration.ofSeconds(3600));
                return classroomDO;
            }
        } else {
            return BeanUtil.toBean(map, ClassroomDO.class, ProjectOption.stringBlankToNull());
        }
        return null;
    }

    /**
     * 根据教室编号获取教室信息
     * <p>
     * 该方法通过传入的教室编号从 Redis 缓存中查找教室信息。如果缓存中不存在，则从数据库中查询，并将结果缓存到 Redis 中。
     * 如果数据库中也不存在对应的教室信息，则返回 null。
     * </p>
     *
     * @param number 教室编号 {@code String}
     * @return 返回教室信息，如果找不到则返回 null {@code ClassroomDO}
     */
    public ClassroomDO getClassroomByNumber(String number) {
        RBucket<String> bucket = redisson.getBucket(StringConstant.Redis.CLASSROOM_NUMBER + number);
        if (!bucket.isExists()) {
            ClassroomDO classroomDO = this.lambdaQuery().eq(ClassroomDO::getNumber, number).one();
            if (classroomDO != null) {
                bucket.set(classroomDO.getClassroomUuid());
                bucket.expire(Duration.ofSeconds(3600));
                Optional.ofNullable(redisson.getMap(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getNumber()))
                        .ifPresent(map -> {
                            map.putAll(ConvertUtil.convertObjectToMapString(classroomDO));
                            map.expire(Duration.ofSeconds(3600));
                        });
                return classroomDO;
            }
        } else {
            return this.getById(bucket.get());
        }
        return null;
    }

    /**
     * 更新教室信息
     * <p>
     * 该方法用于更新教室信息。首先会删除 Redis 中与该教室相关的缓存，然后调用 MyBatis-Plus 的 updateClassAssignment 方法将更新后的教室信息保存到数据库中。
     * </p>
     *
     * @param classroomDO 要更新的教室实体对象 {@code ClassroomDO}
     */
    public void updateClassroom(ClassroomDO classroomDO) {
        Optional.of(redisson.getKeys())
                .ifPresent(rKeys -> {
                    rKeys.delete(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getClassroomUuid());
                    rKeys.delete(StringConstant.Redis.CLASSROOM_NUMBER + classroomDO.getNumber());
                });
        this.updateById(classroomDO);
    }

    /**
     * 删除教室信息
     * <p>
     * 该方法用于根据传入的教室 UUID 删除对应的教室信息。首先会从数据库中查找该教室信息，如果找到，则删除 Redis 中与该教室相关的缓存，
     * 然后从数据库中删除该教室记录。如果删除成功，返回 true；否则，返回 false。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符 {@code String}
     * @return 如果删除成功，返回 true；否则，返回 false
     */
    public boolean deleteClassroom(String classroomUuid) {
        ClassroomDO classroomDO = this.getClassroomByUuid(classroomUuid);
        if (classroomDO != null) {
            Optional.ofNullable(redisson.getKeys())
                    .ifPresent(rKeys -> {
                        rKeys.delete(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getClassroomUuid());
                        rKeys.delete(StringConstant.Redis.CLASSROOM_NUMBER + classroomDO.getNumber());
                        rKeys.deleteByPattern(StringConstant.Redis.CLASSROOM_STATUS + "*");
                        rKeys.deleteByPattern(StringConstant.Redis.CLASSROOM_PAGE + "*");
                    });
            this.removeById(classroomDO);
            return true;
        }
        return false;
    }

    /**
     * 根据状态获取教室列表
     * <p>
     * 该方法用于获取指定状态的所有教室信息。首先会尝试从 Redis 缓存中获取数据，
     * 如果缓存中不存在，则从数据库中查询并将结果缓存到 Redis 中。
     * </p>
     *
     * @param status 教室状态（true 表示启用，false 表示禁用）
     * @return 返回符合状态条件的教室列表
     */
    @Nullable
    public List<ClassroomDO> getClassroomByStatus(boolean status) {
        RList<ClassroomDO> list = redisson.getList(StringConstant.Redis.CLASSROOM_STATUS + status);
        if (!list.isExists()) {
            List<ClassroomDO> classrooms = this.lambdaQuery()
                    .eq(ClassroomDO::getStatus, status)
                    .orderByAsc(ClassroomDO::getNumber)
                    .list();
            if (!classrooms.isEmpty()) {
                list.addAll(classrooms);
                list.expire(Duration.ofSeconds(3600));
                return classrooms;
            }
        } else {
            return list.readAll();
        }
        return null;
    }

    /**
     * 根据建筑UUID获取教室列表
     * 首先尝试从Redis中获取缓存数据，如果缓存不存在，则从数据库中查询数据，
     * 并将查询结果缓存到Redis中，以提高下次查询效率
     * @param buildingUuid 建筑的唯一标识符
     * @return 返回教室列表，如果找不到则返回空列表
     */
    @Nullable
    public List<ClassroomDO> getClassroomByBuilding(String buildingUuid) {
        // 从Redis中获取缓存的教室列表
        RList<ClassroomDO> rList = redisson.getList(StringConstant.Redis.CLASSROOM_BUILDING + buildingUuid);
        if (!rList.isExists()){
            // 如果缓存不存在，从数据库中查询教室列表
            List<ClassroomDO> classroomDOList = this.lambdaQuery().eq(ClassroomDO::getBuildingUuid, buildingUuid).list();
            if (!classroomDOList.isEmpty()){
                // 如果查询结果不为空，将其添加到Redis缓存中，并设置过期时间
                rList.addAll(classroomDOList);
                rList.expire(Duration.ofSeconds(3600));
                return classroomDOList;
            }
            // 如果查询结果为空，返回空列表
            return null;
        }
        // 如果缓存存在，读取并返回缓存中的教室列表
        return rList.readAll();
    }

}
