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
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.BackAddClassroomDTO;
import com.frontleaves.scheduling.models.dto.ClassroomImportDTO;
import com.frontleaves.scheduling.models.dto.base.*;
import com.frontleaves.scheduling.models.dto.merge.ClassroomAndTypeDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomLiteDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.BatchAddClassroomVO;
import com.frontleaves.scheduling.models.vo.ClassroomVO;
import com.frontleaves.scheduling.services.ClassroomService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

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
@Slf4j
@Service
public class ClassroomLogic extends BaseClassroomLogic implements ClassroomService {

    private final TablesChairsTypeDAO tablesChairsTypeDAO;

    /**
     * 教室逻辑处理构造函数
     * <p>
     * 该构造函数用于初始化教室逻辑处理类，通过注入多个数据访问对象来提供对教室、标签、类型、校园和建筑等信息的访问与操作能力。
     * <p>
     *
     * @param classroomTagDAO  用于管理教室标签的数据访问对象
     * @param classroomTypeDAO 用于管理教室类型的数据访问对象
     * @param classroomDAO     用于管理教室基本信息的数据访问对象
     * @param campusDAO        用于管理校园信息的数据访问对象
     * @param buildingDAO      用于管理建筑物信息的数据访问对象
     * @param tablesChairsTypeDAO 用于管理桌椅类型的数据访问对象
     */
    public ClassroomLogic(
            ClassroomTagDAO classroomTagDAO,
            ClassroomTypeDAO classroomTypeDAO,
            ClassroomDAO classroomDAO,
            CampusDAO campusDAO,
            BuildingDAO buildingDAO,
            TablesChairsTypeDAO tablesChairsTypeDAO
    ) {
        super(classroomTagDAO, classroomTypeDAO, classroomDAO, campusDAO, buildingDAO);
        this.tablesChairsTypeDAO = tablesChairsTypeDAO;
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
     * 该方法用于根据给定的教室 UUID 获取对应的教室信息。如果找到匹配的记录，则返回一个 {@code ClassroomInfoDTO} 对象，否则返回 {@code null}。
     * 返回的对象包含教室的基本信息、标签、类型、所属校区及所在楼宇等详细信息。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符
     * @return 返回与给定教室 UUID 匹配的教室信息，如果没有找到匹配的记录则返回 {@code null}
     */
    @Override
    @Nullable
    public ClassroomInfoDTO getClassroomByUuid(String classroomUuid) {
        // 从 DAO 获取 DO 对象
        ClassroomDO classroomDO = classroomDAO.getClassroomByUuid(classroomUuid);
        if (classroomDO == null) {
            return null;
        }

        // 在 Logic 层进行 DO 到 DTO 的转换
        return new ClassroomInfoDTO()
                .setClassroom(BeanUtil.toBean(classroomDO, ClassroomDTO.class))
                .setTag(getTagListForJson(classroomDO.getTag()))
                .setType(this.cacheSaveClassroomType(classroomDO.getType()))
                .setCampus(this.cacheSaveCampus(classroomDO.getCampusUuid()))
                .setBuilding(this.cacheSaveBuilding(classroomDO.getBuildingUuid()));
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

    @Override
    public List<ClassroomLiteDTO> listClassroomLite(String keyword) {
        return Optional.ofNullable(classroomDAO.getClassroomByStatus(true))
                .map(classroom -> {
                    if (keyword != null && !keyword.isBlank()) {
                        return classroom.stream()
                                .filter(classroomDO -> classroomDO.getNumber().contains(keyword) || classroomDO.getName().contains(keyword))
                                .toList();
                    }
                    return classroom;
                })
                .map(classroom ->
                        classroom.stream()
                                .map(classroomDO -> BeanUtil.toBean(classroomDO, ClassroomLiteDTO.class))
                                .toList()
                )
                .orElse(List.of());
    }

    /**
     * 读取教室导入通知文本文件
     * 该方法尝试从类路径下的"notes/classroom-import-notice.txt"文件中读取通知内容
     * 如果文件存在，则读取并返回文件内容；如果文件不存在或读取过程中发生异常，则返回默认的注意事项文本
     *
     * @return 文件内容或默认的注意事项文本
     */
    private @NotNull String readClassroomNoticeFile() {
        try {
            // 从资源文件夹读取 notice.txt
            Resource resource = new ClassPathResource("notes/classroom-import-notice.txt");
            // 尝试读取资源
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    // 读取并返回文件内容
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                throw new IOException("文件不存在");
            }
        } catch (IOException e) {
            // 如果读取失败，返回默认文本
            return """
                    注意事项：
                    1. 请严格按照模板格式填写信息
                    2. 标有"*"的为必填项
                    3. 必备信息请按照示例表格填写
                    4. 请勿修改模板结构
                    5. 状态请填入1(启用)或0(禁用)
                    6. 标签多个时请用英文逗号分隔 
                    """;
        }
    }

    /**
     * 获取教室导入模板的字节数组
     * <p>
     * 该方法生成用于批量导入教室信息的Excel模板，返回包含模板数据的字节数组。
     * 模板包含必填字段、可选字段的说明以及示例数据。
     * </p>
     *
     * @return 包含教室导入模板的字节数组
     */
    @Override
    public byte[] getClassroomImportTemplate() {
        // 创建ExcelWriter对象
        ExcelWriter writer = ExcelUtil.getWriter(true);

        // 创建居中样式
        CellStyle centerStyle = writer.getWorkbook().createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 创建自动换行的样式
        CellStyle wrapStyle = writer.getWorkbook().createCellStyle();
        wrapStyle.cloneStyleFrom(centerStyle);
        wrapStyle.setWrapText(true);

        // 设置列宽
        for (int i = 0; i <= 25; i++) {
            Sheet sheet = writer.getSheet();
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth == sheet.getDefaultColumnWidth() * 256) {
                currentWidth = 2048;
            }
            sheet.setColumnWidth(i, currentWidth * 2);
        }

        // 合并第一行的前16列，并设置居中
        writer.getSheet().addMergedRegion(new CellRangeAddress(0, 0, 0, 16));

        // 写入标题到合并的单元格
        Cell titleCell = writer.getSheet().createRow(0).createCell(0);
        titleCell.setCellValue("导入教室模板");
        titleCell.setCellStyle(centerStyle);

        // 写入表头（第1行）
        writer.writeCellValue(0, 1, "*校区名称");
        writer.writeCellValue(1, 1, "*楼栋名称");
        writer.writeCellValue(2, 1, "*教室编号");
        writer.writeCellValue(3, 1, "*教室名称");
        writer.writeCellValue(4, 1, "*楼层");
        writer.writeCellValue(5, 1, "*教室类型");
        writer.writeCellValue(6, 1, "教室标签");
        writer.writeCellValue(7, 1, "*教室容量");
        writer.writeCellValue(8, 1, "*是否是考场");
        writer.writeCellValue(9, 1, "考场容量");
        writer.writeCellValue(10, 1, "*是否是多媒体教室");
        writer.writeCellValue(11, 1, "*是否有空调");
        writer.writeCellValue(12, 1, "*教室状态");
        writer.writeCellValue(13, 1, "教室描述");
        writer.writeCellValue(14, 1, "管理部门");
        writer.writeCellValue(15, 1, "*教室面积");
        writer.writeCellValue(16, 1, "桌椅类型");

        // 创建红色字体样式
        Font redFont = writer.getWorkbook().createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);

        CellStyle redWrapStyle = writer.getWorkbook().createCellStyle();
        redWrapStyle.cloneStyleFrom(wrapStyle);
        redWrapStyle.setFont(redFont);

        // 读取注意事项文本
        String noticeText = this.readClassroomNoticeFile();

        // 合并注意事项单元格
        writer.getSheet().addMergedRegion(new CellRangeAddress(1, 20, 17, 20));

        // 写入注意事项，并应用自动换行和红色样式
        Cell noticeCell = writer.getOrCreateRow(1).createCell(17);
        noticeCell.setCellValue(noticeText);
        noticeCell.setCellStyle(redWrapStyle);

        // 添加参考数据标题
        writer.writeCellValue(21, 2, "校区模板");
        writer.writeCellValue(22, 2, "楼栋模板");
        writer.writeCellValue(23, 2, "教室类型模板");
        writer.writeCellValue(24, 2, "教室标签模板");
        writer.writeCellValue(25, 2, "桌椅类型模板");

        // 获取并写入校区列表
        List<CampusDO> campusList = campusDAO.getAllCampus();
        if (campusList != null && !campusList.isEmpty()) {
            for (int i = 0; i < campusList.size(); i++) {
                writer.writeCellValue(21, i + 3, campusList.get(i).getCampusName());
            }
        }

        // 获取并写入楼栋列表
        List<BuildingDO> buildingList = buildingDAO.getBuildingListByKey("");
        if (buildingList != null && !buildingList.isEmpty()) {
            for (int i = 0; i < buildingList.size(); i++) {
                writer.writeCellValue(22, i + 3, buildingList.get(i).getBuildingName());
            }
        }

        // 获取并写入教室类型列表
        List<ClassroomTypeDO> typeList = classroomTypeDAO.getTypes();
        if (typeList != null && !typeList.isEmpty()) {
            for (int i = 0; i < typeList.size(); i++) {
                writer.writeCellValue(23, i + 3, typeList.get(i).getName());
            }
        }

        // 获取并写入教室标签列表
        List<ClassroomTagDO> tagList = classroomTagDAO.getTags();
        if (tagList != null && !tagList.isEmpty()) {
            StringBuilder tagExample = new StringBuilder();
            for (int i = 0; i < tagList.size(); i++) {
                if (i > 0) {
                    tagExample.append(",");
                }
                tagExample.append(tagList.get(i).getName());

                // 单独写入每个标签，方便查看
                writer.writeCellValue(24, i + 3, tagList.get(i).getName());
            }

            // 写入标签组合示例
            if (tagList.size() > 1) {
                // 靠左不是居中
                CellStyle leftStyle = writer.getWorkbook().createCellStyle();
                leftStyle.setAlignment(HorizontalAlignment.LEFT);
                writer.writeCellValue(21, 1, "多个标签示例: " + tagExample);
                writer.getSheet().getRow(1).getCell(17).setCellStyle(leftStyle);
            }
        }

        // 写入桌椅类型示例
        List<TablesChairsTypeDO> chairsTypeList = tablesChairsTypeDAO.getTablesChairsTypeList();
        if (chairsTypeList != null && !chairsTypeList.isEmpty()) {
            for (int i = 0; i < chairsTypeList.size(); i++) {
                writer.writeCellValue(25, i + 3, chairsTypeList.get(i).getName());
            }
        }

        // 写入示例数据（第2行）
        writer.writeCellValue(0, 2, "示例校区");
        writer.writeCellValue(1, 2, "示例楼栋");
        writer.writeCellValue(2, 2, "101");
        writer.writeCellValue(3, 2, "示例教室");
        writer.writeCellValue(4, 2, "1");
        writer.writeCellValue(5, 2, "普通教室");
        writer.writeCellValue(6, 2, "电子屏,投影仪");
        writer.writeCellValue(7, 2, "60");
        writer.writeCellValue(8, 2, "1");
        writer.writeCellValue(9, 2, "40");
        writer.writeCellValue(10, 2, "1");
        writer.writeCellValue(11, 2, "1");
        writer.writeCellValue(12, 2, "1");
        writer.writeCellValue(13, 2, "示例描述");
        writer.writeCellValue(14, 2, "教务处");
        writer.writeCellValue(15, 2, "80.0");
        writer.writeCellValue(16, 2, "固定桌椅");

        // 将Excel写入字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        writer.close();
        return outputStream.toByteArray();
    }

    /**
     * 解析Excel文件为教室列表
     * <p>
     * 该方法解析Excel文件中的教室数据，从第3行开始读取（跳过标题和示例行）
     * </p>
     *
     * @param excelBytes Excel文件字节数组
     * @return 教室导入对象列表
     */
    private List<ClassroomImportDTO> parseExcelToClassroomList(byte[] excelBytes) {
        // 首先解析Excel文件获取行数据列表，从第3行开始读取
        List<List<Object>> rowList = ProjectUtil.parseExcelToRowList(excelBytes, 3, 17);
        // 创建结果列表
        List<ClassroomImportDTO> classroomList = new ArrayList<>();

        // 处理每一行数据
        for (List<Object> row : rowList) {
            try {
                // 确保行数据至少有一个元素，避免处理完全空的行
                if (!row.isEmpty() && row.stream().anyMatch(cell -> cell != null && !cell.toString().trim().isEmpty())) {
                    ClassroomImportDTO classroom = new ClassroomImportDTO();

                    // 设置教室数据（注意处理可能的null值）
                    classroom.setCampusName(row.size() > 0 && row.get(0) != null ? row.get(0).toString() : null);
                    classroom.setBuildingName(row.size() > 1 && row.get(1) != null ? row.get(1).toString() : null);
                    classroom.setNumber(row.size() > 2 && row.get(2) != null ? row.get(2).toString() : null);
                    classroom.setName(row.size() > 3 && row.get(3) != null ? row.get(3).toString() : null);
                    classroom.setFloor(row.size() > 4 && row.get(4) != null ? row.get(4).toString() : null);
                    classroom.setType(row.size() > 5 && row.get(5) != null ? row.get(5).toString() : null);
                    classroom.setTag(row.size() > 6 && row.get(6) != null ? row.get(6).toString() : null);

                    // 处理数字类型字段
                    if (row.size() > 7 && row.get(7) != null && !row.get(7).toString().isEmpty()) {
                        try {
                            classroom.setCapacity(Integer.parseInt(row.get(7).toString().trim()));
                        } catch (NumberFormatException e) {
                            classroom.setCapacity(null);
                        }
                    }

                    // 处理布尔类型字段
                    if (row.size() > 8 && row.get(8) != null && !row.get(8).toString().isEmpty()) {
                        String value = row.get(8).toString().trim();
                        classroom.setExaminationRoom("1".equals(value) || "true".equalsIgnoreCase(value) || "是".equals(value));
                    }

                    // 处理考场容量
                    if (row.size() > 9 && row.get(9) != null && !row.get(9).toString().isEmpty()) {
                        try {
                            classroom.setExaminationRoomCapacity(Integer.parseInt(row.get(9).toString().trim()));
                        } catch (NumberFormatException e) {
                            classroom.setExaminationRoomCapacity(null);
                        }
                    }

                    // 多媒体教室标志
                    if (row.size() > 10 && row.get(10) != null && !row.get(10).toString().isEmpty()) {
                        String value = row.get(10).toString().trim();
                        classroom.setIsMultimedia("1".equals(value) || "true".equalsIgnoreCase(value) || "是".equals(value));
                    }

                    // 空调标志
                    if (row.size() > 11 && row.get(11) != null && !row.get(11).toString().isEmpty()) {
                        String value = row.get(11).toString().trim();
                        classroom.setIsAirConditioned("1".equals(value) || "true".equalsIgnoreCase(value) || "是".equals(value));
                    }

                    // 教室状态
                    if (row.size() > 12 && row.get(12) != null && !row.get(12).toString().isEmpty()) {
                        String value = row.get(12).toString().trim();
                        classroom.setStatus("1".equals(value) || "true".equalsIgnoreCase(value) || "是".equals(value));
                    }

                    // 教室描述
                    classroom.setDescription(row.size() > 13 && row.get(13) != null ? row.get(13).toString() : null);

                    // 管理部门
                    classroom.setManagementDepartment(row.size() > 14 && row.get(14) != null ? row.get(14).toString() : null);

                    // 教室面积
                    if (row.size() > 15 && row.get(15) != null && !row.get(15).toString().isEmpty()) {
                        try {
                            classroom.setArea(new BigDecimal(row.get(15).toString().trim()));
                        } catch (NumberFormatException e) {
                            classroom.setArea(null);
                        }
                    }

                    // 桌椅类型
                    classroom.setTablesChairsType(row.size() > 16 && row.get(16) != null ? row.get(16).toString() : null);

                    classroomList.add(classroom);
                }
            } catch (Exception e) {
                // 记录异常并继续处理下一行
                log.error("解析教室数据时出错: {}", e.getMessage());
            }
        }
        return classroomList;
    }

    /**
     * 验证批量导入教室数据并返回处理后的文件
     * <p>
     * 该方法用于验证通过Base64编码传入的Excel文件，确保其格式正确并且可以用于教室信息的批量导入。
     * </p>
     *
     * @param batchAddClassroomVO 包含Excel文件的Base64编码和导入设置的对象
     * @return 处理后的Excel文件字节数组
     */
    @Override
    public byte[] verifyClassroomBatchAndBackFile(BatchAddClassroomVO batchAddClassroomVO) {
        if (batchAddClassroomVO.getFile() == null || batchAddClassroomVO.getFile().isEmpty()) {
            throw new BusinessException("文件不能为空", ErrorCode.BODY_ERROR);
        }

        // 解码Base64文件
        String base64File = batchAddClassroomVO.getFile();
        if (base64File.contains(",")) {
            base64File = base64File.split(",")[1];
        }

        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(base64File);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("文件解码失败，请检查文件格式", ErrorCode.BODY_ERROR);
        }

        try {
            // 解析Excel文件中的教室数据
            List<ClassroomImportDTO> classroomList = parseExcelToClassroomList(fileBytes);

            // 创建ExcelWriter对象
            ExcelWriter writer = ExcelUtil.getWriter(true);

            // 设置列宽
            for (int i = 0; i <= 19; i++) {
                Sheet sheet = writer.getSheet();
                sheet.setColumnWidth(i, 4096); // 设置统一宽度
            }

            // 设置标题行
            writer.writeCellValue(0, 0, "校区名称");
            writer.writeCellValue(1, 0, "楼栋名称");
            writer.writeCellValue(2, 0, "教室编号");
            writer.writeCellValue(3, 0, "教室名称");
            writer.writeCellValue(4, 0, "楼层");
            writer.writeCellValue(5, 0, "教室类型");
            writer.writeCellValue(6, 0, "教室标签");
            writer.writeCellValue(7, 0, "教室容量");
            writer.writeCellValue(8, 0, "是否考场");
            writer.writeCellValue(9, 0, "考场容量");
            writer.writeCellValue(10, 0, "是否多媒体");
            writer.writeCellValue(11, 0, "是否有空调");
            writer.writeCellValue(12, 0, "教室状态");
            writer.writeCellValue(13, 0, "教室描述");
            writer.writeCellValue(14, 0, "管理部门");
            writer.writeCellValue(15, 0, "教室面积");
            writer.writeCellValue(16, 0, "桌椅类型");
            writer.writeCellValue(17, 0, "验证状态");
            writer.writeCellValue(18, 0, "错误信息");
            writer.writeCellValue(19, 0, "行号");

            // 创建样式
            CellStyle normalStyle = writer.getWorkbook().createCellStyle();
            CellStyle errorStyle = writer.getWorkbook().createCellStyle();
            CellStyle warnStyle = writer.getWorkbook().createCellStyle();

            // 设置错误样式（红色）
            Font redFont = writer.getWorkbook().createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            errorStyle.setFont(redFont);

            // 设置警告样式（橙色）
            Font orangeFont = writer.getWorkbook().createFont();
            orangeFont.setColor(IndexedColors.ORANGE.getIndex());
            warnStyle.setFont(orangeFont);

            // 逐行写入和验证数据
            for (int i = 0; i < classroomList.size(); i++) {
                ClassroomImportDTO classroom = classroomList.get(i);
                int rowIndex = i + 1; // Excel行从1开始（0是标题行）

                // 写入数据
                writer.writeCellValue(0, rowIndex, classroom.getCampusName());
                writer.writeCellValue(1, rowIndex, classroom.getBuildingName());
                writer.writeCellValue(2, rowIndex, classroom.getNumber());
                writer.writeCellValue(3, rowIndex, classroom.getName());
                writer.writeCellValue(4, rowIndex, classroom.getFloor());
                writer.writeCellValue(5, rowIndex, classroom.getType());
                writer.writeCellValue(6, rowIndex, classroom.getTag());
                writer.writeCellValue(7, rowIndex, classroom.getCapacity());
                writer.writeCellValue(8, rowIndex, classroom.getExaminationRoom() != null ?
                        (classroom.getExaminationRoom() ? "1" : "0") : null);
                writer.writeCellValue(9, rowIndex, classroom.getExaminationRoomCapacity());
                writer.writeCellValue(10, rowIndex, classroom.getIsMultimedia() != null ?
                        (classroom.getIsMultimedia() ? "1" : "0") : null);
                writer.writeCellValue(11, rowIndex, classroom.getIsAirConditioned() != null ?
                        (classroom.getIsAirConditioned() ? "1" : "0") : null);
                writer.writeCellValue(12, rowIndex, classroom.getStatus() != null ?
                        (classroom.getStatus() ? "1" : "0") : null);
                writer.writeCellValue(13, rowIndex, classroom.getDescription());
                writer.writeCellValue(14, rowIndex, classroom.getManagementDepartment());
                writer.writeCellValue(15, rowIndex, classroom.getArea());
                writer.writeCellValue(16, rowIndex, classroom.getTablesChairsType());
                writer.writeCellValue(19, rowIndex, i + 4); // 实际Excel文件中的行号（标题行+示例行+起始索引3）

                try {
                    // 验证数据完整性
                    validateClassroomData(classroom, i);
                    writer.writeCellValue(17, rowIndex, "通过");
                } catch (BusinessException e) {
                    // 设置错误信息和状态
                    writer.writeCellValue(17, rowIndex, "错误");
                    writer.writeCellValue(18, rowIndex, e.getMessage());

                    // 设置错误行的样式
                    for (int j = 0; j <= 18; j++) {
                        Cell cell = writer.getOrCreateCell(j, rowIndex);
                        cell.setCellStyle(errorStyle);
                    }
                }
            }

            // 将Excel写入字节数组
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writer.flush(outputStream, true);
            writer.close();
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("文件处理失败: " + e.getMessage(), ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }

    /**
     * 验证教室数据的完整性和合法性
     *
     * @param classroom 教室导入数据对象
     * @param index     数据索引，用于错误消息显示
     * @throws BusinessException 如果数据验证失败
     */
    private void validateClassroomData(ClassroomImportDTO classroom, int index) throws BusinessException {
        int rowNumber = index + 4; // Excel实际行号（标题行+示例行+起始索引3）

        // 验证必填字段
        if (classroom.getCampusName() == null || classroom.getCampusName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行校区名称不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getBuildingName() == null || classroom.getBuildingName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行楼栋名称不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getNumber() == null || classroom.getNumber().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行教室编号不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getName() == null || classroom.getName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行教室名称不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getFloor() == null || classroom.getFloor().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行楼层不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getType() == null || classroom.getType().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行教室类型不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getCapacity() == null) {
            throw new BusinessException("第" + rowNumber + "行教室容量不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getExaminationRoom() == null) {
            throw new BusinessException("第" + rowNumber + "行是否考场不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getIsMultimedia() == null) {
            throw new BusinessException("第" + rowNumber + "行是否多媒体教室不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getIsAirConditioned() == null) {
            throw new BusinessException("第" + rowNumber + "行是否有空调不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getStatus() == null) {
            throw new BusinessException("第" + rowNumber + "行教室状态不能为空", ErrorCode.BODY_ERROR);
        }

        if (classroom.getArea() == null) {
            throw new BusinessException("第" + rowNumber + "行教室面积不能为空", ErrorCode.BODY_ERROR);
        }

        // 验证校区和楼栋是否存在
        CampusDO campusDO = campusDAO.getCampusByName(classroom.getCampusName());
        if (campusDO == null) {
            throw new BusinessException("第" + rowNumber + "行校区名称不存在: " + classroom.getCampusName(), ErrorCode.BODY_ERROR);
        }

        BuildingDO buildingDO = buildingDAO.getBuildingByName(classroom.getBuildingName());
        if (buildingDO == null) {
            throw new BusinessException("第" + rowNumber + "行楼栋名称不存在: " + classroom.getBuildingName(), ErrorCode.BODY_ERROR);
        }

        // 验证校区和楼栋的关联
        if (!buildingDO.getCampusUuid().equals(campusDO.getCampusUuid())) {
            throw new BusinessException("第" + rowNumber + "行楼栋 '" + classroom.getBuildingName() +
                    "' 不属于校区 '" + classroom.getCampusName() + "'", ErrorCode.BODY_ERROR);
        }

        // 验证教室类型是否存在
        ClassroomTypeDO typeDO = classroomTypeDAO.getTypeByName(classroom.getType());
        if (typeDO == null) {
            throw new BusinessException("第" + rowNumber + "行教室类型不存在: " + classroom.getType(), ErrorCode.BODY_ERROR);
        }

        // 验证数据类型和格式
        if (classroom.getCapacity() <= 0) {
            throw new BusinessException("第" + rowNumber + "行教室容量必须大于0", ErrorCode.BODY_ERROR);
        }

        if (Boolean.TRUE.equals(classroom.getExaminationRoom()) &&
            (classroom.getExaminationRoomCapacity() == null || classroom.getExaminationRoomCapacity() <= 0)) {
            throw new BusinessException("第" + rowNumber + "行考场容量必须大于0", ErrorCode.BODY_ERROR);
        }

        // 验证教室编号是否已存在
        ClassroomDO existingClassroom = classroomDAO.getClassroomByNumber(classroom.getNumber());
        if (existingClassroom != null) {
            throw new BusinessException("第" + rowNumber + "行教室编号已存在: " + classroom.getNumber(), ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 批量导入教室信息，不忽略错误
     * <p>
     * 该方法用于批量导入教室信息，当遇到任何数据错误时会立即停止导入并抛出异常。
     * </p>
     *
     * @param file Excel文件的字节数组
     * @return 包含导入结果统计的对象
     */
    @Override
    @Transactional
    public BackAddClassroomDTO batchImportNoIgnoreError(byte[] file) {
        // 解析Excel文件
        List<ClassroomImportDTO> classroomList = parseExcelToClassroomList(file);
        log.debug("第一个教室信息{}", classroomList.get(0));

        int totalCount = classroomList.size();
        int successCount = 0;

        // 不忽略错误模式，任何错误都会中断导入
        for (int i = 0; i < classroomList.size(); i++) {
            ClassroomImportDTO classroomImport = classroomList.get(i);

            // 验证教室数据
            validateClassroomData(classroomImport, i);

            // 获取相关实体
            CampusDO campusDO = campusDAO.getCampusByName(classroomImport.getCampusName());
            BuildingDO buildingDO = buildingDAO.getBuildingByName(classroomImport.getBuildingName());
            ClassroomTypeDO typeDO = classroomTypeDAO.getTypeByName(classroomImport.getType());

            // 处理标签
            String tagJson = null;
            if (classroomImport.getTag() != null && !classroomImport.getTag().isEmpty()) {
                JSONArray tagArray = new JSONArray();
                String[] tags = classroomImport.getTag().split(",");
                for (String tagName : tags) {
                    tagName = tagName.trim();
                    if (!tagName.isEmpty()) {
                        ClassroomTagDO tagDO = classroomTagDAO.getTagByName(tagName);
                        if (tagDO != null) {
                            tagArray.put(tagDO.getClassTagUuid());
                        } else {
                            // 创建新标签
                            ClassroomTagDO newTagDO = new ClassroomTagDO()
                                    .setName(tagName)
                                    .setDescription("通过导入创建的标签");
                            classroomTagDAO.save(newTagDO);
                            tagArray.put(newTagDO.getClassTagUuid());
                        }
                    }
                }
                tagJson = tagArray.toString();
            }

            // 处理管理部门
            String departmentUuid = null;
            if (classroomImport.getManagementDepartment() != null && !classroomImport.getManagementDepartment().isEmpty()) {
                // 假设有一个service可以根据部门名称获取部门UUID
                // departmentUuid = departmentService.getDepartmentUuidByName(classroomImport.getManagementDepartment());
            }

            // 处理桌椅类型
            String tablesChairsTypeUuid = null;
            if (classroomImport.getTablesChairsType() != null && !classroomImport.getTablesChairsType().isEmpty()) {
                // 假设有一个service可以根据桌椅类型名称获取UUID
                // tablesChairsTypeUuid = tablesChairsTypeService.getTablesChairsTypeUuidByName(classroomImport.getTablesChairsType());
            }

            // 创建教室DO对象
            ClassroomDO classroomDO = new ClassroomDO()
                    .setNumber(classroomImport.getNumber())
                    .setName(classroomImport.getName())
                    .setCampusUuid(campusDO.getCampusUuid())
                    .setBuildingUuid(buildingDO.getBuildingUuid())
                    .setFloor(classroomImport.getFloor())
                    .setType(typeDO.getClassTypeUuid())
                    .setTag(tagJson)
                    .setCapacity(classroomImport.getCapacity())
                    .setExaminationRoom(classroomImport.getExaminationRoom())
                    .setExaminationRoomCapacity(classroomImport.getExaminationRoomCapacity())
                    .setIsMultimedia(classroomImport.getIsMultimedia())
                    .setIsAirConditioned(classroomImport.getIsAirConditioned())
                    .setStatus(classroomImport.getStatus())
                    .setDescription(classroomImport.getDescription())
                    .setManagementDepartment(departmentUuid)
                    .setArea(classroomImport.getArea())
                    .setTablesChairsType(tablesChairsTypeUuid);

            // 保存教室数据
            classroomDAO.save(classroomDO);
            successCount++;
        }

        // 清除缓存
        classroomDAO.deleteClassroomCache();

        // 返回结果
        return new BackAddClassroomDTO()
                .setTotalCount(totalCount)
                .setSuccessCount(successCount)
                .setFailedCount(0)
                .setFailedDetails(null);
    }

    /**
     * 批量导入教室信息，忽略错误
     * <p>
     * 该方法用于批量导入教室信息，当遇到数据错误时会继续处理其他数据，并记录错误信息。
     * </p>
     *
     * @param file Excel文件的字节数组
     * @return 包含导入结果统计和错误详情的对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BackAddClassroomDTO batchImportIgnoreError(byte[] file) {
        // 解析Excel文件
        List<ClassroomImportDTO> classroomList = parseExcelToClassroomList(file);
        BackAddClassroomDTO backAddClassroomDTO = new BackAddClassroomDTO();

        // 创建失败详情列表
        List<BackAddClassroomDTO.FailedDetail> failedDetails = new ArrayList<>();
        int successCount = 0;

        // 循环处理每条教室记录
        for (int i = 0; i < classroomList.size(); i++) {
            try {
                ClassroomImportDTO classroomImport = classroomList.get(i);

                // 验证教室数据
                validateClassroomData(classroomImport, i);

                // 获取相关实体
                CampusDO campusDO = campusDAO.getCampusByName(classroomImport.getCampusName());
                BuildingDO buildingDO = buildingDAO.getBuildingByName(classroomImport.getBuildingName());
                ClassroomTypeDO typeDO = classroomTypeDAO.getTypeByName(classroomImport.getType());

                // 处理标签
                String tagJson = null;
                if (classroomImport.getTag() != null && !classroomImport.getTag().isEmpty()) {
                    JSONArray tagArray = new JSONArray();
                    String[] tags = classroomImport.getTag().split(",");
                    for (String tagName : tags) {
                        tagName = tagName.trim();
                        if (!tagName.isEmpty()) {
                            ClassroomTagDO tagDO = classroomTagDAO.getTagByName(tagName);
                            if (tagDO != null) {
                                tagArray.put(tagDO.getClassTagUuid());
                            } else {
                                // 创建新标签
                                ClassroomTagDO newTagDO = new ClassroomTagDO()
                                        .setName(tagName)
                                        .setDescription("通过导入创建的标签");
                                classroomTagDAO.save(newTagDO);
                                tagArray.put(newTagDO.getClassTagUuid());
                            }
                        }
                    }
                    tagJson = tagArray.toString();
                }

                // 处理管理部门
                String departmentUuid = null;
                if (classroomImport.getManagementDepartment() != null && !classroomImport.getManagementDepartment().isEmpty()) {
                    // 假设有一个service可以根据部门名称获取部门UUID
                    // departmentUuid = departmentService.getDepartmentUuidByName(classroomImport.getManagementDepartment());
                }

                // 处理桌椅类型
                String tablesChairsTypeUuid = null;
                if (classroomImport.getTablesChairsType() != null && !classroomImport.getTablesChairsType().isEmpty()) {
                    // 假设有一个service可以根据桌椅类型名称获取UUID
                    // tablesChairsTypeUuid = tablesChairsTypeService.getTablesChairsTypeUuidByName(classroomImport.getTablesChairsType());
                }

                // 创建教室DO对象
                ClassroomDO classroomDO = new ClassroomDO()
                        .setNumber(classroomImport.getNumber())
                        .setName(classroomImport.getName())
                        .setCampusUuid(campusDO.getCampusUuid())
                        .setBuildingUuid(buildingDO.getBuildingUuid())
                        .setFloor(classroomImport.getFloor())
                        .setType(typeDO.getClassTypeUuid())
                        .setTag(tagJson)
                        .setCapacity(classroomImport.getCapacity())
                        .setExaminationRoom(classroomImport.getExaminationRoom())
                        .setExaminationRoomCapacity(classroomImport.getExaminationRoomCapacity())
                        .setIsMultimedia(classroomImport.getIsMultimedia())
                        .setIsAirConditioned(classroomImport.getIsAirConditioned())
                        .setStatus(classroomImport.getStatus())
                        .setDescription(classroomImport.getDescription())
                        .setManagementDepartment(departmentUuid)
                        .setArea(classroomImport.getArea())
                        .setTablesChairsType(tablesChairsTypeUuid);

                // 保存教室数据
                classroomDAO.save(classroomDO);
                successCount++;
            } catch (Exception e) {
                // 创建失败详情
                BackAddClassroomDTO.FailedDetail failedDetail = new BackAddClassroomDTO.FailedDetail()
                        .setRow(i + 4) // Excel实际行号（标题行+示例行+起始索引3）
                        .setReason(e.getMessage());
                failedDetails.add(failedDetail);
            }
        }

        // 清除缓存
        classroomDAO.deleteClassroomCache();

        // 设置统计结果
        return backAddClassroomDTO
                .setTotalCount(classroomList.size())
                .setSuccessCount(successCount)
                .setFailedCount(failedDetails.size())
                .setFailedDetails(failedDetails.isEmpty() ? null : failedDetails);
    }

    @Override
    public List<ClassroomDTO> getClassroomUuidsByBuildingId(String buildingId) {
        List<ClassroomDO> classroomDOList = classroomDAO.getClassroomByBuilding(buildingId);
        if (classroomDOList == null || classroomDOList.isEmpty()) {
            return Collections.emptyList();
        }
        return classroomDOList.stream()
                .map(classroomDO -> BeanUtil.toBean(classroomDO, ClassroomDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * 根据UUID获取教室及其类型信息，如果教室不存在或类型信息获取失败，则抛出相应异常
     * @param buildingUuid 教室的唯一标识符
     * @return 返回包含教室及其类型信息的DTO对象
     * @throws BusinessException 如果教室不存在或获取教室类型时发生错误，则抛出此异常
     */
    @Override
    public @NotNull List<ClassroomAndTypeDTO> getClassroomAndTypeByUuidWihError(String buildingUuid) {
        //根据UUID查询教室信息
        List<ClassroomDO> classroomDOList = classroomDAO.getClassroomByBuilding(buildingUuid);
        //如果教室信息为空，则抛出不存在的异常
        if (classroomDOList != null && classroomDOList.isEmpty()) {
            throw new BusinessException("教室不存在", ErrorCode.NOT_EXIST);
        }
        // 获取所有教室类型信息并缓存到Map中
        Map<String, ClassroomTypeDO> classroomTypeDoMap = classroomTypeDAO.getTypes().stream()
                .collect(Collectors.toMap(ClassroomTypeDO::getClassTypeUuid, type -> type));
        List<ClassroomAndTypeDTO> classroomAndTypeDTOList = new ArrayList<>();
        if (classroomDOList != null) {
            for (ClassroomDO classroomDO : classroomDOList) {
                // 从Map中获取对应的教室类型信息
                ClassroomTypeDO classroomTypeDO = classroomTypeDoMap.get(classroomDO.getType());
                if (classroomTypeDO == null) {
                    throw new BusinessException("教室类型不存在", ErrorCode.NOT_EXIST);
                }
                ClassroomAndTypeDTO classroomAndTypeDTO = new ClassroomAndTypeDTO();
                classroomAndTypeDTO.setClassroom(BeanUtil.toBean(classroomDO, ClassroomDTO.class))
                        .setClassroomType(BeanUtil.toBean(classroomTypeDO, ClassroomTypeDTO.class));
                classroomAndTypeDTOList.add(classroomAndTypeDTO);
            }
        }
        return classroomAndTypeDTOList;
    }

}
