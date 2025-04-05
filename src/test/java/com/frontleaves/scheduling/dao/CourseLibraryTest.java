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

package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.ClassroomDAO;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.CourseTypeDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.CourseTypeDO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * 课程库DAO的完整单元测试类
 */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
 class CourseLibraryTest {

    @Resource
    private CourseLibraryDAO courseLibraryDAO;

    @Resource
    private CourseTypeDAO courseTypeDAO;

    @Resource
    private DepartmentDAO departmentDAO;

    @Resource
    private ClassroomDAO classroomDAO;

    @Resource
    private RedissonClient redisson;

    // 测试数据
    private CourseLibraryDO testCourseLibrary;
    private final String testId = "TEST_COURSE_" + System.currentTimeMillis();
    private final String testName = "测试课程库" + System.currentTimeMillis();
    
    /**
     * 在每个测试方法执行前准备测试数据
     */
    @BeforeEach
    void setUp() {

        CourseTypeDO testCourseType = courseTypeDAO.getCourseTypeList().get(0);  // 从数据库获取一个已存在的课程类型
        DepartmentDO testDepartment = departmentDAO.getDepartmentList().get(0);  // 从数据库获取一个已存在的部门


        // 创建测试课程库数据
        testCourseLibrary = new CourseLibraryDO();
        testCourseLibrary.setId(testId);
        testCourseLibrary.setName(testName);
        testCourseLibrary.setEnglishName("Test Course");
        testCourseLibrary.setIsEnabled(true);
        testCourseLibrary.setType(testCourseType.getCourseTypeUuid());
        testCourseLibrary.setDepartment(testDepartment.getDepartmentUuid());
        testCourseLibrary.setDepartment(testCourseLibrary.getDepartment());
        testCourseLibrary.setTotalHours(BigDecimal.valueOf(40));
        testCourseLibrary.setWeekHours(BigDecimal.valueOf(4));
        testCourseLibrary.setTheoryHours(BigDecimal.valueOf(30));
        testCourseLibrary.setExperimentHours(BigDecimal.valueOf(10));
        testCourseLibrary.setPracticeHours(BigDecimal.valueOf(0));
        testCourseLibrary.setComputerHours(BigDecimal.valueOf(0));
        testCourseLibrary.setOtherHours(BigDecimal.valueOf(0));
        testCourseLibrary.setCredit(BigDecimal.valueOf(2.5));
        testCourseLibrary.setCreatedAt(Timestamp.from(Instant.now()));
        testCourseLibrary.setUpdatedAt(Timestamp.from(Instant.now()));
        
        // 保存测试数据到数据库
        boolean isSaved = courseLibraryDAO.save(testCourseLibrary);
        Assertions.assertTrue(isSaved, "保存测试数据失败");
        
        // 确保缓存为空初始状态
        clearCourseLibraryCache(testCourseLibrary);
    }
    
    /**
     * 在每个测试方法执行后清理测试数据
     */
    @AfterEach
    void tearDown() {
        // 清理测试数据
        courseLibraryDAO.removeById(testCourseLibrary.getCourseLibraryUuid());
        
        // 清理缓存
        clearCourseLibraryCache(testCourseLibrary);
    }
    
    /**
     * 清理课程库相关的缓存
     * 
     * @param courseLibraryDO 课程库对象
     */
    private void clearCourseLibraryCache(CourseLibraryDO courseLibraryDO) {
        if (courseLibraryDO != null) {
            if (courseLibraryDO.getCourseLibraryUuid() != null) {
                redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryDO.getCourseLibraryUuid()).delete();
            }
            if (courseLibraryDO.getId() != null) {
                redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_ID + courseLibraryDO.getId()).delete();
            }
        }
        
        // 清理课程列表缓存
        String cacheKey = StringConstant.Redis.COURSE_LIBRARY_LITE_LIST + "all:all:all:all:all";
        redisson.getKeys().delete(cacheKey);
    }
    
    /**
     * 测试根据UUID获取课程库信息
     */
    @Test
    void testGetCourseLibraryByUuid() {
        log.debug("测试获取课程库信息(UUID)");
        String courseLibraryUuid = testCourseLibrary.getCourseLibraryUuid();
        
        // 从缓存中获取课程库信息（首次应从数据库获取并缓存）
        CourseLibraryDO courseData = courseLibraryDAO.getCourseLibraryByUuid(courseLibraryUuid);
        
        // 验证能成功获取数据
        Assertions.assertNotNull(courseData);
        Assertions.assertEquals(testId, courseData.getId());
        Assertions.assertEquals(testName, courseData.getName());
        
        // 验证Redis缓存已创建
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryUuid);
        Assertions.assertTrue(map.isExists());
        
        log.debug("删除缓存并再次获取");
        // 删除Redis缓存
        map.delete();
        
        // 再次获取，这次应该从数据库获取并重新缓存
        CourseLibraryDO courseData2 = courseLibraryDAO.getCourseLibraryByUuid(courseLibraryUuid);
        
        // 验证仍然能获到正确数据
        Assertions.assertNotNull(courseData2);
        Assertions.assertEquals(testId, courseData2.getId());
        
        // 验证Redis缓存已重新创建
        Assertions.assertTrue(map.isExists());
    }

    /**
     * 测试获取不存在的课程库
     */
    @Test
    void testGetNonExistingCourseLibrary() {
        log.debug("测试获取不存在的课程库");
        
        // 使用一个肯定不存在的UUID
        String nonExistingUuid = UuidUtil.generateUuidNoDash();
        
        // 尝试获取不存在的课程库
        CourseLibraryDO courseData = courseLibraryDAO.getCourseLibraryByUuid(nonExistingUuid);
        
        // 应该返回null
        Assertions.assertNull(courseData);
    }

    /**
     * 测试更新课程库信息
     */
    @Test
    void testUpdateCourseLibrary() {
        String originalName = testCourseLibrary.getName();
        String newName = "测试更新课程库" + System.currentTimeMillis();
        
        try {
            // 更新课程库名称
            testCourseLibrary.setName(newName);
            
            // 更新课程库信息
            courseLibraryDAO.updateCourseLibrary(testCourseLibrary);
            
            // 验证Redis缓存已被删除
            RMap<String, String> uuidCache = redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + testCourseLibrary.getCourseLibraryUuid());
            Assertions.assertFalse(uuidCache.isExists());
            
            // 从数据库重新获取，验证更新是否成功
            CourseLibraryDO updatedCourse = courseLibraryDAO.getCourseLibraryByUuid(testCourseLibrary.getCourseLibraryUuid());
            Assertions.assertEquals(newName, updatedCourse.getName());
            
        } finally {
            // 恢复原始数据
            testCourseLibrary.setName(originalName);
            courseLibraryDAO.updateById(testCourseLibrary);
        }
    }
    
    /**
     * 测试更新课程库信息异常情况
     */
    @Test
    void testUpdateCourseLibraryException() {
        // 创建一个不存在的课程库对象
        CourseLibraryDO nonExistingCourse = new CourseLibraryDO();
        nonExistingCourse.setCourseLibraryUuid(UuidUtil.generateUuidNoDash());
        nonExistingCourse.setName("不存在的课程");
        
        // 尝试更新不存在的课程库，应抛出异常
        Assertions.assertThrows(ServerInternalErrorException.class, () -> {
            courseLibraryDAO.updateCourseLibrary(nonExistingCourse);
        });
    }

    /**
     * 测试删除课程库信息
     */
    @Test
    void testDeleteCourseLibrary() {

        CourseTypeDO testCourseType = courseTypeDAO.getCourseTypeList().get(0);  // 从数据库获取一个已存在的课程类型
        DepartmentDO testDepartment = departmentDAO.getDepartmentList().get(0);  //
        // 创建额外的测试课程库数据用于删除测试
        CourseLibraryDO courseToDelete = new CourseLibraryDO();
        courseToDelete.setId("DEL_TEST_" + System.currentTimeMillis());
        courseToDelete.setName("测试删除课程库");
        courseToDelete.setType(testCourseType.getCourseTypeUuid());
        courseToDelete.setDepartment(testDepartment.getDepartmentUuid());
        courseToDelete.setIsEnabled(true);
        courseToDelete.setTotalHours(BigDecimal.valueOf(40));
        courseToDelete.setCredit(BigDecimal.valueOf(2));
        
        // 保存测试数据
        boolean saved = courseLibraryDAO.save(courseToDelete);
        Assertions.assertTrue(saved);
        
        String courseLibraryUuid = courseToDelete.getCourseLibraryUuid();
        
        try {
            // 删除课程库
            courseLibraryDAO.deleteCourseLibrary(courseToDelete);
            
            // 验证Redis缓存已被删除
            RMap<String, String> uuidCache = redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryUuid);
            Assertions.assertFalse(uuidCache.isExists());
            
            // 从数据库验证课程库已被删除
            CourseLibraryDO deletedCourse = courseLibraryDAO.getById(courseLibraryUuid);
            Assertions.assertNull(deletedCourse);
            
        } finally {
            // 确保测试数据被清理
            courseLibraryDAO.removeById(courseLibraryUuid);
        }
    }
    
    /**
     * 测试获取课程库分页信息
     */
    @Test
    void testGetCourseLibraryPage() {
        // 测试不带名称条件的分页查询
        Page<CourseLibraryDO> page1 = courseLibraryDAO.getCourseLibraryPage(1, 10, null);
        Assertions.assertNotNull(page1);
        Assertions.assertFalse(page1.getRecords().isEmpty());
        
        // 测试带具体名称条件的分页查询
        Page<CourseLibraryDO> page2 = courseLibraryDAO.getCourseLibraryPage(1, 10, testName);
        Assertions.assertNotNull(page2);
        Assertions.assertFalse(page2.getRecords().isEmpty());
        Assertions.assertTrue(page2.getRecords().stream().anyMatch(course -> 
                course.getName().equals(testName)));
        
        // 测试一个不存在的名称
        String nonExistingName = "ThisCourseNameShouldNotExist" + System.currentTimeMillis();
        Page<CourseLibraryDO> page3 = courseLibraryDAO.getCourseLibraryPage(1, 10, nonExistingName);
        Assertions.assertNotNull(page3);
        Assertions.assertTrue(page3.getRecords().isEmpty());
    }
    
    /**
     * 测试根据条件获取课程库列表
     */
    @Test
    void testGetCourseLibraryList() {
        // 测试无条件获取所有课程库
        List<CourseLibraryDO> allCourses = courseLibraryDAO.getCourseLibraryList(null, null, null, null, null);
        Assertions.assertNotNull(allCourses);
        Assertions.assertFalse(allCourses.isEmpty());
        
        // 验证Redis缓存已创建
        String cacheKey = StringConstant.Redis.COURSE_LIBRARY_LITE_LIST + "all:all:all:all:all";
        RList<CourseLibraryDO> cachedList = redisson.getList(cacheKey);
        Assertions.assertTrue(cachedList.isExists());
        
        // 清除Redis缓存后再次获取
        redisson.getKeys().delete(cacheKey);
        
        List<CourseLibraryDO> allCoursesAgain = courseLibraryDAO.getCourseLibraryList(null, null, null, null, null);
        Assertions.assertNotNull(allCoursesAgain);
        Assertions.assertFalse(allCoursesAgain.isEmpty());
        
        // 验证Redis缓存已重新创建
        Assertions.assertTrue(cachedList.isExists());
        
        // 测试带类别条件的查询（假设testCourseLibrary有category字段）
        if (testCourseLibrary.getCategory() != null) {
            List<CourseLibraryDO> categoryCourses = courseLibraryDAO.getCourseLibraryList(
                    testCourseLibrary.getCategory(), null, null, null, null);
            Assertions.assertNotNull(categoryCourses);
            Assertions.assertTrue(categoryCourses.stream().allMatch(course -> 
                    testCourseLibrary.getCategory().equals(course.getCategory())));
        }
    }
    
    /**
     * 测试根据ID获取课程库
     */
    @Test
    void testGetCourseLibraryById() {
        String existingId = testCourseLibrary.getId();
        
        log.debug("测试获取课程库信息(ID)");
        CourseLibraryDO courseData = courseLibraryDAO.getCourseLibraryById(existingId);
        
        // 验证能成功获取数据
        Assertions.assertNotNull(courseData);
        Assertions.assertEquals(testName, courseData.getName());
        
        // 验证Redis缓存已创建
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_ID + existingId);
        Assertions.assertTrue(map.isExists());
        
        log.debug("删除缓存并再次获取");
        // 删除Redis缓存
        map.delete();
        
        // 再次获取，这次应该从数据库获取并重新缓存
        CourseLibraryDO courseData2 = courseLibraryDAO.getCourseLibraryById(existingId);
        
        // 验证仍然能获到正确数据
        Assertions.assertNotNull(courseData2);
        Assertions.assertEquals(testName, courseData2.getName());
        
        // 验证Redis缓存已重新创建
        Assertions.assertTrue(map.isExists());
        
        // 测试获取不存在的ID
        String nonExistingId = "NONEXIST_" + System.currentTimeMillis();
        CourseLibraryDO nonExistingCourse = courseLibraryDAO.getCourseLibraryById(nonExistingId);
        Assertions.assertNull(nonExistingCourse);
    }

}