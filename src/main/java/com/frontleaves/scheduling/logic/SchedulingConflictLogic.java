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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.SchedulingConflictDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.entity.base.SchedulingConflictDO;
import com.frontleaves.scheduling.services.SchedulingConflictService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 排课冲突逻辑实现类
 * <p>
 * 该类提供了排课冲突相关的业务逻辑实现，包括查询冲突详情、分页查询冲突列表等功能。
 * 所有操作仅提供查询功能，不提供修改、删除等操作。
 * 同时实现了SchedulingConflictService接口，直接作为服务层实现类。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingConflictLogic implements SchedulingConflictService {

    private final SchedulingConflictDAO schedulingConflictDAO;

    /**
     * 获取排课冲突详情
     *
     * @param conflictUuid 冲突UUID
     * @return 排课冲突DTO
     */
    @Override
    public SchedulingConflictDTO getConflictDetail(String conflictUuid) {
        if (conflictUuid == null || conflictUuid.isBlank()) {
            throw new BusinessException("冲突UUID不能为空", ErrorCode.BODY_ERROR);
        }

        SchedulingConflictDO conflictDO = schedulingConflictDAO.getById(conflictUuid);
        if (conflictDO == null) {
            throw new BusinessException("未找到该冲突记录", ErrorCode.BODY_ERROR);
        }

        return BeanUtil.toBean(conflictDO, SchedulingConflictDTO.class);
    }

    /**
     * 分页查询排课冲突列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param semesterUuid 学期UUID
     * @param conflictType 冲突类型
     * @param resolutionStatus 解决状态
     * @return 分页数据
     */
    @Override
    public PageDTO<SchedulingConflictDTO> page(Integer page, Integer size, 
                                              String semesterUuid,
                                              Integer conflictType, 
                                              Integer resolutionStatus) {
        LambdaQueryWrapper<SchedulingConflictDO> queryWrapper = new LambdaQueryWrapper<>();
        
        // 构建查询条件
        if (semesterUuid != null && !semesterUuid.isBlank()) {
            queryWrapper.eq(SchedulingConflictDO::getSemesterUuid, semesterUuid);
        }
        
        if (conflictType != null) {
            queryWrapper.eq(SchedulingConflictDO::getConflictType, conflictType);
        }
        
        if (resolutionStatus != null) {
            queryWrapper.eq(SchedulingConflictDO::getResolutionStatus, resolutionStatus);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(SchedulingConflictDO::getCreatedAt);
        
        try {
            // 执行分页查询
            Page<SchedulingConflictDO> resultPage = schedulingConflictDAO.page(
                    new Page<>(page, size), queryWrapper);
            
            // 转换为DTO列表
            List<SchedulingConflictDTO> dtoList = new ArrayList<>();
            for (SchedulingConflictDO entity : resultPage.getRecords()) {
                dtoList.add(BeanUtil.toBean(entity, SchedulingConflictDTO.class));
            }
            
            // 构建并返回PageDTO
            PageDTO<SchedulingConflictDTO> pageDTO = new PageDTO<>(resultPage.getTotal(), resultPage.getSize());
            pageDTO.setCurrent(resultPage.getCurrent());
            pageDTO.setRecords(dtoList);
            
            return pageDTO;
        } catch (Exception e) {
            log.error("分页查询排课冲突列表失败", e);
            throw new BusinessException("查询排课冲突列表失败", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 获取简单冲突列表
     *
     * @param semesterUuid 学期UUID
     * @param resolutionStatus 解决状态
     * @return 冲突列表
     */
    @Override
    public List<SchedulingConflictDTO> listSimple(String semesterUuid, Integer resolutionStatus) {
        if (semesterUuid == null || semesterUuid.isBlank()) {
            throw new BusinessException("学期UUID不能为空", ErrorCode.BODY_ERROR);
        }

        LambdaQueryWrapper<SchedulingConflictDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SchedulingConflictDO::getSemesterUuid, semesterUuid);
        
        if (resolutionStatus != null) {
            queryWrapper.eq(SchedulingConflictDO::getResolutionStatus, resolutionStatus);
        }
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(SchedulingConflictDO::getCreatedAt);
        
        try {
            List<SchedulingConflictDO> entities = schedulingConflictDAO.list(queryWrapper);
            List<SchedulingConflictDTO> dtoList = new ArrayList<>();
            
            for (SchedulingConflictDO entity : entities) {
                dtoList.add(BeanUtil.toBean(entity, SchedulingConflictDTO.class));
            }
            
            return dtoList;
        } catch (Exception e) {
            log.error("查询简单冲突列表失败", e);
            throw new BusinessException("查询冲突列表失败", ErrorCode.BODY_ERROR);
        }
    }
} 