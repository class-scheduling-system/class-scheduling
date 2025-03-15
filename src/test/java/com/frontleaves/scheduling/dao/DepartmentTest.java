package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.MajorDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
@Slf4j
class DepartmentTest {
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private RedissonClient redisson;
    @Autowired
    private TeacherDAO teacherDAO;
    @Autowired
    private CourseLibraryDAO courseLibraryDAO;
    @Autowired
    private MajorDAO majorDAO;
    @Resource
    private DepartmentService departmentService;

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    // 测试在没有Redis缓存的情况下，通过UUID获取部门信息
    @Test
    void testGetDepartmentByUuidNoRedis() {
        // 从数据库中查询第一个部门记录
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        // 删除Redis中与该部门UUID相关的缓存
        redisson.getKeys().delete(StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid());
        // 通过UUID从数据库中获取部门信息
        DepartmentDO departmentDO1 = departmentDAO.getDepartmentByUuid(departmentDO.getDepartmentUuid());
        // 断言获取到的部门信息不为空
        Assertions.assertNotNull(departmentDO1);
        // 获取Redis中与该部门UUID相关的缓存映射
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid());
        // 断言缓存映射存在
        Assertions.assertTrue(map.isExists());
    }


    @Test
    void testDeleteDepartmentNotUser() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        // 删除所有教师记录
        teacherDAO.lambdaUpdate()
                .eq(TeacherDO::getUnitUuid, departmentDO.getDepartmentUuid())
                .remove();
        courseLibraryDAO.lambdaUpdate()
                .eq(CourseLibraryDO::getDepartment, departmentDO.getDepartmentUuid())
                .remove();
        majorDAO.lambdaUpdate()
                .eq(MajorDO::getDepartmentUuid, departmentDO.getDepartmentUuid())
                .remove();
        // 查询并获取第一个部门对象，用于后续的删除操作
        // 执行删除部门的操作
        departmentDAO.deleteDepartment(departmentDO);
        // 尝试根据部门的唯一标识符获取被删除的部门信息
        DepartmentDO getDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, departmentDO.getDepartmentUuid())
                .one();
        // 验证删除操作是否成功，即验证数据库中是否真的不再存在该部门
        Assertions.assertNull(getDepartment);
    }

    @Test
    void testDeleteDepartmentHasUser() {
        MajorDO majorDO = majorDAO.lambdaQuery().list().get(0);
        DepartmentDO departmentDO = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, majorDO.getDepartmentUuid())
                .one();
        // 查询并获取第一个部门对象，用于后续的删除操作
        Assertions.assertThrows(BusinessException.class, () -> departmentDAO.deleteDepartment(departmentDO));
    }

    @Test
    void testUpdateDepartment() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        DepartmentDO newdepartmentDO = new DepartmentDO();
        newdepartmentDO.setDepartmentUuid(departmentDO.getDepartmentUuid())
                       .setDepartmentCode("1111")
                       .setDepartmentName("测试部门")
                       .setDepartmentOrder(99)
                       .setDepartmentEnglishName("Test Department")
                       .setDepartmentShortName("TD")
                       .setDepartmentAddress("测试地址")
                       .setIsEntity(true)
                       .setAdministrativeHead("测试负责人")
                       .setPartyCommitteeHead("测试党支部书记")
                       .setEstablishmentDate(new Date(System.currentTimeMillis()))
                       .setExpirationDate(new Date(System.currentTimeMillis() + 31536000000L))
                       .setIsTeachingCollege(true)
                       .setIsEnabled(true);
        departmentDAO.updateDepartment(newdepartmentDO);
        DepartmentDO getDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, newdepartmentDO.getDepartmentUuid())
                .one();
        Assertions.assertEquals(getDepartment.getDepartmentCode(), newdepartmentDO.getDepartmentCode());
        Assertions.assertEquals(getDepartment.getDepartmentName(), newdepartmentDO.getDepartmentName());
        Assertions.assertEquals(getDepartment.getDepartmentOrder(), newdepartmentDO.getDepartmentOrder());
        Assertions.assertEquals(getDepartment.getDepartmentEnglishName(), newdepartmentDO.getDepartmentEnglishName());
        Assertions.assertEquals(getDepartment.getDepartmentShortName(), newdepartmentDO.getDepartmentShortName());
        Assertions.assertEquals(getDepartment.getDepartmentAddress(), newdepartmentDO.getDepartmentAddress());
        Assertions.assertEquals(getDepartment.getIsEntity(), newdepartmentDO.getIsEntity());
        Assertions.assertEquals(getDepartment.getAdministrativeHead(), newdepartmentDO.getAdministrativeHead());
        Assertions.assertEquals(getDepartment.getPartyCommitteeHead(), newdepartmentDO.getPartyCommitteeHead());
        // 不涉及小时分钟秒，到日期截止
        Assertions.assertTrue(getDepartment.getEstablishmentDate().before(newdepartmentDO.getEstablishmentDate()));
        Assertions.assertTrue(getDepartment.getExpirationDate().before(newdepartmentDO.getExpirationDate()));
        Assertions.assertEquals(getDepartment.getIsTeachingCollege(), newdepartmentDO.getIsTeachingCollege());
        Assertions.assertEquals(getDepartment.getIsEnabled(), newdepartmentDO.getIsEnabled());
        // 验证Redis缓存是否被清除
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + newdepartmentDO.getDepartmentUuid());
        Assertions.assertFalse(map.isExists());
    }

    @BeforeEach
    void setUp() {
        log.debug("测试准备开始");
        // 确保数据库中至少有一些部门数据用于测试
        long departmentCount = departmentDAO.lambdaQuery().count();
        if (departmentCount == 0) {
            log.warn("数据库中没有部门数据，测试可能无法正常进行");
        } else {
            log.debug("数据库中有 {} 个部门", departmentCount);
        }
    }

    /**
     * 测试正常获取部门列表
     * <p>
     * 预期：能够成功获取部门列表，并且返回数据与查询参数一致
     * </p>
     */
    @Test
    @DisplayName("测试正常获取部门列表")
    void testGetDepartmentListSuccess() {
        log.debug("测试正常获取部门列表");

        // 调用getDepartmentList方法获取第一页数据
        PageDTO<DepartmentDTO> pageDTO = departmentService.getDepartmentPage(DEFAULT_PAGE, DEFAULT_SIZE, false, null);

        // 验证返回的数据不为空
        Assertions.assertNotNull(pageDTO, "返回的分页数据不应为空");

        // 验证分页信息正确
        Assertions.assertEquals(DEFAULT_PAGE, pageDTO.getCurrent(), "当前页码应该与请求的页码一致");
        Assertions.assertEquals(DEFAULT_SIZE, pageDTO.getSize(), "每页大小应该与请求的大小一致");

        // 如果总数大于0，验证记录列表不为空
        if (pageDTO.getTotal() > 0) {
            Assertions.assertNotNull(pageDTO.getRecords(), "返回的记录列表不应为空");
            Assertions.assertFalse(pageDTO.getRecords().isEmpty(), "返回的记录列表不应为空");

            // 验证返回的记录数不超过每页大小
            Assertions.assertTrue(pageDTO.getRecords().size() <= DEFAULT_SIZE,
                    "返回的记录数不应超过每页大小");

            // 验证第一条记录的关键字段不为空
            DepartmentDTO firstDept = pageDTO.getRecords().get(0);
            Assertions.assertNotNull(firstDept.getDepartmentUuid(), "部门UUID不应为空");
            Assertions.assertNotNull(firstDept.getDepartmentName(), "部门名称不应为空");
        }

        log.debug("获取到 {} 条部门记录，总计 {} 条",
                pageDTO.getRecords() != null ? pageDTO.getRecords().size() : 0,
                pageDTO.getTotal());
    }

    /**
     * 测试使用名称搜索部门
     * <p>
     * 预期：能够根据部门名称搜索匹配的部门
     * </p>
     */
    @Test
    @DisplayName("测试使用名称搜索部门")
    void testGetDepartmentListByName() {
        log.debug("测试使用名称搜索部门");

        // 先获取一个部门名称用于搜索
        Page<DepartmentDO> allDepts = departmentDAO.getDepartmentPage(DEFAULT_PAGE, 1, false, null);
        if (allDepts.getRecords().isEmpty()) {
            log.warn("数据库中没有部门数据，跳过搜索测试");
            return;
        }

        String searchName = allDepts.getRecords().get(0).getDepartmentName();
        if (searchName == null || searchName.isEmpty()) {
            searchName = "部门"; // 使用通用名称作为备选
        }
        log.debug("使用 '{}' 作为搜索关键词", searchName);

        // 使用部门名称进行搜索
        PageDTO<DepartmentDTO> searchResult = departmentService.getDepartmentPage(
                DEFAULT_PAGE, DEFAULT_SIZE, false, searchName);

        // 验证返回的数据不为空
        Assertions.assertNotNull(searchResult, "搜索结果不应为空");

        // 如果搜索结果非空，验证记录是否包含搜索关键词
        if (searchResult.getTotal() > 0 && searchResult.getRecords() != null) {
            String finalSearchName = searchName;
            boolean hasMatch = searchResult.getRecords().stream()
                    .anyMatch(dept -> dept.getDepartmentName() != null &&
                            dept.getDepartmentName().contains(finalSearchName));
            Assertions.assertTrue(hasMatch, "搜索结果应包含匹配的部门名称");
        }

        log.debug("搜索 '{}' 获取到 {} 条部门记录", searchName, searchResult.getTotal());
    }

    /**
     * 测试分页功能
     * <p>
     * 预期：能够正确返回指定页码和大小的数据
     * </p>
     */
    @Test
    @DisplayName("测试部门列表分页功能")
    void testGetDepartmentListPagination() {
        log.debug("测试部门列表分页功能");

        // 获取总记录数
        Page<DepartmentDO> countPage = departmentDAO.getDepartmentPage(DEFAULT_PAGE, 1, false, null);
        long totalCount = countPage.getTotal();

        if (totalCount <= DEFAULT_SIZE) {
            log.warn("部门总数不足以测试分页，跳过分页测试");
            return;
        }

        // 获取第一页数据
        PageDTO<DepartmentDTO> page1 = departmentService.getDepartmentPage(1, DEFAULT_SIZE, false, null);

        // 获取第二页数据
        PageDTO<DepartmentDTO> page2 = departmentService.getDepartmentPage(2, DEFAULT_SIZE, false, null);

        // 验证两页数据不同
        Assertions.assertNotEquals(
                page1.getRecords().get(0).getDepartmentUuid(),
                page2.getRecords().get(0).getDepartmentUuid(),
                "第一页和第二页的第一条记录应该不同");

        // 验证总记录数一致
        Assertions.assertEquals(page1.getTotal(), page2.getTotal(), "不同页的总记录数应该一致");

        log.debug("第一页和第二页数据成功验证不同");
    }

    /**
     * 测试获取空结果
     * <p>
     * 预期：使用不存在的部门名称搜索时，返回空结果集
     * </p>
     */
    @Test
    @DisplayName("测试获取空的部门列表")
    void testGetDepartmentListEmpty() {
        log.debug("测试获取空的部门列表");

        // 使用一个极不可能存在的名称进行搜索
        String nonExistentName = "非常不可能存在的部门名称" + System.currentTimeMillis();

        // 调用搜索方法
        PageDTO<DepartmentDTO> emptyResult = departmentService.getDepartmentPage(
                DEFAULT_PAGE, DEFAULT_SIZE, false, nonExistentName);

        // 验证返回的数据不为空但结果为空
        Assertions.assertNotNull(emptyResult, "返回的PageDTO对象不应为空");
        Assertions.assertEquals(0, emptyResult.getTotal(), "总记录数应为0");

        // 根据方法实现，验证records是否为null或空列表
        if (emptyResult.getRecords() != null) {
            Assertions.assertTrue(emptyResult.getRecords().isEmpty(), "记录列表应为空");
        }

        log.debug("空结果集验证完成");
    }

    /**
     * 测试每页大小为0的特殊情况
     * <p>
     * 预期：应该正常处理，可能返回默认每页大小的结果或空结果
     * </p>
     */
    @Test
    @DisplayName("测试每页大小为0的情况")
    void testGetDepartmentListZeroSize() {
        log.debug("测试每页大小为0的情况");

        // 调用方法，设置每页大小为0
        PageDTO<DepartmentDTO> result = departmentService.getDepartmentPage(DEFAULT_PAGE, 0, false, null);

        // 不对具体结果做断言，因为处理方式可能不同，只要不抛出异常即可
        Assertions.assertNotNull(result, "返回的PageDTO对象不应为空");

        log.debug("每页大小为0的情况测试完成，返回记录数: {}",
                result.getRecords() != null ? result.getRecords().size() : 0);
    }

    /**
     * 测试页码为负数的特殊情况
     * <p>
     * 预期：应该正常处理，可能使用默认页码或返回空结果
     * </p>
     */
    @Test
    @DisplayName("测试页码为负数的情况")
    void testGetDepartmentListNegativePage() {
        log.debug("测试页码为负数的情况");

        // 调用方法，设置页码为负数
        PageDTO<DepartmentDTO> result = departmentService.getDepartmentPage(-1, DEFAULT_SIZE, false, null);

        // 不对具体结果做断言，因为处理方式可能不同，只要不抛出异常即可
        Assertions.assertNotNull(result, "返回的PageDTO对象不应为空");

        log.debug("页码为负数的情况测试完成");
    }

}
