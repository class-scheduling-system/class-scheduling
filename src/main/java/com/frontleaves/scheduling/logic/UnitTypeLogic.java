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
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.UnitTypeDTO;
import com.frontleaves.scheduling.models.dto.UnitTypeLiteDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.UnitTypeVO;
import com.frontleaves.scheduling.services.UnitTypeService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 单位办别逻辑实现类
 *
 * @author xiao_lfeng
 * @version v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnitTypeLogic implements UnitTypeService {

    private final UnitTypeDAO unitTypeDAO;
    private final DepartmentDAO departmentDAO;

    /**
     * 添加单位办别前检查
     *
     * @param unitTypeVO 单位办别VO
     */
    @Override
    public void checkAddUnitTypeVO(UnitTypeVO unitTypeVO) {
        // 检查单位名称是否已存在
        UnitTypeDO existedUnitType = unitTypeDAO.getUnitTypeByName(unitTypeVO.getName());
        if (existedUnitType != null) {
            throw new BusinessException("单位办别名称已存在", ErrorCode.PARAMETER_INVALID);
        }
    }

    /**
     * 添加单位办别
     *
     * @param unitTypeVO 单位办别VO
     * @return 单位办别DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitTypeDTO addUnitType(UnitTypeVO unitTypeVO) {
        UnitTypeDO unitTypeDO = new UnitTypeDO();
        BeanUtil.copyProperties(unitTypeVO, unitTypeDO);
        unitTypeDAO.addUnitType(unitTypeDO);
        return BeanUtil.toBean(unitTypeDO, UnitTypeDTO.class);
    }

    /**
     * 更新单位办别前检查
     *
     * @param unitTypeUuid 单位办别UUID
     * @param unitTypeVO   单位办别VO
     * @return 单位办别DO
     */
    @Override
    public UnitTypeDO checkUpdateUnitTypeVO(String unitTypeUuid, UnitTypeVO unitTypeVO) {
        // 检查单位办别是否存在
        UnitTypeDO unitTypeDO = unitTypeDAO.getById(unitTypeUuid);
        if (unitTypeDO == null) {
            throw new BusinessException(StringConstant.UNIT_TYPE_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 检查新名称是否与其他单位办别重复
        UnitTypeDO existedUnitType = unitTypeDAO.getUnitTypeByName(unitTypeVO.getName());
        if (existedUnitType != null) {
            throw new BusinessException("单位办别名称已存在", ErrorCode.PARAMETER_INVALID);
        }

        return unitTypeDO;
    }

    /**
     * 更新单位办别
     *
     * @param unitTypeVO 单位办别VO
     * @param unitTypeDO 单位办别DO
     * @return 单位办别DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UnitTypeDTO updateUnitType(UnitTypeVO unitTypeVO, UnitTypeDO unitTypeDO) {
        // 更新属性
        Optional.ofNullable(unitTypeVO.getName())
                .filter(name -> !name.equals(unitTypeDO.getName()))
                .ifPresent(unitTypeDO::setName);
        Optional.ofNullable(unitTypeVO.getEnglishName())
                .filter(englishName -> !englishName.equals(unitTypeDO.getEnglishName()))
                .ifPresent(unitTypeDO::setEnglishName);
        Optional.ofNullable(unitTypeVO.getShortName())
                .filter(shortName -> !shortName.equals(unitTypeDO.getShortName()))
                .ifPresent(unitTypeDO::setShortName);
        Optional.ofNullable(unitTypeVO.getOrder())
                .filter(order -> !order.equals(unitTypeDO.getOrder()))
                .ifPresent(unitTypeDO::setOrder);

        unitTypeDO.setUpdatedAt(Timestamp.valueOf(LocalDateTime.now()));

        if (!unitTypeDAO.updateById(unitTypeDO)) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }

        UnitTypeDTO unitTypeDTO = new UnitTypeDTO();
        BeanUtil.copyProperties(unitTypeDO, unitTypeDTO);
        return unitTypeDTO;
    }

    /**
     * 删除单位办别前检查
     *
     * @param unitTypeUuid 单位办别UUID
     * @return 单位办别DO
     */
    @Override
    public UnitTypeDO checkDeleteUnitType(String unitTypeUuid) {
        // 检查单位办别是否存在
        UnitTypeDO unitTypeDO = unitTypeDAO.getById(unitTypeUuid);
        if (unitTypeDO == null) {
            throw new BusinessException(StringConstant.UNIT_TYPE_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 检查是否有部门使用该单位办别
        DepartmentDO getDepartment = departmentDAO.getDepartmentByUuid(unitTypeUuid);
        if (getDepartment != null) {
            throw new BusinessException("该单位办别已被部门使用，无法删除", ErrorCode.OPERATION_INVALID);
        }

        return unitTypeDO;
    }

    /**
     * 删除单位办别
     *
     * @param unitTypeDO 单位办别DO
     */
    @Override
    @Transactional
    public void deleteUnitType(UnitTypeDO unitTypeDO) {
        unitTypeDAO.deleteUnitType(unitTypeDO);
    }

    /**
     * 获取单位办别详情
     *
     * @param unitTypeUuid 单位办别UUID
     * @return 单位办别DTO
     */
    @Override
    public UnitTypeDTO getUnitTypeDetail(String unitTypeUuid) {
        UnitTypeDO unitTypeDO = unitTypeDAO.getById(unitTypeUuid);
        if (unitTypeDO == null) {
            throw new BusinessException(StringConstant.UNIT_TYPE_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        UnitTypeDTO unitTypeDTO = new UnitTypeDTO();
        BeanUtil.copyProperties(unitTypeDO, unitTypeDTO);
        return unitTypeDTO;
    }

    /**
     * 获取单位办别分页数据
     *
     * @param page    页码
     * @param size    每页大小
     * @param isDesc  是否降序
     * @param keyword 关键词
     * @return 分页数据
     */
    @Override
    public PageDTO<UnitTypeDTO> getPageOfUnitType(Integer page, Integer size, Boolean isDesc, String keyword) {
        // 执行分页查询
        Page<UnitTypeDO> pageResult = unitTypeDAO.getPageOfUnitType(page, size, isDesc, keyword);
        if (pageResult == null) {
            return new PageDTO<>();
        }
        // 使用项目工具类转换为PageDTO
        return ProjectUtil.convertPageToPageDTO(pageResult, UnitTypeDTO.class);
    }

    /**
     * 获取单位办别列表
     *
     * @return 单位办别列表
     */
    @Override
    public List<UnitTypeLiteDTO> getUnitTypeList() {
        return unitTypeDAO.getUnitTypeList();
    }
}
