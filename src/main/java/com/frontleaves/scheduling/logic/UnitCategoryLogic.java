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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.UnitCategoryDTO;
import com.frontleaves.scheduling.models.dto.UnitCategoryLiteDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.vo.UnitCategoryVO;
import com.frontleaves.scheduling.services.UnitCategoryService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 单位类别逻辑实现类
 *
 * @author xiao_lfeng
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
        UnitCategoryDO existedUnitCategory = unitCategoryDAO.getUnitCategoryByName(unitCategoryVO.getName());
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
        UnitCategoryDO getCategory = unitCategoryDAO.saveUnitCategory(
                BeanUtil.toBean(unitCategoryVO, UnitCategoryDO.class)
        );

        return BeanUtil.toBean(getCategory, UnitCategoryDTO.class);
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
        UnitCategoryDO existedUnitCategory = unitCategoryDAO.getUnitCategoryByNameExceptUuid(
                unitCategoryVO.getName(),
                unitCategoryUuid
        );
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
        Optional.ofNullable(unitCategoryVO.getName())
                .filter(name -> !name.equals(unitCategoryDO.getName()))
                .ifPresent(unitCategoryDO::setName);
        Optional.ofNullable(unitCategoryVO.getEnglishName())
                .filter(englishName -> !englishName.equals(unitCategoryDO.getEnglishName()))
                .ifPresent(unitCategoryDO::setEnglishName);
        Optional.ofNullable(unitCategoryVO.getShortName())
                .filter(shortName -> !shortName.equals(unitCategoryDO.getShortName()))
                .ifPresent(unitCategoryDO::setShortName);
        Optional.ofNullable(unitCategoryVO.getOrder())
                .filter(order -> !order.equals(unitCategoryDO.getOrder()))
                .ifPresent(unitCategoryDO::setOrder);
        Optional.ofNullable(unitCategoryVO.getIsEntity())
                .filter(isEntity -> !isEntity.equals(unitCategoryDO.getIsEntity()))
                .ifPresent(unitCategoryDO::setIsEntity);

        unitCategoryDO.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        UnitCategoryDO newUnitCategory = unitCategoryDAO.updateUnitCategory(unitCategoryDO);
        return BeanUtil.copyProperties(newUnitCategory, UnitCategoryDTO.class);
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
        DepartmentDO getDepartment = departmentDAO.getDepartmentByUuid(unitCategoryUuid);
        if (getDepartment != null) {
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
        unitCategoryDAO.deleteUnitCategoryCache(unitCategoryDO);
        try {
            unitCategoryDAO.removeById(unitCategoryDO.getUnitCategoryUuid());
        } catch (DataIntegrityViolationException e) {
            log.warn(e.getMessage());
            if (e.getRootCause() instanceof SQLIntegrityConstraintViolationException integrityException) {
                throw new BusinessException("该单位类别已被其他数据使用，无法删除", ErrorCode.OPERATION_INVALID, integrityException.getMessage());
            }
        } catch (Exception e) {
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
        // 执行分页查询
        Page<UnitCategoryDO> pageResult = unitCategoryDAO.getPageOfUnitCategory(page, size, isDesc, keyword);
        if (pageResult == null) {
            return new PageDTO<>();
        }
        // 使用项目工具类转换为PageDTO
        return ProjectUtil.convertPageToPageDTO(pageResult, UnitCategoryDTO.class);
    }

    /**
     * 获取单位类别列表
     *
     * @return 单位类别列表
     */
    @Override
    public List<UnitCategoryLiteDTO> getUnitCategoryList() {
        return unitCategoryDAO.getUnitCategoryList();
    }
}
