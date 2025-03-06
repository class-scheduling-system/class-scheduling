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
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.ClassroomDO;
import com.frontleaves.scheduling.models.entity.ClassroomTagDO;
import com.frontleaves.scheduling.models.entity.ClassroomTypeDO;
import com.frontleaves.scheduling.models.vo.ClassroomVO;
import com.frontleaves.scheduling.services.ClassroomService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 教室逻辑处理类，实现了 {@code ClassroomService} 接口。
 * <p>
 * 该类提供了教室管理相关的具体实现，包括添加教室、删除教室、查询教室信息等操作。通过依赖注入的方式，可以与其他服务进行交互，完成复杂的业务逻辑。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
public class ClassroomLogic extends ClassroomLogicOperate implements ClassroomService {

    /**
     * 教室逻辑处理构造函数
     * <p>
     * 该构造函数用于初始化教室逻辑处理类，通过注入多个数据访问对象来提供对教室、标签、类型、校园和建筑等信息的访问与操作能力。
     * <p>
     * @param classroomTagDAO 用于管理教室标签的数据访问对象
     * @param classroomTypeDAO 用于管理教室类型的数据访问对象
     * @param classroomDAO 用于管理教室基本信息的数据访问对象
     * @param campusDAO 用于管理校园信息的数据访问对象
     * @param buildingDAO 用于管理建筑物信息的数据访问对象
     */
    public ClassroomLogic(
            ClassroomTagDAO classroomTagDAO,
            ClassroomTypeDAO classroomTypeDAO,
            ClassroomDAO classroomDAO,
            CampusDAO campusDAO,
            BuildingDAO buildingDAO
    ) {
        super(classroomTagDAO, classroomTypeDAO, classroomDAO, campusDAO, buildingDAO);
    }

    /**
     * 获取教室标签列表
     * <p>
     * 该方法用于从数据库中获取所有教室标签，并将其转换为 {@code ClassroomTagDTO} 对象的列表返回。
     * 通过调用 {@code classroomTagDAO.getTags()} 方法获取数据，
     * 然后使用 Hutool 的 {@code BeanUtil.copyToList} 方法进行对象转换。
     * </p>
     *
     * @return 返回包含所有教室标签的 {@code List<ClassroomTagDTO>} 对象
     */
    @Override
    public List<ClassroomTagDTO> listClassroomTags() {
        List<ClassroomTagDO> tags = classroomTagDAO.getTags();
        return BeanUtil.copyToList(tags, ClassroomTagDTO.class);
    }

    /**
     * 获取教室类型列表
     * <p>
     * 该方法用于从数据库中获取所有教室类型，并将其转换为 {@code ClassroomTypeDTO} 对象的列表返回。
     * 通过调用 {@code classroomTypeDAO.getTypes()} 方法获取数据，
     * 然后使用 Hutool 的 {@code BeanUtil.copyToList} 方法进行对象转换。
     * </p>
     *
     * @return 返回包含所有教室类型的 {@code List<ClassroomTypeDTO>} 对象
     */
    @Override
    public List<ClassroomTypeDTO> listClassroomTypes() {
        List<ClassroomTypeDO> types = classroomTypeDAO.getTypes();
        return BeanUtil.copyToList(types, ClassroomTypeDTO.class);
    }

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
    @Override
    public PageDTO<ClassroomInfoDTO> getClassroomPage(
            int page,
            int size,
            boolean isDesc,
            @Nullable String keyword,
            @Nullable String tag,
            @Nullable String type
    ) {
        String tagUuid = null;
        String typeUuid = null;
        if (tag != null && !tag.isBlank()) {
            ClassroomTagDO getTag = classroomTagDAO.getTagByUuid(tag);
            if (getTag != null) {
                tagUuid = getTag.getClassTagUuid();
            }
        }
        if (type != null && !type.isBlank()) {
            ClassroomTypeDO getType = classroomTypeDAO.getTypeByUuid(type);
            if (getType != null) {
                typeUuid = getType.getClassTypeUuid();
            }
        }
        if (keyword != null && keyword.isBlank()) {
            keyword = null;
        }
        Page<ClassroomDO> classroomPage = classroomDAO.getClassroomPage(page, size, isDesc, keyword, tagUuid, typeUuid);
        PageDTO<ClassroomInfoDTO> classroomInfoDTO = new PageDTO<>();
        BeanUtil.copyProperties(classroomPage, classroomInfoDTO, "records");
        classroomInfoDTO.setRecords(
                classroomPage.getRecords()
                        .stream()
                        .map(getRecord -> new ClassroomInfoDTO()
                                .setClassroom(BeanUtil.toBean(getRecord, ClassroomDTO.class))
                                .setTag(getTagListForJson(getRecord.getTag()))
                                .setType(this.cacheSaveClassroomType(getRecord.getType()))
                                .setCampus(this.cacheSaveCampus(getRecord.getCampusUuid()))
                                .setBuilding(this.cacheSaveBuilding(getRecord.getBuildingUuid())))
                        .toList()
        );
        classroomTypeCache.clear();
        campusCache.clear();
        buildingCache.clear();
        return classroomInfoDTO;
    }

    /**
     * 根据 UUID 获取教室类型
     * <p>
     * 该方法用于根据给定的 UUID 获取对应的教室类型信息。如果找到匹配的记录，则返回一个 {@code ClassroomTypeDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param uuid 教室类型的唯一标识符
     * @return 返回与给定 UUID 匹配的教室类型数据传输对象，如果没有找到匹配的记录则返回 {@code null}
     */
    @Override
    public @Nullable ClassroomTypeDTO getClassroomTypeByUuid(String uuid) {
        ClassroomTypeDO classroomTypeDO = classroomTypeDAO.getTypeByUuid(uuid);
        if (classroomTypeDO == null) {
            return null;
        }
        return BeanUtil.toBean(classroomTypeDO, ClassroomTypeDTO.class);
    }

    /**
     * 根据 UUID 获取教室标签
     * <p>
     * 该方法用于根据给定的 UUID 获取对应的教室标签信息。如果找到匹配的记录，则返回一个 {@code ClassroomTagDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param uuid 教室标签的唯一标识符
     * @return 返回与指定 UUID 匹配的教室标签信息，如果没有找到匹配的记录则返回 {@code null}
     */
    @Override
    public @Nullable ClassroomTagDTO getClassroomTagByUuid(String uuid) {
        ClassroomTagDO classroomTagDO = classroomTagDAO.getTagByUuid(uuid);
        if (classroomTagDO == null) {
            return null;
        }
        return BeanUtil.toBean(classroomTagDO, ClassroomTagDTO.class);
    }

    /**
     * 添加新教室
     * <p>
     * 该方法用于根据传入的 {@code ClassroomVO} 对象添加一个新的教室记录。首先，将 {@code ClassroomVO} 转换为
     * {@code ClassroomDO} 对象并保存到数据库中。接着，通过新创建的教室 UUID 从数据库中获取刚刚添加的教室记录。
     * 如果未能成功获取教室记录，则抛出一个 {@code ServerInternalErrorException} 异常。最后，构建一个包含教室信息、标签、类型、所属校区及所在楼宇等详细信息的
     * {@code ClassroomInfoDTO} 对象并返回。
     *
     * @param classroomVO 用于表示要添加的新教室的基本信息
     * @return 包含新增教室详细信息的 {@code ClassroomInfoDTO} 对象
     */
    @Override
    public ClassroomInfoDTO addClassroom(ClassroomVO classroomVO) {
        ClassroomDO newClassroom = this.classroomDataVerify(classroomVO);
        classroomDAO.save(newClassroom);
        ClassroomDO classroom = classroomDAO.getClassroomByUuid(newClassroom.getClassroomUuid());
        if (classroom == null) {
            throw new ServerInternalErrorException(StringConstant.UNKNOWN_ERROR);
        }
        return new ClassroomInfoDTO()
                .setClassroom(BeanUtil.toBean(classroom, ClassroomDTO.class))
                .setTag(getTagListForJson(classroom.getTag()))
                .setType(BeanUtil.toBean(classroomTypeDAO.getTypeByUuid(classroom.getType()), ClassroomTypeDTO.class))
                .setCampus(BeanUtil.toBean(campusDAO.getCampusByUuid(classroom.getCampusUuid()), CampusDTO.class))
                .setBuilding(BeanUtil.toBean(buildingDAO.getBuildingByUuid(classroom.getBuildingUuid()), BuildingDTO.class));
    }

    /**
     * 根据教室编号获取教室信息
     * <p>
     * 该方法用于根据给定的教室编号获取对应的教室信息。如果找到匹配的记录，则返回一个 {@code ClassroomDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param number 教室编号
     * @return 返回与给定教室编号匹配的教室数据传输对象，如果没有找到匹配的记录则返回 {@code null}
     */
    @Override
    public ClassroomDTO getClassroomByNumber(String number) {
        ClassroomDO classroomDO = classroomDAO.getClassroomByNumber(number);
        if (classroomDO == null) {
            return null;
        }
        return BeanUtil.toBean(classroomDO, ClassroomDTO.class);
    }

    /**
     * 根据教室 UUID 获取教室信息
     * <p>
     * 该方法用于根据给定的教室 UUID 获取对应的教室信息。如果找到匹配的记录，则返回一个 {@code ClassroomDTO} 对象，否则返回 {@code null}。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符
     * @return 返回与给定教室 UUID 匹配的教室数据传输对象，如果没有找到匹配的记录则返回 {@code null}
     */
    @Override
    public ClassroomDTO getClassroomByUuid(String classroomUuid) {
        ClassroomDO classroomDO = classroomDAO.getClassroomByUuid(classroomUuid);
        if (classroomDO == null) {
            return null;
        }
        return BeanUtil.toBean(classroomDO, ClassroomDTO.class);
    }

    /**
     * 编辑教室
     * <p>
     * 该方法用于根据传入的 {@code ClassroomVO} 对象编辑指定的教室。在编辑过程中，会进行一系列数据可用性检查，确保关联的教学楼、校区、教室类型、管理部门以及桌椅类型均存在。
     * 如果任何一项数据不存在，则抛出 {@code BusinessException} 异常，并附带相应的错误码。如果所有数据验证通过，则调用服务层的方法将新的教室信息保存到数据库中，并返回包含成功信息及新教室详情的响应。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符
     * @param classroomVO   包含待编辑教室详细信息的视图对象
     * @return 响应实体，包含操作结果和新创建的教室信息
     */
    @Override
    public ClassroomInfoDTO editClassroom(String classroomUuid, ClassroomVO classroomVO) {
        ClassroomDO classroomDO = this.classroomDataVerify(classroomVO)
                        .setClassroomUuid(classroomUuid);
        classroomDAO.updateClassroom(classroomDO);
        ClassroomDO classroom = classroomDAO.getClassroomByUuid(classroomUuid);
        if (classroom == null) {
            throw new ServerInternalErrorException(StringConstant.UNKNOWN_ERROR);
        }
        return new ClassroomInfoDTO()
                .setClassroom(BeanUtil.toBean(classroom, ClassroomDTO.class))
                .setTag(getTagListForJson(classroom.getTag()))
                .setType(BeanUtil.toBean(classroomTypeDAO.getTypeByUuid(classroom.getType()), ClassroomTypeDTO.class))
                .setCampus(BeanUtil.toBean(campusDAO.getCampusByUuid(classroom.getCampusUuid()), CampusDTO.class))
                .setBuilding(BeanUtil.toBean(buildingDAO.getBuildingByUuid(classroom.getBuildingUuid()), BuildingDTO.class));
    }

    /**
     * 删除教室
     * <p>
     * 根据给定的教室唯一标识符 {@code classroomUuid}，从系统中删除对应的教室记录。
     * 该操作不可逆，请谨慎使用。删除后，与该教室相关的所有数据将被清除。
     *
     * @param classroomUuid 教室的唯一标识符，用于定位需要删除的具体教室
     */
    @Override
    public void deleteClassroom(String classroomUuid) {
        if (!classroomDAO.deleteClassroom(classroomUuid)) {
            throw new BusinessException("教室不存在", ErrorCode.NOT_EXIST);
        }
    }

    /**
     * 验证并转换教室数据
     * <p>
     * 该方法接收一个 {@code ClassroomVO} 对象，将其转换为 {@code ClassroomDO} 对象，并进行必要的验证和处理。
     * 具体包括：将标签字段转换为 JSON 字符串，如果标签为空则设置为默认值 "[]"；如果管理单位或桌椅类型为空白字符串，则设置为 null。
     *
     * @param classroomVO 教室视图对象，包含需要验证和转换的数据
     * @return 经过验证和处理后的教室数据对象
     */
    private @NotNull ClassroomDO classroomDataVerify(@NotNull ClassroomVO classroomVO) {
        ClassroomDO classroomDO = BeanUtil.toBean(classroomVO, ClassroomDO.class);
        if (classroomVO.getTag() != null && !classroomVO.getTag().isEmpty()) {
            classroomDO.setTag(JSONUtil.toJsonStr(classroomVO.getTag()));
        } else {
            classroomDO.setTag("[]");
        }
        if (classroomVO.getManagementDepartment() == null || classroomVO.getManagementDepartment().isBlank()) {
            classroomDO.setManagementDepartment(null);
        }
        if (classroomVO.getTablesChairsType() == null || classroomVO.getTablesChairsType().isBlank()) {
            classroomDO.setTablesChairsType(null);
        }
        return classroomDO;
    }

    /**
     * 从 JSON 字符串中解析标签列表
     * <p>
     * 该方法接收一个包含标签 UUID 的 JSON 字符串，从中提取每个 UUID 并查询数据库获取对应的标签信息，
     * 然后将这些标签信息转换为 {@code ClassroomTagDTO} 对象，并返回一个包含所有标签的列表。
     * 如果传入的 JSON 字符串为空或无法解析，则返回空列表。
     *
     * @param getJsonTags 包含标签 UUID 的 JSON 字符串
     * @return 根据传入的 JSON 字符串解析得到的标签列表
     */
    private @NotNull List<ClassroomTagDTO> getTagListForJson(String getJsonTags) {
        List<ClassroomTagDTO> tags = new ArrayList<>();
        if (getJsonTags != null && !getJsonTags.isBlank()) {
            JSONArray getTags = new JSONArray(getJsonTags);
            getTags.forEach(tagUuidStr -> {
                ClassroomTagDO tagDO = classroomTagDAO.getTagByUuid(tagUuidStr.toString());
                if (tagDO != null) {
                    tags.add(BeanUtil.toBean(tagDO, ClassroomTagDTO.class));
                }
            });
        }
        return tags;
    }
}
