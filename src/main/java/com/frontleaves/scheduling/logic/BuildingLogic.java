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
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.models.dto.BuildingDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.BuildingDO;
import com.frontleaves.scheduling.services.BuildingService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * 教学楼逻辑处理类
 * <p>
 * 该类实现了 {@code BuildingService} 接口，提供了分页查询教学楼信息的方法。
 * 包含了获取所有教学楼列表和根据关键词搜索教学楼列表的功能。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
@RequiredArgsConstructor
public class BuildingLogic implements BuildingService {
    private final BuildingDAO buildingDAO;

    /**
     * 获取包含关键词的教学楼列表
     * <p>
     * 该方法用于分页查询系统中所有名称或相关信息包含指定关键词的教学楼信息。
     * 通过传入的页码、每页显示的数量以及是否降序排列来控制返回的数据量和排序方式。
     * 返回的是一个包含教学楼数据传输对象 {@code BuildingDTO} 的分页结果，其中包含了教学楼的基本信息，
     * 如教学楼主键、名称、校区主键、状态、创建时间和更新时间等。
     * </p>
     *
     * @param page    当前页码
     * @param size    每页显示的数量
     * @param isDesc  是否降序排列
     * @param keyword 搜索关键词，用于匹配教学楼名称或其他相关信息
     * @return 分页的教学楼数据传输对象 {@code PageDTO<BuildingDTO>}
     */
    @Override
    @NotNull
    public PageDTO<BuildingDTO> getBuildingListHasKeyword(int page, int size, boolean isDesc, String keyword) {
        Page<BuildingDO> buildingList = buildingDAO.getBuildingListHasKeyword(page, size, isDesc, keyword);
        return ProjectUtil.convertPageToPageDTO(buildingList, BuildingDTO.class);
    }

    /**
     * 获取教学楼列表
     * <p>
     * 该方法用于分页查询系统中所有的教学楼信息。通过指定的页码、每页大小以及排序方式，返回符合条件的教学楼数据。
     * 返回的数据封装在 {@code PageDTO<BuildingDTO>} 中，其中包含分页信息和教学楼的基本信息。
     * </p>
     *
     * @param page   当前页码，从1开始
     * @param size   每页显示的记录数
     * @param isDesc 排序方式，true 表示降序，false 表示升序
     * @return 包含分页信息和教学楼数据的 {@code PageDTO<BuildingDTO>}
     */
    @Override
    @NotNull
    public PageDTO<BuildingDTO> getBuildingList(int page, int size, boolean isDesc) {
        Page<BuildingDO> buildingList = buildingDAO.getBuildingList(page, size, isDesc);
        return ProjectUtil.convertPageToPageDTO(buildingList, BuildingDTO.class);
    }
}
