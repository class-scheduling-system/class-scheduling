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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.SchedulingConflictMapper;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.entity.base.SchedulingConflictDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 排课冲突数据访问对象
 * 使用MybatisPlus处理排课冲突的数据库操作
 *
 * @author xiao_lfeng
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SchedulingConflictDAO extends ServiceImpl<SchedulingConflictMapper, SchedulingConflictDO> {

    private final RedissonClient redisson;
    /**
     * 批量保存排课冲突信息
     *
     * @param conflictDTOList 排课冲突DTO列表
     * @param semesterUuid 学期UUID
     * @return 保存成功的数量
     */
    @Transactional
    public int batchSaveConflicts(List<SchedulingConflictDTO> conflictDTOList, String semesterUuid) {
        if (conflictDTOList == null || conflictDTOList.isEmpty()) {
            log.info("没有排课冲突需要保存");
            return 0;
        }

        log.info("开始保存{}条排课冲突信息到数据库", conflictDTOList.size());

        List<SchedulingConflictDO> entityList = new ArrayList<>(conflictDTOList.size());
        int updatedCount = 0;

        for (SchedulingConflictDTO dto : conflictDTOList) {
            // 检查冲突是否已存在（无论顺序）
            SchedulingConflictDO existingConflict = checkConflictExists(
                    dto.getFirstAssignmentUuid(),
                    dto.getSecondAssignmentUuid(),
                    dto.getConflictType());

            if (existingConflict != null) {
                // 如果冲突已存在，更新冲突信息而不是创建新记录
                existingConflict.setConflictTime(cn.hutool.json.JSONUtil.parseObj(dto.getConflictTime()));
                existingConflict.setDescription(dto.getDescription());
                existingConflict.setResolutionStatus(0);
                this.updateById(existingConflict);
                updatedCount++;
            } else {
                // 如果冲突不存在，创建新记录
                SchedulingConflictDO conflictDO = BeanUtil.toBean(dto, SchedulingConflictDO.class);
                // 同样需要转换TimeSlotDTO
                if (dto.getConflictTime() != null) {
                    conflictDO.setConflictTime(cn.hutool.json.JSONUtil.parseObj(dto.getConflictTime()));
                }
                entityList.add(conflictDO);
            }
        }

        if (!entityList.isEmpty()) {
            // 使用MybatisPlus批量保存
            boolean success = this.saveBatch(entityList);
            if (success) {
                log.info("批量保存排课冲突信息成功, 共{}条新增, {}条更新", entityList.size(), updatedCount);

                // 清除所有相关缓存
                RKeys rKeys = redisson.getKeys();
                // 清除冲突列表缓存
                rKeys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST + "*");
                // 清除与排课安排关联的冲突列表缓存
                rKeys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST_CLASS_ASSIGNMENT + "*");
                log.info("排课冲突相关缓存已清除");

                return entityList.size() + updatedCount;
            } else {
                log.error("批量保存排课冲突信息失败");
                return updatedCount;
            }
        }

        return updatedCount;
    }

    /**
     * 检查冲突是否已存在
     * 注意：会检查两个排课安排无论顺序的冲突
     *
     * @param firstAssignmentUuid 第一个排课安排UUID
     * @param secondAssignmentUuid 第二个排课安排UUID
     * @param conflictType 冲突类型
     * @return 已存在的冲突对象，不存在则返回null
     */
    private SchedulingConflictDO checkConflictExists(String firstAssignmentUuid, String secondAssignmentUuid, Integer conflictType) {
        // 查询可能的两种排列方式
        return this.lambdaQuery()
                .eq(SchedulingConflictDO::getConflictType, conflictType)
                .and(q -> q
                        .or(i -> i
                                .eq(SchedulingConflictDO::getFirstAssignmentUuid, firstAssignmentUuid)
                                .eq(SchedulingConflictDO::getSecondAssignmentUuid, secondAssignmentUuid))
                        .or(i -> i
                                .eq(SchedulingConflictDO::getFirstAssignmentUuid, secondAssignmentUuid)
                                .eq(SchedulingConflictDO::getSecondAssignmentUuid, firstAssignmentUuid))
                )
                .last("LIMIT 1")
                .one();
    }

    /**
     * 根据班级安排的UUID获取冲突列表
     * @param classAssignmentUuid 排课安排的UUID
     * @return 冲突列表
     */
    public List<SchedulingConflictDO> getConflictByClassAssignmentUuid(String classAssignmentUuid) {
        RList<SchedulingConflictDO> list = redisson.getList(StringConstant.Redis.SCHEDULING_CONFLICT_LIST_CLASS_ASSIGNMENT + classAssignmentUuid);
        if (!list.isExists()) {
            List<SchedulingConflictDO> getList = this.lambdaQuery()
                            .eq(SchedulingConflictDO::getResolutionStatus,0)
                            .or(i -> i.eq(SchedulingConflictDO::getFirstAssignmentUuid, classAssignmentUuid))
                            .or(i -> i.eq(SchedulingConflictDO::getSecondAssignmentUuid,classAssignmentUuid))
                            .list();
            list.addAll(getList);
            list.expire(Duration.ofHours(1));
            return getList;
        } else {
            return list.readAll();
        }
    }

    /**0
     * 获取学期的冲突列表
     * @param currentSemesterUuid 学期UUID
     * @return 冲突列表
     */
    public List<SchedulingConflictDO> getConflictListBySemester(String currentSemesterUuid) {
        RList<SchedulingConflictDO> list = redisson.getList(StringConstant.Redis.SCHEDULING_CONFLICT_LIST);
        if (!list.isExists()) {
            List<SchedulingConflictDO> getList = this.lambdaQuery()
                            .eq(SchedulingConflictDO::getResolutionStatus,0)
                            .eq(SchedulingConflictDO::getSemesterUuid, currentSemesterUuid)
                            .list();
            list.addAll(getList);
            list.expire(Duration.ofHours(1));
            return getList;
        } else {
            return list.readAll();
        }
    }

    /**
     * 删除冲突记录
     * @param conflict 排课冲突对象
     * @return 是否删除成功
     */
    @Transactional
    public boolean deleteConflict(SchedulingConflictDO conflict) {
        boolean success = this.removeById(conflict.getConflictUuid());
        if (success) {
            // 清除所有相关缓存
            RKeys rKeys = redisson.getKeys();
            // 清除冲突列表缓存
            rKeys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST + "*");
            // 清除与排课安排关联的冲突列表缓存
            rKeys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST_CLASS_ASSIGNMENT + "*");
            log.info("排课冲突相关缓存已清除");
        }
        return success;
    }
}
