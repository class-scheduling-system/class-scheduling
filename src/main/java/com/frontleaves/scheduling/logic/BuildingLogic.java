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
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.exceptions.lib.DataInvalidException;
import com.frontleaves.scheduling.exceptions.lib.DataNotFoundException;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.dto.base.BuildingDTO;
import com.frontleaves.scheduling.models.dto.base.CampusDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.lite.BuildingLiteDTO;
import com.frontleaves.scheduling.models.dto.merge.PrepareBuildingDTO;
import com.frontleaves.scheduling.models.entity.BuildingDO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.BatchAddBuildingVO;
import com.frontleaves.scheduling.services.BuildingService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingLogic implements BuildingService {
    private final BuildingDAO buildingDAO;
    private final CampusDAO campusDAO;

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
    public PageDTO<BuildingDTO> getBuildingPage(int page, int size, boolean isDesc, String keyword) {
        Page<BuildingDO> buildingList = buildingDAO.getBuildingPage(page, size, isDesc, keyword);

        // 直接获取BuildingDO列表并进行手动转换
        List<BuildingDTO> buildingDTOList = new ArrayList<>();
        for (BuildingDO buildingDO : buildingList.getRecords()) {
            BuildingDTO buildingDTO = BeanUtil.toBean(buildingDO, BuildingDTO.class);
            // 手动设置CampusDTO
            if (buildingDO.getCampusUuid() != null) {
                CampusDO campusDO = campusDAO.getCampusByUuid(buildingDO.getCampusUuid());
                if (campusDO != null) {
                    buildingDTO.setCampus(BeanUtil.toBean(campusDO, CampusDTO.class));
                }
            }
            buildingDTOList.add(buildingDTO);
        }

        // 创建新的PageDTO并手动设置
        PageDTO<BuildingDTO> pageDTO = new PageDTO<>(buildingList.getTotal(), buildingList.getSize());
        pageDTO.setCurrent(buildingList.getCurrent());
        pageDTO.setRecords(JSONUtil.toJsonStr(buildingDTOList), BuildingDTO.class);

        return pageDTO;
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
    @Nullable
    public BuildingDTO getBuildingByUuidOrName(@NotNull String building) {
        BuildingDO buildingDO = Optional.of(building)
                .filter(data -> data.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .map(buildingDAO::getBuildingByUuid)
                .orElseGet(() -> buildingDAO.getBuildingByName(building));
        return Optional.ofNullable(buildingDO)
                .map(data -> {
                    BuildingDTO buildingDTO = BeanUtil.toBean(data, BuildingDTO.class);
                    // 设置CampusDTO
                    if (data.getCampusUuid() != null) {
                        CampusDO campusDO = campusDAO.getCampusByUuid(data.getCampusUuid());
                        if (campusDO != null) {
                            buildingDTO.setCampus(BeanUtil.toBean(campusDO, CampusDTO.class));
                        }
                    }
                    return buildingDTO;
                }).orElse(null);
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
            // 获取校区信息
            CampusDO campusDO = campusDAO.getCampusByUuid(campusUuid);
            CampusDTO campusDTO = null;
            if (campusDO != null) {
                campusDTO = BeanUtil.toBean(campusDO, CampusDTO.class);
            }

            // 手动转换BuildingDO列表为BuildingDTO列表
            List<BuildingDTO> buildingDTOList = new ArrayList<>();
            for (BuildingDO buildingDO : buildingList.getRecords()) {
                BuildingDTO buildingDTO = BeanUtil.toBean(buildingDO, BuildingDTO.class);
                // 设置CampusDTO
                if (campusDTO != null) {
                    buildingDTO.setCampus(campusDTO);
                }
                buildingDTOList.add(buildingDTO);
            }

            // 创建新的PageDTO并手动设置
            PageDTO<BuildingDTO> pageDTO = new PageDTO<>(buildingList.getTotal(), buildingList.getSize());
            pageDTO.setCurrent(buildingList.getCurrent());
            pageDTO.setRecords(JSONUtil.toJsonStr(buildingDTOList), BuildingDTO.class);

            return pageDTO;
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
            buildingDAO.addBuilding(buildingDO);
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
     * 如果没有找到匹配的教学楼，将抛出一个 {@link BusinessException} 异常，提示"教学楼不存在"。
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

    /**
     * 根据关键词获取建筑列表
     * <p>
     * 此方法旨在通过特定关键词来检索一组建筑信息，并以轻量级数据传输对象的形式返回
     * 它首先调用DAO层方法获取数据库中的建筑实体列表，然后检查列表是否为空或不存在
     * 如果列表为空或不存在，则返回一个空列表否则，它将实体列表转换为DTO列表并返回
     *
     * @param keyword 搜索关键词，用于在数据库中查找匹配的建筑
     * @return 包含建筑信息的列表，如果找不到匹配项，则返回空列表
     */
    @Override
    public List<BuildingLiteDTO> getBuildingList(String keyword) {
        // 通过关键词从数据库中获取建筑实体列表
        List<BuildingDO> buildingList = buildingDAO.getBuildingListByKey(keyword);

        // 检查列表是否为空或不存在，如果为空或不存在，则返回一个空列表
        if (buildingList == null || buildingList.isEmpty()) {
            return List.of();
        }

        // 将非空的建筑实体列表转换为轻量级数据传输对象列表并返回
        return BeanUtil.copyToList(buildingList, BuildingLiteDTO.class);
    }


    /**
     * 从给定的列表中提取教学楼信息并创建一个BuildingImportDTO对象
     * 该方法主要用于从导入的列表数据中解析出教学楼的相关信息，并将其封装到DTO对象中
     *
     * @param rowlist 包含教学楼信息的列表，不能为空
     * @return 返回一个填充了教学楼信息的BuildingImportDTO对象，不能为空
     */
    private static @NotNull BuildingImportDTO getBuildingImportDTO(@NotNull List<Object> rowlist) {
        // 创建一个新的BuildingImportDTO对象
        BuildingImportDTO building = new BuildingImportDTO();

        // 从列表中提取教学楼信息并设置到DTO对象中
        building.setCampusName(rowlist.get(0).toString().trim())
                .setBuildingName(rowlist.get(1).toString().trim());

        // 处理状态字段 - 可以是数字(1/0)或文本(启用/禁用)
        String status = rowlist.get(2).toString().trim();
        // 默认为启用状态
        if ("1".equals(status) || "启用".equals(status)) {
            building.setStatus(true);
        } else {
            building.setStatus(!"0".equals(status) && !"禁用".equals(status));
        }

        // 返回填充了教学楼信息的DTO对象
        return building;
    }


    /**
     * 读取建筑导入通知文件的内容
     * <p>
     * 此方法尝试从类路径下的特定位置读取一个名为 "building-import-notice.txt" 的文件
     * 如果文件存在，则读取其内容并以字符串形式返回如果文件不存在，或在读取过程中发生错误，
     * 则返回一个预定义的默认注意事项文本
     *
     * @return 文件内容或默认注意事项文本
     */
    private @NotNull String readBuildingNoticeFile() {
        try {
            // 从资源文件夹读取 notice.txt
            Resource resource = new ClassPathResource("notes/building-import-notice.txt");
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
            return """
                    注意事项：
                    1. 请严格按照模板填写信息
                    2. 所有信息必须准确无误
                    3. 请勿修改模板结构""";
        }
    }


    /**
     * 准备校园数据
     * <p>
     * 本方法用于准备校园的相关数据，通过访问数据库获取所有校园信息，并将其转换为所需的DTO格式
     * 主要包括从数据库模型对象（DAO）到传输对象（DTO）的转换，以及数据的收集和封装
     *
     * @return List<CampusDTO> 包含所有转换后的校园数据的传输对象
     */
    @Override
    public List<ListOfCampusDTO> prepareCampusData() {
        return Optional.ofNullable(campusDAO.getCampusList())
                .map(list -> list.stream()
                        .map(campus -> BeanUtil.toBean(campus, ListOfCampusDTO.class))
                        .toList())
                .orElse(List.of());
    }

    /**
     * 获取导入教学楼信息的Excel模板
     *
     * @param prepareBuildingExampleDTO 包含校区数据的DTO
     * @return 包含模板的字节数组
     */
    //生成模板
    @Override
    public byte[] getBuildingImportTemplate(List<ListOfCampusDTO> prepareBuildingExampleDTO) {
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
        for (int i = 0; i <= 6; i++) {
            Sheet sheet = writer.getSheet();
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth == sheet.getDefaultColumnWidth() * 256) {
                currentWidth = 2048;
            }
            sheet.setColumnWidth(i, currentWidth * 2);
        }

        // 合并第一行的前3列，并设置居中
        writer.getSheet().addMergedRegion(new CellRangeAddress(0, 0, 0, 2));

        // 写入标题到合并的单元格
        Cell titleCell = writer.getSheet().createRow(0).createCell(0);
        titleCell.setCellValue("导入教学楼模板");
        titleCell.setCellStyle(centerStyle);

        // 写入表头（第1行）
        writer.writeCellValue(0, 1, "所属校区");
        writer.writeCellValue(1, 1, "教学楼名称");
        writer.writeCellValue(2, 1, "状态");

        // 创建红色字体样式
        Font redFont = writer.getWorkbook().createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);

        CellStyle redWrapStyle = writer.getWorkbook().createCellStyle();
        redWrapStyle.cloneStyleFrom(wrapStyle);
        redWrapStyle.setFont(redFont);

        // 读取注意事项文本并写入Excel
        String noticeText = this.readBuildingNoticeFile();
        writer.getSheet().addMergedRegion(new CellRangeAddress(1, 20, 3, 5));
        Cell noticeCell = writer.getSheet().getRow(1).createCell(3);
        noticeCell.setCellValue(noticeText);
        noticeCell.setCellStyle(redWrapStyle);

        // 写入校区名称列表标题
        writer.writeCellValue(6, 1, "校区名称列表");

        // 填充校区数据
        if (prepareBuildingExampleDTO != null) {
            for (int i = 0; i < prepareBuildingExampleDTO.size(); i++) {
                writer.writeCellValue(6, i + 2, prepareBuildingExampleDTO.get(i).getCampusName());
            }
        }

        // 填充状态示例
        writer.writeCellValue(7, 1, "状态示例");
        writer.writeCellValue(7, 2, "1 (启用)");
        writer.writeCellValue(7, 3, "0 (禁用)");

        // 输出到字节流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        writer.close();

        // 返回模板的字节数组
        return outputStream.toByteArray();
    }


    /**
     * 验证批量添加教学楼信息的请求体和Excel文件
     * 此方法确保提供的批量添加教学楼信息是有效的，包括检查信息是否为空、文件是否为空、
     * 文件是否是有效的Excel格式，以及文件大小是否超过限制
     *
     * @param batchAddBuildingVO 批量添加教学楼的视图对象，包含要添加的教学楼信息和Excel文件
     * @return 返回解码后的Excel文件字节数组，用于后续处理
     * @throws IllegalArgumentException 如果批量添加信息为空、文件为空、Base64解码失败、
     *                                  文件大小超过限制或文件不是有效的Excel格式
     */
    //文件验证
    @Override
    public byte[] verifyBuildingBatchAndBackFile(BatchAddBuildingVO batchAddBuildingVO) {
        // 1. 检查 VO 对象是否为空
        if (batchAddBuildingVO == null) {
            throw new IllegalArgumentException("批量添加教学楼信息不能为空");
        }

        // 2. 检查 file 字段是否为空
        String base64File = batchAddBuildingVO.getFile();
        if (base64File == null || base64File.trim().isEmpty()) {
            throw new IllegalArgumentException("Excel文件不能为空");
        }

        // 3. 处理Base64字符串，去除可能的前缀和干扰字符
        // 如果有前缀，移除前缀
        if (base64File.contains(",")) {
            base64File = base64File.split(",")[1];
        }
        // 移除可能的空格、换行符等
        base64File = base64File.replaceAll("\\s", "");

        // 4. 解码 Base64 字符串为字节数组
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(base64File);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Base64 解码失败: " + e.getMessage());
        }

        // 5. 检查文件大小（10MB = 10 * 1024 * 1024 字节）
        long fileSizeInBytes = fileBytes.length;
        // 10MB
        long maxSizeInBytes = 10L * 1024 * 1024;
        if (fileSizeInBytes > maxSizeInBytes) {
            throw new IllegalArgumentException("文件大小超过10MB限制");
        }

        // 6. 验证是否为 Excel 文件
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            // 尝试读取为 Excel 工作簿，这会在非 Excel 文件时抛出异常
            WorkbookFactory.create(inputStream);
        } catch (Exception e) {
            throw new IllegalArgumentException("提供的文件不是有效的 Excel 文件: " + e.getMessage());
        }

        // 返回解码后的文件字节数组，用于后续处理
        return fileBytes;
    }


    /**
     * 解析Excel字节流为BuildingImportDTO对象列表
     * 该方法负责读取Excel文件的内容，从指定的起始行开始，检查一定数量的列，并将每行数据转换为BuildingImportDTO对象
     *
     * @param excelBytes Excel文件的字节流，用于读取Excel文件内容
     * @return 解析后的BuildingImportDTO对象列表，包含Excel中解析出的建筑信息
     */
    //解析文件
    private static List<BuildingImportDTO> parseExcelToBuildingList(byte[] excelBytes) {
        // 解析Excel文件获取行数据列表
        List<List<Object>> rowList = ProjectUtil.parseExcelToRowList(excelBytes, 2, 3);
        log.debug("原生列表{}", rowList);
        // 创建结果列表
        List<BuildingImportDTO> buildingList = new ArrayList<>();
        // 处理每一行数据，即使数据不完整也创建对象
        for (List<Object> row : rowList) {
            try {
                // 确保行数据至少有一个元素，避免处理完全空的行
                if (!row.isEmpty() && row.size() >= 3) {
                    // 转换为BuildingImportDTO对象并添加到结果列表
                    BuildingImportDTO building = getBuildingImportDTO(row);
                    buildingList.add(building);
                }
            } catch (Exception e) {
                // 记录异常并继续处理下一行
                log.error("解析教学楼数据时出错: {}", e.getMessage());
            }
        }
        return buildingList;
    }


    /**
     * 获取导入教学楼所需的基础数据
     * <p>
     * 该方法获取系统中所有的校区数据，用于校验导入的教学楼信息。
     * </p>
     *
     * @return 包含所有校区数据的列表
     */
    private List<CampusDO> fetchImportBaseBuildingData() {
        List<CampusDO> campusList = campusDAO.getAllCampus();
        if (campusList == null || campusList.isEmpty()) {
            throw new BusinessException("系统中不存在校区信息，请先添加校区", ErrorCode.NOT_EXIST);
        }
        return campusList;
    }

    /**
     * 根据校园名称查找校园信息
     *
     * @param campusList 校园信息列表，从中查找指定名称的校园
     * @param campusName 要查找的校园名称
     * @return 匹配的校园信息对象，如果找不到则抛出异常
     * @throws DataNotFoundException 当指定名称的校园不存在时抛出此异常
     */
    private CampusDO findCampusByName(List<CampusDO> campusList, String campusName) throws DataNotFoundException {
        // 使用流处理从校园列表中过滤出与指定名称匹配的校园信息
        CampusDO campusDO = campusList.stream()
                .filter(campus -> campus.getCampusName().equals(campusName))
                .findFirst()
                .orElse(null);

        // 如果没有找到匹配的校园信息，则抛出异常
        if (campusDO == null) {
            throw new DataNotFoundException(DataNotFoundException.TypeEnum.CAMPUS_NAME_NOT_FOUND);
        }

        // 返回找到的校园信息对象
        return campusDO;
    }

    /**
     * 验证教学楼信息的合法性
     * <p>
     * 该方法用于验证导入的教学楼信息是否符合系统要求，包括校区名称是否存在、教学楼名称是否为空等。
     * </p>
     *
     * @param buildingList    教学楼导入列表，包含待验证的教学楼信息
     * @param campusList      系统中的校区列表，用于校验校区名称
     * @param i               当前验证的教学楼在列表中的索引
     * @return 验证通过的校区对象
     * @throws BusinessException 当教学楼信息中的校区名称不存在，或教学楼名称为空时抛出
     */
    //数据验证
    private CampusDO validateBuilding(
            @NotNull List<BuildingImportDTO> buildingList,
            @NotNull List<CampusDO> campusList,
            int i
    ) throws BusinessException {
        BuildingImportDTO building = buildingList.get(i);
        // 行号（假设从第3行开始）
        int rowNumber = i + 3;

        // 验证教学楼名称是否为空
        if (building.getBuildingName() == null || building.getBuildingName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行教学楼名称不能为空", ErrorCode.BODY_ERROR);
        }

        // 验证校区名称是否为空
        if (building.getCampusName() == null || building.getCampusName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行校区名称不能为空", ErrorCode.BODY_ERROR);
        }

        // 通过校区名称查找对应的校区对象
        CampusDO campusDO = campusList.stream()
                .filter(campus -> campus.getCampusName().equals(building.getCampusName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("第" + rowNumber + "行校区名称不存在: " + building.getCampusName(), ErrorCode.BODY_ERROR));

        // 检查教学楼名称是否已存在
        BuildingDO existingBuilding = buildingDAO.getBuildingByName(building.getBuildingName());
        if (existingBuilding != null) {
            throw new BusinessException("第" + rowNumber + "行教学楼名称已存在: " + building.getBuildingName(), ErrorCode.EXISTED);
        }

        return campusDO;
    }


    /**
     * 批量导入教学楼信息，忽略错误
     * <p>
     * 该方法用于批量导入教学楼信息，并在遇到错误时忽略这些错误。
     * </p>
     *
     * @param file Excel文件的字节数组，包含要导入的教学楼信息
     * @return 返回包含导入结果的BackAddBuildingDTO对象
     */
    //数据导入
    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public BackAddBuildingDTO batchImportIgnoreError(byte[] file) {
        // 解析Excel文件
        List<BuildingImportDTO> buildingList = parseExcelToBuildingList(file);
        BackAddBuildingDTO backAddBuildingDTO = new BackAddBuildingDTO();

        // 获取所有校区数据
        List<CampusDO> campusList = fetchImportBaseBuildingData();

        // 创建失败详情列表
        List<BackAddBuildingDTO.FailedDetail> failedDetails = new ArrayList<>();
        int successCount = 0;

        // 循环处理每条教学楼记录
        for (int i = 0; i < buildingList.size(); i++) {
            try {
                BuildingImportDTO building = buildingList.get(i);

                // 验证教学楼名称
                if (building.getBuildingName() == null || building.getBuildingName().isEmpty()) {
                    throw new DataInvalidException(DataInvalidException.TypeEnum.NAME_EMPTY_ERROR);
                }

                // 验证并获取校区
                CampusDO campusDO = findCampusByName(campusList, building.getCampusName());

                // 创建教学楼对象
                BuildingDO buildingDO = new BuildingDO()
                        .setBuildingName(building.getBuildingName())
                        .setCampusUuid(campusDO.getCampusUuid())
                        .setStatus(building.getStatus());

                // 保存教学楼数据
                List<BackAddBuildingDTO.FailedDetail> saveFailedDetails = buildingDAO.saveBuildingIgnoreError(buildingDO, i);

                if (saveFailedDetails.isEmpty()) {
                    successCount++;
                } else {
                    failedDetails.addAll(saveFailedDetails);
                }
            } catch (RuntimeException e) {
                BackAddBuildingDTO.FailedDetail failedDetail = getFailedDetail(e, i);
                failedDetails.add(failedDetail);
            }
        }

        // 删除分页和列表缓存
        buildingDAO.deleteBuildingCache();

        // 设置统计结果
        return backAddBuildingDTO.setTotalCount(buildingList.size())
                .setSuccessCount(successCount)
                .setFailedCount(failedDetails.size())
                .setFailedDetails(failedDetails.isEmpty() ? null : failedDetails);
    }

    /**
     * 批量导入教学楼信息，不忽略错误
     * <p>
     * 该方法用于批量导入教学楼信息，并在遇到错误时抛出异常。
     * </p>
     *
     * @param file Excel文件的字节数组，包含要导入的教学楼信息
     * @return 返回包含导入结果的BackAddBuildingDTO对象
     */
    @Override
    @Transactional
    public BackAddBuildingDTO batchImportNoIgnoreError(byte[] file) {
        // 解析Excel文件
        List<BuildingImportDTO> buildingList = parseExcelToBuildingList(file);
        log.debug("第一个教学楼信息{}", buildingList.get(0));

        // 获取基础数据
        List<CampusDO> campusList = fetchImportBaseBuildingData();

        // 不忽略警告提醒报错
        for (int i = 0; i < buildingList.size(); i++) {
            // 使用validateBuilding方法验证教学楼信息，返回校区对象
            CampusDO campusDO = validateBuilding(buildingList, campusList, i);
            BuildingImportDTO building = buildingList.get(i);

            // 创建新的教学楼对象
            BuildingDO buildingDO = new BuildingDO()
                    .setBuildingName(building.getBuildingName())
                    .setCampusUuid(campusDO.getCampusUuid())
                    .setStatus(building.getStatus());

            // 使用非忽略错误的保存方法
            buildingDAO.saveBuildingBackError(buildingDO, i);
        }

        // 删除分页和列表缓存
        buildingDAO.deleteBuildingCache();

        //成功
        return new BackAddBuildingDTO()
                .setTotalCount(buildingList.size())
                .setFailedCount(0)
                .setSuccessCount(buildingList.size())
                .setFailedDetails(null);
    }


    /**
     * 获取失败的详细信息
     * 当添加建筑的过程中遇到错误时，此方法用于生成包含失败详情的对象
     *
     * @param e 异常对象，表示添加建筑过程中遇到的错误
     * @param i 表示失败的行号，用于追踪问题所在位置
     * @return 返回一个FailedDetail对象，包含失败的行号和原因
     */
    private static BackAddBuildingDTO.@NotNull FailedDetail getFailedDetail(RuntimeException e, int i) {
        // 创建一个FailedDetail对象来存储失败的详细信息
        BackAddBuildingDTO.FailedDetail failedDetail = new BackAddBuildingDTO.FailedDetail();
        // 设置失败的行号，+3是因为数据开始于第4行
        failedDetail.setRow(i + 3);
        // 根据异常类型设置失败的原因
        if (e instanceof DataNotFoundException error) {
            // 如果是数据不存在异常，设置具体原因
            failedDetail.setReason("数据不存在：" + error.getReason());
        } else if (e instanceof DataInvalidException error) {
            // 如果是数据无效异常，设置具体原因
            failedDetail.setReason("数据无效：" + error.getReason());
        } else {
            // 如果是其他未知异常，设置通用错误信息
            failedDetail.setReason("未知错误：" + e.getMessage());
        }
        // 返回包含失败信息的对象
        return failedDetail;
    }
}
