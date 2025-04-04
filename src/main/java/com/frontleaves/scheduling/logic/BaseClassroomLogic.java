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
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.base.BuildingDTO;
import com.frontleaves.scheduling.models.dto.base.CampusDTO;
import com.frontleaves.scheduling.models.dto.base.ClassroomTypeDTO;
import com.frontleaves.scheduling.models.entity.ClassroomTagDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 教室逻辑操作
 * <p>
 * 该类提供了对教室相关的逻辑操作，包括但不限于教室的创建、删除、查询等。
 * 通过调用此类中的方法，可以实现对教室数据的有效管理与维护。
 * 每个方法都针对特定的业务需求设计，旨在提供清晰且易于使用的接口来处理教室相关的任务。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RequiredArgsConstructor
class BaseClassroomLogic {
    protected final ClassroomTagDAO classroomTagDAO;
    protected final ClassroomTypeDAO classroomTypeDAO;
    protected final ClassroomDAO classroomDAO;
    protected final CampusDAO campusDAO;
    protected final BuildingDAO buildingDAO;

    protected final Map<String, ClassroomTypeDTO> classroomTypeCache = new HashMap<>();
    protected final Map<String, CampusDTO> campusCache = new HashMap<>();
    protected final Map<String, BuildingDTO> buildingCache = new HashMap<>();

    /**
     * 缓存教室类型数据
     * <p>
     * 该方法首先尝试将传入的 {@code typeDTO} 转换为 {@code ClassroomTypeDTO} 对象，然后检查缓存中是否已存在具有相同 {@code classTypeUuid} 的教室类型。
     * 如果缓存中已经存在，则直接返回缓存中的对象；否则，将新的 {@code ClassroomTypeDTO} 对象添加到缓存中并返回。
     *
     * @param typeDTO 教室类型的唯一标识符
     * @return 返回与传入参数对应的 {@code ClassroomTypeDTO} 对象，如果缓存中已有则返回缓存中的对象，否则返回新创建的对象
     */
    ClassroomTypeDTO cacheSaveClassroomType(@NotNull String typeDTO) {
        ClassroomTypeDTO classroomTypeDTO = BeanUtil.toBean(classroomTypeDAO.getTypeByUuid(typeDTO), ClassroomTypeDTO.class);
        if (classroomTypeCache.containsKey(classroomTypeDTO.getClassTypeUuid())) {
            return classroomTypeCache.get(classroomTypeDTO.getClassTypeUuid());
        } else {
            classroomTypeCache.put(classroomTypeDTO.getClassTypeUuid(), classroomTypeDTO);
            return classroomTypeDTO;
        }
    }

    /**
     * 缓存校区数据
     * <p>
     * 该方法首先尝试将传入的 {@code campusDTO} 转换为 {@code CampusDTO} 对象，然后检查缓存中是否已存在具有相同 {@code campusUuid} 的校区。
     * 如果缓存中已经存在，则直接返回缓存中的对象；否则，将新的 {@code CampusDTO} 对象添加到缓存中并返回。
     *
     * @param campusDTO 校区的唯一标识符
     * @return 返回与传入参数对应的 {@code CampusDTO} 对象，如果缓存中已有则返回缓存中的对象，否则返回新创建的对象
     */
    CampusDTO cacheSaveCampus(@NotNull String campusDTO) {
        CampusDTO campus = BeanUtil.toBean(campusDAO.getCampusByUuid(campusDTO), CampusDTO.class);
        if (campusCache.containsKey(campus.getCampusUuid())) {
            return campusCache.get(campus.getCampusUuid());
        } else {
            campusCache.put(campus.getCampusUuid(), campus);
            return campus;
        }
    }

    /**
     * 缓存建筑物数据
     * <p>
     * 该方法首先尝试将传入的 {@code buildingDTO} 转换为 {@code BuildingDTO} 对象，然后检查缓存中是否已存在具有相同 {@code buildingUuid} 的建筑物。
     * 如果缓存中已经存在，则直接返回缓存中的对象；否则，将新的 {@code BuildingDTO} 对象添加到缓存中并返回。
     *
     * @param buildingDTO 建筑物的唯一标识符
     * @return 返回与传入参数对应的 {@code BuildingDTO} 对象，如果缓存中已有则返回缓存中的对象，否则返回新创建的对象
     */
    BuildingDTO cacheSaveBuilding(@NotNull String buildingDTO) {
        BuildingDTO building = BeanUtil.toBean(buildingDAO.getBuildingByUuid(buildingDTO), BuildingDTO.class);
        if (buildingCache.containsKey(building.getBuildingUuid())) {
            return buildingCache.get(building.getBuildingUuid());
        } else {
            buildingCache.put(building.getBuildingUuid(), building);
            return building;
        }
    }

    /**
     * 从JSON字符串获取标签列表
     * <p>
     * 该方法用于将存储在数据库中的JSON格式的标签UUID列表转换为标签DTO对象列表。
     * 首先解析JSON字符串获取标签UUID数组，然后根据每个UUID查询完整的标签信息并构建DTO对象。
     * </p>
     *
     * @param tagJson 标签UUID的JSON字符串
     * @return 标签DTO对象列表，如果输入为null或空字符串，则返回空列表
     */
    protected List<ClassroomTagDTO> getTagListForJson(String tagJson) {
        if (tagJson == null || tagJson.isEmpty()) {
            return List.of();
        }
        try {
            JSONArray jsonArray = new JSONArray(tagJson);
            List<ClassroomTagDTO> result = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String tagUuid = jsonArray.getString(i);
                ClassroomTagDO tagDO = classroomTagDAO.getTagByUuid(tagUuid);
                if (tagDO != null) {
                    result.add(BeanUtil.toBean(tagDO, ClassroomTagDTO.class));
                }
            }
            return result;
        } catch (Exception e) {
            log.error("解析教室标签JSON失败: {}", e.getMessage());
            return List.of();
        }
    }
}
