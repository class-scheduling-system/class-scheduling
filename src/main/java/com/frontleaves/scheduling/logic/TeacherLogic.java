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
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.TeacherTypeDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.exceptions.lib.DataInvalidException;
import com.frontleaves.scheduling.exceptions.lib.DataNotFoundException;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherDTO;
import com.frontleaves.scheduling.models.dto.TeacherDisableDTO;
import com.frontleaves.scheduling.models.dto.TeacherLiteDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.multiple.UserAndTeacherDO;
import com.frontleaves.scheduling.models.vo.TeacherBatchImportVO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import com.frontleaves.scheduling.services.TeacherService;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.beans.BeanUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * 教师逻辑
 * <p>
 * 该类用于实现教师相关的业务逻辑，包括添加、删除、更新和查询教师信息等功能。
 * 该类实现了教师服务接口，用于处理教师相关的业务逻辑。
 *
 * @author qiyu | xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherLogic implements TeacherService {
    private final DepartmentDAO departmentDAO;
    private final UserDAO userDAO;
    private final TeacherDAO teacherDAO;
    private final TeacherTypeDAO teacherTypeDAO;
    private final UserService userService;
    private final AcademicAffairsPermissionDAO academicAffairsPermissionDAO;

    /**
     * 获取教师简单列表
     * <p>
     * 该方法用于获取教师的基本信息列表，包括UUID、姓名、部门和类型。
     * 支持按部门和教师类型进行筛选。
     * </p>
     *
     * @param departmentUuid  部门UUID，可选参数
     * @param teacherTypeUuid 教师类型UUID，可选参数
     * @return 返回教师简单信息列表
     */
    @Override
    public List<TeacherLiteDTO> getTeacherLiteList(String departmentUuid, String teacherTypeUuid) {
        List<TeacherDO> teacherList = teacherDAO.getTeacherLiteList(departmentUuid, teacherTypeUuid);
        if (teacherList.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为 TeacherLiteDTO
        return teacherList.stream().map(teacher -> {
            TeacherLiteDTO simpleDTO = new TeacherLiteDTO()
                    .setTeacherUuid(teacher.getTeacherUuid())
                    .setTeacherName(teacher.getName());

            // 获取部门信息
            DepartmentDO department = departmentDAO.getDepartmentByUuid(teacher.getUnitUuid());
            if (department != null) {
                simpleDTO.setDepartmentName(department.getDepartmentName());
            }

            // 获取教师类型信息
            TeacherTypeDO teacherType = teacherTypeDAO.getTeacherTypeByUuid(teacher.getType());
            if (teacherType != null) {
                simpleDTO.setTeacherTypeName(teacherType.getTypeName());
            }

            return simpleDTO;
        }).toList();
    }

    /**
     * 添加教师信息
     * <p>
     * 本方法首先根据传入的部门UUID和用户UUID验证部门和用户的存在性，
     * 如果不存在则抛出业务异常接着将TeacherVO对象的属性复制到TeacherDO对象中，
     * 生成一个新的UUID作为教师UUID，并保存到数据库中
     *
     * @param teacherVO 教师视图对象，包含要添加的教师的相关信息
     * @throws BusinessException 如果部门或用户不存在，则抛出此异常
     */
    @Override
    public void addTeacher(@NotNull TeacherVO teacherVO) {
        // 根据部门UUID获取部门信息
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(teacherVO.getUnitUuid());
        // 如果部门不存在，抛出业务异常
        if (departmentDO == null) {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }

        // 根据教师类型UUID获取教师类型信息
        TeacherTypeDO teacherTypeDO = teacherTypeDAO.getTeacherTypeByUuid(teacherVO.getType());
        // 如果教师类型不存在，抛出业务异常
        if (teacherTypeDO == null) {
            throw new BusinessException("教师类型不存在", ErrorCode.NOT_EXIST);
        }

        // 创建一个新的TeacherDO对象
        TeacherDO teacherDO = new TeacherDO();
        // 将TeacherVO对象的属性复制到TeacherDO对象中
        BeanUtil.copyProperties(teacherVO, teacherDO, ProjectOption.stringBlankToNull());

        // 保存TeacherDO对象到数据库中
        teacherDAO.save(teacherDO);
    }

    /**
     * 根据教师UUID获取教师详情
     * <p>
     * 此方法首先验证给定的教师UUID是否符合无连字符的UUID正则表达式，
     * 如果符合，则调用数据访问对象方法获取对应的教师详细信息，
     * 如果教师信息不存在，则抛出业务异常，表示教师不存在，
     * 最后，将获取到的教师信息转换为目标DTO类并返回
     *
     * @param teacherUuid 教师的UUID，用于唯一标识教师
     * @return TeacherDTO 教师详情的数据传输对象
     * @throws BusinessException 当教师不存在时抛出的业务异常
     */
    @Override
    public TeacherDTO getTeacher(String teacherUuid) {
        TeacherDO teacherDTO = null;
        // 验证教师UUID是否符合无连字符的UUID正则表达式
        if (teacherUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            // 根据UUID获取教师详细信息
            teacherDTO = teacherDAO.getTeacherByUuid(teacherUuid);
        }
        // 如果教师信息不存在，抛出业务异常
        if (teacherDTO == null) {
            throw new BusinessException(StringConstant.TEACHER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        // 将教师信息转换为目标DTO类并返回
        return BeanUtil.toBean(teacherDTO, TeacherDTO.class);
    }

    /**
     * 获取教师列表方法
     * <p>
     * 该方法用于根据指定的分页参数和筛选条件获取教师信息列表它首先调用DAO层方法获取原始数据，
     * 然后根据获取的数据情况决定返回一个空的PageDTO还是转换后的TeacherDTO页面对象
     *
     * @param page       页码，表示请求的页面编号
     * @param size       页面大小，表示每页包含的记录数
     * @param isDesc     是否降序，用于指定结果的排序方式
     * @param department 部门名称，用于筛选属于特定部门的教师
     * @param status     状态，用于筛选具有特定状态的教师
     * @param name       教师姓名，用于筛选姓名中包含特定字符串的教师
     * @return 返回一个PageDTO<TeacherDTO>对象，其中包含根据筛选条件查询到的教师信息
     */
    @Override
    public PageDTO<TeacherDTO> getTeacherList(Integer page, Integer size, Boolean isDesc, String department, String status, String name) {
        // 调用DAO层方法获取教师列表
        List<UserAndTeacherDO> teacherList;
        if (status == null || status.isBlank()) {
            teacherList = teacherDAO.getTeacherList(page, size, isDesc, department, null, name);
        } else {
            switch (status) {
                case "1" -> teacherList = teacherDAO.getTeacherList(page, size, isDesc, department, 1, name);
                case "0" -> teacherList = teacherDAO.getTeacherList(page, size, isDesc, department, 0, name);
                case "2" -> teacherList = teacherDAO.getTeacherNoRegisterUserList(page, size, isDesc, department, name);
                default -> throw new BusinessException("状态参数错误", ErrorCode.PARAMETER_ERROR);
            }
        }

        // 如果查询结果为null，应该返回一个空的PageDTO对象
        if (teacherList == null || teacherList.isEmpty()) {
            return new PageDTO<TeacherDTO>()
                    .setTotal(0L)
                    .setSize(Long.valueOf(size))
                    .setRecords(new ArrayList<>())
                    .setCurrent(Long.valueOf(page));
        } else {
            // 将获取的教师列表转换为TeacherDTO对象列表
            List<TeacherDTO> teacherDTOList = teacherList.stream()
                    .map(teacher -> BeanUtil
                            .toBean(teacher.getTeacher(), TeacherDTO.class)
                            .setStatus(teacher.getUser() == null ? 2 : teacher.getUser().getStatus())
                    )
                    .toList();
            return new PageDTO<TeacherDTO>()
                    .setTotal((long) teacherList.size())
                    .setSize(Long.valueOf(size))
                    .setRecords(teacherDTOList)
                    .setCurrent(Long.valueOf(page));
        }
    }

    /**
     * 禁用教师接口的实现方法
     *
     * @param teacherUuid 教师的唯一标识符
     * @param disable     是否禁用教师，true代表禁用，false代表不禁用
     * @return 返回一个包含教师禁用信息的DTO对象
     * <p>
     * 此方法首先根据教师的UUID从数据库中获取教师对象，然后检查教师是否存在以及是否已注册
     * 如果教师存在且已注册，则根据提供的禁用状态更新其用户账户状态
     */
    @Override
    public TeacherDisableDTO disableTeacher(String teacherUuid, Boolean disable) {
        // 根据教师UUID获取教师对象
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(teacherUuid);
        // 如果教师不存在，抛出异常
        if (teacherDO == null) {
            throw new BusinessException(StringConstant.TEACHER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        // 如果教师未注册，抛出异常
        if (teacherDO.getUserUuid() == null || teacherDO.getUserUuid().isBlank()) {
            throw new BusinessException("教师未注册", ErrorCode.NOT_EXIST);
        }
        // 根据教师关联的用户UUID获取用户对象
        UserDO oldUserDO = userDAO.getUserByUuid(teacherDO.getUserUuid());
        // 创建一个新的用户对象，用于更新用户状态
        UserDO newUserDO = BeanUtil.toBean(oldUserDO, UserDO.class);
        // 如果用户不存在，抛出异常
        if (oldUserDO == null) {
            throw new BusinessException(StringConstant.USER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        // 更新用户状态为禁用或不禁用
        newUserDO.setStatus((byte) (Boolean.compare(disable, true) == 0 ? 0 : 1));
        // 更新数据库中的用户对象
        userDAO.updateUser(oldUserDO, newUserDO);
        // 创建并返回一个包含教师禁用信息的DTO对象
        return new TeacherDisableDTO()
                .setTeacherUuid(teacherDO.getTeacherUuid())
                .setStatus(disable);
    }

    /**
     * 删除教师信息
     *
     * @param teacherUuid 教师的唯一标识符UUID
     * @throws BusinessException 如果教师不存在或已注册，将抛出此异常
     */
    @Override
    public void deleteTeacher(String teacherUuid) {
        // 根据UUID获取教师对象
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(teacherUuid);

        // 检查教师是否存在，如果不存在则抛出异常
        if (teacherDO == null) {
            throw new BusinessException(StringConstant.TEACHER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }

        // 检查教师是否已注册，如果已注册则抛出异常
        if (teacherDO.getUserUuid() != null && !teacherDO.getUserUuid().isBlank()) {
            throw new BusinessException("教师已注册，无法删除", ErrorCode.EXISTED);
        }

        // 调用DAO方法删除教师信息
        teacherDAO.deleteTeacher(teacherDO);
    }

    /**
     * 更新教师信息
     * <p>
     * 此方法首先根据教师的UUID从数据库中获取教师对象如果教师对象存在，则根据提供的教师视图对象中的信息更新教师对象
     * 如果视图对象中包含了单位UUID，则验证该单位是否存在如果单位不存在，则抛出业务异常
     * 同样，如果视图对象中包含了用户UUID，则验证该用户是否存在如果用户不存在，则抛出业务异常
     * 最后，将视图对象中的属性复制到教师对象中，并更新数据库中的教师信息
     *
     * @param teacherUuid 教师的唯一标识符
     * @param teacherVO   包含教师更新信息的视图对象
     */
    @Override
    public void updateTeacher(String teacherUuid, TeacherVO teacherVO) {
        // 根据UUID获取教师对象
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(teacherUuid);
        if (teacherDO != null) {
            // 如果教师视图对象中包含单位UUID，则检查单位是否存在
            if (teacherVO.getUnitUuid() != null) {
                DepartmentDO getDepartment = departmentDAO.getDepartmentByUuid(teacherVO.getUnitUuid());
                if (getDepartment == null) {
                    throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
                }
            }
            // 如果教师视图对象中包含教师类型UUID，则检查教师类型是否存在
            if (teacherVO.getType() != null) {
                TeacherTypeDO getType = teacherTypeDAO.getTeacherTypeByUuid(teacherVO.getType());
                if (getType == null) {
                    throw new BusinessException("教师类型不存在", ErrorCode.NOT_EXIST);
                }
            }
            // 将视图对象中的属性复制到教师对象中，并更新数据库中的教师信息
            BeanUtils.copyProperties(teacherVO, teacherDO);
            teacherDAO.updateTeacher(teacherDO);
        }
    }

    /**
     * 生成教师导入模板的Excel文件
     * <p>
     * 本方法负责创建一个Excel文件，作为教师信息导入的模板它包括了教师信息的各种字段，
     * 并提供了一些示例数据和注意事项本方法使用了Hutool的ExcelWriter来简化Excel文件的创建和写入过程
     * </p>
     *
     * @param prepareTeacherExampleDTO 准备教师示例的DTO，可能包含单位和教师类型列表
     * @return 生成的Excel文件的字节流
     */
    @Override
    public byte[] getExample(PrepareTeacherExampleDTO prepareTeacherExampleDTO) {
        // 使用 Hutool 的 ExcelWriter 创建工作簿
        try (ExcelWriter writer = ExcelUtil.getWriter(true)) {
            // 定义表头
            String[] headers = {
                    "教师工号", "教师姓名", "教师英文名", "教师民族",
                    "教师性别", "所属单位", "教师类型",
                    "教师电话", "教师邮箱", "教师职称", "教师描述"
            };

            // 创建一个居中样式（用于表头）
            CellStyle centerStyle = writer.getWorkbook().createCellStyle();
            centerStyle.setAlignment(HorizontalAlignment.CENTER);
            centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            // 创建自动换行样式（用于后续红色样式克隆）
            CellStyle wrapStyle = writer.getWorkbook().createCellStyle();
            wrapStyle.cloneStyleFrom(centerStyle);
            wrapStyle.setWrapText(true);

            // 设置列宽（循环所有列，宽度为当前宽度的两倍，若是默认宽则赋初始值）
            Sheet sheet = writer.getSheet();
            int defaultWidth = sheet.getDefaultColumnWidth();
            for (int i = 0; i < headers.length; i++) {
                int currentWidth = sheet.getColumnWidth(i);
                if (currentWidth == defaultWidth * 256) {
                    currentWidth = 2048;
                }
                sheet.setColumnWidth(i, currentWidth * 2);
            }

            // 合并第一行所有列，用于写入标题
            writer.getSheet().addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));
            // 创建第一行并写入标题
            Row titleRow = writer.getSheet().createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("教师导入模板");
            titleCell.setCellStyle(centerStyle);

            // 写入表头
            int headerRowIndex = 1;
            Row headerRow = writer.getSheet().createRow(headerRowIndex);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(centerStyle);
            }

            // 创建红色字体样式
            Font redFont = writer.getWorkbook().createFont();
            redFont.setColor(IndexedColors.RED.getIndex());
            // 加粗
            redFont.setBold(true);
            CellStyle redWrapStyle = writer.getWorkbook().createCellStyle();
            // 克隆之前创建的 wrapStyle
            redWrapStyle.cloneStyleFrom(wrapStyle);
            redWrapStyle.setFont(redFont);

            // 读取注意事项文本
            String noticeText = this.readTeacherNoticeTxtFile();
            // 合并注意事项单元格（5-20行，2-4列）
            writer.getSheet().addMergedRegion(new CellRangeAddress(5, 20, 2, 4));
            // 获取第 6 行，如果不存在则创建一行（确保合并区域内有单元格对象）
            Row noticeRow = writer.getSheet().getRow(5);
            if (noticeRow == null) {
                noticeRow = writer.getSheet().createRow(5);
            }
            // 写入注意事项，并应用自动换行和红色样式
            Cell noticeCell = noticeRow.createCell(2);
            noticeCell.setCellValue(noticeText);
            noticeCell.setCellStyle(redWrapStyle);

            // 写入示例数据（写在第2行）
            int dataRowIndex = 2;
            Row dataRow = writer.getSheet().createRow(dataRowIndex);
            dataRow.createCell(0).setCellValue("T001");
            dataRow.createCell(1).setCellValue("张三");
            dataRow.createCell(2).setCellValue("Zhang San");
            dataRow.createCell(3).setCellValue("汉族");
            dataRow.createCell(4).setCellValue("男");

            // 所属单位：如果 DTO 中有单位数据，则使用第一个单位名称，否则使用默认值
            if (prepareTeacherExampleDTO != null
                    && prepareTeacherExampleDTO.getUnitList() != null
                    && !prepareTeacherExampleDTO.getUnitList().isEmpty()) {
                dataRow.createCell(5).setCellValue(prepareTeacherExampleDTO.getUnitList().get(0).getUnitName());
            } else {
                dataRow.createCell(5).setCellValue("单位示例");
            }

            // 教师类型：同理
            if (prepareTeacherExampleDTO != null
                    && prepareTeacherExampleDTO.getTeacherTypeList() != null
                    && !prepareTeacherExampleDTO.getTeacherTypeList().isEmpty()) {
                dataRow.createCell(6).setCellValue(prepareTeacherExampleDTO.getTeacherTypeList().get(0).getTypeName());
            } else {
                dataRow.createCell(6).setCellValue("类型示例");
            }

            dataRow.createCell(7).setCellValue("19896666666");
            dataRow.createCell(8).setCellValue("teacher@example.com");
            dataRow.createCell(9).setCellValue("副教授");
            dataRow.createCell(10).setCellValue("示例描述");

            // 将生成的工作簿写入字节流
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            writer.flush(outputStream, true);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BusinessException("生成教师导入模板失败", ErrorCode.OPERATION_FAILED);
        }
    }

    /**
     * 读取教师通知的文本文件
     * <p>
     * 此方法尝试从类路径下的"notes/student-import-notice.txt"文件中读取通知内容
     * 如果文件存在，则读取并返回文件内容；如果文件不存在或读取过程中发生异常，则返回默认的注意事项文本
     * </p>
     *
     * @return 文件内容或默认注意事项文本
     */
    private @NotNull String readTeacherNoticeTxtFile() {
        try {
            // 从资源文件夹中读取 notice.txt
            ClassPathResource resource = new ClassPathResource("notes/teacher-import-notice.txt");

            // 尝试读取资源
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                // 资源不存在，返回默认文本
                return """
                        注意事项：
                        1. 请严格按照模板填写信息
                        2. 所有信息必须准确无误
                        3. 请勿修改模板结构
                        """;
            }
        } catch (IOException e) {
            return """
                    注意事项：
                    1. 请严格按照模板填写信息
                    2. 所有信息必须准确无误
                    3. 请勿修改模板结构
                    """;
        }
    }

    /**
     * 验证教师信息批量导入的文件并返回文件的字节数组
     * <p>
     * 此方法首先检查传入的教师批量导入对象是否为空，然后解码其中的Base64文件内容，
     * 验证文件大小不超过限制，并确保文件是有效的Excel格式
     * </p>
     *
     * @param teacherBatchImportVO 教师批量导入视图对象，包含要导入的教师信息和文件
     * @return 解码后的文件字节数组，用于进一步处理或保存
     * @throws BusinessException 如果对象为空、Base64解码失败、文件大小超过限制或文件不是有效的Excel格式
     */
    @Override
    public byte[] verifyTeacherBatchAndBackFile(TeacherBatchImportVO teacherBatchImportVO) {
        // 检查 VO 对象是否为空
        if (teacherBatchImportVO == null) {
            throw new BusinessException("批量添加教师信息不能为空", ErrorCode.BODY_ERROR);
        }

        // 检查 file 字段是否空
        String base64File = getString(teacherBatchImportVO);

        // 解码 Base64 字符串为字节数组
        byte[] fileBytes;
        try {
            fileBytes = Base64.getDecoder().decode(base64File);
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Base64 解码失败：" + e.getMessage(), ErrorCode.BODY_ERROR);
        }

        // 检查文件大小（10MB = 10 * 1024 * 1024 字节）
        final long maxSizeInBytes = 10L * 1024 * 1024;
        if (fileBytes.length > maxSizeInBytes) {
            throw new BusinessException("文件大小超过10MB限制", ErrorCode.BODY_ERROR);
        }

        // 验证是否为 Excel 文件：尝试通过 POI 解析
        try (InputStream inputStream = new ByteArrayInputStream(fileBytes)) {
            WorkbookFactory.create(inputStream);
        } catch (Exception e) {
            throw new BusinessException("提供的文件不是有效的 Excel 文件" + e.getMessage(), ErrorCode.BODY_ERROR);
        }

        // 返回解码后的文件字节数组
        return fileBytes;

    }

    /**
     * 从 TeacherBatchImportVO 对象中提取并处理 Base64 编码的 Excel 文件字符串
     * <p>
     * 此方法确保提供的 Base64 字符串是有效的，并且适合进一步处理或转换
     * </p>
     *
     * @param teacherBatchImportVO 包含教师批量导入信息的对象，包括 Base64 编码的文件字符串
     * @return 处理后的 Base64 编码字符串，没有前缀和干扰字符串
     * @throws BusinessException 如果文件字符串为空或格式不正确，则抛出此异常
     */
    private static @NotNull String getString(TeacherBatchImportVO teacherBatchImportVO) {
        String base64File = teacherBatchImportVO.getFile();
        if (base64File.trim().isEmpty()) {
            throw new BusinessException("Excel文件不能为空", ErrorCode.BODY_ERROR);
        }

        // 处理 Base64 字符串，去除可能的前缀和干扰字符串
        if (base64File.contains(",")) {
            String[] parts = base64File.split(",", 2);
            if (parts.length < 2) {
                throw new BusinessException("Excel文件格式错误", ErrorCode.BODY_ERROR);
            }
            base64File = parts[1];
        }
        base64File = base64File.replaceAll("\\s", "");
        return base64File;
    }

    /**
     * 重写获取部门UUID方法
     * <p>
     * 本方法通过HttpServletRequest对象来获取当前操作用户的部门UUID主要解决的问题是判断用户是否有权限进行教务操作
     * 方法首先会根据请求获取用户信息，然后根据用户UUID获取教务权限信息，最终返回部门UUID
     * </p>
     *
     * @param request HTTP请求对象，用于获取当前请求的用户信息
     * @return 返回用户的部门UUID如果用户不存在或用户没有教务权限，则抛出业务异常
     * @throws BusinessException 当用户不存在或教务权限不存在时抛出此异常
     */
    @Override
    public String getDepartmentUuid(HttpServletRequest request) {
        // 根据请求获取用户信息
        UserDO userDO = userService.getUserByRequest(request);
        // 检查用户是否存在
        if (userDO == null) {
            throw new BusinessException("用户不存在，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }
        // 根据用户UUID获取教务权限信息
        AcademicAffairsPermissionDO academicAffairsPermissionDO =
                academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(userDO.getUserUuid());
        // 检查教务权限是否存在
        if (academicAffairsPermissionDO == null) {
            throw new BusinessException("教务权限不存在，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }
        // 返回部门UUID
        return academicAffairsPermissionDO.getDepartment();
    }

    /**
     * 批量导入教师信息，忽略错误数据
     *
     * @param file           Excel文件字节数组，包含教师信息
     * @param departmentUuid 部门UUID，用于关联教师和部门
     * @return 返回包含导入结果的DTO对象
     */
    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public BackAddTeacherDTO batchImportIgnoreError(byte[] file, String departmentUuid) {
        // 解析 Excel 文件，生成教师导入DTO列表（第3行开始，第11列结束）
        List<TeacherImportDTO> teacherList = parseExcelToTeacherList(file, 2, 11);
        BackAddTeacherDTO backAddTeacherDTO = new BackAddTeacherDTO();

        // 获取教师导入所需的基本数据
        TeacherBaseImportDTO teacherBaseImport = fetchBaseImportDTO(departmentUuid);

        // 创建失败详情列表
        List<BackAddTeacherDTO.FailedDetail> failedDetails = new ArrayList<>();
        int successCount = 0;

        // 循环处理每条教师记录
        for (int i = 0; i < teacherList.size(); i++) {
            try {
                TeacherImportDTO teacherImportDTO = teacherList.get(i);
                // 验证数据完整性
                validateTeacherName(teacherImportDTO.getName());
                validateTeacherId(teacherImportDTO.getId());

                // 验证并获取教师类型，根据教师类型名称在基础数据中查找对应的教师类型 DO
                findTeacherTypeByName(teacherBaseImport.getTeacherTypes(), teacherImportDTO.getType());

                // 构建教师对象，并设置各字段
                TeacherDO teacherDO = new TeacherDO();
                teacherDO.setId(teacherList.get(i).getId())
                        .setName(teacherList.get(i).getName())
                        .setEnglishName(teacherList.get(i).getEnglishName())
                        .setEthnic(teacherList.get(i).getEthnic());
                // 设置性别
                if ("男".equals(teacherImportDTO.getSex())) {
                    teacherDO.setSex(true);
                } else if ("女".equals(teacherImportDTO.getSex())) {
                    teacherDO.setSex(false);
                } else {
                    throw new BusinessException("第" + (i + 3) + "行性别填写错误", ErrorCode.BODY_ERROR);
                }

                // 设置所属单位（使用基础数据中部门UUID）
                teacherDO.setUnitUuid(teacherBaseImport.getDepartment().getDepartmentUuid());

                // 设置教师类型
                teacherDO.setType(teacherList.get(i).getType());

                // 设置其他字段
                teacherDO.setPhone(teacherList.get(i).getPhone())
                        .setEmail(teacherList.get(i).getEmail())
                        .setJobTitle(teacherList.get(i).getJobTitle())
                        .setDesc(teacherList.get(i).getDescription());
                // 保存学生数据
                List<BackAddTeacherDTO.FailedDetail> saveFailedDetails = teacherDAO.saveTeacherIgnoreError(teacherDO, i);
                if (saveFailedDetails.isEmpty()) {
                    // 保存成功
                    successCount++;
                } else {
                    failedDetails.addAll(saveFailedDetails);
                }
            } catch (RuntimeException e) {
                // 捕获验证时的异常
                BackAddTeacherDTO.FailedDetail failedDetail = getFailedDetail(e, i);
                failedDetails.add(failedDetail);
            }
        }
        // 设置统计结果
        backAddTeacherDTO.setTotalCount(teacherList.size())
                .setSuccessCount(successCount)
                .setFailedCount(failedDetails.size())
                .setFailedDetails(failedDetails.isEmpty() ? null : failedDetails);
        return backAddTeacherDTO;
    }

    /**
     * 批量导入教师信息，不忽略错误
     * 当出现错误时，中断导入过程并抛出异常
     *
     * @param file           Excel文件字节数组，包含教师信息
     * @param departmentUuid 部门唯一标识符，用于关联教师和部门
     * @return 返回导入结果统计DTO，包括总记录数、成功记录数、失败记录数和失败详情
     */
    @Override
    @Transactional
    public BackAddTeacherDTO batchImportNoIgnoreError(byte[] file, String departmentUuid) {
        // 解析 Excel 文件，生成教师导入DTO列表
        List<TeacherImportDTO> teacherList = parseExcelToTeacherList(file, 2, 11);
        log.debug("第一个教师信息是：{}", teacherList.get(0));

        // 获取导入教师信息的基础DTO,包含部门信息和教师类型列表
        TeacherBaseImportDTO teacherBaseImport = fetchBaseImportDTO(departmentUuid);

        // 循环处理每条教师记录
        for (int i = 0; i < teacherList.size(); i++) {
            TeacherImportDTO teacherImportDTO = teacherList.get(i);

            // 验证数据完整性：教师姓名、教师工号不能为空
            validateTeacherName(teacherImportDTO.getName());
            validateTeacherId(teacherImportDTO.getId());

            // 创建教师对象
            TeacherDO teacherDO = new TeacherDO();

            // 设置教师性别
            if ("男".equals(teacherImportDTO.getSex())) {
                teacherDO.setSex(true);
            } else if ("女".equals(teacherImportDTO.getSex())) {
                teacherDO.setSex(false);
            } else {
                throw new BusinessException("第" + (i + 3) + "行性别填写错误", ErrorCode.BODY_ERROR);
            }

            // 获取并验证教师类型，根据教师类型名称在基础数据中查找对应的教师类型DO
            TeacherTypeDO teacherTypeDO = findTeacherTypeByName(teacherBaseImport.getTeacherTypes(), teacherImportDTO.getType());

            // 设置教师其他基本信息
            teacherDO.setId(teacherImportDTO.getId())
                    .setName(teacherImportDTO.getName())
                    .setEnglishName(teacherImportDTO.getEnglishName())
                    .setEthnic(teacherImportDTO.getEthnic())
                    // 保存教师类型的UUID(而非名称)
                    .setType(teacherTypeDO.getTeacherTypeUuid())
                    .setPhone(teacherImportDTO.getPhone())
                    .setEmail(teacherImportDTO.getEmail())
                    .setJobTitle(teacherImportDTO.getJobTitle())
                    .setDesc(teacherImportDTO.getDescription())
                    // 使用基础信息中的部门信息设置所属单位
                    .setUnitUuid(teacherBaseImport.getDepartment().getDepartmentUuid());

            // 保存教师数据
            teacherDAO.saveTeacherIgnoreError(teacherDO, i);
        }
        // 如果所有数据均成功保存，返回统计结果（失败记录为0）
        return new BackAddTeacherDTO()
                .setTotalCount(teacherList.size())
                .setSuccessCount(teacherList.size())
                .setFailedCount(0)
                .setFailedDetails(null);
    }

    /**
     * 准备部门数据，用于生成教师示例
     * <p>
     * 此方法根据当前请求获取用户信息，进而获取该用户的教务事务权限信息，
     * 并根据权限信息中的部门UUID获取部门详情此外，它还负责获取所有教师类型，
     * 并将其转换为DTO列表最后，它将部门信息和教师类型列表封装到一个教师示例DTO中返回
     * </p>
     *
     * @param request HTTP请求，用于获取用户信息
     * @return PrepareTeacherExampleDTO 包含部门信息和教师类型列表的教师示例DTO
     * @throws BusinessException 当用户没有教务事务权限时抛出业务异常
     */
    @Override
    public PrepareTeacherExampleDTO prepareDepartmentData(HttpServletRequest request) {
        // 根据请求获取用户信息
        UserDO userDO = userService.getUserByRequest(request);
        assert userDO != null;

        // 根据用户UUID获取教务事务权限信息
        AcademicAffairsPermissionDO academicAffairsPermissionDO =
                academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(userDO.getUserUuid());
        // 检查教务事务权限信息是否存在，如果不存在则抛出异常
        if (academicAffairsPermissionDO == null) {
            throw new BusinessException("系统错误，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }

        // 根据教务事务权限中的部门 UUID 获取部门信息
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuidLastUpdate(academicAffairsPermissionDO.getDepartment());
        assert departmentDO != null;

        // 查询教师类型列表
        List<TeacherTypeDO> teacherTypeDOList = teacherTypeDAO.getAllTeacherTypes();
        // DO -> DTO
        List<TeacherTypeDTO> teacherTypeDTOList = teacherTypeDOList.stream().map(teacherTypeDO -> {
            TeacherTypeDTO dto = new TeacherTypeDTO();
            dto.setTeacherTypeUuid(teacherTypeDO.getTeacherTypeUuid());
            dto.setTypeName(teacherTypeDO.getTypeName());
            return dto;
        }).toList();

        return getPrepareTeacherExampleDTO(departmentDO, teacherTypeDTOList);
    }

    /**
     * 从给定的列表中提取教师信息，并将其封装到TeacherImportDTO对象中
     * 此方法确保列表中的每个元素都对应于TeacherImportDTO中的一个字段
     *
     * @param rowlist 包含教师信息的列表，不能为空
     * @return 填充了从列表中提取的教师信息的TeacherImportDTO对象
     */
    private static @NotNull TeacherImportDTO getTeacherImportDTO(
            @NotNull List<Object> rowlist
    ) {
        TeacherImportDTO teacher = new TeacherImportDTO();
        // 从列表中提取教师信息并设置到DTO对象中
        teacher.setId(rowlist.get(0).toString().trim())
                .setName(rowlist.get(1).toString().trim())
                .setEnglishName(rowlist.get(2).toString().trim())
                .setEthnic(rowlist.get(3).toString().trim())
                .setSex(rowlist.get(4).toString().trim())
                .setDepartmentName(rowlist.get(5).toString().trim())
                .setType(rowlist.get(6).toString().trim())
                .setPhone(rowlist.get(7).toString().trim())
                .setEmail(rowlist.get(8).toString().trim())
                .setJobTitle(rowlist.get(9).toString().trim())
                .setDescription(rowlist.get(10).toString().trim());
        // 返回填充了教师信息的DTO对象
        return teacher;
    }

    /**
     * 将 Excel 文件字节解析为教师信息列表
     *
     * @param excelBytes     Excel 文件字节数组
     * @param startRow       开始解析的行号（从 0 开始）
     * @param columnsToCheck 需要检查的列数
     * @return 解析后的教师信息列表
     */
    public static List<TeacherImportDTO> parseExcelToTeacherList(
            byte[] excelBytes, int startRow, int columnsToCheck
    ) {
        // 解析 Excel 文件获取行数据列表
        List<List<Object>> rowList = ProjectUtil.parseExcelToRowList(excelBytes, startRow, columnsToCheck);
        log.debug("原生列表{}", rowList);
        // 创建结果列表
        List<TeacherImportDTO> teacherList = new ArrayList<>();
        for (List<Object> row : rowList) {
            try {
                if (!row.isEmpty()) {
                    TeacherImportDTO teacherImportDTO = getTeacherImportDTO(row);
                    teacherList.add(teacherImportDTO);
                }
            } catch (Exception e) {
                log.error("解析教师数据时出错：{}", e.getMessage());
            }
        }
        return teacherList;
    }

    /**
     * 根据部门和教师类型列表构建PrepareTeacherExampleDTO对象
     * <p>
     * 此方法用于准备教师示例数据传输对象（DTO），该对象包含部门信息和教师类型列表
     * 它主要用于将部门视为一个单元，并结合教师类型列表，用于后续的处理或传输
     * </p>
     *
     * @param departmentDO       部门数据对象，包含部门的详细信息
     * @param teacherTypeDTOList 教师类型数据传输对象列表，表示不同类型的教师
     * @return PrepareTeacherExampleDTO 返回准备好的教师示例DTO，包含单位和教师类型信息
     */
    private static @org.jetbrains.annotations.NotNull PrepareTeacherExampleDTO getPrepareTeacherExampleDTO(
            DepartmentDO departmentDO, List<TeacherTypeDTO> teacherTypeDTOList
    ) {
        PrepareTeacherExampleDTO.UnitDTO unitDTO = new PrepareTeacherExampleDTO.UnitDTO();
        unitDTO.setUnitUuid(departmentDO.getDepartmentUuid());
        unitDTO.setUnitName(departmentDO.getDepartmentName());

        // 构建并返回 PrepareTeacherExampleDTO，将单位和教师类型数据封装到 DTO 中
        PrepareTeacherExampleDTO prepareTeacherExampleDTO = new PrepareTeacherExampleDTO();
        prepareTeacherExampleDTO.setUnitList(Collections.singletonList(unitDTO));
        prepareTeacherExampleDTO.setTeacherTypeList(teacherTypeDTOList);
        return prepareTeacherExampleDTO;
    }

    /**
     * 根据部门UUID获取基础导入数据对象
     * <p>
     * 此方法旨在为教师信息导入过程准备必要的部门和教师类型信息
     * </p>
     *
     * @param departmentUuid 部门唯一标识符，用于查询部门信息
     * @return 返回一个包含了部门信息和教师类型列表的TeacherBaseImportDTO对象
     * @throws BusinessException 当部门信息不存在时抛出业务异常
     */
    private TeacherBaseImportDTO fetchBaseImportDTO(String departmentUuid) {
        // 获取部门信息
        DepartmentDO department = departmentDAO.getDepartmentByUuidLastUpdate(departmentUuid);
        if (department == null) {
            throw new BusinessException("部门信息不存在", ErrorCode.NOT_EXIST);
        }
        // 从缓存中获取所有教师类型列表
        List<TeacherTypeDO> teacherTypes = teacherTypeDAO.getAllTeacherTypes();

        // 构造 TeacherBaseImportDTO 对象
        return new TeacherBaseImportDTO()
                .setDepartment(department)
                .setTeacherTypes(teacherTypes);
    }

    /**
     * 检查教师姓名是否合法
     *
     * @param name 教师姓名
     * @throws DataInvalidException 当姓名为空或为空字符串时抛出异常
     */
    private void validateTeacherName(String name) {
        if (name == null || name.isEmpty()) {
            throw new DataInvalidException(DataInvalidException.TypeEnum.NAME_EMPTY_ERROR);
        }
    }

    /**
     * 验证教师ID的有效性
     *
     * @param id 教师ID，需验证的字符串
     * @throws DataInvalidException 如果教师ID为空或null，则抛出数据无效异常，异常类型为TEACHER_ID_EMPTY_ERROR
     */
    private void validateTeacherId(String id) {
        if (id == null || id.isEmpty()) {
            throw new DataInvalidException(DataInvalidException.TypeEnum.TEACHER_ID_EMPTY_ERROR);
        }
    }

    /**
     * 根据类型名称在给定的教师类型列表中查找教师类型
     *
     * @param teacherTypeList 教师类型列表，用于搜索指定的教师类型
     * @param typeName        要查找的教师类型名称
     * @return 如果找到匹配的教师类型，则返回该教师类型的对象；如果未找到，则抛出异常
     * @throws DataNotFoundException 如果没有找到指定的教师类型，则抛出数据未找到异常
     */
    private TeacherTypeDO findTeacherTypeByName(
            List<TeacherTypeDO> teacherTypeList, String typeName
    ) {
        TeacherTypeDO teacherTypeDO = teacherTypeList.stream()
                .filter(teacherType -> teacherType.getTypeName().equals(typeName))
                .findFirst()
                .orElse(null);
        if (teacherTypeDO == null) {
            throw new DataNotFoundException(DataNotFoundException.TypeEnum.TEACHER_TYPE_NOT_FOUND);
        }
        return teacherTypeDO;
    }

    /**
     * 获取添加教师操作失败的详细信息
     *
     * @param e RuntimeException的实例，表示添加教师过程中发生的异常
     * @param i 教师数据在Excel中的行索引，用于确定失败的具体位置
     * @return BackAddTeacherDTO.FailedDetail对象，包含失败的详细信息
     */
    private static BackAddTeacherDTO.FailedDetail getFailedDetail(RuntimeException e, int i) {
        // 创建对象存储失败信息
        BackAddTeacherDTO.FailedDetail failedDetail = new BackAddTeacherDTO.FailedDetail();
        // 实际数据从第4行开始
        failedDetail.setRow(i + 3);

        // 根据异常类型设置失败原因
        if (e instanceof DataNotFoundException error) {
            failedDetail.setReason("数据无效：" + error.getReason());
        } else {
            failedDetail.setReason("未知错误：" + e.getMessage());
        }
        return failedDetail;
    }
}
