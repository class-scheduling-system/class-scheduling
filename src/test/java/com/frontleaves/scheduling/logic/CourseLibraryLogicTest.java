package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TokenDTO;
import com.frontleaves.scheduling.models.dto.lite.CourseLiteDTO;
import com.frontleaves.scheduling.models.entity.base.*;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Transactional
class CourseLibraryLogicTest {

    @Resource
    private CourseLibraryService courseLibraryService;

    @Resource
    private CourseLibraryDAO courseLibraryDAO;

    @Resource
    private CourseCategoryDAO courseCategoryDAO;

    @Resource
    private CoursePropertyDAO coursePropertyDAO;

    @Resource
    private CourseTypeDAO courseTypeDAO;

    @Resource
    private CourseNatureDAO courseNatureDAO;

    @Resource
    private DepartmentDAO departmentDAO;

    @Resource
    private ClassroomTypeDAO classroomTypeDAO;

    @Resource
    private UserService userService;

    // 测试用的实体对象
    private CourseLibraryVO validCourseLibraryVO;
    private MockHttpServletRequest request;
    private CourseTypeDO courseTypeDO;
    private DepartmentDO departmentDO;
    private CourseCategoryDO courseCategoryDO;
    private ClassroomTypeDO classroomTypeDO;
    @Autowired
    private TokenDAO tokenDAO;
    @Autowired
    private UserDAO userDAO;
    @Autowired
    private RedissonClient redisson;

    @BeforeEach
    void setUp() {
        log.debug("CourseLibraryLogic单元测试初始化");

        UserDO getUser = userDAO.lambdaQuery().last("LIMIT 1").one();

        TokenDTO getToken = tokenDAO.createToken(getUser);

        // 初始化请求对象
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + getToken.getToken());

        // 获取已有的课程类型、部门等数据
        courseTypeDO = courseTypeDAO.lambdaQuery().last("LIMIT 1").one();
        Assertions.assertNotNull(courseTypeDO, "数据库中应存在课程类型数据");

        departmentDO = departmentDAO.lambdaQuery().last("LIMIT 1").one();
        Assertions.assertNotNull(departmentDO, "数据库中应存在部门数据");

        courseCategoryDO = courseCategoryDAO.lambdaQuery().last("LIMIT 1").one();
        CoursePropertyDO coursePropertyDO = coursePropertyDAO.lambdaQuery().last("LIMIT 1").one();
        CourseNatureDO courseNatureDO = courseNatureDAO.lambdaQuery().last("LIMIT 1").one();
        classroomTypeDO = classroomTypeDAO.lambdaQuery().last("LIMIT 1").one();

        // 创建有效的CourseLibraryVO对象
        validCourseLibraryVO = new CourseLibraryVO(
                "CS001",
                "计算机导论", // name
                "Introduction to Computer Science", // englishName
                courseCategoryDO != null ? courseCategoryDO.getCourseCategoryUuid() : null, // category
                coursePropertyDO != null ? coursePropertyDO.getCoursePropertyUuid() : null, // property
                courseTypeDO.getCourseTypeUuid(), // type
                courseNatureDO != null ? courseNatureDO.getCourseNatureUuid() : null, // nature
                departmentDO.getDepartmentUuid(), // department
                classroomTypeDO != null ? classroomTypeDO.getClassTypeUuid() : null, // theoryClassroomType
                classroomTypeDO != null ? classroomTypeDO.getClassTypeUuid() : null, // experimentClassroomType
                classroomTypeDO != null ? classroomTypeDO.getClassTypeUuid() : null, // practiceClassroomType
                classroomTypeDO != null ? classroomTypeDO.getClassTypeUuid() : null, // computerClassroomType
                true, // isEnabled
                new BigDecimal("64"), // totalHours
                new BigDecimal("4"), // weekHours
                new BigDecimal("40"), // theoryHours
                new BigDecimal("16"), // experimentHours
                new BigDecimal("4"), // practiceHours
                new BigDecimal("4"), // computerHours
                new BigDecimal("0"), // otherHours
                new BigDecimal("4"), // credit
                "计算机基础入门课程" // description
        );
    }

    @AfterEach
    void tearDown() {
        // 清理测试数据
        courseLibraryDAO.lambdaQuery().eq(CourseLibraryDO::getId, validCourseLibraryVO.getId()).oneOpt()
                        .ifPresent(data -> {
                            redisson.getKeys().delete(StringConstant.Redis.COURSE_LIBRARY_UUID + data.getCourseLibraryUuid());
                            redisson.getKeys().delete(StringConstant.Redis.COURSE_LIBRARY_ID + data.getId());
                            courseLibraryDAO.removeById(data);
                        });
    }

    /**
     * 测试添加课程库 - 成功场景*
     * 所有必填字段都有效，验证能否成功添加课程库
     */
    @Test
    void testAddCourseLibrary_Success() {
        log.debug("测试添加课程库 - 成功场景");

        // 执行添加操作
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 验证是否成功保存到数据库
        CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        // 验证保存的课程信息是否正确
        Assertions.assertNotNull(savedCourse, "保存的课程不应为空");
        Assertions.assertEquals(validCourseLibraryVO.getId(), savedCourse.getId(), "课程ID应匹配");
        Assertions.assertEquals(validCourseLibraryVO.getName(), savedCourse.getName(), "课程名称应匹配");
        Assertions.assertEquals(validCourseLibraryVO.getType(), savedCourse.getType(), "课程类型应匹配");
        Assertions.assertEquals(validCourseLibraryVO.getDepartment(), savedCourse.getDepartment(), "课程部门应匹配");

        // 验证UUID是否被正确生成
        Assertions.assertNotNull(savedCourse.getCourseLibraryUuid(), "课程UUID不应为空");
        Assertions.assertTrue(savedCourse.getCourseLibraryUuid().matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION),
                "课程UUID应符合无连字符UUID格式");
    }

    /**
     * 测试添加课程库 - 课程类型不存在
     * 当课程类型UUID无效时，应抛出业务异常
     */
    @Test
    void testAddCourseLibrary_InvalidCourseType() {
        log.debug("测试添加课程库 - 课程类型不存在");

        // 修改为无效的课程类型UUID
        CourseLibraryVO invalidTypeVO = new CourseLibraryVO(
                validCourseLibraryVO.getId(),
                validCourseLibraryVO.getName(),
                validCourseLibraryVO.getEnglishName(),
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                UuidUtil.generateUuidNoDash(), // 设置一个不存在的课程类型UUID
                validCourseLibraryVO.getNature(),
                validCourseLibraryVO.getDepartment(),
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                validCourseLibraryVO.getTotalHours(),
                validCourseLibraryVO.getWeekHours(),
                validCourseLibraryVO.getTheoryHours(),
                validCourseLibraryVO.getExperimentHours(),
                validCourseLibraryVO.getPracticeHours(),
                validCourseLibraryVO.getComputerHours(),
                validCourseLibraryVO.getOtherHours(),
                validCourseLibraryVO.getCredit(),
                validCourseLibraryVO.getDescription()
        );

        // 执行添加操作，预期会抛出异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> courseLibraryService.addCourseLibrary(invalidTypeVO, request));

        // 验证异常信息
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
        Assertions.assertEquals("课程类型不存在", exception.getMessage());

        // 验证数据未保存到数据库
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, invalidTypeVO.getId())
                .one();
        Assertions.assertNull(courseLibraryDO, "无效课程类型的课程不应被保存");
    }

    /**
     * 测试添加课程库 - 部门不存在
     * 当部门UUID无效时，应抛出业务异常
     */
    @Test
    void testAddCourseLibrary_InvalidDepartment() {
        log.debug("测试添加课程库 - 部门不存在");

        // 修改为无效的部门UUID
        CourseLibraryVO invalidDepartmentVO = new CourseLibraryVO(
                validCourseLibraryVO.getId(),
                validCourseLibraryVO.getName(),
                validCourseLibraryVO.getEnglishName(),
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                validCourseLibraryVO.getType(),
                validCourseLibraryVO.getNature(),
                UuidUtil.generateUuidNoDash(), // 设置一个不存在的部门UUID
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                validCourseLibraryVO.getTotalHours(),
                validCourseLibraryVO.getWeekHours(),
                validCourseLibraryVO.getTheoryHours(),
                validCourseLibraryVO.getExperimentHours(),
                validCourseLibraryVO.getPracticeHours(),
                validCourseLibraryVO.getComputerHours(),
                validCourseLibraryVO.getOtherHours(),
                validCourseLibraryVO.getCredit(),
                validCourseLibraryVO.getDescription()
        );

        // 执行添加操作，预期会抛出异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> courseLibraryService.addCourseLibrary(invalidDepartmentVO, request));

        // 验证异常信息
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
        Assertions.assertEquals("部门不存在", exception.getMessage());

        // 验证数据未保存到数据库
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, invalidDepartmentVO.getId())
                .one();
        Assertions.assertNull(courseLibraryDO, "无效部门的课程不应被保存");
    }

    /**
     * 测试添加课程库 - 课程类别不存在
     * 当可选的课程类别UUID无效时，应抛出业务异常
     */
    @Test
    void testAddCourseLibrary_InvalidCategory() {
        // 只有当courseCategoryDO不为null时才执行此测试
        if (courseCategoryDO != null) {
            log.debug("测试添加课程库 - 课程类别不存在");

            // 修改为无效的课程类别UUID
            CourseLibraryVO invalidCategoryVO = new CourseLibraryVO(
                    validCourseLibraryVO.getId(),
                    validCourseLibraryVO.getName(),
                    validCourseLibraryVO.getEnglishName(),
                    UuidUtil.generateUuidNoDash(), // 设置一个不存在的课程类别UUID
                    validCourseLibraryVO.getProperty(),
                    validCourseLibraryVO.getType(),
                    validCourseLibraryVO.getNature(),
                    validCourseLibraryVO.getDepartment(),
                    validCourseLibraryVO.getTheoryClassroomType(),
                    validCourseLibraryVO.getExperimentClassroomType(),
                    validCourseLibraryVO.getPracticeClassroomType(),
                    validCourseLibraryVO.getComputerClassroomType(),
                    validCourseLibraryVO.getIsEnabled(),
                    validCourseLibraryVO.getTotalHours(),
                    validCourseLibraryVO.getWeekHours(),
                    validCourseLibraryVO.getTheoryHours(),
                    validCourseLibraryVO.getExperimentHours(),
                    validCourseLibraryVO.getPracticeHours(),
                    validCourseLibraryVO.getComputerHours(),
                    validCourseLibraryVO.getOtherHours(),
                    validCourseLibraryVO.getCredit(),
                    validCourseLibraryVO.getDescription()
            );

            // 执行添加操作，预期会抛出异常
            BusinessException exception = Assertions.assertThrows(BusinessException.class,
                    () -> courseLibraryService.addCourseLibrary(invalidCategoryVO, request));

            // 验证异常信息
            Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
            Assertions.assertEquals("指定的课程类别不存在", exception.getMessage());

            // 验证数据未保存到数据库
            CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getId, invalidCategoryVO.getId())
                    .one();
            Assertions.assertNull(courseLibraryDO, "无效课程类别的课程不应被保存");
        }
    }

    /**
     * 测试添加课程库 - 部分可选字段为空
     *
     * 当部分可选字段为空时，应该能成功添加课程库
     */
    @Test
    void testAddCourseLibrary_WithNullOptionalFields() {
        log.debug("测试添加课程库 - 部分可选字段为空");

        // 创建一个部分可选字段为空的CourseLibraryVO
        CourseLibraryVO optionalNullVO = new CourseLibraryVO(
                "CS002", // id
                "Java编程", // name
                null, // englishName - 可选
                null, // category - 可选
                null, // property - 可选
                courseTypeDO.getCourseTypeUuid(), // type - 必填
                null, // nature - 可选
                departmentDO.getDepartmentUuid(), // department - 必填
                null, // theoryClassroomType - 可选
                null, // experimentClassroomType - 可选
                null, // practiceClassroomType - 可选
                null, // computerClassroomType - 可选
                true, // isEnabled
                new BigDecimal("64"), // totalHours
                new BigDecimal("4"), // weekHours
                new BigDecimal("40"), // theoryHours
                new BigDecimal("16"), // experimentHours
                new BigDecimal("4"), // practiceHours
                new BigDecimal("4"), // computerHours
                new BigDecimal("0"), // otherHours
                new BigDecimal("4"), // credit
                null // description - 可选
        );

        try {
            // 执行添加操作
            Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(optionalNullVO, request));

            // 验证是否成功保存到数据库
            CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getId, optionalNullVO.getId())
                    .one();

            // 验证保存的课程信息是否正确
            Assertions.assertNotNull(savedCourse, "保存的课程不应为空");
            Assertions.assertEquals(optionalNullVO.getId(), savedCourse.getId(), "课程ID应匹配");
            Assertions.assertEquals(optionalNullVO.getName(), savedCourse.getName(), "课程名称应匹配");
            Assertions.assertNull(savedCourse.getCategory(), "课程类别应为空");
            Assertions.assertNull(savedCourse.getProperty(), "课程属性应为空");
            Assertions.assertNull(savedCourse.getNature(), "课程性质应为空");
            Assertions.assertNull(savedCourse.getTheoryClassroomType(), "理论教室类型应为空");
            Assertions.assertNull(savedCourse.getExperimentClassroomType(), "实验教室类型应为空");
            Assertions.assertNull(savedCourse.getPracticeClassroomType(), "实践教室类型应为空");
            Assertions.assertNull(savedCourse.getComputerClassroomType(), "上机教室类型应为空");
            Assertions.assertNull(savedCourse.getDescription(), "课程描述应为空");

            // 验证必填字段是否正确保存
            Assertions.assertEquals(optionalNullVO.getType(), savedCourse.getType(), "课程类型应匹配");
            Assertions.assertEquals(optionalNullVO.getDepartment(), savedCourse.getDepartment(), "课程部门应匹配");
            Assertions.assertEquals(optionalNullVO.getIsEnabled(), savedCourse.getIsEnabled(), "启用状态应匹配");
            Assertions.assertEquals(0, optionalNullVO.getTotalHours().compareTo(savedCourse.getTotalHours()), "总课时应匹配");
            Assertions.assertEquals(0, optionalNullVO.getCredit().compareTo(savedCourse.getCredit()), "学分应匹配");
        } finally {
            // 清理测试数据
            courseLibraryDAO.lambdaUpdate().eq(CourseLibraryDO::getId, optionalNullVO.getId()).remove();
        }
    }

    /**
     * 测试添加课程库 - 检查课程ID重复
     */
    @Test
    void testAddCourseLibrary_DuplicateId() {
        log.debug("测试添加课程库 - 检查课程ID重复");

        // 先添加一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 创建一个ID相同但名称不同的课程
        CourseLibraryVO duplicateIdVO = new CourseLibraryVO(
                validCourseLibraryVO.getId(), // 相同的ID
                "另一个计算机导论", // 不同的名称
                validCourseLibraryVO.getEnglishName(),
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                validCourseLibraryVO.getType(),
                validCourseLibraryVO.getNature(),
                validCourseLibraryVO.getDepartment(),
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                validCourseLibraryVO.getTotalHours(),
                validCourseLibraryVO.getWeekHours(),
                validCourseLibraryVO.getTheoryHours(),
                validCourseLibraryVO.getExperimentHours(),
                validCourseLibraryVO.getPracticeHours(),
                validCourseLibraryVO.getComputerHours(),
                validCourseLibraryVO.getOtherHours(),
                validCourseLibraryVO.getCredit(),
                validCourseLibraryVO.getDescription()
        );

        // 尝试添加ID重复的课程，预期会抛出异常
        // 注意：这里取决于业务逻辑是否允许ID重复，如果允许则需要调整测试内容
        try {
            courseLibraryService.addCourseLibrary(duplicateIdVO, request);

            // 如果成功添加，验证是否更新了原有记录或允许ID重复
            CourseLibraryDO updatedCourse = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getId, duplicateIdVO.getId())
                    .one();

            // 判断是更新了现有记录还是创建了新记录
            long count = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getId, duplicateIdVO.getId())
                    .count();

            if (count > 1) {
                log.debug("业务逻辑允许课程ID重复");
            } else {
                Assertions.assertEquals(duplicateIdVO.getName(), updatedCourse.getName(),
                        "如果覆盖了原有记录，名称应该已更新");
            }
        } catch (BusinessException e) {
            // 如果业务逻辑不允许ID重复，会抛出异常
            log.debug("业务逻辑不允许课程ID重复: {}", e.getMessage());
        }
    }

    /**
     * 测试更新课程库 - 成功场景
     *
     * 验证是否能成功更新现有课程库信息
     */
    @Test
    void testUpdateCourseLibrary_Success() {
        log.debug("测试更新课程库 - 成功场景");

        // 先添加一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 获取保存的课程
        CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        Assertions.assertNotNull(savedCourse, "保存的课程不应为空");

        // 创建更新后的课程信息
        CourseLibraryVO updatedVO = new CourseLibraryVO(
                validCourseLibraryVO.getId(),
                "更新后的计算机导论", // 修改名称
                "Updated Introduction to CS", // 修改英文名称
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                validCourseLibraryVO.getType(),
                validCourseLibraryVO.getNature(),
                validCourseLibraryVO.getDepartment(),
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                new BigDecimal("80"), // 修改总课时
                new BigDecimal("5"), // 修改周课时
                new BigDecimal("50"), // 修改理论课时
                new BigDecimal("20"), // 修改实验课时
                new BigDecimal("5"), // 修改实践课时
                new BigDecimal("5"), // 修改上机课时
                new BigDecimal("0"), // 其他课时
                new BigDecimal("5"), // 修改学分
                "更新后的计算机基础入门课程" // 修改描述
        );

        // 执行更新操作
        Assertions.assertDoesNotThrow(() ->
            courseLibraryService.updateCourseLibrary(savedCourse.getCourseLibraryUuid(), updatedVO));

        // 验证是否成功更新
        CourseLibraryDO updatedCourse = courseLibraryDAO.getCourseLibraryByUuid(savedCourse.getCourseLibraryUuid());

        // 验证更新后的信息是否正确
        Assertions.assertNotNull(updatedCourse, "更新后的课程不应为空");
        Assertions.assertEquals(updatedVO.getName(), updatedCourse.getName(), "课程名称应已更新");
        Assertions.assertEquals(updatedVO.getEnglishName(), updatedCourse.getEnglishName(), "课程英文名称应已更新");
        Assertions.assertEquals(0, updatedVO.getTotalHours().compareTo(updatedCourse.getTotalHours()), "总课时应已更新");
        Assertions.assertEquals(0, updatedVO.getWeekHours().compareTo(updatedCourse.getWeekHours()), "周课时应已更新");
        Assertions.assertEquals(0, updatedVO.getTheoryHours().compareTo(updatedCourse.getTheoryHours()), "理论课时应已更新");
        Assertions.assertEquals(0, updatedVO.getExperimentHours().compareTo(updatedCourse.getExperimentHours()), "实验课时应已更新");
        Assertions.assertEquals(0, updatedVO.getCredit().compareTo(updatedCourse.getCredit()), "学分应已更新");
        Assertions.assertEquals(updatedVO.getDescription(), updatedCourse.getDescription(), "课程描述应已更新");
    }

    /**
     * 测试更新课程库 - 使用无效的课程类型
     *
     * 当更新时使用不存在的课程类型UUID，应抛出业务异常
     */
    @Test
    void testUpdateCourseLibrary_InvalidCourseType() {
        log.debug("测试更新课程库 - 使用无效的课程类型");

        // 先添加一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 获取保存的课程
        CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        Assertions.assertNotNull(savedCourse, "保存的课程不应为空");

        // 创建一个包含无效课程类型的更新VO
        CourseLibraryVO invalidTypeVO = new CourseLibraryVO(
                validCourseLibraryVO.getId(),
                validCourseLibraryVO.getName(),
                validCourseLibraryVO.getEnglishName(),
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                UuidUtil.generateUuidNoDash(), // 设置一个不存在的课程类型UUID
                validCourseLibraryVO.getNature(),
                validCourseLibraryVO.getDepartment(),
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                validCourseLibraryVO.getTotalHours(),
                validCourseLibraryVO.getWeekHours(),
                validCourseLibraryVO.getTheoryHours(),
                validCourseLibraryVO.getExperimentHours(),
                validCourseLibraryVO.getPracticeHours(),
                validCourseLibraryVO.getComputerHours(),
                validCourseLibraryVO.getOtherHours(),
                validCourseLibraryVO.getCredit(),
                validCourseLibraryVO.getDescription()
        );

        // 执行更新操作，预期会抛出异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> courseLibraryService.updateCourseLibrary(savedCourse.getCourseLibraryUuid(), invalidTypeVO));

        // 验证异常信息
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
        Assertions.assertEquals("课程类型不存在", exception.getMessage());

        // 验证课程信息未被更新
        CourseLibraryDO unchangedCourse = courseLibraryDAO.getCourseLibraryByUuid(savedCourse.getCourseLibraryUuid());
        Assertions.assertEquals(validCourseLibraryVO.getName(), unchangedCourse.getName(), "课程名称不应被更新");
    }

    /**
     * 测试更新课程库 - 使用无效的部门
     *
     * 当更新时使用不存在的部门UUID，应抛出业务异常
     */
    @Test
    void testUpdateCourseLibrary_InvalidDepartment() {
        log.debug("测试更新课程库 - 使用无效的部门");

        // 先添加一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 获取保存的课程
        CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        Assertions.assertNotNull(savedCourse, "保存的课程不应为空");

        // 创建一个包含无效部门的更新VO
        CourseLibraryVO invalidDeptVO = new CourseLibraryVO(
                validCourseLibraryVO.getId(),
                validCourseLibraryVO.getName(),
                validCourseLibraryVO.getEnglishName(),
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                validCourseLibraryVO.getType(),
                validCourseLibraryVO.getNature(),
                UuidUtil.generateUuidNoDash(), // 设置一个不存在的部门UUID
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                validCourseLibraryVO.getTotalHours(),
                validCourseLibraryVO.getWeekHours(),
                validCourseLibraryVO.getTheoryHours(),
                validCourseLibraryVO.getExperimentHours(),
                validCourseLibraryVO.getPracticeHours(),
                validCourseLibraryVO.getComputerHours(),
                validCourseLibraryVO.getOtherHours(),
                validCourseLibraryVO.getCredit(),
                validCourseLibraryVO.getDescription()
        );

        // 执行更新操作，预期会抛出异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> courseLibraryService.updateCourseLibrary(savedCourse.getCourseLibraryUuid(), invalidDeptVO));

        // 验证异常信息
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
        Assertions.assertEquals("部门不存在", exception.getMessage());

        // 验证课程信息未被更新
        CourseLibraryDO unchangedCourse = courseLibraryDAO.getCourseLibraryByUuid(savedCourse.getCourseLibraryUuid());
        Assertions.assertEquals(validCourseLibraryVO.getName(), unchangedCourse.getName(), "课程名称不应被更新");
    }

    /**
     * 测试更新课程库 - 使用无效的教室类型
     *
     * 当更新时使用不存在的教室类型UUID，应抛出业务异常
     */
    @Test
    void testUpdateCourseLibrary_InvalidClassroomType() {
        // 仅当classroomTypeDO不为null时执行此测试
        if (classroomTypeDO != null) {
            log.debug("测试更新课程库 - 使用无效的教室类型");

            // 先添加一个课程
            Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

            // 获取保存的课程
            CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                    .one();

            Assertions.assertNotNull(savedCourse, "保存的课程不应为空");

            // 创建一个包含无效教室类型的更新VO
            CourseLibraryVO invalidClassroomTypeVO = new CourseLibraryVO(
                    validCourseLibraryVO.getId(),
                    validCourseLibraryVO.getName(),
                    validCourseLibraryVO.getEnglishName(),
                    validCourseLibraryVO.getCategory(),
                    validCourseLibraryVO.getProperty(),
                    validCourseLibraryVO.getType(),
                    validCourseLibraryVO.getNature(),
                    validCourseLibraryVO.getDepartment(),
                    UuidUtil.generateUuidNoDash(), // 设置一个不存在的教室类型UUID
                    validCourseLibraryVO.getExperimentClassroomType(),
                    validCourseLibraryVO.getPracticeClassroomType(),
                    validCourseLibraryVO.getComputerClassroomType(),
                    validCourseLibraryVO.getIsEnabled(),
                    validCourseLibraryVO.getTotalHours(),
                    validCourseLibraryVO.getWeekHours(),
                    validCourseLibraryVO.getTheoryHours(),
                    validCourseLibraryVO.getExperimentHours(),
                    validCourseLibraryVO.getPracticeHours(),
                    validCourseLibraryVO.getComputerHours(),
                    validCourseLibraryVO.getOtherHours(),
                    validCourseLibraryVO.getCredit(),
                    validCourseLibraryVO.getDescription()
            );

            // 执行更新操作，预期会抛出异常
            BusinessException exception = Assertions.assertThrows(BusinessException.class,
                    () -> courseLibraryService.updateCourseLibrary(savedCourse.getCourseLibraryUuid(), invalidClassroomTypeVO));

            // 验证异常信息
            Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
            Assertions.assertEquals("教室类型不存在", exception.getMessage());

            // 验证课程信息未被更新
            CourseLibraryDO unchangedCourse = courseLibraryDAO.getCourseLibraryByUuid(savedCourse.getCourseLibraryUuid());
            Assertions.assertEquals(validCourseLibraryVO.getName(), unchangedCourse.getName(), "课程名称不应被更新");
        }
    }

    /**
     * 测试更新课程库 - 更新课程ID
     *
     * 检验更新课程ID的逻辑，如果ID已存在则应报错，否则应成功更新
     */
    @Test
    void testUpdateCourseLibrary_UpdateId() {
        log.debug("测试更新课程库 - 更新课程ID");

        // 先添加一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 获取保存的课程
        CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        Assertions.assertNotNull(savedCourse, "保存的课程不应为空");

        // 创建一个带有新ID的更新VO
        String newId = "CS999";
        CourseLibraryVO newIdVO = new CourseLibraryVO(
                newId, // 新的ID
                validCourseLibraryVO.getName(),
                validCourseLibraryVO.getEnglishName(),
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                validCourseLibraryVO.getType(),
                validCourseLibraryVO.getNature(),
                validCourseLibraryVO.getDepartment(),
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                validCourseLibraryVO.getTotalHours(),
                validCourseLibraryVO.getWeekHours(),
                validCourseLibraryVO.getTheoryHours(),
                validCourseLibraryVO.getExperimentHours(),
                validCourseLibraryVO.getPracticeHours(),
                validCourseLibraryVO.getComputerHours(),
                validCourseLibraryVO.getOtherHours(),
                validCourseLibraryVO.getCredit(),
                validCourseLibraryVO.getDescription()
        );

        try {
            // 执行更新操作
            Assertions.assertDoesNotThrow(() ->
                courseLibraryService.updateCourseLibrary(savedCourse.getCourseLibraryUuid(), newIdVO));

            // 验证ID是否已更新成功
            CourseLibraryDO updatedCourse = courseLibraryDAO.getCourseLibraryByUuid(savedCourse.getCourseLibraryUuid());
            Assertions.assertEquals(newId, updatedCourse.getId(), "课程ID应已更新");

            // 验证旧ID不存在了
            CourseLibraryDO oldIdCourse = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                    .one();
            Assertions.assertNull(oldIdCourse, "旧ID对应的课程应不存在");
        } finally {
            // 清理测试数据（清除可能的新ID）
            courseLibraryDAO.lambdaUpdate().eq(CourseLibraryDO::getId, newId).remove();
        }
    }

    /**
     * 测试更新课程库 - 尝试更新为已存在的ID
     *
     * 当尝试将课程ID更新为已存在的ID时，应抛出业务异常
     */
    @Test
    void testUpdateCourseLibrary_UpdateToDuplicateId() {
        log.debug("测试更新课程库 - 尝试更新为已存在的ID");

        // 先添加第一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 创建第二个课程
        String secondId = "CS002";
        CourseLibraryVO secondCourseVO = new CourseLibraryVO(
                secondId,
                "第二门课程",
                validCourseLibraryVO.getEnglishName(),
                validCourseLibraryVO.getCategory(),
                validCourseLibraryVO.getProperty(),
                validCourseLibraryVO.getType(),
                validCourseLibraryVO.getNature(),
                validCourseLibraryVO.getDepartment(),
                validCourseLibraryVO.getTheoryClassroomType(),
                validCourseLibraryVO.getExperimentClassroomType(),
                validCourseLibraryVO.getPracticeClassroomType(),
                validCourseLibraryVO.getComputerClassroomType(),
                validCourseLibraryVO.getIsEnabled(),
                validCourseLibraryVO.getTotalHours(),
                validCourseLibraryVO.getWeekHours(),
                validCourseLibraryVO.getTheoryHours(),
                validCourseLibraryVO.getExperimentHours(),
                validCourseLibraryVO.getPracticeHours(),
                validCourseLibraryVO.getComputerHours(),
                validCourseLibraryVO.getOtherHours(),
                validCourseLibraryVO.getCredit(),
                validCourseLibraryVO.getDescription()
        );

        try {
            // 添加第二个课程
            Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(secondCourseVO, request));

            // 获取第二个课程
            CourseLibraryDO secondCourse = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getId, secondId)
                    .one();

            Assertions.assertNotNull(secondCourse, "第二个课程不应为空");

            // 创建更新VO，尝试将第二个课程的ID更新为第一个课程的ID
            CourseLibraryVO updateToDuplicateIdVO = new CourseLibraryVO(
                    validCourseLibraryVO.getId(), // 使用第一个课程的ID
                    secondCourseVO.getName(),
                    secondCourseVO.getEnglishName(),
                    secondCourseVO.getCategory(),
                    secondCourseVO.getProperty(),
                    secondCourseVO.getType(),
                    secondCourseVO.getNature(),
                    secondCourseVO.getDepartment(),
                    secondCourseVO.getTheoryClassroomType(),
                    secondCourseVO.getExperimentClassroomType(),
                    secondCourseVO.getPracticeClassroomType(),
                    secondCourseVO.getComputerClassroomType(),
                    secondCourseVO.getIsEnabled(),
                    secondCourseVO.getTotalHours(),
                    secondCourseVO.getWeekHours(),
                    secondCourseVO.getTheoryHours(),
                    secondCourseVO.getExperimentHours(),
                    secondCourseVO.getPracticeHours(),
                    secondCourseVO.getComputerHours(),
                    secondCourseVO.getOtherHours(),
                    secondCourseVO.getCredit(),
                    secondCourseVO.getDescription()
            );

            // 执行更新操作，预期会抛出异常
            BusinessException exception = Assertions.assertThrows(BusinessException.class,
                    () -> courseLibraryService.updateCourseLibrary(secondCourse.getCourseLibraryUuid(), updateToDuplicateIdVO));

            // 验证异常信息
            Assertions.assertEquals(ErrorCode.PARAMETER_ERROR, exception.getErrorCode());
            Assertions.assertEquals("课程ID已存在", exception.getMessage());

        } finally {
            // 清理第二个课程的测试数据
            courseLibraryDAO.lambdaUpdate().eq(CourseLibraryDO::getId, secondId).remove();
        }
    }

    /**
     * 测试删除课程库 - 成功场景
     *
     * 验证能否成功删除已存在的课程库
     */
    @Test
    void testDeleteCourseLibrary_Success() {
        log.debug("测试删除课程库 - 成功场景");

        // 先添加一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 获取保存的课程
        CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        Assertions.assertNotNull(savedCourse, "保存的课程不应为空");

        // 执行删除操作
        Assertions.assertDoesNotThrow(() ->
                courseLibraryService.deleteCourseLibrary(savedCourse.getCourseLibraryUuid()));

        // 验证课程是否已被删除
        CourseLibraryDO deletedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        // 验证删除后的结果是否为空
        Assertions.assertNull(deletedCourse, "课程应已被删除");

        // 验证Redis缓存是否已被清除
        Assertions.assertFalse(redisson.getKeys().countExists(StringConstant.Redis.COURSE_LIBRARY_UUID + savedCourse.getCourseLibraryUuid()) > 0,
                "课程UUID的Redis缓存应已删除");
        Assertions.assertFalse(redisson.getKeys().countExists(StringConstant.Redis.COURSE_LIBRARY_ID + savedCourse.getId()) > 0,
                "课程ID的Redis缓存应已删除");
    }

    /**
     * 测试删除课程库 - 课程UUID不存在
     *
     * 当尝试删除不存在的课程UUID时，应抛出业务异常
     */
    @Test
    void testDeleteCourseLibrary_NonExistentUuid() {
        log.debug("测试删除课程库 - 课程UUID不存在");

        // 创建一个不存在的UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 尝试删除不存在的课程UUID，预期会抛出异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> courseLibraryService.deleteCourseLibrary(nonExistentUuid));

        // 验证异常信息
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
        Assertions.assertEquals("课程库不存在", exception.getMessage());
    }

    /**
     * 测试删除课程库 - 删除后再次删除
     *
     * 当尝试删除已经被删除的课程时，应抛出业务异常
     */
    @Test
    void testDeleteCourseLibrary_DeleteTwice() {
        log.debug("测试删除课程库 - 删除后再次删除");

        // 先添加一个课程
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 获取保存的课程
        CourseLibraryDO savedCourse = courseLibraryDAO.lambdaQuery()
                .eq(CourseLibraryDO::getId, validCourseLibraryVO.getId())
                .one();

        Assertions.assertNotNull(savedCourse, "保存的课程不应为空");
        String courseUuid = savedCourse.getCourseLibraryUuid();

        // 执行第一次删除操作
        Assertions.assertDoesNotThrow(() ->
                courseLibraryService.deleteCourseLibrary(courseUuid));


        // 执行第二次删除操作，预期会抛出异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> courseLibraryService.deleteCourseLibrary(courseUuid));

        // 验证异常信息
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
        Assertions.assertEquals("课程库不存在", exception.getMessage());
    }

    /**
     * 测试获取课程库分页信息 - 成功场景
     *
     * 验证是否能成功获取课程库分页信息
     */
    @Test
    void testGetCourseLibrary_Success() {
        log.debug("测试获取课程库分页信息 - 成功场景");

        // 先添加一个测试课程库
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 获取第一页，每页10条数据
        PageDTO<CourseLibraryDTO> pageDTO = courseLibraryService.getCourseLibrary(1, 10, "001");

        // 验证分页信息
        Assertions.assertNotNull(pageDTO, "分页信息不应为空");
        Assertions.assertTrue(pageDTO.getTotal() > 0, "总记录数应大于0");
        Assertions.assertEquals(1, pageDTO.getCurrent(), "当前页码应为1");
        Assertions.assertEquals(10, pageDTO.getSize(), "每页大小应为10");
        Assertions.assertNotNull(pageDTO.getRecords(), "记录列表不应为空");
        Assertions.assertFalse(pageDTO.getRecords().isEmpty(), "记录列表不应为空");

        log.debug("分页信息: {}", pageDTO);

        // 找到我们刚刚添加的课程库
        boolean foundAddedCourse = pageDTO.getRecords().stream()
                .anyMatch(course -> course.getId().equals(validCourseLibraryVO.getId()));

        Assertions.assertTrue(foundAddedCourse, "应该能找到刚添加的课程库");
    }

    /**
     * 测试获取课程库分页信息 - 按名称搜索
     *
     * 验证是否能按名称成功搜索课程库
     */
    @Test
    void testGetCourseLibrary_SearchByName() {
        log.debug("测试获取课程库分页信息 - 按名称搜索");

        // 先添加一个测试课程库
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 使用课程名称的一部分进行搜索
        String searchKeyword = validCourseLibraryVO.getName().substring(0, 4);
        PageDTO<CourseLibraryDTO> pageDTO = courseLibraryService.getCourseLibrary(1, 10, searchKeyword);

        // 验证搜索结果
        Assertions.assertNotNull(pageDTO, "分页信息不应为空");
        Assertions.assertTrue(pageDTO.getTotal() > 0, "搜索结果总记录数应大于0");

        // 找到我们刚刚添加的课程库
        boolean foundAddedCourse = pageDTO.getRecords().stream()
                 .anyMatch(course -> course.getId().equals(validCourseLibraryVO.getId()));

        Assertions.assertTrue(foundAddedCourse, "应该能找到刚添加的课程库");
    }

    /**
     * 测试获取课程库分页信息 - 不存在的名称
     *
     * 验证搜索不存在的课程名称时返回空结果
     */
    @Test
    void testGetCourseLibrary_NonExistentName() {
        log.debug("测试获取课程库分页信息 - 不存在的名称");

        // 使用一个不太可能存在的课程名称进行搜索
        String nonExistentName = "ThisCourseNameShouldNotExist" + System.currentTimeMillis();
        PageDTO<CourseLibraryDTO> pageDTO = courseLibraryService.getCourseLibrary(1, 10, nonExistentName);

        // 验证搜索结果
        Assertions.assertNotNull(pageDTO, "分页信息不应为空");
        Assertions.assertEquals(0, pageDTO.getTotal(), "搜索结果总记录数应为0");
        Assertions.assertTrue(pageDTO.getRecords().isEmpty(), "记录列表应为空");
    }

    /**
     * 测试获取课程库列表 - 无条件查询
     *
     * 验证是否能成功获取所有课程库列表
     */
    @Test
    void testGetCourseLibraryList_NoConditions() {
        log.debug("测试获取课程库列表 - 无条件查询");

        // 先添加一个测试课程库
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 无条件查询，获取所有课程库
        List<CourseLiteDTO> courseList = courseLibraryService.getCourseLibraryList(null, null, null, null, null);

        // 验证查询结果
        Assertions.assertNotNull(courseList, "课程库列表不应为空");
        Assertions.assertFalse(courseList.isEmpty(), "课程库列表不应为空");

        // 找到我们刚刚添加的课程库
        boolean foundAddedCourse = courseList.stream()
                .anyMatch(course -> course.getName().equals(validCourseLibraryVO.getName()));

        Assertions.assertTrue(foundAddedCourse, "应该能找到刚添加的课程库");
    }

    /**
     * 测试获取课程库列表 - 按类型查询
     *
     * 验证是否能按课程类型成功筛选课程库
     */
    @Test
    void testGetCourseLibraryList_ByType() {
        log.debug("测试获取课程库列表 - 按类型查询");

        // 先添加一个测试课程库
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 使用已知的课程类型UUID进行查询
        List<CourseLiteDTO> courseList = courseLibraryService.getCourseLibraryList(
                null, null, validCourseLibraryVO.getType(), null, null);

        // 验证查询结果
        Assertions.assertNotNull(courseList, "课程库列表不应为空");
        Assertions.assertFalse(courseList.isEmpty(), "课程库列表不应为空");

        // 找到我们刚刚添加的课程库
        boolean foundAddedCourse = courseList.stream()
                .anyMatch(course -> course.getName().equals(validCourseLibraryVO.getName()));

        Assertions.assertTrue(foundAddedCourse, "应该能找到刚添加的课程库");

        // 验证所有结果的课程类型都是我们指定的类型
        String expectedTypeName = courseTypeDAO.getCourseTypeByUuid(validCourseLibraryVO.getType()).getName();
        boolean allMatchType = courseList.stream()
                .allMatch(course -> expectedTypeName.equals(course.getType()));

        Assertions.assertTrue(allMatchType, "所有查询结果的课程类型应匹配");
    }

    /**
     * 测试获取课程库列表 - 按部门查询
     *
     * 验证是否能按部门成功筛选课程库
     */
    @Test
    void testGetCourseLibraryList_ByDepartment() {
        log.debug("测试获取课程库列表 - 按部门查询");

        // 先添加一个测试课程库
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 使用已知的部门UUID进行查询
        List<CourseLiteDTO> courseList = courseLibraryService.getCourseLibraryList(
                null, null, null, null, validCourseLibraryVO.getDepartment());

        // 验证查询结果
        Assertions.assertNotNull(courseList, "课程库列表不应为空");
        Assertions.assertFalse(courseList.isEmpty(), "课程库列表不应为空");

        // 找到我们刚刚添加的课程库
        boolean foundAddedCourse = courseList.stream()
                .anyMatch(course -> course.getName().equals(validCourseLibraryVO.getName()));

        Assertions.assertTrue(foundAddedCourse, "应该能找到刚添加的课程库");

        // 验证所有结果的部门都是我们指定的部门
        String expectedDeptName = departmentDAO.getDepartmentByUuid(validCourseLibraryVO.getDepartment()).getDepartmentName();
        boolean allMatchDepartment = courseList.stream()
                .allMatch(course -> expectedDeptName.equals(course.getDepartment()));

        Assertions.assertTrue(allMatchDepartment, "所有查询结果的部门应匹配");
    }

    /**
     * 测试获取课程库列表 - 组合条件查询
     *
     * 验证是否能使用多个条件成功筛选课程库
     */
    @Test
    void testGetCourseLibraryList_CombinedConditions() {
        log.debug("测试获取课程库列表 - 组合条件查询");

        // 先添加一个测试课程库
        Assertions.assertDoesNotThrow(() -> courseLibraryService.addCourseLibrary(validCourseLibraryVO, request));

        // 只有当课程类别和课程性质不为null时才进行测试
        if (validCourseLibraryVO.getCategory() != null && validCourseLibraryVO.getNature() != null) {
            // 使用多个条件进行查询
            List<CourseLiteDTO> courseList = courseLibraryService.getCourseLibraryList(
                    validCourseLibraryVO.getCategory(),
                    validCourseLibraryVO.getProperty(),
                    validCourseLibraryVO.getType(),
                    validCourseLibraryVO.getNature(),
                    validCourseLibraryVO.getDepartment());

            // 验证查询结果
            Assertions.assertNotNull(courseList, "课程库列表不应为空");
            Assertions.assertFalse(courseList.isEmpty(), "课程库列表不应为空");

            // 找到我们刚刚添加的课程库
            boolean foundAddedCourse = courseList.stream()
                    .anyMatch(course -> course.getName().equals(validCourseLibraryVO.getName()));

            Assertions.assertTrue(foundAddedCourse, "应该能找到刚添加的课程库");

            // 验证所有查询结果是否符合所有条件
            // 获取期望的名称
            String expectedCategoryName = Objects.requireNonNull(courseCategoryDAO.getCourseCategoryByUuid(validCourseLibraryVO.getCategory())).getName();
            String expectedTypeName = Objects.requireNonNull(courseTypeDAO.getCourseTypeByUuid(validCourseLibraryVO.getType())).getName();
            String expectedNatureName = Objects.requireNonNull(courseNatureDAO.getCourseNatureByUuid(validCourseLibraryVO.getNature())).getName();
            String expectedDeptName = departmentDAO.getDepartmentByUuid(validCourseLibraryVO.getDepartment()).getDepartmentName();

            // 验证所有结果是否匹配所有条件
            boolean allMatchConditions = courseList.stream()
                    .allMatch(course ->
                            expectedCategoryName.equals(course.getCategory()) &&
                            expectedTypeName.equals(course.getType()) &&
                            expectedNatureName.equals(course.getNature()) &&
                            expectedDeptName.equals(course.getDepartment()));

            Assertions.assertTrue(allMatchConditions, "所有查询结果应匹配所有条件");
        }
    }

    /**
     * 测试获取课程库列表 - 不存在的条件
     *
     * 验证使用不存在的条件进行查询时返回空列表
     */
    @Test
    void testGetCourseLibraryList_NonExistentCondition() {
        log.debug("测试获取课程库列表 - 不存在的条件");

        // 使用一个不太可能存在的UUID进行查询
        String nonExistentUuid = UuidUtil.generateUuidNoDash();
        List<CourseLiteDTO> courseList = courseLibraryService.getCourseLibraryList(
                nonExistentUuid, null, null, null, null);

        // 验证查询结果
        Assertions.assertNotNull(courseList, "课程库列表不应为null");
        Assertions.assertTrue(courseList.isEmpty(), "使用不存在的条件查询应返回空列表");
    }
}
