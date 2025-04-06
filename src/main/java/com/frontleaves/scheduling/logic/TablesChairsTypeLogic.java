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
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TablesChairsTypeDAO;
import com.frontleaves.scheduling.models.dto.TablesChairsTypeLiteDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TablesChairsTypeDTO;
import com.frontleaves.scheduling.models.entity.TablesChairsTypeDO;
import com.frontleaves.scheduling.services.TablesChairsTypeService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 桌椅类型逻辑处理类
 * <p>
 * 该类实现了 {@code TablesChairsTypeService} 接口，提供了对桌椅类型的管理功能。
 * 包括添加、删除、更新和查询桌椅类型等操作。具体实现细节在本类中定义。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see TablesChairsTypeService
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TablesChairsTypeLogic implements TablesChairsTypeService {
    private final TablesChairsTypeDAO tablesChairsTypeDAO;

    /**
     * 根据 UUID 获取桌椅类型信息
     * <p>
     * 该方法通过传入的 {@code uuid} 参数，从数据库中查询对应的桌椅类型信息，并将其转换为 {@code TablesChairsTypeDTO} 对象返回。
     * 如果没有找到对应的桌椅类型信息，则返回 {@code null}。
     * </p>
     *
     * @param uuid 桌椅类型的唯一标识符
     * @return 返回与指定 UUID 对应的桌椅类型信息的 DTO 对象，如果没有找到则返回 {@code null}
     */
    @Override
    @Nullable
    public TablesChairsTypeDTO getTablesChairsTypeByUuid(String uuid) {
        TablesChairsTypeDO tablesChairsType = tablesChairsTypeDAO.getTablesChairsTypeByUuid(uuid);
        if (tablesChairsType == null) {
            return null;
        }
        return BeanUtil.toBean(tablesChairsType, TablesChairsTypeDTO.class);
    }

    /**
     * 获取包含关键词的桌椅类型列表
     * <p>
     * 该方法用于分页查询系统中所有名称或相关信息包含指定关键词的桌椅类型信息。
     * 通过传入的页码、每页显示的数量以及是否降序排列来控制返回的数据量和排序方式。
     * 返回的是一个包含桌椅类型数据传输对象 {@code TablesChairsTypeDTO} 的分页结果。
     * </p>
     *
     * @param page    当前页码
     * @param size    每页显示的数量
     * @param isDesc  是否降序排列
     * @param keyword 搜索关键词，用于匹配桌椅类型名称或其他相关信息
     * @return 分页的桌椅类型数据传输对象 {@code PageDTO<TablesChairsTypeDTO>}
     */
    @Override
    @NotNull
    public PageDTO<TablesChairsTypeDTO> getTablesChairsTypePage(int page, int size, boolean isDesc, String keyword) {
        Page<TablesChairsTypeDO> tablesChairsTypeList = tablesChairsTypeDAO.getTablesChairsTypePage(page, size, isDesc, keyword);

        // 直接获取 TablesChairsTypeDO 列表并进行手动转换
        List<TablesChairsTypeDTO> tablesChairsTypeDTOList = new ArrayList<>();
        for (TablesChairsTypeDO tablesChairsTypeDO : tablesChairsTypeList.getRecords()) {
            TablesChairsTypeDTO tablesChairsTypeDTO = BeanUtil.toBean(tablesChairsTypeDO, TablesChairsTypeDTO.class);
            tablesChairsTypeDTOList.add(tablesChairsTypeDTO);
        }

        // 创建新的 PageDTO 并手动设置
        PageDTO<TablesChairsTypeDTO> pageDTO = new PageDTO<>(tablesChairsTypeList.getTotal(), tablesChairsTypeList.getSize());
        pageDTO.setCurrent(tablesChairsTypeList.getCurrent());
        pageDTO.setRecords(JSONUtil.toJsonStr(tablesChairsTypeDTOList), TablesChairsTypeDTO.class);

        return pageDTO;
    }

    /**
     * 获取所有桌椅类型的简化列表
     * <p>
     * 该方法用于获取系统中所有桌椅类型的简化信息列表，通常用于下拉选择等场景。
     * 返回的列表包含每个桌椅类型的基本信息，如UUID和名称。
     * </p>
     *
     * @return 桌椅类型简化信息列表
     */
    @Override
    @NotNull
    public List<TablesChairsTypeLiteDTO> getTablesChairsTypeList() {
        List<TablesChairsTypeDO> tablesChairsTypeList = tablesChairsTypeDAO.getTablesChairsTypeList();

        return tablesChairsTypeList.stream()
                .map(type -> BeanUtil.toBean(type, TablesChairsTypeLiteDTO.class))
                .toList();
    }

    /**
     * 添加新的桌椅类型
     * <p>
     * 该方法用于向系统中添加一个新的桌椅类型记录。通过传入桌椅类型名称 {@code name}、描述 {@code description} 和图片 {@code base64Img}，
     * 可以创建一条新的桌椅类型信息。其中，{@code name} 是新增桌椅类型的名称；{@code description} 是对桌椅类型的描述；
     * 而 {@code base64Img} 则是桌椅类型的图片（以 Base64 字符串形式存储）。成功调用此方法后，新的桌椅类型将被保存到数据库中。
     * </p>
     *
     * @param name        桌椅类型名称
     * @param description 桌椅类型描述
     * @param base64Img   桌椅类型图片（Base64 字符串）
     * @return 新添加的桌椅类型信息
     */
    @Override
    public TablesChairsTypeDTO addTablesChairsType(String name, String description, String base64Img) {
        // 检查名称是否已存在
        TablesChairsTypeDO existingType = tablesChairsTypeDAO.getTablesChairsTypeByName(name);
        if (existingType != null) {
            throw new BusinessException(StringConstant.TABLES_CHAIRS_NAME_EXISTS, ErrorCode.EXISTED);
        }

        // 创建新的桌椅类型对象
        TablesChairsTypeDO tablesChairsTypeDO = new TablesChairsTypeDO();
        tablesChairsTypeDO
                .setName(name)
                .setDescription(description)
                .setBase64Img(base64Img);

        // 保存到数据库
        tablesChairsTypeDAO.addTablesChairsType(tablesChairsTypeDO);

        // 返回新添加的桌椅类型信息
        return BeanUtil.toBean(tablesChairsTypeDO, TablesChairsTypeDTO.class);
    }

    /**
     * 更新桌椅类型信息
     * <p>
     * 该方法用于根据提供的参数更新指定的桌椅类型信息。首先通过 {@code uuid} 获取桌椅类型信息，如果桌椅类型存在，
     * 则更新其名称、描述和图片。如果桌椅类型不存在，则抛出业务异常。
     * </p>
     *
     * @param uuid        桌椅类型的唯一标识符
     * @param name        桌椅类型的新名称
     * @param description 桌椅类型的新描述
     * @param base64Img   桌椅类型的新图片（Base64 字符串）
     * @return 更新后的桌椅类型信息
     */
    @Override
    public TablesChairsTypeDTO updateTablesChairsType(String uuid, String name, String description, String base64Img) {
        // 检查桌椅类型是否存在
        TablesChairsTypeDO tablesChairsTypeDO = tablesChairsTypeDAO.getTablesChairsTypeByUuid(uuid);
        if (tablesChairsTypeDO == null) {
            throw new BusinessException(StringConstant.TABLES_CHAIRS_NOT_EXIST, ErrorCode.NOT_EXIST);
        }

        // 如果名称发生变化，检查新名称是否已存在
        if (!tablesChairsTypeDO.getName().equals(name)) {
            TablesChairsTypeDO existingType = tablesChairsTypeDAO.getTablesChairsTypeByName(name);
            if (existingType != null) {
                throw new BusinessException(StringConstant.TABLES_CHAIRS_NAME_EXISTS, ErrorCode.EXISTED);
            }
        }

        // 更新桌椅类型信息
        tablesChairsTypeDO
                .setName(name)
                .setDescription(description)
                .setBase64Img(base64Img);

        // 保存更新
        tablesChairsTypeDAO.updateTablesChairsType(tablesChairsTypeDO);

        // 返回更新后的桌椅类型信息
        return BeanUtil.toBean(tablesChairsTypeDO, TablesChairsTypeDTO.class);
    }

    /**
     * 删除桌椅类型
     * <p>
     * 该方法根据给定的桌椅类型唯一标识 {@code uuid} 删除指定的桌椅类型。
     * 如果存在与给定 UUID 匹配的桌椅类型，则从数据库中删除该桌椅类型。
     * 如果没有找到匹配的桌椅类型，将抛出一个业务异常。
     * </p>
     *
     * @param uuid 桌椅类型的唯一标识
     */
    @Override
    public void deleteTablesChairsType(String uuid) {
        // 检查桌椅类型是否存在
        TablesChairsTypeDO tablesChairsTypeDO = tablesChairsTypeDAO.getTablesChairsTypeByUuid(uuid);
        if (tablesChairsTypeDO == null) {
            throw new BusinessException(StringConstant.TABLES_CHAIRS_NOT_EXIST, ErrorCode.NOT_EXIST);
        }

        // 删除桌椅类型
        tablesChairsTypeDAO.deleteTablesChairsType(tablesChairsTypeDO);
    }
}
