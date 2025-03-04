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

import com.frontleaves.scheduling.models.dto.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.ClassroomTagDTO;
import com.frontleaves.scheduling.models.dto.ClassroomTypeDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * 教室服务接口，定义了教室相关的操作。
 * <p>
 * 该接口提供了教室管理相关的基础方法，包括添加教室、删除教室、查询教室信息等。具体实现细节由实现类决定。
 * </p>
 *
 * @author xiao_lfeng
 * @since v1.0.0
 * @since v1.0.0
 */
public interface ClassroomService {

    /**
     * 列出所有教室标签
     * <p>
     * 该方法用于获取系统中所有的教室标签信息。每个教室标签由 {@code ClassroomTagDTO} 对象表示，包含标签的主键、名称、描述以及创建和更新时间。
     * </p>
     *
     * @return 返回一个包含所有教室标签的列表
     */
    List<ClassroomTagDTO> listClassroomTags();

    /**
     * 列出所有教室类型
     * <p>
     * 该方法用于获取系统中所有的教室类型信息。每个教室类型由 {@code ClassroomTypeDTO} 对象表示，包含教室类型的主键、名称、描述以及创建和更新时间。
     * </p>
     *
     * @return 返回一个包含所有教室类型的列表
     */
    List<ClassroomTypeDTO> listClassroomTypes();


    /**
     * 获取教室分页数据
     * <p>
     * 该方法用于根据指定的分页参数、排序方式以及搜索条件获取教室信息的分页结果。返回的结果包含当前页的数据记录、总记录数等信息。
     * </p>
     *
     * @param page    当前页码，从1开始
     * @param size    每页显示的记录数
     * @param isDesc  是否降序排列，如果为 {@code true} 则按降序排列，否则按升序排列
     * @param keyword 搜索关键词，用于在教室名称或编号中进行模糊搜索
     * @param tag     教室标签，用于筛选具有特定标签的教室
     * @param type    教室类型，用于筛选特定类型的教室
     * @return 返回一个包含教室分页数据的 {@code PageDTO<ClassroomDTO>} 对象
     */
    PageDTO<ClassroomInfoDTO> getClassroomPage(
            int page,
            int size,
            boolean isDesc,
            @Nullable String keyword,
            @Nullable String tag,
            @Nullable String type
    );
}
