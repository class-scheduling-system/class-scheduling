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
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.UnitCategoryDTO;
import com.frontleaves.scheduling.models.dto.UnitCategoryLiteDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.vo.UnitCategoryVO;
import com.frontleaves.scheduling.services.UnitCategoryService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 单位类别逻辑实现类
 *
 * @author Claude
 * @version v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnitCategoryLogic implements UnitCategoryService {

    private final UnitCategoryDAO unitCategoryDAO;
    private final DepartmentDAO departmentDAO;

    /**
     * 添加单位类别前检查
     *
     * @param unitCategoryVO 单位类别VO
     */
    @Override
    public void checkAddUnitCategoryVO(UnitCategoryVO unitCategoryVO) {
        // 检查单位类别名称是否已存在
        UnitCategoryDO existedUnitCategory = unitCategoryDAO.lambdaQuery()
                .eq(UnitCategoryDO::getName, unitCategoryVO.getName())
                .one();
        if (existedUnitCategory != null) {
            throw new BusinessException("单位类别名称已存在", ErrorCode.PARAMETER_INVALID);
        }
    }

    /**
     * 添加单位类别
     *
     * @param unitCategoryVO 单位类别VO
     * @return 单位类别DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitCategoryDTO addUnitCategory(UnitCategoryVO unitCategoryVO) {
        UnitCategoryDO unitCategoryDO = new UnitCategoryDO();
        BeanUtil.copyProperties(unitCategoryVO, unitCategoryDO);

        // 设置UUID
        unitCategoryDO.setUnitCategoryUuid(UuidUtil.generateUuidNoDash());

        // 设置创建和更新时间
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        unitCategoryDO.setCreatedAt(now);
        unitCategoryDO.setUpdatedAt(now);

        if (!unitCategoryDAO.save(unitCategoryDO)) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }

        UnitCategoryDTO unitCategoryDTO = new UnitCategoryDTO();
        BeanUtil.copyProperties(unitCategoryDO, unitCategoryDTO);
        return unitCategoryDTO;
    }

    /**
     * 更新单位类别前检查
     *
     * @param unitCategoryUuid 单位类别UUID
     * @param unitCategoryVO   单位类别VO
     * @return 单位类别DO
     */
    @Override
    public UnitCategoryDO checkUpdateUnitCategoryVO(String unitCategoryUuid, UnitCategoryVO unitCategoryVO) {
        // 检查单位类别是否存在
        UnitCategoryDO unitCategoryDO = unitCategoryDAO.getById(unitCategoryUuid);
        if (unitCategoryDO == null) {
            throw new BusinessException(StringConstant.UNIT_CATEGORY_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 检查新名称是否与其他单位类别重复
        UnitCategoryDO existedUnitCategory = unitCategoryDAO.lambdaQuery()
                .eq(UnitCategoryDO::getName, unitCategoryVO.getName())
                .ne(UnitCategoryDO::getUnitCategoryUuid, unitCategoryUuid)
                .one();
        if (existedUnitCategory != null) {
            throw new BusinessException("单位类别名称已存在", ErrorCode.PARAMETER_INVALID);
        }

        return unitCategoryDO;
    }

    /**
     * 更新单位类别
     *
     * @param unitCategoryVO 单位类别VO
     * @param unitCategoryDO 单位类别DO
     * @return 单位类别DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitCategoryDTO updateUnitCategory(UnitCategoryVO unitCategoryVO, UnitCategoryDO unitCategoryDO) {
        // 更新属性
        if (unitCategoryVO.getName() != null) {
            unitCategoryDO.setName(unitCategoryVO.getName());
        }
        if (unitCategoryVO.getEnglishName() != null) {
            unitCategoryDO.setEnglishName(unitCategoryVO.getEnglishName());
        }
        if (unitCategoryVO.getShortName() != null) {
            unitCategoryDO.setShortName(unitCategoryVO.getShortName());
        }
        if (unitCategoryVO.getOrder() != null) {
            unitCategoryDO.setOrder(unitCategoryVO.getOrder());
        }
        if (unitCategoryVO.getIsEntity() != null) {
            unitCategoryDO.setIsEntity(unitCategoryVO.getIsEntity());
        }

        unitCategoryDO.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        if (!unitCategoryDAO.updateById(unitCategoryDO)) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }

        UnitCategoryDTO unitCategoryDTO = new UnitCategoryDTO();
        BeanUtil.copyProperties(unitCategoryDO, unitCategoryDTO);
        return unitCategoryDTO;
    }

    /**
     * 删除单位类别前检查
     *
     * @param unitCategoryUuid 单位类别UUID
     * @return 单位类别DO
     */
    @Override
    public UnitCategoryDO checkDeleteUnitCategory(String unitCategoryUuid) {
        // 检查单位类别是否存在
        UnitCategoryDO unitCategoryDO = unitCategoryDAO.getById(unitCategoryUuid);
        if (unitCategoryDO == null) {
            throw new BusinessException(StringConstant.UNIT_CATEGORY_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 检查是否有部门使用该单位类别
        List<DepartmentDO> departments = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getUnitCategory, unitCategoryUuid)
                .list();
        if (!departments.isEmpty()) {
            throw new BusinessException("该单位类别已被部门使用，无法删除", ErrorCode.OPERATION_INVALID);
        }

        return unitCategoryDO;
    }

    /**
     * 删除单位类别
     *
     * @param unitCategoryDO 单位类别DO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUnitCategory(UnitCategoryDO unitCategoryDO) {
        if (!unitCategoryDAO.removeById(unitCategoryDO.getUnitCategoryUuid())) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 获取单位类别详情
     *
     * @param unitCategoryUuid 单位类别UUID
     * @return 单位类别DTO
     */
    @Override
    public UnitCategoryDTO getUnitCategoryDetail(String unitCategoryUuid) {
        UnitCategoryDO unitCategoryDO = unitCategoryDAO.getById(unitCategoryUuid);
        if (unitCategoryDO == null) {
            throw new BusinessException(StringConstant.UNIT_CATEGORY_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        UnitCategoryDTO unitCategoryDTO = new UnitCategoryDTO();
        BeanUtil.copyProperties(unitCategoryDO, unitCategoryDTO);
        return unitCategoryDTO;
    }

    /**
     * 获取单位类别分页数据
     *
     * @param page    页码
     * @param size    每页大小
     * @param isDesc  是否降序
     * @param keyword 关键词
     * @return 分页数据
     */
    @Override
    public PageDTO<UnitCategoryDTO> getPageOfUnitCategory(Integer page, Integer size, Boolean isDesc, String keyword) {
        LambdaQueryWrapper<UnitCategoryDO> wrapper = new LambdaQueryWrapper<>();

        // 添加搜索条件
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(UnitCategoryDO::getName, keyword)
                    .or()
                    .like(UnitCategoryDO::getEnglishName, keyword)
                    .or()
                    .like(UnitCategoryDO::getShortName, keyword);
        }

        // 添加排序
        if (Boolean.TRUE.equals(isDesc)) {
            wrapper.orderByDesc(UnitCategoryDO::getOrder, UnitCategoryDO::getCreatedAt);
        } else {
            wrapper.orderByAsc(UnitCategoryDO::getOrder, UnitCategoryDO::getCreatedAt);
        }

        // 执行分页查询
        Page<UnitCategoryDO> pageable = new Page<>(page, size);
        Page<UnitCategoryDO> result = unitCategoryDAO.page(pageable, wrapper);

        // 使用项目工具类转换为PageDTO
        return ProjectUtil.convertPageToPageDTO(result, UnitCategoryDTO.class);
    }

    /**
     * 获取单位类别列表
     *
     * @return 单位类别列表
     */
    @Override
    public List<UnitCategoryLiteDTO> getUnitCategoryList() {
        List<UnitCategoryDO> unitCategories = unitCategoryDAO.lambdaQuery()
                .orderByAsc(UnitCategoryDO::getOrder, UnitCategoryDO::getCreatedAt)
                .list();

        if (unitCategories.isEmpty()) {
            return new ArrayList<>();
        }

        return unitCategories.stream()
                .map(unitCategory -> {
                    UnitCategoryLiteDTO dto = new UnitCategoryLiteDTO();
                    BeanUtil.copyProperties(unitCategory, dto);
                    return dto;
                })
                .toList();
    }
}
