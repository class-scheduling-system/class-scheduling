package com.frontleaves.scheduling.logic;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import cn.hutool.poi.excel.sax.Excel07SaxReader;
import cn.hutool.poi.excel.sax.handler.RowHandler;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.dto.PrepareStudentExampleDTO;
import com.frontleaves.scheduling.models.dto.StudentImportDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.BatchAddStudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 学生逻辑
 *
 * @author FALSHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentLogic implements StudentService {
    private final DepartmentDAO departmentDAO;
    private final MajorDAO majorDAO;
    private final AdministrativeClassDAO administrativeClassDAO;
    private final GradeDAO gradeDAO;
    private final AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    private final UserService userService;
    private final StudentDAO studentDAO;

    private static @NotNull StudentImportDTO getStudentImportDTO(List<Object> rowlist) {
        StudentImportDTO student = new StudentImportDTO();
        student.setDepartmentName(rowlist.get(0).toString().trim())
                .setMajorName(rowlist.get(1).toString().trim())
                .setGradeName(rowlist.get(2).toString().trim())
                .setClassName(rowlist.get(3).toString().trim())
                .setId(rowlist.get(4).toString().trim())
                .setName(rowlist.get(5).toString().trim())
                .setGender(rowlist.get(6).toString().trim());
        return student;
    }

    /**
     * 读取学生通知文本文件
     * 该方法尝试从类路径下的"notes/student-import-notice.txt"文件中读取通知内容
     * 如果文件存在，则读取并返回文件内容；如果文件不存在或读取过程中发生异常，则返回默认的注意事项文本
     *
     * @return 文件内容或默认的注意事项文本
     */
    private @NotNull String readStudentNoticeTxtFile() {
        try {
            // 从资源文件夹读取 notice.txt
            Resource resource = new ClassPathResource("notes/student-import-notice.txt");
            // 尝试读取资源
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    // 读取并返回文件内容
                    return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                }
            } else {
                // 资源不存在，返回默认文本
                return """
                        注意事项：
                        1. 请严格按照模板填写信息
                        2. 所有信息必须准确无误
                        3. 请勿修改模板结构""";
            }
        } catch (IOException e) {
            // 如果读取失败，返回默认文本
            return """
                    注意事项：
                    1. 请严格按照模板填写信息
                    2. 所有信息必须准确无误
                    3. 请勿修改模板结构""";
        }
    }

    /**
     * 判断给定字节数组是否代表一个Excel 2007或更高版本的文件
     * 该方法通过检查字节数组的 前几个字节来确定文件类型
     * Excel 2007+ 文件的 前8个字节是固定的PK头部
     *
     * @param bytes 字节数组，代表要检查的文件内容
     * @return 如果字节数组代表一个Excel 2007或更高版本的文件，则返回true；否则返回false
     */
    private boolean isExcel2007(byte[] bytes) {
        // Excel 2007+ 文件的前8个字节是固定的PK头部
        if (bytes.length >= 4) {
            // 检查前4个字节是否与Excel 2007+ 文件的PK头部匹配
            return bytes[0] == 'P' && bytes[1] == 'K' && bytes[2] == 0x03 && bytes[3] == 0x04;
        }
        // 如果字节数组长度不足4，不可能是Excel 2007+ 文件
        return false;
    }

    /**
     * 将Excel字节流解析为学生信息列表
     *
     * @param excelBytes Excel文件的字节流
     * @return 解析后的学生信息列表
     * @throws IOException 如果文件读取过程中发生错误
     */
    public List<StudentImportDTO> parseExcelToStudents(byte[] excelBytes) throws IOException {
        // 创建结果容器
        List<StudentImportDTO> resultList = new ArrayList<>();
        try {
            // 创建处理每行数据的handler
            RowHandler rowHandler = (sheetIndex, rowIndex, rowlist) -> {
                // 跳过第一行（通常是说明行）
                if (rowIndex == 0) {
                    return;
                }
                // 第二行是标题行，跳过
                if (rowIndex == 1) {
                    return;
                }
                if (!rowlist.isEmpty() && rowlist.get(0) != null
                        && !rowlist.get(0).toString().trim().isEmpty()) {
                    StudentImportDTO student = getStudentImportDTO(rowlist);
                    resultList.add(student);
                }
            };
            // 使用ByteArrayInputStream读取文件内容
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(excelBytes)) {
                // 判断Excel版本并选择合适的解析方式
                if (isExcel2007(excelBytes)) {
                    // 使用Excel07SaxReader处理xlsx格式
                    Excel07SaxReader reader = new Excel07SaxReader(rowHandler);
                    // 只读取第一个sheet
                    reader.read(byteArrayInputStream, 0);
                } else {
                    // 使用ExcelUtil.readBySax处理Excel文件（自动判断格式）
                    ExcelUtil.readBySax(byteArrayInputStream, 0, rowHandler);
                }
                return resultList;
            }
        } catch (Exception e) {
            // 捕获并处理解析过程中可能发生的异常
            throw new IllegalArgumentException("Excel解析失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public BackAddStudentDTO batchImportNoIgnoreError(
            byte[] file, String departmentUuid
    ) {
        List<StudentImportDTO> studentList;
        try {
            studentList = this.parseExcelToStudents(file);
        } catch (IOException e) {
            throw new BusinessException("IO异常，Excel解析失败", ErrorCode.OPERATION_ERROR);
        }
        if (studentList.isEmpty()) {
            throw new BusinessException("Excel文件中没有学生信息或数据格式错误请检查", ErrorCode.BODY_ERROR);
        }
        log.debug("解析Excel文件成功，共解析出{}条学生信息", studentList.size());
        log.debug("第一行数据为:{}", studentList.get(0));
        BackAddStudentDTO backAddStudentDTO = new BackAddStudentDTO();
        // 不忽略警告提醒报错
        for (int i = 0; i < studentList.size(); i++) {
            DepartmentDO departmentDO = departmentDAO.getDepartmentByDepartmentName(
                    studentList.get(i).getDepartmentName());
            if (departmentDO == null) {
                throw new BusinessException("第" + (i + 3) + "行学院名称不存在", ErrorCode.BODY_ERROR);
            }
            MajorDO majorDO = majorDAO.getMajorByDepartmentUuidAndMajorName(
                    departmentDO.getDepartmentUuid(), studentList.get(i).getMajorName());
            if (majorDO == null) {
                throw new BusinessException("第" + (i + 3) + "行专业名称不存在", ErrorCode.BODY_ERROR);
            }
            GradeDO gradeDO = gradeDAO.getGradeByName(studentList.get(i).getGradeName());
            if (gradeDO == null) {
                throw new BusinessException("第" + (i + 3) + "行年级名称不存在", ErrorCode.BODY_ERROR);
            }
            AdministrativeClassDO administrativeClassDO = administrativeClassDAO
                    .getAdministrativeClassByDepartmentAndClassName(departmentDO.getDepartmentUuid(),
                            studentList.get(i).getClassName());
            if (administrativeClassDO == null) {
                throw new BusinessException("第" + (i + 3) + "行班级名称不存在", ErrorCode.BODY_ERROR);
            }
            // 创建并设置学生DO对象
            StudentDO studentDO = new StudentDO();
            // 设置性别
            if ("男".equals(studentList.get(i).getGender())) {
                studentDO.setGender(true);
            } else if ("女".equals(studentList.get(i).getGender())) {
                studentDO.setGender(false);
            } else {
                throw new BusinessException("第" + (i + 3) + "行性别填写错误", ErrorCode.BODY_ERROR);
            }
            studentDO.setId(studentList.get(i).getId())
                    .setName(studentList.get(i).getName())
                    .setGradeUuid(gradeDO.getGradeUuid())
                    .setDepartment(departmentDO.getDepartmentUuid())
                    .setMajor(majorDO.getMajorUuid())
                    .setClazz(administrativeClassDO.getAdministrativeClassUuid())
                    .setGraduated(false);
            //继续数据存储
            studentDAO.saveStudentBackError(studentDO, i);
        }
        //成功
        backAddStudentDTO.setTotalCount(studentList.size())
                .setFailedCount(0)
                .setSuccessCount(studentList.size())
                .setFailedDetails(null);
        return backAddStudentDTO;
    }

    /**
     * 获取学生导入信息模板
     *
     * @param prepareStudentExampleDTO 学生导入信息模板
     * @return 学生导入信息模板
     */
    @Override
    public byte[] getExample(PrepareStudentExampleDTO prepareStudentExampleDTO) {
        // 创建ExcelWriter对象，用于写入Excel文件
        ExcelWriter writer = ExcelUtil.getWriter(true);
        // 创建居中样式
        CellStyle centerStyle = writer.getWorkbook().createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        // 创建自动换行的样式
        CellStyle wrapStyle = writer.getWorkbook().createCellStyle();
        wrapStyle.cloneStyleFrom(centerStyle);
        // 关键代码：设置自动换行
        wrapStyle.setWrapText(true);
        // 设置列宽为原来的两倍
        for (int i = 0; i <= 14; i++) {
            Sheet sheet = writer.getSheet();
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth == sheet.getDefaultColumnWidth() * 256) {
                currentWidth = 2048;
            }
            sheet.setColumnWidth(i, currentWidth * 2);
        }
        // 合并第一行的前7列，并设置居中
        writer.getSheet().addMergedRegion(new CellRangeAddress(0, 0, 0, 6));
        // 写入标题到合并的单元格
        Cell titleCell = writer.getSheet().createRow(0).createCell(0);
        titleCell.setCellValue("导入学生模板");
        titleCell.setCellStyle(centerStyle);
        // 写入表头（第1行）
        writer.writeCellValue(0, 1, "学院");
        writer.writeCellValue(1, 1, "专业");
        writer.writeCellValue(2, 1, "年级");
        writer.writeCellValue(3, 1, "班级");
        writer.writeCellValue(4, 1, "学号");
        writer.writeCellValue(5, 1, "姓名");
        writer.writeCellValue(6, 1, "性别");
        // 创建红色字体样式
        Font redFont = writer.getWorkbook().createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        // 加粗
        redFont.setBold(true);
        CellStyle redWrapStyle = writer.getWorkbook().createCellStyle();
        redWrapStyle.cloneStyleFrom(wrapStyle);
        redWrapStyle.setFont(redFont);
        // 读取注意事项文本
        String noticeText = this.readStudentNoticeTxtFile();
        // 合并注意事项单元格（1-3行，7-9列）
        writer.getSheet().addMergedRegion(new CellRangeAddress(1, 20, 7, 9));
        // 写入注意事项，并应用自动换行和红色样式
        Cell noticeCell = writer.getSheet().getRow(1).createCell(7);
        noticeCell.setCellValue(noticeText);
        noticeCell.setCellStyle(redWrapStyle);
        //存入数据库数据
        // 存入学院名称
        writer.writeCellValue(0, 2, prepareStudentExampleDTO.getDepartmentName());
        // 存入专业信息
        Optional.ofNullable(prepareStudentExampleDTO.getClassInfoList())
                .ifPresent(classInfoList -> {
                    Set<String> writtenMajorNames = new HashSet<>();
                    IntStream.range(0, classInfoList.size())
                            .forEach(i -> {
                                PrepareStudentExampleDTO.ClassInfo classInfo = classInfoList.get(i);
                                // 如果该专业名称还没有写入过，则写入
                                if (!writtenMajorNames.contains(classInfo.getMajorName())) {
                                    writer.writeCellValue(10, i + 4, classInfo.getMajorName());
                                    writtenMajorNames.add(classInfo.getMajorName());
                                }
                                writer.writeCellValue(11, i + 4, classInfo.getClassName());
                            });
                });
        // 存入年级信息
        Optional.ofNullable(prepareStudentExampleDTO.getGradeList())
                .ifPresent(gradeList ->
                        IntStream.range(0, gradeList.size())
                                .forEach(i -> {
                                    GradeDO grade = gradeList.get(i);
                                    writer.writeCellValue(12, i + 4, grade.getName());
                                })
                );
        // 输出到字节流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        // 关闭writer，释放资源
        writer.close();
        // 返回字节数组
        return outputStream.toByteArray();
    }


    @Override
    public PrepareStudentExampleDTO prepareDepartmentData(HttpServletRequest request) {
        // 根据请求获取用户信息
        UserDO userDO = userService.getUserByRequest(request);
        // 断言用户信息不为空
        assert userDO != null;
        // 根据用户UUID获取学术事务权限信息
        AcademicAffairsPermissionDO academicAffairsPermissionDO =
                academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(userDO.getUserUuid());
        // 检查学术事务权限信息是否存在，如果不存在则抛出异常
        if (academicAffairsPermissionDO == null) {
            throw new BusinessException("系统错误，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }
        // 根据部门UUID获取部门信息
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(academicAffairsPermissionDO.getDepartment());
        // 断言部门信息不为空
        assert departmentDO != null;
        //查询出来专业组
        List<MajorDO> majorDOList = majorDAO.getMajorListByDepartmentUuid(
                academicAffairsPermissionDO.getDepartment());
        //查询出来班级组
        List<AdministrativeClassDO> administrativeClassDOList =
                administrativeClassDAO
                        .getAdministrativeClassListByDepartment(academicAffairsPermissionDO.getDepartment());
        //查询出来年级组
        List<GradeDO> gradeDOList = gradeDAO.getGradeList();
        // 将专业列表转换为 Map，key 为 majorUuid，value 为班级列表
        Map<String, List<String>> majorClassMap = administrativeClassDOList.stream()
                .collect(Collectors.groupingBy(
                        AdministrativeClassDO::getMajorUuid,
                        Collectors.mapping(AdministrativeClassDO::getClassName, Collectors.toList())
                ));
        // 创建 ClassInfo 列表
        List<PrepareStudentExampleDTO.ClassInfo> classInfoList =
                majorDOList.stream()
                        .flatMap(major -> {
                            List<String> classes = majorClassMap.getOrDefault(major.getMajorUuid(), Collections.emptyList());
                            return classes.stream()
                                    // 按照班级名称中的数字进行排序
                                    .sorted((class1, class2) -> {
                                        // 提取数字部分进行比较
                                        String num1 = class1.replaceAll("\\D", "");
                                        String num2 = class2.replaceAll("\\D", "");
                                        // 如果都是数字，则按数字大小排序
                                        if (!num1.isEmpty() && !num2.isEmpty()) {
                                            return Integer.compare(
                                                    Integer.parseInt(num1),
                                                    Integer.parseInt(num2)
                                            );
                                        }
                                        // 如果无法提取数字，则使用字符串默认排序
                                        return class1.compareTo(class2);
                                    })
                                    .map(className -> new PrepareStudentExampleDTO.ClassInfo()
                                            .setMajorName(major.getMajorName())
                                            .setClassName(className)
                                    );
                        }).toList();
        // 设置到 DTO 中
        PrepareStudentExampleDTO prepareStudentExampleDTO = new PrepareStudentExampleDTO();
        prepareStudentExampleDTO.setClassInfoList(classInfoList)
                .setGradeList(gradeDOList)
                .setDepartmentName(departmentDO.getDepartmentName());
        // 返回学术事务权限的唯一标识符
        return prepareStudentExampleDTO;
    }

    @Override
    public byte[] checkBatchAddStudentVO(BatchAddStudentVO batchAddStudentVO) {
        // 1. 检查 VO 对象是否为空
        if (batchAddStudentVO == null) {
            throw new IllegalArgumentException("批量添加学生信息不能为空");
        }

        // 2. 检查 file 字段是否为空
        String base64File = batchAddStudentVO.getFile();
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
        return fileBytes;
    }

    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public BackAddStudentDTO batchImportIgnoreError(byte[] file, String departmentUuid) {
        return null;
    }

    @Override
    public String getDepartmentUuid(HttpServletRequest request) {
        UserDO userDO = userService.getUserByRequest(request);
        if (userDO == null) {
            throw new BusinessException("用户不存在，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }
        AcademicAffairsPermissionDO academicAffairsPermissionDO =
                academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(userDO.getUserUuid());
        if (academicAffairsPermissionDO == null) {
            throw new BusinessException("教务权限不存在，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }
        return academicAffairsPermissionDO.getDepartment();
    }

}
