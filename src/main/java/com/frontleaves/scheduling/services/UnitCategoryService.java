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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.UnitCategoryDTO;
import com.frontleaves.scheduling.models.dto.UnitCategoryLiteDTO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.vo.UnitCategoryVO;

import java.util.List;

/**
 * 单位类别服务接口
 *
 * @author xiao_lfeng
 * @version v1.0.0
 */
public interface UnitCategoryService {

    /**
     * 添加单位类别前检查
     *
     * @param unitCategoryVO 单位类别VO
     */
    void checkAddUnitCategoryVO(UnitCategoryVO unitCategoryVO);

    /**
     * 添加单位类别
     *
     * @param unitCategoryVO 单位类别VO
     * @return 单位类别DTO
     */
    UnitCategoryDTO addUnitCategory(UnitCategoryVO unitCategoryVO);

    /**
     * 更新单位类别前检查
     *
     * @param unitCategoryUuid 单位类别UUID
     * @param unitCategoryVO   单位类别VO
     * @return 单位类别DO
     */
    UnitCategoryDO checkUpdateUnitCategoryVO(String unitCategoryUuid, UnitCategoryVO unitCategoryVO);

    /**
     * 更新单位类别
     *
     * @param unitCategoryVO 单位类别VO
     * @param unitCategoryDO 单位类别DO
     * @return 单位类别DTO
     */
    UnitCategoryDTO updateUnitCategory(UnitCategoryVO unitCategoryVO, UnitCategoryDO unitCategoryDO);

    /**
     * 删除单位类别前检查
     *
     * @param unitCategoryUuid 单位类别UUID
     * @return 单位类别DO
     */
    UnitCategoryDO checkDeleteUnitCategory(String unitCategoryUuid);

    /**
     * 删除单位类别
     *
     * @param unitCategoryDO 单位类别DO
     */
    void deleteUnitCategory(UnitCategoryDO unitCategoryDO);

    /**
     * 获取单位类别详情
     *
     * @param unitCategoryUuid 单位类别UUID
     * @return 单位类别DTO
     */
    UnitCategoryDTO getUnitCategoryDetail(String unitCategoryUuid);

    /**
     * 获取单位类别分页数据
     *
     * @param page    页码
     * @param size    每页大小
     * @param isDesc  是否降序
     * @param keyword 关键词
     * @return 分页数据
     */
    PageDTO<UnitCategoryDTO> getPageOfUnitCategory(Integer page, Integer size, Boolean isDesc, String keyword);

    /**
     * 获取单位类别列表
     *
     * @return 单位类别列表
     */
    List<UnitCategoryLiteDTO> getUnitCategoryList();
}
