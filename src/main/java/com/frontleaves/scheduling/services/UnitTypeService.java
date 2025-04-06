/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.UnitTypeDTO;
import com.frontleaves.scheduling.models.dto.lite.UnitTypeLiteDTO;
import com.frontleaves.scheduling.models.entity.base.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.UnitTypeVO;

import java.util.List;

/**
 * 单位办别服务接口
 *
 * @author xiao_lfeng
 * @version v1.0.0
 */
public interface UnitTypeService {

    /**
     * 添加单位办别前检查
     *
     * @param unitTypeVO 单位办别VO
     */
    void checkAddUnitTypeVO(UnitTypeVO unitTypeVO);

    /**
     * 添加单位办别
     *
     * @param unitTypeVO 单位办别VO
     * @return 单位办别DTO
     */
    UnitTypeDTO addUnitType(UnitTypeVO unitTypeVO);

    /**
     * 更新单位办别前检查
     *
     * @param unitTypeUuid 单位办别UUID
     * @param unitTypeVO   单位办别VO
     * @return 单位办别DO
     */
    UnitTypeDO checkUpdateUnitTypeVO(String unitTypeUuid, UnitTypeVO unitTypeVO);

    /**
     * 更新单位办别
     *
     * @param unitTypeVO 单位办别VO
     * @param unitTypeDO 单位办别DO
     * @return 单位办别DTO
     */
    UnitTypeDTO updateUnitType(UnitTypeVO unitTypeVO, UnitTypeDO unitTypeDO);

    /**
     * 删除单位办别前检查
     *
     * @param unitTypeUuid 单位办别UUID
     * @return 单位办别DO
     */
    UnitTypeDO checkDeleteUnitType(String unitTypeUuid);

    /**
     * 删除单位办别
     *
     * @param unitTypeDO 单位办别DO
     */
    void deleteUnitType(UnitTypeDO unitTypeDO);

    /**
     * 获取单位办别详情
     *
     * @param unitTypeUuid 单位办别UUID
     * @return 单位办别DTO
     */
    UnitTypeDTO getUnitTypeDetail(String unitTypeUuid);

    /**
     * 获取单位办别分页数据
     *
     * @param page    页码
     * @param size    每页大小
     * @param isDesc  是否降序
     * @param keyword 关键词
     * @return 分页数据
     */
    PageDTO<UnitTypeDTO> getPageOfUnitType(Integer page, Integer size, Boolean isDesc, String keyword);

    /**
     * 获取单位办别列表
     *
     * @return 单位办别列表
     */
    List<UnitTypeLiteDTO> getUnitTypeList();
}
