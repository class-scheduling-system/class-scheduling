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

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.BuildingDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.BuildingDO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.services.BuildingService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
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
    private final CampusDAO campusDAO;

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
     * 获取教学楼信息
     * <p>
     * 根据传入的教学楼标识符，从数据库中查询对应的教学楼信息。如果传入的 {@code building} 参数符合 UUID 的格式，
     * 则通过 UUID 查询教学楼；否则，通过名称查询教学楼。最后将查询到的 {@code BuildingDO} 对象转换为 {@code BuildingDTO} 返回。
     *
     * @param building 教学楼标识符，可以是 UUID 或名称
     * @return 查询到的教学楼信息，以 {@code BuildingDTO} 形式返回
     */
    @Override
    public BuildingDTO getBuilding(@NotNull String building) {
        BuildingDO buildingDTO;
        if (building.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            buildingDTO = buildingDAO.getBuildingByUuid(building);
        } else {
            buildingDTO = buildingDAO.getBuildingByName(building);
        }
        return BeanUtil.toBean(buildingDTO, BuildingDTO.class);
    }

    /**
     * 根据校区获取教学楼列表
     * <p>
     * 该方法通过给定的校区唯一标识符 {@code campusUuid} 获取指定页数和每页大小的教学楼信息。支持按照是否降序排列返回结果。
     * 返回的数据结构是 {@code PageDTO<BuildingDTO>} 类型，包含了分页信息以及转换后的教学楼数据对象列表。
     *
     * @param campusUuid 校区的唯一标识符
     * @param page       请求的页码，从1开始
     * @param size       每页显示的条目数量
     * @param isDesc     是否按降序排列查询结果，默认为false表示升序
     * @return 包含了请求页码中的教学楼信息及其分页详情的PageDTO对象
     */
    @Override
    public PageDTO<BuildingDTO> getBuildingByCampus(String campusUuid, int page, int size, boolean isDesc) {
        Page<BuildingDO> buildingList = buildingDAO.getBuildingByCampus(campusUuid, page, size, isDesc);
        if (buildingList.getTotal() == 0) {
            return new PageDTO<>();
        } else {
            return ProjectUtil.convertPageToPageDTO(buildingList, BuildingDTO.class);
        }
    }

    /**
     * 添加新的教学楼
     * <p>
     * 该方法用于向系统中添加一个新的教学楼记录。通过传入校区的唯一标识符 {@code campusUuid}、教学楼名称 {@code buildingName} 和状态 {@code status}，
     * 可以创建一条新的教学楼信息。其中，{@code campusUuid} 用于指定新教学楼所属的校区；{@code buildingName} 是新增教学楼的名称；
     * 而 {@code status} 则表示教学楼当前的状态（启用或禁用）。成功调用此方法后，新的教学楼将被保存到数据库中。
     * </p>
     *
     * @param campusUuid   校区的唯一标识符，用于确定新教学楼所在的地理位置
     * @param buildingName 新增教学楼的名称
     * @param status       教学楼的状态，true 表示启用，false 表示禁用
     */
    @Override
    public void addBuilding(String campusUuid, String buildingName, boolean status) {
        CampusDO getCampus = campusDAO.getCampusByUuid(campusUuid);
        if (getCampus != null) {
            BuildingDO buildingDO = new BuildingDO();
            buildingDO
                    .setBuildingName(buildingName)
                    .setCampusUuid(campusUuid)
                    .setStatus(status);
            buildingDAO.save(buildingDO);
        } else {
            throw new BusinessException("校区不存在", ErrorCode.NOT_EXIST);
        }
    }

    /**
     * 更新教学楼信息
     * <p>
     * 该方法用于根据提供的参数更新指定的教学楼信息。首先通过 {@code campusUuid} 获取校区信息，如果校区存在，
     * 则继续通过 {@code buildingUuid} 获取教学楼信息。如果教学楼存在，则更新其所属校区、名称和状态。
     * 如果校区或教学楼不存在，则抛出业务异常。
     *
     * @param buildingUuid 教学楼的唯一标识符
     * @param campusUuid   校区的唯一标识符
     * @param buildingName 教学楼的新名称
     * @param status       教学楼的新状态
     * @throws BusinessException 当校区或教学楼不存在时抛出
     */
    @Override
    public void updateBuilding(String buildingUuid, String campusUuid, String buildingName, Boolean status)
            throws BusinessException {
        CampusDO getCampus = campusDAO.getCampusByUuid(campusUuid);
        if (getCampus != null) {
            BuildingDO buildingDO = buildingDAO.getBuildingByUuid(buildingUuid);
            if (buildingDO != null) {
                buildingDO
                        .setCampusUuid(campusUuid)
                        .setBuildingName(buildingName)
                        .setStatus(status);
                buildingDAO.updateBuilding(buildingDO);
            } else {
                throw new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST);
            }
        } else {
            throw new BusinessException("校区不存在", ErrorCode.NOT_EXIST);
        }
    }

    /**
     * 删除教学楼
     * <p>
     * 该方法根据给定的教学楼唯一标识 {@code buildingUuid} 删除指定的教学楼。
     * 如果存在与给定 UUID 匹配的教学楼，则从数据库中删除该教学楼。
     * 如果没有找到匹配的教学楼，将抛出一个 {@link BusinessException} 异常，提示“教学楼不存在”。
     *
     * @param buildingUuid 教学楼的唯一标识
     * @throws BusinessException 当教学楼不存在时抛出此异常
     */
    @Override
    public void deleteBuilding(String buildingUuid) throws BusinessException {
        BuildingDO buildingDO = buildingDAO.getBuildingByUuid(buildingUuid);
        if (buildingDO != null) {
            buildingDAO.deleteBuilding(buildingDO);
        } else {
            throw new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST);
        }
    }
}
