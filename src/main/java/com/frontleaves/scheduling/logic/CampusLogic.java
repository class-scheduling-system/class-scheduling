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
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.CampusDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.services.CampusService;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 校园逻辑处理类
 * <p>
 * 该类通过依赖注入的方式获取 {@link CampusDAO} 实例，并利用其实现对校区数据的访问和操作。主要功能包括根据校区唯一标识符查询校区信息等。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampusLogic implements CampusService {
    private final CampusDAO campusDAO;

    /**
     * 根据校区唯一标识符获取校区信息
     * <p>
     * 该方法通过提供的校区唯一标识符 {@code campusUuid} 查询对应的校区信息，并返回一个包含校区详细信息的 {@link CampusDTO} 对象。
     * 如果找不到与给定 {@code campusUuid} 匹配的校区，则返回 null。
     * </p>
     *
     * @param campusUuid 校区的唯一标识符
     * @return 返回与给定唯一标识符匹配的校区信息，如果未找到则返回 null
     */
    @Override
    @Nullable
    public CampusDTO getCampusByUuid(String campusUuid) {
        CampusDO campusDO = campusDAO.getCampusByUuid(campusUuid);
        if (campusDO == null) {
            return null;
        }
        return BeanUtil.toBean(campusDO, CampusDTO.class);
    }
}
