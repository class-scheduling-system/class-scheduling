package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.TeacherTypeDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherDTO;
import com.frontleaves.scheduling.models.entity.base.DepartmentDO;
import com.frontleaves.scheduling.models.entity.base.TeacherDO;
import com.frontleaves.scheduling.models.entity.base.TeacherTypeDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.services.TeacherService;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
class TeacherListTest {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    @Resource
    private TeacherDAO teacherDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private UserDAO userDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private TeacherService teacherService;
    private List<TeacherDO> testTeachers;
    private List<UserDO> testUsers;
    private DepartmentDO testDepartment;
    private String testTeacherName;
    @Autowired
    private TeacherTypeDAO teacherTypeDAO;

    /**
     * 测试前准备，创建测试用教师数据
     */
    @BeforeEach
    @Transactional
    void setUpTeacher() {
        log.debug("测试教师列表方法 - 准备测试数据");

        // 获取测试部门
        testDepartment = departmentDAO.lambdaQuery().last("LIMIT 1").one();
        if (testDepartment == null) {
            log.error("无法找到测试部门，测试可能会失败");
            return;
        }

        // 创建测试数据
        testTeachers = new ArrayList<>();
        testUsers = new ArrayList<>();

        // 创建3个测试教师
        for (int i = 1; i <= 3; i++) {
            // 创建用户
            UserDO user = new UserDO();
            String userUuid = UuidUtil.generateUuidNoDash();
            user.setUserUuid(userUuid)
                    .setName("testTeacher" + i)
                    .setPassword(PasswordUtil.encrypt("Test123456"))
                    .setEmail("testteacher" + i + "@test.com")
                    .setPhone("1380000000" + i)
                    .setStatus((byte) (i % 2)) // 交替设置为0(禁用)和1(启用)
                    .setBan(false)
                    .setPermission("[\"teacher:view\"]")
                    .setRoleUuid(SystemConstant.getRoleTeacher());

            // 保存用户并添加到列表
            userDAO.save(user);
            testUsers.add(user);

            TeacherTypeDO teacherTypeDO = teacherTypeDAO.lambdaQuery().eq(TeacherTypeDO::getTypeName, "辅导员").one();

            // 创建教师
            TeacherDO teacher = new TeacherDO();
            teacher.setTeacherUuid(UuidUtil.generateUuidNoDash())
                    .setUnitUuid(testDepartment.getDepartmentUuid())
                    .setUserUuid(userUuid)
                    .setId("T" + (1000 + i))
                    .setName("测试教师" + i)
                    .setEnglishName("TestTeacher" + i)
                    .setEthnic("汉族")
                    .setSex(i % 2 == 0)
                    .setType(teacherTypeDO.getTeacherTypeUuid())
                    .setPhone("1380000000" + i)
                    .setEmail("testteacher" + i + "@test.com")
                    .setJobTitle("讲师")
                    .setDesc("测试用教师数据");

            // 保存教师并添加到列表
            teacherDAO.save(teacher);
            testTeachers.add(teacher);
        }

        // 记录测试教师名称，用于搜索测试
        testTeacherName = "测试教师";

        log.debug("已创建 {} 个测试教师数据", testTeachers.size());
    }

    /**
     * 测试后清理，删除测试用教师数据
     */
    @AfterEach
    @Transactional
    void tearDown2() {
        log.debug("测试教师列表方法 - 清理测试数据");

        // 清理测试教师数据
        if (testTeachers != null) {
            for (TeacherDO teacher : testTeachers) {
                teacherDAO.removeById(teacher.getTeacherUuid());

                // 清理Redis缓存
                redisson.getMap(StringConstant.Redis.TEACHER_UUID + teacher.getTeacherUuid()).delete();
                redisson.getBucket(StringConstant.Redis.TEACHER_ID + teacher.getId()).delete();
                redisson.getBucket(StringConstant.Redis.TEACHER_USER_UUID + teacher.getUserUuid()).delete();
                redisson.getBucket(StringConstant.Redis.TEACHER_TYPE_UUID + teacher.getType()).delete();
            }
        }

        // 清理测试用户数据
        if (testUsers != null) {
            for (UserDO user : testUsers) {
                userDAO.removeById(user.getUserUuid());
            }
        }

        log.debug("测试数据清理完成");
    }

    /**
     * 测试正常获取教师列表
     * <p>
     * 预期：能够成功获取教师列表，并且返回数据与查询参数一致
     * </p>
     */
    @Test
    @DisplayName("测试正常获取教师列表")
    void testGetTeacherListSuccess() {
        log.debug("测试正常获取教师列表");

        // 跳过测试如果没有测试数据
        if (testTeachers == null || testTeachers.isEmpty()) {
            log.warn("没有测试数据，跳过测试");
            return;
        }

        // 调用getTeacherList方法获取第一页数据
        PageDTO<TeacherDTO> pageDTO = teacherService.getTeacherList(DEFAULT_PAGE, DEFAULT_SIZE, false, null, null, null);

        // 验证返回的数据不为空
        Assertions.assertNotNull(pageDTO, "返回的分页数据不应为空");

        // 验证分页信息正确
        Assertions.assertEquals(DEFAULT_PAGE, pageDTO.getCurrent().intValue(), "当前页码应该与请求的页码一致");
        Assertions.assertEquals(DEFAULT_SIZE, pageDTO.getSize().intValue(), "每页大小应该与请求的大小一致");

        // 验证记录列表不为空
        Assertions.assertNotNull(pageDTO.getRecords(), "返回的记录列表不应为空");
        Assertions.assertFalse(pageDTO.getRecords().isEmpty(), "返回的记录列表不应为空");

        // 验证至少包含我们创建的测试数据
        boolean foundTestTeacher = false;
        for (TeacherDTO teacherDTO : pageDTO.getRecords()) {
            if (teacherDTO.getName() != null && teacherDTO.getName().startsWith(testTeacherName)) {
                foundTestTeacher = true;
                break;
            }
        }
        Assertions.assertTrue(foundTestTeacher, "返回的结果中应包含测试教师数据");

        log.debug("获取到 {} 条教师记录，总计 {} 条",
                pageDTO.getRecords().size(),
                pageDTO.getTotal());
    }

    /**
     * 测试使用部门筛选教师
     * <p>
     * 预期：能够根据部门筛选教师数据
     * </p>
     */
    @Test
    @DisplayName("测试使用部门筛选教师")
    void testGetTeacherListByDepartment() {
        log.debug("测试使用部门筛选教师");

        // 跳过测试如果没有测试数据
        if (testTeachers == null || testTeachers.isEmpty() || testDepartment == null) {
            log.warn("没有测试数据或部门数据，跳过测试");
            return;
        }

        // 调用getTeacherList方法，使用部门UUID进行筛选
        PageDTO<TeacherDTO> pageDTO = teacherService.getTeacherList(
                DEFAULT_PAGE, DEFAULT_SIZE, false, testDepartment.getDepartmentUuid(), null, null);

        // 验证返回的数据不为空
        Assertions.assertNotNull(pageDTO, "返回的分页数据不应为空");

        // 如果返回的记录不为空，验证是否只包含指定部门的教师
        if (pageDTO.getRecords() != null && !pageDTO.getRecords().isEmpty()) {
            boolean foundTestTeacher = false;
            for (TeacherDTO teacherDTO : pageDTO.getRecords()) {
                if (teacherDTO.getName() != null && teacherDTO.getName().startsWith(testTeacherName)) {
                    foundTestTeacher = true;
                    // 验证部门UUID是否匹配
                    Assertions.assertEquals(testDepartment.getDepartmentUuid(), teacherDTO.getUnitUuid(),
                            "教师的部门UUID应该与筛选条件一致");
                }
            }
            Assertions.assertTrue(foundTestTeacher, "返回的结果中应包含测试教师数据");
        }

        log.debug("按部门筛选获取到 {} 条教师记录",
                pageDTO.getRecords() != null ? pageDTO.getRecords().size() : 0);
    }

    /**
     * 测试使用名称搜索教师
     * <p>
     * 预期：能够根据教师名称搜索匹配的教师
     * </p>
     */
    @Test
    @DisplayName("测试使用名称搜索教师")
    void testGetTeacherListByName() {
        log.debug("测试使用名称搜索教师");

        // 跳过测试如果没有测试数据
        if (testTeachers == null || testTeachers.isEmpty()) {
            log.warn("没有测试数据，跳过测试");
            return;
        }

        // 使用测试教师名称进行搜索
        PageDTO<TeacherDTO> searchResult = teacherService.getTeacherList(
                DEFAULT_PAGE, DEFAULT_SIZE, false, null, null, testTeacherName);

        // 验证返回的数据不为空
        Assertions.assertNotNull(searchResult, "搜索结果不应为空");

        // 验证记录列表不为空
        Assertions.assertNotNull(searchResult.getRecords(), "返回的记录列表不应为空");
        Assertions.assertFalse(searchResult.getRecords().isEmpty(), "返回的记录列表不应为空");

        // 验证所有返回的记录名称都包含搜索关键词
        for (TeacherDTO teacherDTO : searchResult.getRecords()) {
            Assertions.assertTrue(teacherDTO.getName().contains(testTeacherName),
                    "搜索结果应包含搜索关键词");
        }

        log.debug("搜索 '{}' 获取到 {} 条教师记录",
                testTeacherName,
                searchResult.getRecords().size());
    }

    /**
     * 测试使用不存在的名称搜索教师
     * <p>
     * 预期：返回空结果集
     * </p>
     */
    @Test
    @DisplayName("测试使用不存在的名称搜索教师")
    void testGetTeacherListWithNonExistentName() {
        log.debug("测试使用不存在的名称搜索教师");

        // 使用一个极不可能存在的名称进行搜索
        String nonExistentName = "这个名字绝对不存在" + System.currentTimeMillis();

        // 调用搜索方法
        PageDTO<TeacherDTO> emptyResult = teacherService.getTeacherList(
                DEFAULT_PAGE, DEFAULT_SIZE, false, null, null, nonExistentName);

        // 验证返回的数据对象不为空，但结果为空
        Assertions.assertNotNull(emptyResult, "返回的PageDTO对象不应为空");

        // 验证总数为0或记录为空
        if (emptyResult.getRecords() != null) {
            Assertions.assertTrue(emptyResult.getRecords().isEmpty(), "记录列表应为空");
        }

        log.debug("搜索不存在的名称验证完成");
    }

    /**
     * 测试降序排序功能
     * <p>
     * 预期：使用降序排序时，返回的数据顺序与默认顺序相反
     * </p>
     */
    @Test
    @DisplayName("测试教师列表降序排序")
    void testGetTeacherListDescOrder() {
        log.debug("测试教师列表降序排序");

        // 跳过测试如果没有测试数据
        if (testTeachers == null || testTeachers.isEmpty()) {
            log.warn("没有测试数据，跳过测试");
            return;
        }

        // 获取升序排序数据
        PageDTO<TeacherDTO> ascOrder = teacherService.getTeacherList(
                DEFAULT_PAGE, DEFAULT_SIZE, false, null, null, null);

        // 获取降序排序数据
        PageDTO<TeacherDTO> descOrder = teacherService.getTeacherList(
                DEFAULT_PAGE, DEFAULT_SIZE, true, null, null, null);

        // 验证两个结果都不为空
        Assertions.assertNotNull(ascOrder, "升序排序结果不应为空");
        Assertions.assertNotNull(descOrder, "降序排序结果不应为空");

        // 如果记录数大于1，验证排序结果不同
        if (ascOrder.getRecords() != null && descOrder.getRecords() != null &&
                ascOrder.getRecords().size() > 1 && descOrder.getRecords().size() > 1) {

            // 验证第一条记录不同（排序不同会导致结果顺序不同）
            String firstAscId = ascOrder.getRecords().get(0).getTeacherUuid();
            String firstDescId = descOrder.getRecords().get(0).getTeacherUuid();

            Assertions.assertNotEquals(firstAscId, firstDescId,
                    "升序和降序的第一条记录应该不同");
        }

        log.debug("排序测试完成，升序 {} 条，降序 {} 条",
                ascOrder.getRecords() != null ? ascOrder.getRecords().size() : 0,
                descOrder.getRecords() != null ? descOrder.getRecords().size() : 0);
    }

    /**
     * 测试分页功能
     * <p>
     * 预期：不同页码返回不同的数据集
     * </p>
     */
    @Test
    @DisplayName("测试教师列表分页功能")
    void testGetTeacherListPagination() {
        log.debug("测试教师列表分页功能");

        // 设置较小的页面大小，以确保分页效果
        int smallPageSize = 2;

        // 获取第一页数据
        PageDTO<TeacherDTO> page1 = teacherService.getTeacherList(
                1, smallPageSize, false, null, null, null);

        // 获取第二页数据
        PageDTO<TeacherDTO> page2 = teacherService.getTeacherList(
                2, smallPageSize, false, null, null, null);

        // 验证两个结果都不为空
        Assertions.assertNotNull(page1, "第一页结果不应为空");
        Assertions.assertNotNull(page2, "第二页结果不应为空");

        // 验证页码信息正确
        Assertions.assertEquals(1, page1.getCurrent().intValue(), "第一页的页码应为1");
        Assertions.assertEquals(2, page2.getCurrent().intValue(), "第二页的页码应为2");

        // 如果两页都有数据，验证数据不同
        if (page1.getRecords() != null && page2.getRecords() != null &&
                !page1.getRecords().isEmpty() && !page2.getRecords().isEmpty()) {

            // 第一页第一条记录的UUID
            String page1FirstId = page1.getRecords().get(0).getTeacherUuid();

            // 第二页所有记录的UUID
            boolean foundDuplicate = false;
            for (TeacherDTO teacherDTO : page2.getRecords()) {
                if (teacherDTO.getTeacherUuid().equals(page1FirstId)) {
                    foundDuplicate = true;
                    break;
                }
            }

            Assertions.assertFalse(foundDuplicate, "不同页的数据不应重复");
        }

        log.debug("分页测试完成，第一页 {} 条，第二页 {} 条",
                page1.getRecords() != null ? page1.getRecords().size() : 0,
                page2.getRecords() != null ? page2.getRecords().size() : 0);
    }
}
