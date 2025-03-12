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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.BuildingDTO;
import com.frontleaves.scheduling.models.dto.BuildingLiteDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.xlf.utility.exception.BusinessException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 教学楼服务接口
 * <p>
 * 该接口定义了获取教学楼信息的相关方法，包括分页查询所有教学楼和根据关键词搜索教学楼。
 * 返回的数据封装在 {@code PageDTO<BuildingDTO>} 中，其中包含分页信息和教学楼的基本信息。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface BuildingService {

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
    @NotNull
    PageDTO<BuildingDTO> getBuildingList(int page, int size, boolean isDesc, String keyword);

    /**
     * 根据教学楼标识获取教学楼信息
     * <p>
     * 该方法通过传入的教学楼唯一标识 {@code building} 来查询系统中对应的单个教学楼信息。
     * 如果找到匹配的教学楼，则返回包含该教学楼基本信息的 {@code BuildingDTO} 对象；
     * 若未找到匹配项，则可能抛出异常或返回空对象，具体行为取决于实现细节。
     * 返回的 {@code BuildingDTO} 包含了如教学楼主键、名称、校区主键、状态、创建时间和更新时间等字段。
     * </p>
     *
     * @param building 教学楼的唯一标识符，用于定位特定的教学楼记录
     * @return 包含指定教学楼详细信息的数据传输对象 {@code BuildingDTO}；如果找不到对应的教学楼，行为依据具体实现而定
     */
    @Nullable
    BuildingDTO getBuildingByUuidOrName(String building);

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
    PageDTO<BuildingDTO> getBuildingByCampus(String campusUuid, int page, int size, boolean isDesc);

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
    void addBuilding(String campusUuid, String buildingName, boolean status);

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
    void updateBuilding(String buildingUuid, String campusUuid, String buildingName, Boolean status);

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
    void deleteBuilding(String buildingUuid);

    /**
     * 获取教学楼列表
     *
     * @param keyword 关键字
     * @return 教学楼列表
     */
    List<BuildingLiteDTO> getBuildingPage(
            String keyword);
}
