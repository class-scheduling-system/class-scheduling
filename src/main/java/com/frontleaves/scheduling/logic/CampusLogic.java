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
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.frontleaves.scheduling.models.vo.BatchAddCampusVO;
import com.frontleaves.scheduling.models.vo.CampusVO;
import com.frontleaves.scheduling.services.CampusService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
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
     * 添加校园
     *
     * @param campusVO 校园的视图对象，包含需要添加的校园详细信息
     * @return 返回添加成功的校园信息
     */
    @Override
    public CampusDTO addCampus(CampusVO campusVO) {
        //数据交换
        CampusDO campusDO = BeanUtil.copyProperties(campusVO, CampusDO.class)
                .setCampusUuid(UuidUtil.generateUuidNoDash());
        campusDAO.saveCampus(campusDO);
        CampusDO newCampusDO = campusDAO.getCampusByUuid(campusDO.getCampusUuid());
        return BeanUtil.copyProperties(newCampusDO, CampusDTO.class);
    }

    /**
     * 校验添加校区的输入信息是否合法
     * 此方法主要通过检查校区信息的各个字段是否为空或重复来确保数据的合法性
     * 验证不通过将抛出异常，提示相应的错误信息
     *
     * @param campusVO 校区视图对象，包含了校区的相关信息
     * @throws BusinessException 当校区信息不合法时抛出此异常，包含错误信息和错误代码
     */
    @Override
    public void checkAddCampusVO(CampusVO campusVO) {
        if (campusVO == null) {
            throw new BusinessException("校区信息不能为空", ErrorCode.BODY_ERROR);
        }

        checkFieldNotEmpty(campusVO.getCampusName(), "校区名称");
        checkFieldNotEmpty(campusVO.getCampusCode(), "校区编码");
        checkFieldNotEmpty(campusVO.getCampusDesc(), "校区描述");
        checkFieldNotNull(campusVO.getCampusStatus());
        checkFieldNotEmpty(campusVO.getCampusAddress(), "校区地址");
        if (campusDAO.getCampusByCode(campusVO.getCampusCode()) != null) {
            throw new BusinessException("校区编码已存在", ErrorCode.BODY_ERROR);
        }
        if (campusDAO.getCampusByName(campusVO.getCampusName()) != null) {
            throw new BusinessException("校区名称已存在", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 检查字段是否为空
     *
     * @param field     字段
     * @param fieldName 字段名称
     */
    private void checkFieldNotEmpty(String field, String fieldName) {
        if (field == null || field.isEmpty()) {
            throw new BusinessException(fieldName + "不能为空", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 检查字段是否为空
     *
     * @param field 字段
     */
    private void checkFieldNotNull(Object field) {
        if (field == null) {
            throw new BusinessException("校区状态" + "不能为空", ErrorCode.BODY_ERROR);
        }
    }


    /**
     * 检查并更新校区信息
     * 此方法首先验证校区的唯一性标识（UUID），然后检查校区名称和编码的唯一性
     * 如果校区不存在，或名称、编码重复，将抛出异常
     *
     * @param campusUuid 校区的唯一性标识（UUID）
     * @param campusVO   待更新的校区信息对象
     * @return 返回更新前的校区信息对象
     * @throws BusinessException 如果校区不存在或校区名称、编码重复
     */
    @Override
    public CampusDO checkUpdateCampusVO(String campusUuid, CampusVO campusVO) {
        //检查校区是否存在
        CampusDO campusDO = campusDAO.getCampusByUuid(campusUuid);
        if (campusDO == null) {
            throw new BusinessException("校区不存在", ErrorCode.OPERATION_FAILED);
        }

        //检查校区名称是否重复
        if (campusVO.getCampusName() != null
                && !campusVO.getCampusName().equals(campusDO.getCampusName())
                && campusDAO.getCampusByName(campusVO.getCampusName()) != null) {
            throw new BusinessException("校区名称已存在", ErrorCode.BODY_ERROR);
        }

        //检查校区编码是否重复
        if (campusVO.getCampusCode() != null
                && !campusVO.getCampusCode().equals(campusDO.getCampusCode())
                && campusDAO.getCampusByCode(campusVO.getCampusCode()) != null) {
            throw new BusinessException("校区编码已存在", ErrorCode.BODY_ERROR);
        }

        return campusDO;
    }

    /**
     * 更新校区信息
     *
     * @param campusVO    包含要更新的校区信息的视图对象
     * @param campusOldDO 校区的数据对象，包含校区的当前信息
     * @return 返回更新后的校区信息
     */
    @Override
    public CampusDTO updateCampus(CampusVO campusVO, CampusDO campusOldDO) {
        //数据交换
        CampusDO campusDO = exchangeCampus(campusVO, campusOldDO);
        CampusDO newCampusDO = campusDAO.updateCampus(campusDO);
        return BeanUtil.copyProperties(newCampusDO, CampusDTO.class);
    }

    @Override
    public CampusDO checkDeleteCampus(String campusUuid) {
        CampusDO campusDO = campusDAO.getCampusByUuid(campusUuid);
        if (campusDO == null) {
            throw new BusinessException("校区不存在", ErrorCode.OPERATION_FAILED);
        }
        return campusDO;
    }

    @Override
    public void deleteCampus(CampusDO campusDO) {
        if (campusDO == null) {
            throw new BusinessException("删除校区时，校区不存在", ErrorCode.OPERATION_FAILED);
        }
        campusDAO.deleteCampus(campusDO);
    }

    /**
     * 交换校区信息
     * 此方法用于将校区视图对象（VO）中的非空字段值复制到校区数据对象（DO）中，
     * 同时保留原有校区的唯一标识符（UUID）。如果视图对象中的字段为空，则保留数据对象中原有的值。
     *
     * @param campusVO    包含要更新的校区信息的视图对象，不能为空
     * @param campusOldDO 原有的校区数据对象，作为默认值来源，不能为空
     * @return 返回一个包含更新后校区信息的数据对象（DO），该对象一定不为空
     */
    private @NotNull CampusDO exchangeCampus(
            @NotNull CampusVO campusVO, @NotNull CampusDO campusOldDO) {
        CampusDO campusDO = new CampusDO();
        // 保留原有校区的唯一标识符
        campusDO.setCampusUuid(campusOldDO.getCampusUuid());
        // 根据视图对象中的非空字段，更新数据对象中的相应字段
        if (campusVO.getCampusName() != null) {
            campusDO.setCampusName(campusVO.getCampusName());
        }
        if (campusVO.getCampusCode() != null) {
            campusDO.setCampusCode(campusVO.getCampusCode());
        }
        if (campusVO.getCampusDesc() != null) {
            campusDO.setCampusDesc(campusVO.getCampusDesc());
        }
        if (campusVO.getCampusStatus() != null) {
            campusDO.setCampusStatus(campusVO.getCampusStatus());
        }
        if (campusVO.getCampusAddress() != null) {
            campusDO.setCampusAddress(campusVO.getCampusAddress());
        }
        return campusDO;
    }

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

    /**
     * 获取校园信息分页数据
     * <p>
     * 该方法用于根据指定的分页参数和搜索关键字获取校园信息的分页数据。返回的数据包括符合条件的校园记录列表、总记录数、每页大小、当前页码和总页数。
     * </p>
     *
     * @param page    当前页码，从1开始
     * @param size    每页显示的记录数
     * @param isDesc  是否降序排列，默认为false表示升序
     * @param keyword 搜索关键字，可为空。如果提供，则在查询时会根据此关键字进行模糊匹配
     * @return 返回一个包含校园信息分页数据的 {@link PageDTO} 对象
     */
    @Override
    public PageDTO<CampusDTO> getPageOfCampus(int page, int size, boolean isDesc, @Nullable String keyword) {
        Page<CampusDO> campusPage = campusDAO.getPageOfCampus(page, size, isDesc, keyword);
        if (campusPage == null) {
            return new PageDTO<>();
        }
        return ProjectUtil.convertPageToPageDTO(campusPage, CampusDTO.class);
    }

    /**
     * 获取校区列表
     * <p>
     * 该方法用于获取系统中所有校区的简要信息列表。返回的数据为 {@link ListOfCampusDTO} 对象的列表，每个对象包含校区的主键、名称和编码。
     * </p>
     *
     * @return 返回一个包含所有校区简要信息的 {@code List<ListOfCampusDTO>} 列表
     */
    @Override
    public List<ListOfCampusDTO> getCampusList() {
        return campusDAO.getCampusList();
    }

    /**
     * 解析Excel字节流为CampusImportDTO对象列表
     *
     * @param excelBytes Excel文件的字节流
     * @return 解析后的CampusImportDTO对象列表
     */
    private static List<CampusImportDTO> parseExcelToCampusList(byte[] excelBytes) {
        // 解析Excel文件获取行数据列表，从第2行开始，读取7列数据
        List<List<Object>> rowList = ProjectUtil.parseExcelToRowList(excelBytes, 2, 7);
        log.debug("校区原生列表{}", rowList);
        // 创建结果列表
        List<CampusImportDTO> campusList = new ArrayList<>();
        // 处理每一行数据，即使数据不完整也创建对象
        for (List<Object> row : rowList) {
            try {
                // 确保行数据至少有一个元素，避免处理完全空的行
                if (!row.isEmpty() && row.size() >= 5) {
                    // 转换为CampusImportDTO对象并添加到结果列表
                    CampusImportDTO campus = getCampusImportDTO(row);
                    campusList.add(campus);
                }
            } catch (Exception e) {
                // 记录异常并继续处理下一行
                log.error("解析校区数据时出错: {}", e.getMessage());
            }
        }
        return campusList;
    }

    /**
     * 将Excel行数据转换为CampusImportDTO对象
     *
     * @param row Excel行数据
     * @return 转换后的CampusImportDTO对象
     */
    private static @NotNull CampusImportDTO getCampusImportDTO(@NotNull List<Object> row) {
        CampusImportDTO campusImportDTO = new CampusImportDTO();

        // 设置校区名称（第1列）
        if (row.size() > 0 && row.get(0) != null) {
            campusImportDTO.setCampusName(row.get(0).toString());
        }

        // 设置校区编码（第2列）
        if (row.size() > 1 && row.get(1) != null) {
            campusImportDTO.setCampusCode(row.get(1).toString());
        }

        // 设置校区描述（第3列）
        if (row.size() > 2 && row.get(2) != null) {
            campusImportDTO.setCampusDesc(row.get(2).toString());
        }

        // 设置校区状态（第4列）
        if (row.size() > 3 && row.get(3) != null) {
            String statusStr = row.get(3).toString().trim();
            if ("1".equals(statusStr) || "启用".equals(statusStr)) {
                campusImportDTO.setCampusStatus(true);
            } else if ("0".equals(statusStr) || "禁用".equals(statusStr)) {
                campusImportDTO.setCampusStatus(false);
            } else {
                // 默认为启用
                campusImportDTO.setCampusStatus(true);
            }
        } else {
            // 默认为启用
            campusImportDTO.setCampusStatus(true);
        }

        // 设置校区地址（第5列）
        if (row.size() > 4 && row.get(4) != null) {
            campusImportDTO.setCampusAddress(row.get(4).toString());
        }

        // 设置纬度（第6列）
        if (row.size() > 5 && row.get(5) != null) {
            try {
                campusImportDTO.setLatitude(Double.parseDouble(row.get(5).toString()));
            } catch (NumberFormatException e) {
                log.warn("纬度格式错误: {}", row.get(5));
            }
        }

        // 设置经度（第7列）
        if (row.size() > 6 && row.get(6) != null) {
            try {
                campusImportDTO.setLongitude(Double.parseDouble(row.get(6).toString()));
            } catch (NumberFormatException e) {
                log.warn("经度格式错误: {}", row.get(6));
            }
        }

        return campusImportDTO;
    }

    /**
     * 获取校区导入模板的字节数组
     * <p>
     * 该方法生成用于批量导入校区信息的Excel模板，返回包含模板数据的字节数组。
     * </p>
     *
     * @return 包含校区导入模板的字节数组
     */
    @Override
    public byte[] getCampusImportTemplate() {
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
        for (int i = 0; i <= 9; i++) {
            // A 列和 C 列需要宽一点
            Sheet sheet = writer.getSheet();
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth == sheet.getDefaultColumnWidth() * 256) {
                currentWidth = 2048;
            }
            if (i == 0) { // A 列
                sheet.setColumnWidth(i, currentWidth * 3); // A 列宽度增加
            } else if (i == 2) { // C 列
                sheet.setColumnWidth(i, currentWidth * 5); // C 列宽度增加
            } else {
                sheet.setColumnWidth(i, currentWidth * 2); // 其他列保持原样
            }
        }

        // 合并第一行的前7列，并设置居中
        writer.getSheet().addMergedRegion(new CellRangeAddress(0, 0, 0, 6));

        // 写入标题到合并的单元格
        Cell titleCell = writer.getSheet().createRow(0).createCell(0);
        titleCell.setCellValue("导入校区模板");
        titleCell.setCellStyle(centerStyle);

        // 创建红色字体样式
        Font redFont = writer.getWorkbook().createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);

        // 创建表头样式
        CellStyle headerStyle = writer.getWorkbook().createCellStyle();
        headerStyle.cloneStyleFrom(centerStyle);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        // 创建表头红色样式（用于必填项）
        CellStyle redHeaderStyle = writer.getWorkbook().createCellStyle();
        redHeaderStyle.cloneStyleFrom(headerStyle);
        redHeaderStyle.setFont(redFont);

        // 写入表头（第1行）- 创建一行
        Row headerRow = writer.getSheet().createRow(1);

        // 创建并设置必填项单元格
        Cell nameCell = headerRow.createCell(0);
        nameCell.setCellValue("校区名称 *");
        nameCell.setCellStyle(redHeaderStyle);

        Cell codeCell = headerRow.createCell(1);
        codeCell.setCellValue("校区编码 *");
        codeCell.setCellStyle(redHeaderStyle);

        Cell descCell = headerRow.createCell(2);
        descCell.setCellValue("校区描述");
        descCell.setCellStyle(headerStyle);

        Cell statusCell = headerRow.createCell(3);
        statusCell.setCellValue("状态 *");
        statusCell.setCellStyle(redHeaderStyle);

        Cell addressCell = headerRow.createCell(4);
        addressCell.setCellValue("校区地址 *");
        addressCell.setCellStyle(redHeaderStyle);

        Cell latCell = headerRow.createCell(5);
        latCell.setCellValue("纬度");
        latCell.setCellStyle(headerStyle);

        Cell lngCell = headerRow.createCell(6);
        lngCell.setCellValue("经度");
        lngCell.setCellStyle(headerStyle);

        // 创建自动换行的红色样式（用于注意事项）
        CellStyle redWrapStyle = writer.getWorkbook().createCellStyle();
        redWrapStyle.cloneStyleFrom(wrapStyle);
        redWrapStyle.setFont(redFont);

        // 读取注意事项文本并写入Excel
        String noticeText = this.readCampusNoticeFile();
        // 合并注意事项单元格（与学生模板样式一致）
        writer.getSheet().addMergedRegion(new CellRangeAddress(1, 20, 7, 9));
        Cell noticeCell = writer.getSheet().getRow(1).createCell(7);
        noticeCell.setCellValue(noticeText);
        noticeCell.setCellStyle(redWrapStyle);

        // 填充状态示例
        writer.writeCellValue(0, 2, "示例校区（需要删除）");
        writer.writeCellValue(1, 2, "CAMPUSA");
        writer.writeCellValue(2, 2, "主校区");
        writer.writeCellValue(3, 2, "1");
        writer.writeCellValue(4, 2, "北京市海淀区xxx路123号");
        writer.writeCellValue(5, 2, "39.9042");
        writer.writeCellValue(6, 2, "116.4074");

        // 状态参考
        writer.writeCellValue(11, 1, "状态参考");
        writer.writeCellValue(11, 2, "1 (启用)");
        writer.writeCellValue(11, 3, "0 (禁用)");

        // 输出到字节流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        writer.close();

        // 返回模板的字节数组
        return outputStream.toByteArray();
    }

    /**
     * 读取校区导入注意事项文件
     *
     * @return 注意事项文本内容
     */
    private @NotNull String readCampusNoticeFile() {
        try {
            // A 从资源文件夹读取 campus-import-notice.txt
            Resource resource = new ClassPathResource("notes/campus-import-notice.txt");
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
                    校区批量导入注意事项：
                    1. 请严格按照模板格式填写信息，不要修改表格结构。
                    2. 校区名称为必填项，且不能重复，长度应控制在50个字符以内。
                    3. 校区编码为必填项，且必须唯一，建议使用字母和数字的组合，长度不超过20个字符。
                    4. 校区描述可选填，但建议填写，以便于管理和查询，长度不超过200个字符。
                    5. 校区状态请填写"启用"或"禁用"，默认为"启用"。
                    6. 校区地址为必填项，请尽量填写详细的地址信息，长度不超过200个字符。
                    7. 纬度和经度为可选项，如果填写，请确保格式正确。
                    8. 导入前请检查数据的完整性和准确性，避免因数据问题导致导入失败。
                    """;
        }
    }

    /**
     * 验证批量导入校区数据并返回处理后的文件
     * <p>
     * 该方法用于验证通过Base64编码传入的Excel文件，确保其格式正确并且可以用于校区信息的批量导入。
     * </p>
     *
     * @param batchAddCampusVO 包含Excel文件的Base64编码和导入设置的对象
     * @return 处理后的Excel文件字节数组
     */
    @Override
    public byte[] verifyCampusBatchAndBackFile(BatchAddCampusVO batchAddCampusVO) {
        // 1. 检查 VO 对象是否为空
        if (batchAddCampusVO == null) {
            throw new IllegalArgumentException("批量添加校区信息不能为空");
        }

        // 2. 检查 file 字段是否为空
        String base64File = batchAddCampusVO.getFile();
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
     * 验证校区信息是否合法
     *
     * @param campusList 校区导入对象列表
     * @param i 当前校区的索引
     * @throws BusinessException 如果校区信息不合法则抛出异常
     */
    private void validateCampus(
            @NotNull List<CampusImportDTO> campusList,
            int i
    ) throws BusinessException {
        CampusImportDTO campus = campusList.get(i);
        // 行号（假设从第3行开始）
        int rowNumber = i + 3;

        // 验证校区名称是否为空
        if (campus.getCampusName() == null || campus.getCampusName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行校区名称不能为空", ErrorCode.BODY_ERROR);
        }

        // 验证校区编码是否为空
        if (campus.getCampusCode() == null || campus.getCampusCode().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行校区编码不能为空", ErrorCode.BODY_ERROR);
        }

        // 验证校区地址是否为空
        if (campus.getCampusAddress() == null || campus.getCampusAddress().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行校区地址不能为空", ErrorCode.BODY_ERROR);
        }

        // 验证校区描述是否为空，如果为空设置默认值
        if (campus.getCampusDesc() == null || campus.getCampusDesc().isEmpty()) {
            campus.setCampusDesc("暂无描述");
        }

        // 检查校区名称是否已存在
        CampusDO existingCampusByName = campusDAO.getCampusByName(campus.getCampusName());
        if (existingCampusByName != null) {
            throw new BusinessException("第" + rowNumber + "行校区名称已存在: " + campus.getCampusName(), ErrorCode.EXISTED);
        }

        // 检查校区编码是否已存在
        CampusDO existingCampusByCode = campusDAO.getCampusByCode(campus.getCampusCode());
        if (existingCampusByCode != null) {
            throw new BusinessException("第" + rowNumber + "行校区编码已存在: " + campus.getCampusCode(), ErrorCode.EXISTED);
        }
    }

    /**
     * 批量导入校区信息，不忽略错误
     * <p>
     * 该方法用于批量导入校区信息，当遇到任何数据错误时会立即停止导入并抛出异常。
     * </p>
     *
     * @param file Excel文件的字节数组
     * @return 包含导入结果统计的对象
     */
    @Override
    @Transactional
    public BackAddCampusDTO batchImportNoIgnoreError(byte[] file) {
        // 解析Excel文件
        List<CampusImportDTO> campusList = parseExcelToCampusList(file);
        log.debug("第一个校区信息{}", campusList.get(0));

        // 不忽略警告提醒报错
        for (int i = 0; i < campusList.size(); i++) {
            // 使用validateCampus方法验证校区信息
            validateCampus(campusList, i);
            CampusImportDTO campus = campusList.get(i);

            // 创建新的校区对象
            CampusDO campusDO = new CampusDO()
                    .setCampusName(campus.getCampusName())
                    .setCampusCode(campus.getCampusCode())
                    .setCampusDesc(campus.getCampusDesc() != null ? campus.getCampusDesc() : "暂无描述")
                    .setCampusStatus(campus.getCampusStatus() != null ? campus.getCampusStatus() : true)
                    .setCampusAddress(campus.getCampusAddress())
                    .setLatitude(campus.getLatitude())
                    .setLongitude(campus.getLongitude());

            // 保存校区数据
            campusDAO.saveCampus(campusDO);
        }

        // 清除缓存
        campusDAO.deleteCampusCache();

        // 成功
        return new BackAddCampusDTO()
                .setTotalCount(campusList.size())
                .setSuccessCount(campusList.size())
                .setFailedCount(0)
                .setFailedDetails(null);
    }

    /**
     * 批量导入校区信息，忽略错误
     * <p>
     * 该方法用于批量导入校区信息，当遇到数据错误时会继续处理其他数据，并记录错误信息。
     * </p>
     *
     * @param file Excel文件的字节数组
     * @return 包含导入结果统计和错误详情的对象
     */
    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public BackAddCampusDTO batchImportIgnoreError(byte[] file) {
        // 解析Excel文件
        List<CampusImportDTO> campusList = parseExcelToCampusList(file);
        BackAddCampusDTO backAddCampusDTO = new BackAddCampusDTO();

        // 创建失败详情列表
        List<BackAddCampusDTO.FailedDetail> failedDetails = new ArrayList<>();
        int successCount = 0;

        // 循环处理每条校区记录
        for (int i = 0; i < campusList.size(); i++) {
            try {
                CampusImportDTO campus = campusList.get(i);

                // 验证校区名称
                if (campus.getCampusName() == null || campus.getCampusName().isEmpty()) {
                    throw new BusinessException("校区名称不能为空", ErrorCode.BODY_ERROR);
                }

                // 验证校区编码
                if (campus.getCampusCode() == null || campus.getCampusCode().isEmpty()) {
                    throw new BusinessException("校区编码不能为空", ErrorCode.BODY_ERROR);
                }

                // 验证校区地址
                if (campus.getCampusAddress() == null || campus.getCampusAddress().isEmpty()) {
                    throw new BusinessException("校区地址不能为空", ErrorCode.BODY_ERROR);
                }

                // 检查校区名称是否已存在
                CampusDO existingCampusByName = campusDAO.getCampusByName(campus.getCampusName());
                if (existingCampusByName != null) {
                    throw new BusinessException("校区名称已存在: " + campus.getCampusName(), ErrorCode.EXISTED);
                }

                // 检查校区编码是否已存在
                CampusDO existingCampusByCode = campusDAO.getCampusByCode(campus.getCampusCode());
                if (existingCampusByCode != null) {
                    throw new BusinessException("校区编码已存在: " + campus.getCampusCode(), ErrorCode.EXISTED);
                }

                // 创建校区对象
                CampusDO campusDO = new CampusDO()
                        .setCampusName(campus.getCampusName())
                        .setCampusCode(campus.getCampusCode())
                        .setCampusDesc(campus.getCampusDesc() != null ? campus.getCampusDesc() : "暂无描述")
                        .setCampusStatus(campus.getCampusStatus() != null ? campus.getCampusStatus() : true)
                        .setCampusAddress(campus.getCampusAddress())
                        .setLatitude(campus.getLatitude())
                        .setLongitude(campus.getLongitude());

                // 保存校区数据
                try {
                    campusDAO.saveCampus(campusDO);
                    successCount++;
                } catch (Exception e) {
                    BackAddCampusDTO.FailedDetail failedDetail = new BackAddCampusDTO.FailedDetail()
                            .setRowNumber(i + 3)
                            .setErrorMessage("保存失败: " + e.getMessage());
                    failedDetails.add(failedDetail);
                }
            } catch (RuntimeException e) {
                BackAddCampusDTO.FailedDetail failedDetail = new BackAddCampusDTO.FailedDetail()
                        .setRowNumber(i + 3)
                        .setErrorMessage(e.getMessage());
                failedDetails.add(failedDetail);
            }
        }

        // 清除缓存
        campusDAO.deleteCampusCache();

        // 设置统计结果
        return backAddCampusDTO.setTotalCount(campusList.size())
                .setSuccessCount(successCount)
                .setFailedCount(failedDetails.size())
                .setFailedDetails(failedDetails.isEmpty() ? null : failedDetails);
    }
}
