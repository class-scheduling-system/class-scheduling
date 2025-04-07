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
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.GradeDAO;
import com.frontleaves.scheduling.models.dto.base.GradeDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.entity.base.GradeDO;
import com.frontleaves.scheduling.services.GradeService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 年级业务逻辑实现类
 * <p>
 * 该类提供了年级相关的业务逻辑实现，包括创建、更新、删除、查询等功能。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeLogic implements GradeService {

    private final GradeDAO gradeDAO;
    private final RedissonClient redisson;

    /**
     * 创建年级
     *
     * @param gradeDTO 年级信息
     * @return 创建后的年级信息，包含主键
     */
    @Override
    @Transactional
    public GradeDTO createGrade(GradeDTO gradeDTO) {
        log.debug("创建年级: {}", gradeDTO.getName());
        // 参数校验
        this.validateGradeParams(gradeDTO);
        
        // 检查同名年级是否存在
        GradeDO existingGradeByName = gradeDAO.getGradeByName(gradeDTO.getName());
        if (existingGradeByName != null) {
            throw new BusinessException(String.format("年级名称 %s 已存在", gradeDTO.getName()), ErrorCode.BODY_ERROR);
        }

        // 检查相同入学年份是否存在
        if (gradeDTO.getYear() != null) {
            GradeDO existingGradeByYear = gradeDAO.getGradeByYear(gradeDTO.getYear());
            if (existingGradeByYear != null) {
                throw new BusinessException(String.format("入学年份 %s 已存在", gradeDTO.getYear()), ErrorCode.BODY_ERROR);
            }
        }

        // 转换DTO为DO
        GradeDO gradeDO = BeanUtil.toBean(gradeDTO, GradeDO.class);
        
        // 保存到数据库
        boolean success = gradeDAO.save(gradeDO);
        if (!success) {
            throw new BusinessException("创建年级失败", ErrorCode.OPERATION_FAILED);
        }
        
        // 清除缓存
        this.clearGradeCache();
        log.debug("年级创建成功: {} (uuid: {})", gradeDO.getName(), gradeDO.getGradeUuid());
        
        // 返回结果
        return BeanUtil.toBean(gradeDO, GradeDTO.class);
    }

    /**
     * 更新年级信息
     *
     * @param gradeDTO 年级信息
     * @return 更新后的年级信息
     */
    @Override
    @Transactional
    public GradeDTO updateGrade(GradeDTO gradeDTO) {
        log.debug("更新年级: {}", gradeDTO.getGradeUuid());
        // 检查必要参数
        if (gradeDTO == null || StrUtil.isBlank(gradeDTO.getGradeUuid())) {
            throw new BusinessException("年级UUID不能为空", ErrorCode.BODY_ERROR);
        }
        
        // 检查年级是否存在
        GradeDO existingGrade = gradeDAO.getGradeByUuid(gradeDTO.getGradeUuid());
        if (existingGrade == null) {
            throw new BusinessException("年级不存在", ErrorCode.BODY_ERROR);
        }
        
        // 检查同名年级是否存在（排除自身）
        if (StrUtil.isNotBlank(gradeDTO.getName()) && !Objects.equals(gradeDTO.getName(), existingGrade.getName())) {
            GradeDO existingGradeByName = gradeDAO.getGradeByName(gradeDTO.getName());
            if (existingGradeByName != null && !existingGradeByName.getGradeUuid().equals(gradeDTO.getGradeUuid())) {
                throw new BusinessException(String.format("年级名称 %s 已存在", gradeDTO.getName()), ErrorCode.BODY_ERROR);
            }
        }
        
        // 检查相同入学年份是否存在（排除自身）
        if (gradeDTO.getYear() != null && !Objects.equals(gradeDTO.getYear(), existingGrade.getYear())) {
            GradeDO existingGradeByYear = gradeDAO.getGradeByYear(gradeDTO.getYear());
            if (existingGradeByYear != null && !existingGradeByYear.getGradeUuid().equals(gradeDTO.getGradeUuid())) {
                throw new BusinessException(String.format("入学年份 %s 已存在", gradeDTO.getYear()), ErrorCode.BODY_ERROR);
            }
        }
        
        // 更新非空字段
        if (StrUtil.isNotBlank(gradeDTO.getName())) {
            existingGrade.setName(gradeDTO.getName());
        }
        if (gradeDTO.getYear() != null) {
            existingGrade.setYear(gradeDTO.getYear());
        }
        if (gradeDTO.getStartDate() != null) {
            existingGrade.setStartDate(gradeDTO.getStartDate());
        }
        if (gradeDTO.getEndDate() != null) {
            existingGrade.setEndDate(gradeDTO.getEndDate());
        }
        if (StrUtil.isNotBlank(gradeDTO.getDescription())) {
            existingGrade.setDescription(gradeDTO.getDescription());
        }
        
        // 更新到数据库
        boolean success = gradeDAO.updateById(existingGrade);
        if (!success) {
            throw new BusinessException("更新年级失败", ErrorCode.OPERATION_FAILED);
        }
        
        // 清除缓存
        this.clearGradeCache();
        log.debug("年级更新成功: {} (uuid: {})", existingGrade.getName(), existingGrade.getGradeUuid());
        
        // 返回结果
        return BeanUtil.toBean(existingGrade, GradeDTO.class);
    }

    /**
     * 根据UUID删除年级
     *
     * @param gradeUuid 年级UUID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteGrade(String gradeUuid) {
        log.debug("删除年级: {}", gradeUuid);
        // 检查参数
        if (StrUtil.isBlank(gradeUuid)) {
            throw new BusinessException("年级UUID不能为空", ErrorCode.BODY_ERROR);
        }
        
        // 检查年级是否存在
        GradeDO existingGrade = gradeDAO.getGradeByUuid(gradeUuid);
        if (existingGrade == null) {
            throw new BusinessException("年级不存在", ErrorCode.BODY_ERROR);
        }
        
        // TODO: 检查是否有关联的专业或班级，如有则不允许删除
        // 此处需要根据实际业务逻辑添加关联检查
        
        // 执行删除
        boolean success = gradeDAO.removeById(gradeUuid);
        if (!success) {
            throw new BusinessException("删除年级失败", ErrorCode.OPERATION_FAILED);
        }
        
        // 清除缓存
        this.clearGradeCache();
        log.debug("年级删除成功: {}", gradeUuid);
        
        return true;
    }

    /**
     * 获取年级详情
     *
     * @param gradeUuid 年级UUID
     * @return 年级详情
     */
    @Override
    public GradeDTO getGradeDetail(String gradeUuid) {
        log.debug("查询年级详情: {}", gradeUuid);
        // 检查参数
        if (StrUtil.isBlank(gradeUuid)) {
            throw new BusinessException("年级UUID不能为空", ErrorCode.BODY_ERROR);
        }
        
        // 使用DAO从缓存或数据库获取年级信息
        GradeDO gradeDO = gradeDAO.getGradeByUuid(gradeUuid);
        if (gradeDO == null) {
            throw new BusinessException("年级不存在", ErrorCode.BODY_ERROR);
        }
        
        // 转换为DTO并返回
        return BeanUtil.toBean(gradeDO, GradeDTO.class);
    }

    /**
     * 分页查询年级列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param name 年级名称，可选，用于模糊查询
     * @param year 入学年份，可选
     * @return 分页数据
     */
    @Override
    public PageDTO<GradeDTO> page(Integer page, Integer size, String name, Short year) {
        log.debug("分页查询年级列表: page={}, size={}, name={}, year={}", page, size, name, year);
        // 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1 || size > 100) {
            size = 10;
        }
        
        // 构造查询条件
        LambdaQueryWrapper<GradeDO> queryWrapper = new LambdaQueryWrapper<>();
        if (StrUtil.isNotBlank(name)) {
            queryWrapper.like(GradeDO::getName, name);
        }
        if (year != null) {
            queryWrapper.eq(GradeDO::getYear, year);
        }
        
        // 按入学年份降序排序
        queryWrapper.orderByDesc(GradeDO::getYear);
        
        // 执行分页查询
        Page<GradeDO> pageResult = gradeDAO.page(new Page<>(page, size), queryWrapper);
        
        // 转换结果
        List<GradeDTO> records = new ArrayList<>();
        for (GradeDO gradeDO : pageResult.getRecords()) {
            records.add(BeanUtil.toBean(gradeDO, GradeDTO.class));
        }
        
        // 构造分页结果
        PageDTO<GradeDTO> pageDTO = new PageDTO<GradeDTO>()
                .setRecords(records)
                .setTotal(pageResult.getTotal())
                .setSize(pageResult.getSize())
                .setCurrent(pageResult.getCurrent());
        
        log.debug("查询结果: 总记录数={}, 当前页={}, 每页大小={}", pageDTO.getTotal(), pageDTO.getCurrent(), pageDTO.getSize());
        return pageDTO;
    }

    /**
     * 获取简单年级列表
     *
     * @return 年级列表
     */
    @Override
    public List<GradeDTO> listSimple() {
        log.debug("查询年级简单列表");
        // 尝试从缓存获取年级列表，如缓存不存在则从数据库获取
        List<GradeDO> gradeDOList = gradeDAO.getGradeListForUpdate();
        
        // 如果列表为空，直接从数据库查询
        if (gradeDOList == null || gradeDOList.isEmpty()) {
            gradeDOList = gradeDAO.getAllGradesOrderByYearDesc();
        }
        
        // 转换为DTO列表
        List<GradeDTO> result = new ArrayList<>();
        for (GradeDO gradeDO : gradeDOList) {
            result.add(BeanUtil.toBean(gradeDO, GradeDTO.class));
        }
        
        log.debug("查询到 {} 条年级记录", result.size());
        return result;
    }
    
    /**
     * 清除年级相关的缓存
     */
    private void clearGradeCache() {
        log.debug("清除年级相关缓存");
        RKeys keys = redisson.getKeys();
        // 删除所有年级UUID相关的缓存
        keys.deleteByPattern(StringConstant.Redis.GRADE_UUID + "*");
        // 删除年级列表缓存
        keys.deleteByPattern(StringConstant.Redis.GRADE_LIST);
    }
    
    /**
     * 校验年级参数
     *
     * @param gradeDTO 年级信息
     */
    private void validateGradeParams(@NotNull GradeDTO gradeDTO) {
        if (StrUtil.isBlank(gradeDTO.getName())) {
            throw new BusinessException("年级名称不能为空", ErrorCode.BODY_ERROR);
        }
        
        if (gradeDTO.getYear() == null) {
            throw new BusinessException("入学年份不能为空", ErrorCode.BODY_ERROR);
        }
        
        if (gradeDTO.getStartDate() == null) {
            throw new BusinessException("年级开始日期不能为空", ErrorCode.BODY_ERROR);
        }
        
        // 如果结束日期不为空，则必须晚于开始日期
        if (gradeDTO.getEndDate() != null && gradeDTO.getStartDate() != null 
                && gradeDTO.getEndDate().before(gradeDTO.getStartDate())) {
            throw new BusinessException("年级结束日期必须晚于开始日期", ErrorCode.BODY_ERROR);
        }
    }
} 
