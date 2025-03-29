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
import com.frontleaves.scheduling.daos.TablesChairsTypeDAO;
import com.frontleaves.scheduling.models.dto.base.TablesChairsTypeDTO;
import com.frontleaves.scheduling.models.entity.TablesChairsTypeDO;
import com.frontleaves.scheduling.services.TablesChairsTypeService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
}
