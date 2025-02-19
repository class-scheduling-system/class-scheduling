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
import com.frontleaves.scheduling.models.dto.PageDTO;

/**
 * 教学楼服务接口
 * <p>
 * 该接口定义了获取教学楼信息的相关方法，包括分页查询所有教学楼和根据关键词搜索教学楼。
 * 返回的数据封装在 {@code PageDTO<BuildingDTO>} 中，其中包含分页信息和教学楼的基本信息。
 * </p>
 *
 * @version v1.0.0
 * @since v1.0.0
 * @author xiao_lfeng
 */
public interface BuildingService {
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
    PageDTO<BuildingDTO> getBuildingList(int page, int size, boolean isDesc);

    /**
     * 获取包含关键词的教学楼列表
     * <p>
     * 该方法用于分页查询系统中所有名称或相关信息包含指定关键词的教学楼信息。
     * 通过传入的页码、每页显示的数量以及是否降序排列来控制返回的数据量和排序方式。
     * 返回的是一个包含教学楼数据传输对象 {@code BuildingDTO} 的分页结果，其中包含了教学楼的基本信息，
     * 如教学楼主键、名称、校区主键、状态、创建时间和更新时间等。
     * </p>
     *
     * @param page 当前页码
     * @param size 每页显示的数量
     * @param isDesc 是否降序排列
     * @param keyword 搜索关键词，用于匹配教学楼名称或其他相关信息
     * @return 分页的教学楼数据传输对象 {@code PageDTO<BuildingDTO>}
     */
    PageDTO<BuildingDTO> getBuildingListHasKeyword(int page, int size, boolean isDesc, String keyword);

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
    BuildingDTO getBuilding(String building);
}
