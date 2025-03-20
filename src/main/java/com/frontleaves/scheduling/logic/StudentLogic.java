package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.ClassMappingDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.dto.StudentDisableDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * 学生逻辑
 *
 * @author fanfan187 | FALSHLACK
 * @version v1.0.0
 * @since v1.0.0
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
    private final UserDAO userDAO;
    private final MappingUtil mappingUtil;

    /**
     * 从给定的列表中提取学生信息并创建一个StudentImportDTO对象
     * 此方法确保从列表中的每个元素正确地提取学生信息，并将其设置到DTO对象中
     *
     * @param rowlist 包含学生信息的列表，包括部门、专业、年级、班级、ID、姓名和性别
     * @return 返回一个填充了学生信息的StudentImportDTO对象
     */
    private static @NotNull StudentImportDTO getStudentImportDTO(@NotNull List<Object> rowlist) {
        // 创建一个新的StudentImportDTO对象
        StudentImportDTO student = new StudentImportDTO();

        // 从列表中提取学生信息并设置到DTO对象中
        student.setDepartmentName(rowlist.get(0).toString().trim())
                .setMajorName(rowlist.get(1).toString().trim())
                .setGradeName(rowlist.get(2).toString().trim())
                .setClassName(rowlist.get(3).toString().trim())
                .setId(rowlist.get(4).toString().trim())
                .setName(rowlist.get(5).toString().trim())
                .setGender(rowlist.get(6).toString().trim());

        // 返回填充了学生信息的DTO对象
        return student;
    }

    /**
     * 将Excel数据转换为学生DTO列表
     * 使用Stream流处理Excel行数据，并将其转换为StudentImportDTO对象列表
     *
     * @param excelBytes Excel文件字节数组
     * @param startRow   开始读取的行号（从0开始计数）
     * @return 返回StudentImportDTO对象列表
     */
    public static List<StudentImportDTO> parseExcelToStudentList(byte[] excelBytes, int startRow,int columnsToCheck) {
        // 首先解析Excel文件获取行数据列表
        List<List<Object>> rowList = ProjectUtil.parseExcelToRowList(excelBytes, startRow,columnsToCheck);
        log.debug("原生列表{}",rowList);
        // 创建结果列表
        List<StudentImportDTO> studentList = new ArrayList<>();
        // 处理每一行数据，即使数据不完整也创建对象
        for (List<Object> row : rowList) {
            try {
                // 确保行数据至少有一个元素，避免处理完全空的行
                if (!row.isEmpty()) {
                    // 转换为StudentImportDTO对象并添加到结果列表
                    StudentImportDTO student = getStudentImportDTO(row);
                    studentList.add(student);
                }
            } catch (Exception e) {
                // 记录异常并继续处理下一行
                log.error("解析学生数据时出错: {}", e.getMessage());
            }
        }
        return studentList;
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
     * 获取导入所需的基础数据
     */
    private ImportBaseStudentDTO fetchImportBaseStudentDTO(String departmentUuid) {
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuidLastUpdate(departmentUuid);
        List<MajorDO> majorDOList = majorDAO.getMajorListByDepartmentUuidForUpdate(departmentUuid);
        List<GradeDO> gradeDOList = gradeDAO.getGradeListForUpdate();
        List<AdministrativeClassDO> administrativeClassDOList =
                administrativeClassDAO.getAdministrativeClassListByDepartmentForUpdate(departmentUuid);

        return new ImportBaseStudentDTO(departmentDO, majorDOList, gradeDOList, administrativeClassDOList);
    }

    /**
     * 验证学院名称
     *
     * @param departmentDO   学院DO对象
     * @param departmentName 要验证的学院名称
     * @throws DataNotFoundException 当学院名称不存在时抛出异常
     */
    private void validateDepartment(DepartmentDO departmentDO, String departmentName) throws DataNotFoundException {
        if (!departmentDO.getDepartmentName().equals(departmentName)) {
            throw new DataNotFoundException(DataNotFoundException.TypeEnum.DEPARTMENT_NAME_NOT_FOUND);
        }
    }

    /**
     * 根据专业名称查找专业对象
     *
     * @param majorList 专业列表
     * @param majorName 专业名称
     * @return 匹配的专业对象
     * @throws DataNotFoundException 当专业名称不存在时抛出异常
     */
    private MajorDO findMajorByName(List<MajorDO> majorList, String majorName) throws DataNotFoundException {
        MajorDO majorDO = majorList.stream()
                .filter(major -> major.getMajorName().equals(majorName))
                .findFirst()
                .orElse(null);
        if (majorDO == null) {
            throw new DataNotFoundException(DataNotFoundException.TypeEnum.MAJOR_NAME_NOT_FOUND);
        }
        return majorDO;
    }

    /**
     * 根据年级名称查找年级对象
     *
     * @param gradeList 年级列表
     * @param gradeName 年级名称
     * @return 匹配的年级对象
     * @throws DataNotFoundException 当年级名称不存在时抛出异常
     */
    private GradeDO findGradeByName(List<GradeDO> gradeList, String gradeName) throws DataNotFoundException {
        GradeDO gradeDO = gradeList.stream()
                .filter(grade -> grade.getName().equals(gradeName))
                .findFirst()
                .orElse(null);
        if (gradeDO == null) {
            throw new DataNotFoundException(DataNotFoundException.TypeEnum.GRADE_NAME_NOT_FOUND);
        }

        return gradeDO;
    }

    /**
     * 根据班级名称查找班级对象
     *
     * @param classList 班级列表
     * @param className 班级名称
     * @return 匹配的班级对象
     * @throws DataNotFoundException 当班级名称不存在时抛出异常
     */
    private AdministrativeClassDO findClassByName(List<AdministrativeClassDO> classList, String className)
            throws DataNotFoundException {
        AdministrativeClassDO administrativeClassDO = classList.stream()
                .filter(administrativeClass -> administrativeClass.getClassName().equals(className))
                .findFirst()
                .orElse(null);

        if (administrativeClassDO == null) {
            throw new DataNotFoundException(DataNotFoundException.TypeEnum.CLASS_NAME_NOT_FOUND);
        }

        return administrativeClassDO;
    }

    /**
     * 根据性别字符串设置学生性别
     *
     * @param studentDO 学生DO对象
     * @param genderStr 性别字符串（"男"或"女"）
     * @throws DataInvalidException 当性别字符串不合法时抛出异常
     */
    private void setStudentGender(StudentDO studentDO, String genderStr) throws DataInvalidException {
        if ("男".equals(genderStr)) {
            studentDO.setGender(true);
        } else if ("女".equals(genderStr)) {
            studentDO.setGender(false);
        } else {
            throw new DataInvalidException(DataInvalidException.TypeEnum.GENDER_ERROR);
        }
    }

    /**
     * 检查学生姓名是否合法
     *
     * @param name 学生姓名
     * @throws DataInvalidException 当姓名为空或为空字符串时抛出异常
     */
    private void validateStudentName(String name) throws DataInvalidException {
        if (name == null || name.isEmpty()) {
            throw new DataInvalidException(DataInvalidException.TypeEnum.NAME_EMPTY_ERROR);
        }
    }

    /**
     * 检查学生学号是否合法
     *
     * @param id 学生学号
     * @throws DataInvalidException 当学号为空或为空字符串时抛出异常
     */
    private void validateStudentId(String id) throws DataInvalidException {
        if (id == null || id.isEmpty()) {
            throw new DataInvalidException(DataInvalidException.TypeEnum.STUDENT_ID_EMPTY_ERROR);
        }
    }

    /**
     * 验证学生信息的合法性
     *
     * @param studentList          学生导入列表，包含待验证的学生信息
     * @param importBaseStudentDTO 导入基础学生信息，包含学院、专业、年级和班级信息
     * @param i                    当前验证的学生在列表中的索引
     * @return 返回验证学生信息的结果，包括专业、年级和班级对象
     * @throws BusinessException 当学生信息中的学院、专业、年级或班级名称不存在，或姓名、学号为空时抛出
     */
    private ValidateStudentReturnDTO validateStudent(
            @NotNull List<StudentImportDTO> studentList,
            @NotNull ImportBaseStudentDTO importBaseStudentDTO, int i
    ) throws BusinessException {
        StudentImportDTO student = studentList.get(i);
        // 行号（假设从第3行开始）
        int rowNumber = i + 3;
        // 验证学院名称是否存在
        if (!importBaseStudentDTO.getDepartment().getDepartmentName().equals(student.getDepartmentName())) {
            throw new BusinessException("第" + rowNumber + "行学院名称不存在", ErrorCode.BODY_ERROR);
        }
        // 通过专业名称查找对应的专业对象
        MajorDO majorDO = importBaseStudentDTO.getMajors().stream()
                .filter(major -> major.getMajorName().equals(student.getMajorName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("第" + rowNumber + "行专业名称不存在", ErrorCode.BODY_ERROR));
        // 通过年级名称查找对应的年级对象
        GradeDO gradeDO = importBaseStudentDTO.getGrades().stream()
                .filter(grade -> grade.getName().equals(student.getGradeName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("第" + rowNumber + "行年级名称不存在", ErrorCode.BODY_ERROR));
        // 通过班级名称查找对应的班级对象
        AdministrativeClassDO administrativeClassDO = importBaseStudentDTO.getClazz().stream()
                .filter(administrativeClass -> administrativeClass.getClassName().equals(student.getClassName()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("第" + rowNumber + "行班级名称不存在", ErrorCode.BODY_ERROR));
        // 验证姓名是否为空
        if (student.getName() == null || student.getName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行姓名不能为空", ErrorCode.BODY_ERROR);
        }
        // 验证学号是否为空
        if (student.getId() == null || student.getId().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行学号不能为空", ErrorCode.BODY_ERROR);
        }
        if (student.getGender() == null || student.getGender().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行性别不能为空", ErrorCode.BODY_ERROR);
        }
        // 创建验证学生信息返回对象，并设置专业、年级和班级对象
        return new ValidateStudentReturnDTO()
                .setMajorDO(majorDO)
                .setGradeDO(gradeDO)
                .setAdministrativeClassDO(administrativeClassDO);
    }

    @Override
    @Transactional
    public BackAddStudentDTO batchImportNoIgnoreError(
            byte[] file, String departmentUuid
    ) {
        // 解析Excel文件
        List<StudentImportDTO> studentList = parseExcelToStudentList(file, 2,7);
        log.debug("第一个学生信息{}", studentList.get(0));
        ImportBaseStudentDTO importBaseStudentDTO = fetchImportBaseStudentDTO(departmentUuid);
        // 不忽略警告提醒报错
        for (int i = 0; i < studentList.size(); i++) {
            // 创建并设置学生DO对象
            StudentDO studentDO = new StudentDO();
            ValidateStudentReturnDTO validateStudentReturnDTO = this.validateStudent(
                    studentList, importBaseStudentDTO, i);
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
                    .setGradeUuid(validateStudentReturnDTO.getGradeDO().getGradeUuid())
                    .setDepartment(importBaseStudentDTO.getDepartment().getDepartmentUuid())
                    .setMajor(validateStudentReturnDTO.getMajorDO().getMajorUuid())
                    .setClazz(validateStudentReturnDTO.getAdministrativeClassDO()
                            .getAdministrativeClassUuid())
                    .setGraduated(false);
            //继续数据存储
            studentDAO.saveStudentBackError(studentDO, i);
        }
        //成功
        return new BackAddStudentDTO()
                .setTotalCount(studentList.size())
                .setFailedCount(0)
                .setSuccessCount(studentList.size())
                .setFailedDetails(null);
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
        writer.writeCellValue(10, 3, "专业模板");
        writer.writeCellValue(11, 3, "班级模板");
        writer.writeCellValue(12, 3, "年级模板");
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


    /**
     * 准备部门数据
     * 根据当前用户权限，准备学生示例所需的部门相关数据
     *
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return PrepareStudentExampleDTO 包含部门数据的DTO对象
     */
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
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuidLastUpdate(academicAffairsPermissionDO.getDepartment());
        // 断言部门信息不为空
        assert departmentDO != null;
        //查询出来专业组
        List<MajorDO> majorDOList = majorDAO.getMajorListByDepartmentUuidForUpdate(
                academicAffairsPermissionDO.getDepartment());
        //查询出来班级组
        List<AdministrativeClassDO> administrativeClassDOList =
                administrativeClassDAO
                        .getAdministrativeClassListByDepartmentForUpdate(academicAffairsPermissionDO.getDepartment());
        //查询出来年级组
        List<GradeDO> gradeDOList = gradeDAO.getGradeListForUpdate();
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

    /**
     * 验证批量添加学生请求的数据有效性
     * 此方法主要负责检查批量添加学生时提交的Excel文件是否符合规范，
     * 包括文件是否存在、是否为有效的Excel格式、大小是否超过限制等
     *
     * @param batchAddStudentVO 批量添加学生的请求对象，包含文件的Base64编码等信息
     * @return 解码后的Excel文件字节数组，用于后续处理
     * @throws IllegalArgumentException 如果文件数据无效，抛出此异常
     */
    @Override
    public byte[] verifyStudentBatchAndBackFile(BatchAddStudentVO batchAddStudentVO) {
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

        // 返回解码后的文件字节数组，用于后续处理
        return fileBytes;
    }

    /**
     * 批量导入学生信息，忽略错误继续执行
     * 该方法使用了事务注解，遇到BusinessException时回滚事务
     *
     * @param file           Excel文件字节数组，包含学生信息
     * @param departmentUuid 部门UUID，用于关联学生记录到正确的部门
     * @return 返回包含导入结果的BackAddStudentDTO对象，包括成功和失败的统计信息
     */
    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public BackAddStudentDTO batchImportIgnoreError(byte[] file, String departmentUuid) {
        // 解析Excel文件
        List<StudentImportDTO> studentList = parseExcelToStudentList(file, 2,7);
        BackAddStudentDTO backAddStudentDTO = new BackAddStudentDTO();
        ImportBaseStudentDTO importBaseStudentDTO = fetchImportBaseStudentDTO(departmentUuid);
        // 创建失败详情列表
        List<BackAddStudentDTO.FailedDetail> failedDetails = new ArrayList<>();
        int successCount = 0;
        // 循环处理每条学生记录
        for (int i = 0; i < studentList.size(); i++) {
            try {
                //检查数据完整性
                this.validateStudentName(studentList.get(i).getName());
                this.validateStudentId(studentList.get(i).getId());
                // 验证学院名称
                validateDepartment(importBaseStudentDTO.getDepartment(), studentList.get(i).getDepartmentName());
                // 验证并获取专业
                MajorDO majorDO = findMajorByName(importBaseStudentDTO.getMajors(), studentList.get(i).getMajorName());
                GradeDO gradeDO = findGradeByName(importBaseStudentDTO.getGrades(), studentList.get(i).getGradeName());
                // 验证并获取班级
                AdministrativeClassDO administrativeClassDO = findClassByName(
                        importBaseStudentDTO.getClazz(),
                        studentList.get(i).getClassName());
                // 创建学生对象并设置性别
                StudentDO studentDO = new StudentDO();
                setStudentGender(studentDO, studentList.get(i).getGender());
                studentDO.setId(studentList.get(i).getId())
                        .setName(studentList.get(i).getName())
                        .setGradeUuid(gradeDO.getGradeUuid())
                        .setDepartment(importBaseStudentDTO.getDepartment().getDepartmentUuid())
                        .setMajor(majorDO.getMajorUuid())
                        .setClazz(administrativeClassDO.getAdministrativeClassUuid())
                        .setGraduated(false);
                // 尝试保存学生数据
                List<BackAddStudentDTO.FailedDetail> saveFailedDetails = studentDAO.saveStudentIgnoreError(studentDO, i);
                if (saveFailedDetails.isEmpty()) {
                    // 保存成功
                    successCount++;
                } else {
                    // 保存失败，将失败详情添加到总的失败详情列表
                    failedDetails.addAll(saveFailedDetails);
                }
            } catch (RuntimeException e) {
                // 捕获验证时的异常
                BackAddStudentDTO.FailedDetail failedDetail = getFailedDetail(e, i);
                failedDetails.add(failedDetail);
            }
        }
        // 设置统计结果
        backAddStudentDTO.setTotalCount(studentList.size())
                .setSuccessCount(successCount)
                .setFailedCount(failedDetails.size())
                .setFailedDetails(failedDetails.isEmpty() ? null : failedDetails);
        return backAddStudentDTO;
    }

    /**
     * 获取失败的详细信息
     * 该方法用于处理在添加学生过程中遇到的异常情况，根据不同的异常类型和行号，生成失败的详细信息
     *
     * @param e 异常对象，用于判断异常类型和获取异常信息
     * @param i 当前行号，用于定位错误发生的位置
     * @return BackAddStudentDTO.FailedDetail 返回包含失败信息的对象，包括行号和错误原因
     */
    private static BackAddStudentDTO.@NotNull FailedDetail getFailedDetail(RuntimeException e, int i) {
        // 创建一个FailedDetail对象来存储失败的详细信息
        BackAddStudentDTO.FailedDetail failedDetail = new BackAddStudentDTO.FailedDetail();
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


    /**
     * 从请求中获取用户的部门UUID
     * 此方法首先根据请求获取用户信息，然后根据用户UUID获取教务权限信息，
     * 最终返回用户所属的部门UUID如果用户或教务权限信息不存在，则抛出业务异常
     *
     * @param request HTTP请求，包含用户信息
     * @return 用户所属的部门UUID
     * @throws BusinessException 如果用户或教务权限信息不存在，则抛出此异常
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
     * 根据 studentUuid 获取学生信息
     */
    @Override
    public StudentDTO getStudentByUuid(String studentUuid) {
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }
        return BeanUtil.toBean(studentDO, StudentDTO.class);
    }

    /**
     * 获取学生列表信息
     *
     * @param page        页码,从1开始
     * @param size        每页记录数
     * @param isDesc      是否降序排列
     * @param clazz       可选参数,班级名称
     * @param isGraduated 可选参数,是否毕业
     * @param name        可选参数,学生姓名
     * @param id          可选参数,学生ID
     * @return 返回一个包含学生信息的PageDTO对象
     */
    @Override
    public PageDTO<StudentDTO> getStudentList(int page, int size, Boolean isDesc,
                                              @Nullable String clazz, @Nullable Boolean isGraduated,
                                              @Nullable String name, @Nullable String id
    ) {
        // 调用DAO层方法获取分页学生数据
        Page<StudentDO> resultPage = studentDAO.listStudents(page, size, isDesc, clazz, isGraduated, name, id);

        // 使用ProjectUtil 中的 convertPageToPageDTO 方法进行抓换
        return ProjectUtil.convertPageToPageDTO(resultPage, StudentDTO.class);
    }

    /**
     * 添加学生信息
     * <p>
     * 此方法负责接收一个学生对象,验证其信息的完整性,格式的正确性,
     * 然后将学生信息保存到数据库中,并转换为学生DTO对象
     * </p>
     *
     * @param studentDTO 学生对象,包含学生的基本信息
     * @return 保存后的学生对象
     */
    @Override
    public StudentDTO addStudent(StudentDTO studentDTO) {
        // 生成 UUID,避免由前端传递
        String studentUuid = UuidUtil.generateUuidNoDash();

        // 校验学生姓名
        if (!Pattern.matches(StringConstant.Regular.STUDENT_NAME_REGULAR_EXPRESSION, studentDTO.getName())) {
            throw new BusinessException("学生姓名格式错误", ErrorCode.PARAMETER_INVALID);
        }
        // 检查班级是否为空(进行外键映射)
        if (studentDTO.getClazz() == null || studentDTO.getClazz().isBlank()) {
            throw new BusinessException("班级不能为空", ErrorCode.PARAMETER_INVALID);
        }

        // 根据班级信息自动定位年级、学院和专业
        ClassMappingDTO classMapping = mappingUtil.getClassMappingByClazz(studentDTO.getClazz());
        // 将映射出来的信息设置到DTO中
        studentDTO.setGradeUuid(classMapping.getGradeUuid());
        studentDTO.setDepartment(classMapping.getDepartmentUuid());
        studentDTO.setMajor(classMapping.getMajorUuid());

        // DTO -> DO
        StudentDO studentDO = BeanUtil.toBean(studentDTO, StudentDO.class);
        studentDO.setStudentUuid(studentUuid);

        // 存入数据库
        try {
            studentDAO.save(studentDO);
        } catch (Exception e) {
            log.error("学生信息保存失败", e);
            throw new BusinessException("学生信息保存失败", ErrorCode.OPERATION_FAILED);
        }
        // 从数据库重新查询完整记录,确保自动填充的数据能获取到
        StudentDO saveStudentDO = studentDAO.getStudentByUuid(studentUuid);
        if (saveStudentDO == null) {
            throw new BusinessException("学生信息保存失败", ErrorCode.OPERATION_FAILED);
        }

        // DO -> DTO
        StudentDTO resultDTO = BeanUtil.toBean(studentDO, StudentDTO.class);
        resultDTO.setStatus(resultDTO.getUserUuid() != null && !resultDTO.getUserUuid().isBlank());
        return resultDTO;
    }

    /**
     * 根据学生UUID禁用或启用学生账户
     * <p>
     * 此方法首先根据学生UUID获取学生详细信息,然后检查该学生是否已创建系统账号
     * 如果学生存在且已创建系统账号,则根据传入的禁用标志更新用户状态
     * </p>
     *
     * @param studentUuid 学生的唯一UUID
     * @param disable 是否禁用学生账户的标志,true表示禁用,false表示启用
     * @return 返回一个包含学生UUID和禁用状态的DTO对象
     */
    @Override
    public StudentDisableDTO disableStudent(String studentUuid, Boolean disable) {
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }

        // 检查学生是否已有账号(是否注册)
        if (studentDO.getUserUuid() == null || studentDO.getUserUuid().isBlank()) {
            throw new BusinessException("该学生未创建系统账号", ErrorCode.NOT_EXIST);
        }

        // 获取用户对象
        UserDO oldUserDO = userDAO.getUserByUuid(studentDO.getUserUuid());
        if (oldUserDO == null) {
            throw new BusinessException(StringConstant.USER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        UserDO newUserDO = BeanUtil.toBean(oldUserDO, UserDO.class);

        // 更新用户状态
        newUserDO.setStatus((byte) (disable ? 0 : 1));
        userDAO.updateUser(oldUserDO, newUserDO);

        return new StudentDisableDTO()
                .setStudentUuid(studentDO.getStudentUuid())
                .setStatus(disable);
    }


    /**
     * 删除学生
     * <p>
     * 此方法首先通过学生UUID检查学生是否存在如果学生存在,进一步检查学生是否已注册
     * 如果学生未注册,则执行删除操作；如果已注册或不存在,则抛出相应的异常
     * </p>
     *
     * @param studentUuid 学生的唯一标识符
     * @throws BusinessException 当学生不存在或学生已注册时抛出
     */
    @Override
    public void deleteStudent(String studentUuid) {
        // 检查学生是否存在
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }

        // 检查学生是否已注册
        boolean isRegistered = userDAO.existsByUserUuid(studentDO.getUserUuid());
        if (isRegistered) {
            throw new BusinessException("学生已注册,无法删除", ErrorCode.OPERATION_FAILED);
        }

        // 删除学生
        studentDAO.removeById(studentUuid);
    }

    /**
     * 编辑学生信息
     *
     * @param studentUuid 学生的唯一标识符
     * @param studentVO 包含学生新信息的视图对象
     * @return 更新后的学生数据传输对象
     * @throws BusinessException 如果学生不存在或更新操作失败
     */
    @Override
    public StudentDTO editStudent(String studentUuid, StudentVO studentVO) {
        StudentDO studentDO = studentDAO.editStudent(studentUuid, studentVO);
        StudentDTO studentDTO = new StudentDTO();
        BeanUtils.copyProperties(studentDO, studentDTO);
        return studentDTO;
    }
}
