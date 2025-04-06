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
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.exceptions.lib.DataInvalidException;
import com.frontleaves.scheduling.exceptions.lib.DataNotFoundException;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.BatchAddCourseVO;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 课程库逻辑处理类，实现了 {@link CourseLibraryService} 接口，提供了课程库管理的具体实现。
 * <p>
 * 该类负责处理与课程库相关的业务逻辑，包括查询、添加、删除和更新课程库信息等。
 * 通过依赖注入的方式获取所需的其他服务或组件。
 * </p>
 *
 * @author Claude AI
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLibraryLogic implements CourseLibraryService {

    private final CourseLibraryDAO courseLibraryDAO;
    private final CourseCategoryDAO courseCategoryDAO;
    private final CoursePropertyDAO coursePropertyDAO;
    private final CourseTypeDAO courseTypeDAO;
    private final CourseNatureDAO courseNatureDAO;
    private final DepartmentDAO departmentDAO;
    private final UserService userService;
    private final ClassroomTypeDAO classroomTypeDAO;
    private final AdministrativeClassDAO administrativeClassDAO;

    /**
     * 批量添加课程库
     * <p>
     * 此方法用于批量添加课程库信息
     * 它会解析 Excel 文件并将其转换为课程列表
     * 然后验证每个课程的有效性，并将其添加到数据库中
     *
     * @param courseLibraryAndTeacherCourseQualificationListDTO 课程库和班级DTO对象，用于存储课程信息
     * @param courseMap                                         包含课程ID和课程库对象的映射，用于查找特定课程
     * @param specificCourseIdVO                                包含特定课程ID的VO对象，用于指定需要查找的课程
     *                                                          本方法首先根据特定课程ID从课程映射中获取课程库对象如果未找到对应的课程，
     *                                                          则抛出业务异常表示未找到匹配的课程如果找到了课程，则将其转换为课程库DTO对象
     *                                                          并设置到CourseLibraryAndClassDTO对象中
     */
    private static @NotNull CourseImportDTO getCourseImportDTO(@NotNull List<Object> rowList) {
        CourseImportDTO courseLibrary = new CourseImportDTO();

        courseLibrary.setId(rowList.get(0).toString().trim())
                .setName(rowList.get(1).toString().trim())
                .setCategory(rowList.get(2).toString().trim())
                .setProperty(rowList.get(3).toString().trim())
                .setType(rowList.get(4).toString().trim())
                .setNature(rowList.get(5).toString().trim())
                .setDepartment(rowList.get(6).toString().trim());

// 处理所有的BigDecimal类型字段
        try {

            // 总课时
            String totalHoursStr = rowList.get(7).toString().trim();
            if (!totalHoursStr.isEmpty()) {
                courseLibrary.setTotalHours(new BigDecimal(totalHoursStr));
            }

            // 周课时
            String weekHoursStr = rowList.get(8).toString().trim();
            if (!weekHoursStr.isEmpty()) {
                courseLibrary.setWeekHours(new BigDecimal(weekHoursStr));
            }

            // 理论课时
            String theoryHoursStr = rowList.get(9).toString().trim();
            if (!theoryHoursStr.isEmpty()) {
                courseLibrary.setTheoryHours(new BigDecimal(theoryHoursStr));
            }

            // 实验课时
            String experimentHoursStr = rowList.get(10).toString().trim();
            if (!experimentHoursStr.isEmpty()) {
                courseLibrary.setExperimentHours(new BigDecimal(experimentHoursStr));
            }

            // 实践课时
            String practiceHoursStr = rowList.get(11).toString().trim();
            if (!practiceHoursStr.isEmpty()) {
                courseLibrary.setPracticeHours(new BigDecimal(practiceHoursStr));
            }

            // 上机课时
            String computerHoursStr = rowList.get(12).toString().trim();
            if (!computerHoursStr.isEmpty()) {
                courseLibrary.setComputerHours(new BigDecimal(computerHoursStr));
            }

            // 其他课时
            String otherHoursStr = rowList.get(13).toString().trim();
            if (!otherHoursStr.isEmpty()) {
                courseLibrary.setOtherHours(new BigDecimal(otherHoursStr));
            }

            // 学分
            String creditStr = rowList.get(14).toString().trim();
            if (!creditStr.isEmpty()) {
                courseLibrary.setCredit(new BigDecimal(creditStr));
            }
            // 计算并验证总学时
            if (courseLibrary.getTotalHours() != null) {
                BigDecimal calculatedTotal = BigDecimal.ZERO;

                // 确保各类课时不为null，如果为null则默认为0
                BigDecimal theoryHours = courseLibrary.getTheoryHours() != null ? courseLibrary.getTheoryHours() : BigDecimal.ZERO;
                BigDecimal experimentHours = courseLibrary.getExperimentHours() != null ? courseLibrary.getExperimentHours() : BigDecimal.ZERO;
                BigDecimal practiceHours = courseLibrary.getPracticeHours() != null ? courseLibrary.getPracticeHours() : BigDecimal.ZERO;
                BigDecimal computerHours = courseLibrary.getComputerHours() != null ? courseLibrary.getComputerHours() : BigDecimal.ZERO;
                BigDecimal otherHours = courseLibrary.getOtherHours() != null ? courseLibrary.getOtherHours() : BigDecimal.ZERO;

                // 求和
                calculatedTotal = calculatedTotal.add(theoryHours)
                                               .add(experimentHours)
                                               .add(practiceHours)
                                               .add(computerHours)
                                               .add(otherHours);

                log.debug("总学时验证 - 输入总学时: {}, 计算总学时: {}, 各类课时: 理论={}, 实验={}, 实践={}, 上机={}, 其他={}",
                         courseLibrary.getTotalHours(), calculatedTotal, theoryHours, experimentHours,
                         practiceHours, computerHours, otherHours);

                // 比较计算得出的总学时和输入的总学时是否相等
                if (calculatedTotal.compareTo(courseLibrary.getTotalHours()) != 0) {
                    throw new BusinessException("总学时应等于各类课时之和：理论课时+实验课时+实践课时+上机课时+其他课时", ErrorCode.PARAMETER_ERROR);
                }
            }
        } catch (BusinessException e) {
            throw e;
        } catch (NumberFormatException e) {
            throw new BusinessException("数值格式不正确，请检查课时或学分的输入", ErrorCode.PARAMETER_ERROR);
        }

        // 设置教室类型
        courseLibrary.setTheoryClassroomType(rowList.get(15).toString().trim())
                .setExperimentClassroomType(rowList.get(16).toString().trim())
                .setPracticeClassroomType(rowList.get(17).toString().trim())
                .setComputerClassroomType(rowList.get(18).toString().trim());


        String isEnableStr = rowList.get(19).toString().trim();

        if ("1".equals(isEnableStr) || "启用".equals(isEnableStr)) {
            courseLibrary.setIsEnabled(true);
        } else {
            courseLibrary.setIsEnabled(!"0".equals(isEnableStr) && !"禁用".equals(isEnableStr));
        }

        return courseLibrary;
    }

    /**
     * 解析 Excel 文件并将其转换为课程列表
     * <p>
     * 此方法接受一个字节数组表示的 Excel 文件，并将其解析为课程列表
     * 它会跳过前两行，并检查列数是否符合预期
     *
     * @param excelBytes     Excel 文件的字节数组
     * @param startRow       开始解析的行号
     * @param columnsToCheck 需要检查的列数
     * @return 解析后的课程列表
     */
    private static List<CourseImportDTO> parseExcelToCourseList(byte[] excelBytes, int startRow, int columnsToCheck) {
        // 解析Excel文件获取行数据列表
        List<List<Object>> rowList = ProjectUtil.parseExcelToRowList(excelBytes, startRow, columnsToCheck);
        log.debug("原生列表{}", rowList);

        // 创建结果列表
        List<CourseImportDTO> courseList = new ArrayList<>();

        // 处理每一行数据，即使数据不完整也创建对象
        for (int i = 0; i < rowList.size(); i++) {
            List<Object> row = rowList.get(i);
            try {
                // 确保行数据至少有一个元素，避免处理完全空的行
                if (!row.isEmpty()) {
                    // 转换为CourseLibraryImportDTO对象并添加到结果列表
                    CourseImportDTO course = getCourseImportDTO(row);
                    courseList.add(course);
                }
            } catch (RuntimeException e) {
                // 记录异常并继续处理下一行
                log.error("解析课程库数据时出错: {}", e.getMessage());
                throw e;
            }
        }
        return courseList;
    }

    /**
     * 获取失败的详细信息
     * <p>
     * 此方法用于创建一个包含失败详细信息的对象
     * 它会根据异常类型设置不同的失败原因
     *
     * @param e 异常对象
     * @param i 行号
     * @return 失败详细信息对象
     */
    private static @NotNull BackAddCourseDTO.FailedDetail getFailedDetail(RuntimeException e, int i) {
        // 创建一个FailedDetail对象来存储失败的详细信息
        BackAddCourseDTO.FailedDetail failedDetail = new BackAddCourseDTO.FailedDetail();
        // 设置失败的行号，+3是因为数据开始于第4行(标题行+示例行)
        failedDetail.setRow(i + 3);
        // 根据异常类型设置失败的原因
        if (e instanceof DataNotFoundException error) {
            // 如果是数据不存在异常，设置具体原因
            failedDetail.setReason("数据不存在：" + error.getReason());
        } else if (e instanceof DataInvalidException error) {
            // 如果是数据无效异常，设置具体原因
            failedDetail.setReason("数据无效：" + error.getReason());
        } else if (e instanceof BusinessException error) {
            // 如果是业务异常，设置异常消息
            failedDetail.setReason(error.getMessage());
        } else {
            // 如果是其他未知异常，设置通用错误信息
            failedDetail.setReason("未知错误：" + e.getMessage());
        }
        // 返回包含失败信息的对象
        return failedDetail;
    }

    /**
     * 添加课程库
     * <p>
     * 此方法用于将课程库信息添加到系统中在此之前，它会验证课程类别、课程属性、课程类型、课程性质和部门的存在性
     * 如果任何一方不存在，则抛出业务异常这确保了课程库信息的完整性和一致性
     *
     * @param courseLibraryVO 课程库的视图对象，包含课程库的相关信息，不能为空
     */
    @Override
    public void addCourseLibrary(@NotNull CourseLibraryVO courseLibraryVO, HttpServletRequest request) {
        // 检查ID是否重复
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.getCourseLibraryById(courseLibraryVO.getId());
        if (courseLibraryDO != null) {
            throw new BusinessException("课程ID已存在", ErrorCode.PARAMETER_ERROR);
        }

        // 验证课程类别（可为空）
        if (courseLibraryVO.getCategory() != null && !courseLibraryVO.getCategory().isEmpty()) {
            CourseCategoryDO courseCategoryDO = courseCategoryDAO.getCourseCategoryByUuid(courseLibraryVO.getCategory());
            if (courseCategoryDO == null) {
                throw new BusinessException("指定的课程类别不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 验证课程属性（可为空）
        if (courseLibraryVO.getProperty() != null && !courseLibraryVO.getProperty().isEmpty()) {
            CoursePropertyDO coursePropertyDO = coursePropertyDAO.getCoursePropertyByUuid(courseLibraryVO.getProperty());
            if (coursePropertyDO == null) {
                throw new BusinessException("指定的课程属性不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 验证课程类型（必填）
        CourseTypeDO courseTypeDO = courseTypeDAO.getCourseTypeByUuid(courseLibraryVO.getType());
        if (courseTypeDO == null) {
            throw new BusinessException("课程类型不存在", ErrorCode.NOT_EXIST);
        }

        // 验证课程性质（可为空）
        if (courseLibraryVO.getNature() != null && !courseLibraryVO.getNature().isEmpty()) {
            CourseNatureDO courseNatureDO = courseNatureDAO.getCourseNatureByUuid(courseLibraryVO.getNature());
            if (courseNatureDO == null) {
                throw new BusinessException("指定的课程性质不存在", ErrorCode.NOT_EXIST);
            }
        }

        // 验证部门（必填）
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(courseLibraryVO.getDepartment());
        if (departmentDO == null) {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }

        // 验证教室类型（可为空）
        Stream.of(
                        courseLibraryVO.getTheoryClassroomType(),
                        courseLibraryVO.getExperimentClassroomType(),
                        courseLibraryVO.getPracticeClassroomType(),
                        courseLibraryVO.getComputerClassroomType()

                )
                .filter(Objects::nonNull)
                .filter(getString -> !getString.isBlank())
                .forEach(classroomType -> {
                    ClassroomTypeDO classroomTypeDO = classroomTypeDAO.getTypeByUuid(classroomType);
                    if (classroomTypeDO == null) {
                        throw new BusinessException("指定的教室类型不存在", ErrorCode.NOT_EXIST);
                    }
                });

        UserDO getUser = userService.getUserByRequest(request);

        // 创建一个新的课程库对象，并从视图对象中复制属性
        CourseLibraryDO newCourseLibraryDO = new CourseLibraryDO();
        BeanUtil.copyProperties(courseLibraryVO, newCourseLibraryDO, ProjectOption.stringBlankToNull());
        newCourseLibraryDO.setEditUser(getUser.getUserUuid());
        log.debug("课程库对象{}", newCourseLibraryDO);
        // 保存课程库对象到数据库
        courseLibraryDAO.saveCourseLibrary(newCourseLibraryDO);
    }

    /**
     * 更新课程库信息
     * <p>
     * 本方法通过课程UUID获取课程库对象，并根据传入的课程库视图对象（CourseLibraryVO）信息进行更新
     * 如果课程库视图对象中的类别、属性、类型、性质或部门不存在，则抛出业务异常
     *
     * @param courseUuid      课程库的唯一标识符
     * @param courseLibraryVO 包含更新信息的课程库视图对象
     */
    @Override
    public void updateCourseLibrary(String courseUuid, CourseLibraryVO courseLibraryVO) {
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.getCourseLibraryById(courseLibraryVO.getId());
        if (courseLibraryDO != null && !courseLibraryDO.getCourseLibraryUuid().equals(courseUuid)) {
            throw new BusinessException("课程ID已存在", ErrorCode.PARAMETER_ERROR);
        }

        // 根据课程UUID获取课程库对象
        CourseLibraryDO updateCourseLibraryDO = courseLibraryDAO.getCourseLibraryByUuid(courseUuid);

        if (updateCourseLibraryDO != null) {
            // 验证课程类别是否存在
            Optional.ofNullable(courseLibraryVO.getCategory())
                    .ifPresent(data -> {
                        if (!data.isBlank()) {
                            CourseCategoryDO courseCategoryDO = courseCategoryDAO.getCourseCategoryByUuid(data);
                            if (courseCategoryDO == null) {
                                throw new BusinessException("课程类别不存在", ErrorCode.NOT_EXIST);
                            }
                        }
                    });

            // 验证课程属性是否存在
            Optional.ofNullable(courseLibraryVO.getProperty())
                    .ifPresent(data -> {
                        if (!data.isBlank()) {
                            CoursePropertyDO coursePropertyDO = coursePropertyDAO.getCoursePropertyByUuid(data);
                            if (coursePropertyDO == null) {
                                throw new BusinessException("课程属性不存在", ErrorCode.NOT_EXIST);
                            }
                        }
                    });

            // 验证课程类型是否存在
            Optional.ofNullable(courseLibraryVO.getType())
                    .map(courseTypeDAO::getCourseTypeByUuid)
                    .orElseThrow(() -> new BusinessException("课程类型不存在", ErrorCode.NOT_EXIST));

            // 验证课程性质是否存在
            Optional.ofNullable(courseLibraryVO.getNature())
                    .ifPresent(data -> {
                        if (!data.isBlank()) {
                            CourseNatureDO courseNatureDO = courseNatureDAO.getCourseNatureByUuid(data);
                            if (courseNatureDO == null) {
                                throw new BusinessException("课程性质不存在", ErrorCode.NOT_EXIST);
                            }
                        }
                    });

            // 验证部门是否存在
            Optional.ofNullable(courseLibraryVO.getDepartment())
                    .map(departmentDAO::getDepartmentByUuid)
                    .orElseThrow(() -> new BusinessException("部门不存在", ErrorCode.NOT_EXIST));

            // 验证教室类型是否存在
            Stream.of(
                            courseLibraryVO.getTheoryClassroomType(),
                            courseLibraryVO.getExperimentClassroomType(),
                            courseLibraryVO.getPracticeClassroomType(),
                            courseLibraryVO.getComputerClassroomType()

                    )
                    .filter(Objects::nonNull)
                    .filter(getString -> !getString.isBlank())
                    .forEach(classroomType -> {
                        ClassroomTypeDO classroomTypeDO = classroomTypeDAO.getTypeByUuid(classroomType);
                        if (classroomTypeDO == null) {
                            throw new BusinessException("教室类型不存在", ErrorCode.NOT_EXIST);
                        }
                    });


            // 将课程库视图对象的属性复制到课程库对象中
            BeanUtil.copyProperties(courseLibraryVO, updateCourseLibraryDO, ProjectOption.stringBlankToNull());

            // 更新课程库信息
            courseLibraryDAO.updateCourseLibrary(updateCourseLibraryDO);
        }
    }

    /**
     * 根据课程UUID删除课程库
     * <p>
     * 此方法首先通过课程UUID获取课程库对象，如果找不到对应的课程库，
     * 则抛出一个表示课程库不存在的业务异常如果课程库存在，则调用DAO层方法将其删除
     *
     * @param courseUuid 课程库的唯一UUID，用于标识要删除的课程库
     * @throws BusinessException 当课程库不存在时抛出的业务异常
     */
    @Override
    public void deleteCourseLibrary(String courseUuid) {
        // 根据UUID获取课程库对象
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.getCourseLibraryByUuid(courseUuid);
        // 检查课程库是否存在，如果不存在则抛出异常
        if (courseLibraryDO == null ) {
            throw new BusinessException("课程库不存在", ErrorCode.NOT_EXIST);
        }
        // 删除课程库
        courseLibraryDAO.deleteCourseLibrary(courseLibraryDO);
    }

    /**
     * 获取课程库分页信息
     *
     * @param page 页码，从1开始
     * @param size 每页记录数
     * @param name 课程库名称，用于模糊查询
     * @return 包含课程库信息的分页数据传输对象
     */
    @Override
    public PageDTO<CourseLibraryDTO> getCourseLibrary(Integer page, Integer size, String name) {
        // 获取课程库分页数据
        Page<CourseLibraryDO> courseLibraryList = courseLibraryDAO.getCourseLibraryPage(page, size, name);

        // 如果查询结果为空，则直接返回一个空的PageDTO对象
        if (courseLibraryList.getTotal() == 0) {
            return new PageDTO<>();
        } else {
            return ProjectUtil.convertPageToPageDTO(courseLibraryList, CourseLibraryDTO.class);
        }
    }

    /**
     * 获取课程库列表
     * <p>
     * 此方法用于获取课程库的列表信息
     * 它会根据传入的参数过滤课程库，并返回一个包含课程库信息的列表
     *
     * @param courseCategoryUuid 课程类别UUID
     * @param coursePropertyUuid 课程属性UUID
     * @param courseTypeUuid     课程类型UUID
     * @param courseNatureUuid   课程性质UUID
     * @param courseDepartmentUuid 课程部门UUID
     * @return 包含课程库信息的列表
     */
    @Override
    public List<CourseLiteDTO> getCourseLibraryList(String courseCategoryUuid, String coursePropertyUuid, String courseTypeUuid, String courseNatureUuid, String courseDepartmentUuid) {
        List<CourseLibraryDO> courseLibraryList = courseLibraryDAO.getCourseLibraryList(courseCategoryUuid, coursePropertyUuid, courseTypeUuid, courseNatureUuid, courseDepartmentUuid);
        if (courseLibraryList.isEmpty()) {
            return new ArrayList<>();
        }

        List<CourseCategoryDO> getCourseCategoryList = courseCategoryDAO.getCourseCategoryList();
        List<CoursePropertyDO> getCoursePropertyList = coursePropertyDAO.getCoursePropertyList();
        List<CourseTypeDO> getCourseTypeList = courseTypeDAO.getCourseTypeList();
        List<CourseNatureDO> getCourseNatureList = courseNatureDAO.getCourseNatureList();
        List<DepartmentDO> getDepartmentList = departmentDAO.getDepartmentList();

        // 转换为 CourseLiteDTO
        return courseLibraryList.stream().map(courseLibrary -> new CourseLiteDTO()
                .setCourseLibraryUuid(courseLibrary.getCourseLibraryUuid())
                .setName(courseLibrary.getName())
                .setCategory(getCourseCategoryList.stream()
                        .filter(courseCategory -> courseCategory.getCourseCategoryUuid().equals(courseLibrary.getCategory()))
                        .findFirst()
                        .map(CourseCategoryDO::getName)
                        .orElse(null))
                .setProperty(getCoursePropertyList.stream()
                        .filter(courseProperty -> courseProperty.getCoursePropertyUuid().equals(courseLibrary.getProperty()))
                        .findFirst()
                        .map(CoursePropertyDO::getName)
                        .orElse(null))
                .setType(getCourseTypeList.stream()
                        .filter(courseType -> courseType.getCourseTypeUuid().equals(courseLibrary.getType()))
                        .findFirst()
                        .map(CourseTypeDO::getName)
                        .orElse(null))
                .setNature(getCourseNatureList.stream()
                        .filter(courseNature -> courseNature.getCourseNatureUuid().equals(courseLibrary.getNature()))
                        .findFirst()
                        .map(CourseNatureDO::getName)
                        .orElse(null))
                .setDepartment(getDepartmentList.stream()
                        .filter(department -> department.getDepartmentUuid().equals(courseLibrary.getDepartment()))
                        .findFirst()
                        .map(DepartmentDO::getDepartmentName)
                        .orElse(null))

        ).toList();
    }

    /**
     * 读取课程库导入通知文件的内容
     * <p>
     * 此方法尝试从类路径下的特定位置读取一个名为 "course-import-notice.txt" 的文件
     * 如果文件存在，则读取其内容并以字符串形式返回如果文件不存在，或在读取过程中发生错误，
     * 则返回一个预定义的默认注意事项文本
     *
     * @return 文件内容或默认注意事项文本
     */
    private @org.jetbrains.annotations.NotNull String readCourseNoticeFile() {
        try {
            // 从资源文件夹读取 notice.txt
            Resource resource = new ClassPathResource("notes/course-import-notice.txt");
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
                    3. 请勿修改模板结构
                    4. 总学时必须等于理论课时+实验课时+实践课时+上机课时+其他课时的总和""";
            }
        } catch (IOException e) {
            // 如果读取失败，返回默认文本
            return """
                    注意事项：
                    1. 请严格按照模板填写信息
                    2. 所有信息必须准确无误
                3. 请勿修改模板结构
                4. 总学时必须等于理论课时+实验课时+实践课时+上机课时+其他课时的总和""";
        }
    }
    /**
     * 准备课程数据
     * <p>
     * 此方法用于准备课程数据，包括获取所有基础数据并将其转换为DTO对象
     * </p>
     *
     * @return 准备好的课程数据传输对象
     */
    @Override
    public PrepareCourseDTO prepareCourseData() {
        // 获取所有基础数据
        List<CourseCategoryDO> categoryList = courseCategoryDAO.getCourseCategoryList();
        List<CoursePropertyDO> propertyList = coursePropertyDAO.getCoursePropertyList();
        List<CourseTypeDO> typeList = courseTypeDAO.getCourseTypeList();
        List<CourseNatureDO> natureList = courseNatureDAO.getCourseNatureList();
        List<DepartmentDO> departmentList = departmentDAO.getDepartmentList();
        List<ClassroomTypeDO> classroomTypeList = classroomTypeDAO.getClassroomTypeList();

        // 转换为DTO对象
        List<PrepareCourseDTO.CourseInfo> courseInfoList = categoryList.stream()
                .flatMap(category -> {
                    // 获取每个类别下的课程属性
                    return propertyList.stream()
                            .map(property -> new PrepareCourseDTO.CourseInfo()
                                    .setCategoryName(category.getName())
                                    .setPropertyName(property.getName())
                            );
                }).toList();

        // 创建PrepareCourseDTO对象
        PrepareCourseDTO prepareCourseDTO = new PrepareCourseDTO();

        // 设置基础数据
        prepareCourseDTO.setCourseInfoList(courseInfoList)
                .setCategoryList(categoryList.stream()
                        .map(category -> new PrepareCourseDTO.CategoryInfo()
                                .setUuid(category.getCourseCategoryUuid())
                                .setName(category.getName()))
                        .toList())
                .setPropertyList(propertyList.stream()
                        .map(property -> new PrepareCourseDTO.PropertyInfo()
                                .setUuid(property.getCoursePropertyUuid())
                                .setName(property.getName()))
                        .toList())
                .setTypeList(typeList.stream()
                        .map(type -> new PrepareCourseDTO.TypeInfo()
                                .setUuid(type.getCourseTypeUuid())
                                .setName(type.getName()))
                        .toList())
                .setNatureList(natureList.stream()
                        .map(nature -> new PrepareCourseDTO.NatureInfo()
                                .setUuid(nature.getCourseNatureUuid())
                                .setName(nature.getName()))
                        .toList())
                .setDepartmentList(departmentList.stream()
                        .map(department -> new PrepareCourseDTO.DepartmentInfo()
                                .setUuid(department.getDepartmentUuid())
                                .setName(department.getDepartmentName()))
                        .toList())
                .setClassroomTypeList(classroomTypeList.stream()
                        .map(classroomType -> new PrepareCourseDTO.ClassroomTypeInfo()
                                .setUuid(classroomType.getClassTypeUuid())
                                .setName(classroomType.getName()))
                        .toList());

        return prepareCourseDTO;
    }

    /**
     * 获取课程导入模板
     * <p>
     * 此方法用于生成一个课程导入模板Excel文件，并返回其字节数组
     * </p>
     *
     * @param prepareCourseDTO 准备好的课程数据传输对象
     * @return 课程导入模板的字节数组
     */
    @Override
    public byte[] getCourseImportTemplate(PrepareCourseDTO prepareCourseDTO) {
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
        for (int i = 0; i <= 19; i++) {
            Sheet sheet = writer.getSheet();
            int currentWidth = sheet.getColumnWidth(i);
            if (currentWidth == sheet.getDefaultColumnWidth() * 256) {
                currentWidth = 2048;
            }
            sheet.setColumnWidth(i, currentWidth * 2);
        }
        // 为参考数据标题列设置特定宽度
        Sheet sheet = writer.getSheet();
        sheet.setColumnWidth(23, 6000); // 课程类别列表
        sheet.setColumnWidth(24, 6000); // 课程属性列表
        sheet.setColumnWidth(25, 6000); // 课程类型列表
        sheet.setColumnWidth(26, 6000); // 课程性质列表
        sheet.setColumnWidth(27, 6000); // 部门列表
        sheet.setColumnWidth(28, 6000); // 教室类型列表
        sheet.setColumnWidth(29, 6000); // 状态示例

        // 为参考数据值列设置合适宽度
        for (int i = 2; i <= 30; i++) {
            sheet.setColumnWidth(i, 6000);
        }

        // 合并第一行的前20列，并设置居中
        writer.getSheet().addMergedRegion(new CellRangeAddress(0, 0, 0, 19));

        // 写入标题到合并的单元格
        Cell titleCell = writer.getSheet().createRow(0).createCell(0);
        titleCell.setCellValue("导入课程库模板");
        titleCell.setCellStyle(centerStyle);

        // 写入表头（第1行）
        writer.writeCellValue(0, 1, "课程ID*");
        writer.writeCellValue(1, 1, "课程名称*");
        writer.writeCellValue(2, 1, "课程类别");
        writer.writeCellValue(3, 1, "课程属性");
        writer.writeCellValue(4, 1, "课程类型*");
        writer.writeCellValue(5, 1, "课程性质");
        writer.writeCellValue(6, 1, "所属部门*");
        writer.writeCellValue(7, 1, "总课时");
        writer.writeCellValue(8, 1, "周课时");
        writer.writeCellValue(9, 1, "理论课时");
        writer.writeCellValue(10, 1, "实验课时");
        writer.writeCellValue(11, 1, "实践课时");
        writer.writeCellValue(12, 1, "上机课时");
        writer.writeCellValue(13, 1, "其他课时");
        writer.writeCellValue(14, 1, "学分");
        writer.writeCellValue(15, 1, "理论教室类型");
        writer.writeCellValue(16, 1, "实验教室类型");
        writer.writeCellValue(17, 1, "实践教室类型");
        writer.writeCellValue(18, 1, "上机教室类型");
        writer.writeCellValue(19, 1, "是否启用");

        // 创建红色字体样式
        Font redFont = writer.getWorkbook().createFont();
        redFont.setColor(IndexedColors.RED.getIndex());
        redFont.setBold(true);

        CellStyle redWrapStyle = writer.getWorkbook().createCellStyle();
        redWrapStyle.cloneStyleFrom(wrapStyle);
        redWrapStyle.setFont(redFont);

        // 读取注意事项文本并写入Excel
        String noticeText = this.readCourseNoticeFile();
        writer.getSheet().addMergedRegion(new CellRangeAddress(1, 20, 20, 22));

        // 创建注意事项行和单元格（如果尚未创建）
        Row noticeRow = writer.getSheet().getRow(1);
        if (noticeRow == null) {
            noticeRow = writer.getSheet().createRow(1);
        }

        Cell noticeCell = noticeRow.createCell(20);
        noticeCell.setCellValue(noticeText);
        noticeCell.setCellStyle(redWrapStyle);


        // 添加参考数据标题
        writer.writeCellValue(23, 1, "课程类别列表");
        writer.writeCellValue(24, 1, "课程属性列表");
        writer.writeCellValue(25, 1, "课程类型列表");
        writer.writeCellValue(26, 1, "课程性质列表");
        writer.writeCellValue(27, 1, "部门列表");
        writer.writeCellValue(28, 1, "教室类型列表");

        // 添加状态参考
        writer.writeCellValue(29, 1, "状态示例");
        writer.writeCellValue(29, 2, "1 (启用)");
        writer.writeCellValue(29, 3, "0 (禁用)");

        // 填充课程类别列表数据
        if (prepareCourseDTO.getCategoryList() != null) {
            for (int i = 0; i < prepareCourseDTO.getCategoryList().size(); i++) {
                writer.writeCellValue(23, i + 2, prepareCourseDTO.getCategoryList().get(i).getName());
            }
        }

        // 填充课程属性列表数据
        if (prepareCourseDTO.getPropertyList() != null) {
            for (int i = 0; i < prepareCourseDTO.getPropertyList().size(); i++) {
                writer.writeCellValue(24, i + 2, prepareCourseDTO.getPropertyList().get(i).getName());
            }
        }

        // 填充课程类型列表数据
        if (prepareCourseDTO.getTypeList() != null) {
            for (int i = 0; i < prepareCourseDTO.getTypeList().size(); i++) {
                writer.writeCellValue(25, i + 2, prepareCourseDTO.getTypeList().get(i).getName());
            }
        }

        // 填充课程性质列表数据
        if (prepareCourseDTO.getNatureList() != null) {
            for (int i = 0; i < prepareCourseDTO.getNatureList().size(); i++) {
                writer.writeCellValue(26, i + 2, prepareCourseDTO.getNatureList().get(i).getName());
            }
        }

        // 填充部门列表数据
        if (prepareCourseDTO.getDepartmentList() != null) {
            for (int i = 0; i < prepareCourseDTO.getDepartmentList().size(); i++) {
                writer.writeCellValue(27, i + 2, prepareCourseDTO.getDepartmentList().get(i).getName());
            }
        }

        // 填充教室类型列表数据
        if (prepareCourseDTO.getClassroomTypeList() != null) {
            for (int i = 0; i < prepareCourseDTO.getClassroomTypeList().size(); i++) {
                writer.writeCellValue(28, i + 2, prepareCourseDTO.getClassroomTypeList().get(i).getName());
            }
        }

        // 输出到字节流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        writer.close();

        // 返回模板的字节数组
        return outputStream.toByteArray();
    }

    /**
     * 验证课程批量添加的 Excel 文件
     * <p>
     * 此方法验证传入的批量添加课程信息的 VO 对象和文件内容
     * 如果验证成功，则返回解码后的文件字节数组
     *
     * @param batchAddCourseVO 批量添加课程的视图对象，包含文件内容
     * @return 解码后的文件字节数组
     */
    @Override
    public byte[] verifyCourseBatchAndBackFile(BatchAddCourseVO batchAddCourseVO) {
        // 1. 检查 VO 对象是否为空
        if (batchAddCourseVO == null) {
            throw new IllegalArgumentException("批量添加教学楼信息不能为空");
        }

        // 2. 检查 file 字段是否为空
        String base64File = batchAddCourseVO.getFile();
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
     * 将行数据转换为 CourseImportDTO 对象
     * <p>
     * 此方法将一行数据转换为 CourseImportDTO 对象
     * 它会根据列索引设置相应的属性
     *
     * @return 转换后的 CourseImportDTO 对象
     */
    private ImportBaseCourseDTO fetchImportBaseCourseData() {
        List<CourseCategoryDO> categoryList = courseCategoryDAO.getCourseCategoryList();
        List<CoursePropertyDO> propertyList = coursePropertyDAO.getCoursePropertyList();
        List<CourseTypeDO> typeList = courseTypeDAO.getCourseTypeList();
        List<CourseNatureDO> natureList = courseNatureDAO.getCourseNatureList();
        List<DepartmentDO> departmentList = departmentDAO.getDepartmentList();
        List<ClassroomTypeDO> classroomTypeList = classroomTypeDAO.getClassroomTypeList();

        return new ImportBaseCourseDTO(categoryList, propertyList, typeList, natureList, departmentList, classroomTypeList);
    }

    /**
     * 根据名称查找课程类别
     *
     * @param categoryList 课程类别列表
     * @param name         类别名称
     * @return 课程类别DO对象，如果未找到则返回null
     */
    private CourseCategoryDO findCourseCategoryByName(List<CourseCategoryDO> categoryList, String name) {
        return categoryList.stream()
                .filter(category -> category.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称查找课程属性
     *
     * @param propertyList 课程属性列表
     * @param name         属性名称
     * @return 课程属性DO对象，如果未找到则返回null
     */
    private CoursePropertyDO findCoursePropertyByName(List<CoursePropertyDO> propertyList, String name) {
        return propertyList.stream()
                .filter(property -> property.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称查找课程类型
     *
     * @param typeList 课程类型列表
     * @param name     类型名称
     * @return 课程类型DO对象，如果未找到则返回null
     */
    private CourseTypeDO findCourseTypeByName(List<CourseTypeDO> typeList, String name) {
        return typeList.stream()
                .filter(type -> type.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称查找课程性质
     *
     * @param natureList 课程性质列表
     * @param name       性质名称
     * @return 课程性质DO对象，如果未找到则返回null
     */
    private CourseNatureDO findCourseNatureByName(List<CourseNatureDO> natureList, String name) {
        return natureList.stream()
                .filter(nature -> nature.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称查找部门
     *
     * @param departmentList 部门列表
     * @param name           部门名称
     * @return 部门DO对象，如果未找到则返回null
     */
    private DepartmentDO findDepartmentByName(List<DepartmentDO> departmentList, String name) {
        return departmentList.stream()
                .filter(department -> department.getDepartmentName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据名称查找教室类型
     *
     * @param classroomTypeList 教室类型列表
     * @param name              教室类型名称
     * @return 教室类型DO对象，如果未找到则返回null
     */
    private ClassroomTypeDO findClassroomTypeByName(List<ClassroomTypeDO> classroomTypeList, String name) {
        return classroomTypeList.stream()
                .filter(classroomType -> classroomType.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * 验证课程信息
     * <p>
     * 此方法验证导入的课程信息，包括课程ID、名称、类型、部门、类别、属性、性质等是否合法
     * 如果验证失败，则抛出业务异常
     *
     * @param courseList          课程列表
     * @param importBaseCourseDTO 导入基础课程数据
     * @param i                   当前处理的课程索引
     * @return 验证课程返回DTO，包含课程验证后的数据
     * @throws BusinessException 如果验证失败
     */
    private ValidateCourseReturnDTO validateCourse(
            List<CourseImportDTO> courseList,
            ImportBaseCourseDTO importBaseCourseDTO,
            int i
    ) throws BusinessException {
        CourseImportDTO course = courseList.get(i);
        // 行号（假设从第3行开始）
        int rowNumber = i + 3;

        // 验证课程ID
        if (course.getId() == null || course.getId().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行课程ID不能为空", ErrorCode.BODY_ERROR);
        }

        // 验证课程名称
        if (course.getName() == null || course.getName().isEmpty()) {
            throw new BusinessException("第" + rowNumber + "行课程名称不能为空", ErrorCode.BODY_ERROR);
        }

        // 验证总学时与各类课时的关系
        if (course.getTotalHours() != null) {
            BigDecimal calculatedTotal = BigDecimal.ZERO;
            if (course.getTheoryHours() != null) calculatedTotal = calculatedTotal.add(course.getTheoryHours());
            if (course.getExperimentHours() != null) calculatedTotal = calculatedTotal.add(course.getExperimentHours());
            if (course.getPracticeHours() != null) calculatedTotal = calculatedTotal.add(course.getPracticeHours());
            if (course.getComputerHours() != null) calculatedTotal = calculatedTotal.add(course.getComputerHours());
            if (course.getOtherHours() != null) calculatedTotal = calculatedTotal.add(course.getOtherHours());

            // 比较计算得出的总学时和输入的总学时是否相等
            if (calculatedTotal.compareTo(course.getTotalHours()) != 0) {
                throw new BusinessException("第" + rowNumber + "行总学时应等于各类课时之和：理论课时+实验课时+实践课时+上机课时+其他课时", ErrorCode.BODY_ERROR);
            }
        }

        // 创建返回对象
        ValidateCourseReturnDTO validateCourseReturnDTO = new ValidateCourseReturnDTO();

        // 验证课程类型（必填）
        CourseTypeDO typeDO = findCourseTypeByName(importBaseCourseDTO.getTypeList(), course.getType());
        if (typeDO == null) {
            throw new BusinessException("第" + rowNumber + "行课程类型不存在", ErrorCode.BODY_ERROR);
        }
        validateCourseReturnDTO.setTypeDO(typeDO);

        // 验证部门（必填）
        DepartmentDO departmentDO = findDepartmentByName(importBaseCourseDTO.getDepartmentList(), course.getDepartment());
        if (departmentDO == null) {
            throw new BusinessException("第" + rowNumber + "行所属部门不存在", ErrorCode.BODY_ERROR);
        }
        validateCourseReturnDTO.setDepartmentDO(departmentDO);

        // 验证课程类别（可选）
        if (course.getCategory() != null && !course.getCategory().isEmpty()) {
            CourseCategoryDO categoryDO = findCourseCategoryByName(importBaseCourseDTO.getCategoryList(), course.getCategory());
            if (categoryDO == null) {
                throw new BusinessException("第" + rowNumber + "行课程类别不存在", ErrorCode.BODY_ERROR);
            }
            validateCourseReturnDTO.setCategoryDO(categoryDO);
        }

        // 验证课程属性（可选）
        if (course.getProperty() != null && !course.getProperty().isEmpty()) {
            CoursePropertyDO propertyDO = findCoursePropertyByName(importBaseCourseDTO.getPropertyList(), course.getProperty());
            if (propertyDO == null) {
                throw new BusinessException("第" + rowNumber + "行课程属性不存在", ErrorCode.BODY_ERROR);
            }
            validateCourseReturnDTO.setPropertyDO(propertyDO);
        }

        // 验证课程性质（可选）
        if (course.getNature() != null && !course.getNature().isEmpty()) {
            CourseNatureDO natureDO = findCourseNatureByName(importBaseCourseDTO.getNatureList(), course.getNature());
            if (natureDO == null) {
                throw new BusinessException("第" + rowNumber + "行课程性质不存在", ErrorCode.BODY_ERROR);
            }
            validateCourseReturnDTO.setNatureDO(natureDO);
        }

        // 验证理论教室类型（可选）
        if (course.getTheoryClassroomType() != null && !course.getTheoryClassroomType().isEmpty()) {
            ClassroomTypeDO theoryClassroomTypeDO = findClassroomTypeByName(
                    importBaseCourseDTO.getClassroomTypeList(),
                    course.getTheoryClassroomType()
            );
            if (theoryClassroomTypeDO == null) {
                throw new BusinessException("第" + rowNumber + "行理论教室类型不存在", ErrorCode.BODY_ERROR);
            }
            validateCourseReturnDTO.setTheoryClassroomTypeDO(theoryClassroomTypeDO);
        }

        // 验证实验教室类型（可选）
        if (course.getExperimentClassroomType() != null && !course.getExperimentClassroomType().isEmpty()) {
            ClassroomTypeDO experimentClassroomTypeDO = findClassroomTypeByName(
                    importBaseCourseDTO.getClassroomTypeList(),
                    course.getExperimentClassroomType()
            );
            if (experimentClassroomTypeDO == null) {
                throw new BusinessException("第" + rowNumber + "行实验教室类型不存在", ErrorCode.BODY_ERROR);
            }
            validateCourseReturnDTO.setExperimentClassroomTypeDO(experimentClassroomTypeDO);
        }

        // 验证实践教室类型（可选）
        if (course.getPracticeClassroomType() != null && !course.getPracticeClassroomType().isEmpty()) {
            ClassroomTypeDO practiceClassroomTypeDO = findClassroomTypeByName(
                    importBaseCourseDTO.getClassroomTypeList(),
                    course.getPracticeClassroomType()
            );
            if (practiceClassroomTypeDO == null) {
                throw new BusinessException("第" + rowNumber + "行实践教室类型不存在", ErrorCode.BODY_ERROR);
            }
            validateCourseReturnDTO.setPracticeClassroomTypeDO(practiceClassroomTypeDO);
        }

        // 验证上机教室类型（可选）
        if (course.getComputerClassroomType() != null && !course.getComputerClassroomType().isEmpty()) {
            ClassroomTypeDO computerClassroomTypeDO = findClassroomTypeByName(
                    importBaseCourseDTO.getClassroomTypeList(),
                    course.getComputerClassroomType()
            );
            if (computerClassroomTypeDO == null) {
                throw new BusinessException("第" + rowNumber + "行上机教室类型不存在", ErrorCode.BODY_ERROR);
            }
            validateCourseReturnDTO.setComputerClassroomTypeDO(computerClassroomTypeDO);
        }

        return validateCourseReturnDTO;
    }

    /**
     * 批量导入课程，忽略错误
     * <p>
     * 此方法在导入过程中，如果遇到错误会继续处理其他记录，并将错误信息记录下来
     *
     * @param file 包含课程信息的Excel文件字节数组
     * @return 批量添加课程的结果DTO
     */
    @Override
    @Transactional(rollbackFor = BusinessException.class)
    public BackAddCourseDTO batchImportIgnoreError(byte[] file) {
        // 解析Excel文件
        List<CourseImportDTO> courseList = parseExcelToCourseList(file, 2, 19);
        BackAddCourseDTO backAddCourseDTO = new BackAddCourseDTO();

        // 获取导入课程库的基础数据
        ImportBaseCourseDTO importBaseCourseDTO = fetchImportBaseCourseData();

        // 创建失败详情列表
        List<BackAddCourseDTO.FailedDetail> failedDetails = new ArrayList<>();
        int successCount = 0;

        // 循环处理每条课程记录
        for (int i = 0; i < courseList.size(); i++) {
            try {
                // 验证课程信息
                ValidateCourseReturnDTO validateCourseReturnDTO = validateCourse(courseList, importBaseCourseDTO, i);
                CourseImportDTO course = courseList.get(i);

                // 直接创建课程库对象
                CourseLibraryDO courseLibraryDO = new CourseLibraryDO();

                // 设置基本信息
                courseLibraryDO.setId(course.getId());
                courseLibraryDO.setName(course.getName());
                courseLibraryDO.setIsEnabled(course.getIsEnabled());

                // 设置各类时间字段
                courseLibraryDO.setTotalHours(course.getTotalHours());
                courseLibraryDO.setWeekHours(course.getWeekHours());
                courseLibraryDO.setTheoryHours(course.getTheoryHours());
                courseLibraryDO.setExperimentHours(course.getExperimentHours());
                courseLibraryDO.setPracticeHours(course.getPracticeHours());
                courseLibraryDO.setComputerHours(course.getComputerHours());
                courseLibraryDO.setOtherHours(course.getOtherHours());
                courseLibraryDO.setCredit(course.getCredit());

                // 设置类型和部门（必填）
                courseLibraryDO.setType(validateCourseReturnDTO.getTypeDO().getCourseTypeUuid());
                courseLibraryDO.setDepartment(validateCourseReturnDTO.getDepartmentDO().getDepartmentUuid());

                // 设置类别（可选）
                if (validateCourseReturnDTO.getCategoryDO() != null) {
                    courseLibraryDO.setCategory(validateCourseReturnDTO.getCategoryDO().getCourseCategoryUuid());
                }

                // 设置属性（可选）
                if (validateCourseReturnDTO.getPropertyDO() != null) {
                    courseLibraryDO.setProperty(validateCourseReturnDTO.getPropertyDO().getCoursePropertyUuid());
                }

                // 设置性质（可选）
                if (validateCourseReturnDTO.getNatureDO() != null) {
                    courseLibraryDO.setNature(validateCourseReturnDTO.getNatureDO().getCourseNatureUuid());
                }

                // 设置教室类型（可选）
                if (validateCourseReturnDTO.getTheoryClassroomTypeDO() != null) {
                    courseLibraryDO.setTheoryClassroomType(validateCourseReturnDTO.getTheoryClassroomTypeDO().getClassTypeUuid());
                }

                if (validateCourseReturnDTO.getExperimentClassroomTypeDO() != null) {
                    courseLibraryDO.setExperimentClassroomType(validateCourseReturnDTO.getExperimentClassroomTypeDO().getClassTypeUuid());
                }

                if (validateCourseReturnDTO.getPracticeClassroomTypeDO() != null) {
                    courseLibraryDO.setPracticeClassroomType(validateCourseReturnDTO.getPracticeClassroomTypeDO().getClassTypeUuid());
                }

                if (validateCourseReturnDTO.getComputerClassroomTypeDO() != null) {
                    courseLibraryDO.setComputerClassroomType(validateCourseReturnDTO.getComputerClassroomTypeDO().getClassTypeUuid());
                }

                // 使用DAO层方法保存课程库对象并处理可能的错误
                List<BackAddCourseDTO.FailedDetail> saveFailedDetails = courseLibraryDAO.saveCourseLibraryIgnoreError(courseLibraryDO, i);
                if (saveFailedDetails.isEmpty()) {
                successCount++;
                } else {
                    failedDetails.addAll(saveFailedDetails);
                }
            } catch (RuntimeException e) {
                // 记录失败信息
                BackAddCourseDTO.FailedDetail failedDetail = getFailedDetail(e, i);
                failedDetails.add(failedDetail);
            } catch (Exception e) {
                // 处理其他类型的异常
                BackAddCourseDTO.FailedDetail failedDetail = new BackAddCourseDTO.FailedDetail();
                failedDetail.setRow(i + 3);
                failedDetail.setReason("未知错误：" + e.getMessage());
                failedDetails.add(failedDetail);
            }
        }

        // 设置统计结果
        backAddCourseDTO.setTotalCount(courseList.size())
                .setSuccessCount(successCount)
                .setFailedCount(failedDetails.size())
                .setFailedDetails(failedDetails.isEmpty() ? null : failedDetails);

        return backAddCourseDTO;
    }

    /**
     * 批量导入课程，不忽略错误
     * <p>
     * 此方法在导入过程中，如果遇到错误会立即停止，并抛出异常
     *
     * @param file 包含课程信息的Excel文件字节数组
     * @return 批量添加课程的结果DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BackAddCourseDTO batchImportNoIgnoreError(byte[] file) {
        // 解析Excel文件
        List<CourseImportDTO> courseList = parseExcelToCourseList(file, 2, 19);
        BackAddCourseDTO backAddCourseDTO = new BackAddCourseDTO();

        // 获取导入课程库的基础数据
        ImportBaseCourseDTO importBaseCourseDTO = fetchImportBaseCourseData();

        // 不忽略错误提醒，遇到错误直接抛出异常
        for (int i = 0; i < courseList.size(); i++) {
            // 验证课程信息
            ValidateCourseReturnDTO validateCourseReturnDTO = validateCourse(courseList, importBaseCourseDTO, i);
            CourseImportDTO course = courseList.get(i);

            // 直接创建课程库对象
            CourseLibraryDO courseLibraryDO = new CourseLibraryDO();

            // 设置基本信息
            courseLibraryDO.setId(course.getId());
            courseLibraryDO.setName(course.getName());
            courseLibraryDO.setIsEnabled(course.getIsEnabled());

            // 设置各类时间字段
            courseLibraryDO.setTotalHours(course.getTotalHours());
            courseLibraryDO.setWeekHours(course.getWeekHours());
            courseLibraryDO.setTheoryHours(course.getTheoryHours());
            courseLibraryDO.setExperimentHours(course.getExperimentHours());
            courseLibraryDO.setPracticeHours(course.getPracticeHours());
            courseLibraryDO.setComputerHours(course.getComputerHours());
            courseLibraryDO.setOtherHours(course.getOtherHours());
            courseLibraryDO.setCredit(course.getCredit());

            // 设置类型和部门（必填）
            courseLibraryDO.setType(validateCourseReturnDTO.getTypeDO().getCourseTypeUuid());
            courseLibraryDO.setDepartment(validateCourseReturnDTO.getDepartmentDO().getDepartmentUuid());

            // 设置类别（可选）
            if (validateCourseReturnDTO.getCategoryDO() != null) {
                courseLibraryDO.setCategory(validateCourseReturnDTO.getCategoryDO().getCourseCategoryUuid());
            }

            // 设置属性（可选）
            if (validateCourseReturnDTO.getPropertyDO() != null) {
                courseLibraryDO.setProperty(validateCourseReturnDTO.getPropertyDO().getCoursePropertyUuid());
            }

            // 设置性质（可选）
            if (validateCourseReturnDTO.getNatureDO() != null) {
                courseLibraryDO.setNature(validateCourseReturnDTO.getNatureDO().getCourseNatureUuid());
            }

            // 设置教室类型（可选）
            if (validateCourseReturnDTO.getTheoryClassroomTypeDO() != null) {
                courseLibraryDO.setTheoryClassroomType(validateCourseReturnDTO.getTheoryClassroomTypeDO().getClassTypeUuid());
            }

            if (validateCourseReturnDTO.getExperimentClassroomTypeDO() != null) {
                courseLibraryDO.setExperimentClassroomType(validateCourseReturnDTO.getExperimentClassroomTypeDO().getClassTypeUuid());
            }

            if (validateCourseReturnDTO.getPracticeClassroomTypeDO() != null) {
                courseLibraryDO.setPracticeClassroomType(validateCourseReturnDTO.getPracticeClassroomTypeDO().getClassTypeUuid());
            }

            if (validateCourseReturnDTO.getComputerClassroomTypeDO() != null) {
                courseLibraryDO.setComputerClassroomType(validateCourseReturnDTO.getComputerClassroomTypeDO().getClassTypeUuid());
            }

            // 使用DAO层方法保存课程库对象，有错误直接抛出异常
            courseLibraryDAO.saveCourseLibraryBackError(courseLibraryDO, i);
        }

        // 设置统计结果（全部成功）
        backAddCourseDTO.setTotalCount(courseList.size())
                .setSuccessCount(courseList.size())
                .setFailedCount(0);

        return backAddCourseDTO;
    }

    /**
     * 设置课程信息到CourseLibraryAndClassDTO对象中
     *
     * @param courseLibraryAndTeacherCourseQualificationListDTO 课程库和班级DTO对象，用于存储课程信息
     * @param courseMap                                         包含课程ID和课程库对象的映射，用于查找特定课程
     * @param specificCourseIdVO                                包含特定课程ID的VO对象，用于指定需要查找的课程
     *                                                          本方法首先根据特定课程ID从课程映射中获取课程库对象如果未找到对应的课程，
     *                                                          则抛出业务异常表示未找到匹配的课程如果找到了课程，则将其转换为课程库DTO对象
     *                                                          并设置到CourseLibraryAndClassDTO对象中
     */
    private void setCourse(
            CourseLibraryAndTeacherCourseQualificationListDTO courseLibraryAndTeacherCourseQualificationListDTO,
            @org.jetbrains.annotations.NotNull Map<String, CourseLibraryDO> courseMap,
            @org.jetbrains.annotations.NotNull SpecificCourseIdVO specificCourseIdVO) {
        // 根据特定课程ID从课程映射中获取课程库对象
        CourseLibraryDO courseLibraryDO = courseMap.get(specificCourseIdVO.getCourseId());
        // 如果未找到对应的课程，则抛出业务异常
        if (courseLibraryDO == null) {
            throw new BusinessException("未找到与 courseId 匹配的课程： " + specificCourseIdVO.getCourseId(), ErrorCode.BODY_ERROR);
        }
        // 将找到的课程库对象转换为课程库DTO对象并设置到CourseLibraryAndClassDTO对象中
        courseLibraryAndTeacherCourseQualificationListDTO.setCourse(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
    }


    /**
     * 计算选课学生的总人数
     *
     * @param courselibraryandclassdto 课程库和班级信息的DTO对象，用于存储班级DTO列表和总学生数
     * @param classMap                 班级ID与班级信息的映射，用于快速获取班级信息
     * @param specificCourseIdVO       包含特定课程ID信息的对象，用于指定需要计算学生数的班级ID列表
     */
    private void calculateStudentCount(
            CourseLibraryAndTeacherCourseQualificationListDTO courselibraryandclassdto,
            Map<String, AdministrativeClassDO> classMap, @org.jetbrains.annotations.NotNull SpecificCourseIdVO specificCourseIdVO) {
        log.debug("计算学生数:");
        // 初始化总学生数为0
        int totalStudentCount = 0;
        // 初始化班级列表
        if (courselibraryandclassdto.getClassList() == null) {
            courselibraryandclassdto.setClassList(new ArrayList<>());
        }
        // 获取班级ID列表
        List<String> classIds = specificCourseIdVO.getClassId();
        // 使用for循环遍历String类型的班级ID
        for (String classId : classIds) {
            // 从班级映射中获取当前班级信息
            AdministrativeClassDO administrativeClassDO = classMap.get(classId);
            // 如果找到了对应的班级信息
            if (administrativeClassDO != null) {
                // 将班级信息转换为DTO对象
                AdministrativeClassDTO classDTO = BeanUtil.toBean(administrativeClassDO, AdministrativeClassDTO.class);
                // 将转换后的班级DTO添加到课程库和班级信息DTO的班级DTO列表中
                courselibraryandclassdto.getClassList().add(classDTO);
                // 累加当前班级的学生数到总学生数中
                totalStudentCount += administrativeClassDO.getStudentCount();
            }
        }
        // 将计算得到的总学生数设置到课程库和班级信息DTO中
        courselibraryandclassdto.setNumber(totalStudentCount);
    }
    /**
     * 根据部门UUID、特定课程ID列表和排除课程ID列表获取课程库列表
     * 如果查询结果为空，则抛出业务异常
     *
     * @param departmentUuid    部门UUID，用于查询课程库
     * @param specificCourseIds 特定课程ID列表，用于过滤课程库
     * @return 课程库列表，如果列表为空则抛出异常
     * @throws BusinessException 当课程库列表为空时抛出的业务异常
     */
    @Override
    public List<CourseLibraryDTO> listCourseLibraryByDepartmentAndSpecifyWithThrow(
            @NotBlank String departmentUuid, List<String> specificCourseIds) {
        // 调用DAO层方法获取课程库列表
        List<CourseLibraryDO> listCourseLibraryByDepartmentAndSpecify = courseLibraryDAO.getListCourseLibraryByDepartmentAndSpecify(
                departmentUuid, specificCourseIds);
        // 检查获取的课程库列表是否为空，如果为空则抛出业务异常
        if (listCourseLibraryByDepartmentAndSpecify != null && listCourseLibraryByDepartmentAndSpecify.isEmpty()) {
            throw new BusinessException("课程库列表为空", ErrorCode.BODY_ERROR);
        }
        // 将获取的课程库列表转换为DTO对象
        List<CourseLibraryDTO> courseLibraryDTOList = new ArrayList<>();
        if (listCourseLibraryByDepartmentAndSpecify != null) {
            for (CourseLibraryDO courseLibraryDO : listCourseLibraryByDepartmentAndSpecify) {
                courseLibraryDTOList.add(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
            }
        }
        // 返回获取的课程库列表
        return courseLibraryDTOList;
    }


    /**
     * 获取特定课程的列表和班级信息DTO
     * 该方法用于根据特定课程ID列表获取相应的课程和班级信息，并计算学生人数
     *
     * @param specificCourseIds 包含特定课程ID的列表，用于查询课程和班级信息，必须不为空
     * @return 返回一个包含课程库和班级信息的DTO列表
     */
    @Override
    public List<CourseLibraryAndTeacherCourseQualificationListDTO> getCourseListAndClassDTO(
            @org.jetbrains.annotations.NotNull List<SpecificCourseIdVO> specificCourseIds, String departmentUuid) {
        List<CourseLibraryAndTeacherCourseQualificationListDTO> lists = new ArrayList<>();
        // 获取所有课程并构建映射
        Map<String, CourseLibraryDO> courseMap = courseLibraryDAO.getCourseListByDepart(departmentUuid).stream()
                .collect(Collectors.toMap(CourseLibraryDO::getCourseLibraryUuid, course -> course));
        // 获取所有班级并构建映射
        Map<String, AdministrativeClassDO> classMap = administrativeClassDAO.getAdministrativeClassListByDepartment(departmentUuid)
                .stream()
                .collect(Collectors.toMap(AdministrativeClassDO::getAdministrativeClassUuid, clazz -> clazz));
        // 遍历特定课程ID列表，为每个课程构建课程库和班级信息DTO
        for (SpecificCourseIdVO specificCourseIdVO : specificCourseIds) {
            CourseLibraryAndTeacherCourseQualificationListDTO libraryAndClassDTO =
                    new CourseLibraryAndTeacherCourseQualificationListDTO();
            // 设置课程
            setCourse(libraryAndClassDTO, courseMap, specificCourseIdVO);
            log.debug("设置人数:{}", specificCourseIdVO.getNumber());
            // 计算学生人数（如果外部没有提供）
            if (specificCourseIdVO.getNumber() == null) {
                // 计算学生数，同时添加班级信息
                calculateStudentCount(libraryAndClassDTO, classMap, specificCourseIdVO);
            } else {
                libraryAndClassDTO.setNumber(specificCourseIdVO.getNumber());
            }
            //设置其他信息
            libraryAndClassDTO.setIsOddWeek(specificCourseIdVO.getIsOddWeek())
                    .setCourseEnuType(specificCourseIdVO.getCourseEnuType())
                    .setWeeklyHours(specificCourseIdVO.getWeeklyHours())
                    .setStartWeek(specificCourseIdVO.getStartWeek())
                    .setEndWeek(specificCourseIdVO.getEndWeek());
            log.debug("设置后人数为:{}", libraryAndClassDTO.getNumber());
            lists.add(libraryAndClassDTO);
        }
        return lists;
    }

    /**
     * 根据课程UUID获取课程信息
     * 此方法用于通过课程的唯一标识符（UUID）来检索课程信息它首先调用课程库DAO中的方法来获取课程对象如果未找到对应的课程，
     * 则抛出一个商业异常，指示课程不存在这样做的目的是确保当请求特定课程时，能够提供明确的错误信息而不是返回null，
     * 从而提高系统的健壮性和用户体验
     *
     * @param courseUuid 课程的唯一标识符（UUID）
     * @return 返回找到的CourseLibraryDO对象
     * @throws BusinessException 如果课程不存在，则抛出此异常
     */
    @Override
    public @org.jetbrains.annotations.NotNull CourseLibraryDTO getCourseByUuid(String courseUuid) {
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.getCourseByUuid(courseUuid);
        if (courseLibraryDO == null) {
            throw new BusinessException("课程不存在", ErrorCode.NOT_EXIST);
        }
        return BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class);
    }
}
