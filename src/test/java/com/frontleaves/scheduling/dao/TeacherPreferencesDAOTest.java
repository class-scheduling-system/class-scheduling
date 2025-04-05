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
import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.TeacherPreferencesDAO;
import com.frontleaves.scheduling.models.entity.SemesterDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.TeacherPreferencesDO;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class TeacherPreferencesDAOTest {

    @Resource
    private TeacherPreferencesDAO teacherPreferencesDAO;

    @Resource
    private TeacherDAO teacherDAO;

    @Resource
    private SemesterDAO semesterDAO;

    @Resource
    private RedissonClient redisson;

    private TeacherPreferencesDO setupPreference;
    private TeacherDO testTeacher;
    private SemesterDO testSemester;

    @BeforeEach
    @Transactional
    void setUp() {
        log.debug("TeacherPreferencesDAO单元测试初始化");

        // 获取实际存在的教师数据
        List<TeacherDO> existingTeachers = teacherDAO.list();
        if (existingTeachers.isEmpty()) {
            throw new RuntimeException("数据库中不存在教师数据，无法完成测试");
        }
        testTeacher = existingTeachers.get(0);

        // 获取实际存在的学期数据
        List<SemesterDO> existingSemesters = semesterDAO.list();
        if (existingSemesters.isEmpty()) {
            throw new RuntimeException("数据库中不存在学期数据，无法完成测试");
        }
        testSemester = existingSemesters.get(0);

        // 清理可能存在的测试数据
        teacherPreferencesDAO.lambdaUpdate()
                .eq(TeacherPreferencesDO::getTeacherUuid, testTeacher.getTeacherUuid())
                .eq(TeacherPreferencesDO::getSemesterUuid, testSemester.getSemesterUuid())
                .eq(TeacherPreferencesDO::getDayOfWeek, 1)
                .eq(TeacherPreferencesDO::getTimeSlot, 2)
                .remove();

        // 创建测试数据
        setupPreference = new TeacherPreferencesDO();
        setupPreference.setPreferenceUuid(UuidUtil.generateUuidNoDash())
                .setTeacherUuid(testTeacher.getTeacherUuid())
                .setSemesterUuid(testSemester.getSemesterUuid())
                .setDayOfWeek(1) // 周一
                .setTimeSlot(2) // 第二节课
                .setPreferenceLevel(5) // 喜欢
                .setReason("测试原因");

        // 保存到数据库
        teacherPreferencesDAO.save(setupPreference);

        log.debug("测试数据初始化成功，教师ID: {}, 学期ID: {}, 偏好ID: {}",
                testTeacher.getTeacherUuid(),
                testSemester.getSemesterUuid(),
                setupPreference.getPreferenceUuid());
    }

    @AfterEach
    @Transactional
    void tearDown() {
        log.debug("TeacherPreferencesDAO单元测试结束");

        // 清理测试数据和缓存
        if (setupPreference != null) {
            teacherPreferencesDAO.lambdaUpdate()
                    .eq(TeacherPreferencesDO::getPreferenceUuid, setupPreference.getPreferenceUuid())
                    .remove();

            redisson.getMap(StringConstant.Redis.TEACHER_PREFERENCES_UUID + setupPreference.getPreferenceUuid()).delete();
            redisson.getKeys().deleteByPattern(StringConstant.Redis.TEACHER_PREFERENCES_LIST + testTeacher.getTeacherUuid() + "*");
            redisson.getKeys().deleteByPattern(StringConstant.Redis.TEACHER_PREFERENCES_PAGE + "*");
        }
    }

    /**
     * 测试根据UUID获取教师课程偏好
     */
    @Test
    void testGetTeacherPreferencesByUuid() {
        log.debug("测试根据UUID获取教师课程偏好");

        // 从数据库获取偏好
        TeacherPreferencesDO preference = teacherPreferencesDAO.getTeacherPreferencesByUuid(setupPreference.getPreferenceUuid());

        // 验证获取到的数据不为空
        Assertions.assertNotNull(preference);
        Assertions.assertEquals(setupPreference.getPreferenceUuid(), preference.getPreferenceUuid());
        Assertions.assertEquals(testTeacher.getTeacherUuid(), preference.getTeacherUuid());
        Assertions.assertEquals(testSemester.getSemesterUuid(), preference.getSemesterUuid());

        log.debug("删除缓存并再次获取");
        // 删除Redis缓存
        redisson.getMap(StringConstant.Redis.TEACHER_PREFERENCES_UUID + setupPreference.getPreferenceUuid()).delete();

        // 再次获取，这次应该从数据库获取并重新缓存
        TeacherPreferencesDO preference2 = teacherPreferencesDAO.getTeacherPreferencesByUuid(setupPreference.getPreferenceUuid());

        // 验证仍然能获取到数据
        Assertions.assertNotNull(preference2);
        Assertions.assertEquals(setupPreference.getPreferenceUuid(), preference2.getPreferenceUuid());
    }

    /**
     * 测试获取不存在的教师课程偏好
     */
    @Test
    void testGetNonExistingPreference() {
        log.debug("测试获取不存在的教师课程偏好");

        // 使用一个肯定不存在的UUID
        String nonExistingUuid = UuidUtil.generateUuidNoDash();

        // 尝试获取不存在的偏好
        TeacherPreferencesDO preference = teacherPreferencesDAO.getTeacherPreferencesByUuid(nonExistingUuid);

        // 应该返回null
        Assertions.assertNull(preference);
    }

    /**
     * 测试根据教师UUID和学期UUID获取教师课程偏好列表
     */
    @Test
    void testGetTeacherPreferencesByTeacherAndSemester() {
        log.debug("测试根据教师UUID和学期UUID获取教师课程偏好列表");

        // 获取教师偏好列表
        List<TeacherPreferencesDO> preferences = teacherPreferencesDAO.getTeacherPreferencesByTeacherAndSemester(
                testTeacher.getTeacherUuid(),
                testSemester.getSemesterUuid()
        );

        // 验证列表不为空并且包含测试数据
        Assertions.assertNotNull(preferences);
        Assertions.assertFalse(preferences.isEmpty());

        // 查找我们创建的测试偏好是否在列表中
        boolean found = preferences.stream()
                .anyMatch(p -> p.getPreferenceUuid().equals(setupPreference.getPreferenceUuid()));
        Assertions.assertTrue(found, "未找到测试创建的偏好记录");

        log.debug("删除缓存并再次获取");
        // 删除Redis缓存
        String cacheKey = StringConstant.Redis.TEACHER_PREFERENCES_LIST + testTeacher.getTeacherUuid() + ":" + testSemester.getSemesterUuid();
        redisson.getList(cacheKey).delete();

        // 再次获取，这次应该从数据库获取并重新缓存
        List<TeacherPreferencesDO> preferences2 = teacherPreferencesDAO.getTeacherPreferencesByTeacherAndSemester(
                testTeacher.getTeacherUuid(),
                testSemester.getSemesterUuid()
        );

        // 验证仍然能获取到数据
        Assertions.assertNotNull(preferences2);
        Assertions.assertFalse(preferences2.isEmpty());
    }

    /**
     * 测试分页获取教师课程偏好
     */
    @Test
    void testGetTeacherPreferencesPage() {
        log.debug("测试分页获取教师课程偏好");

        // 获取分页数据
        Page<TeacherPreferencesDO> page = teacherPreferencesDAO.getTeacherPreferencesPage(
                1, 10, false,
                testTeacher.getTeacherUuid(),
                testSemester.getSemesterUuid()
        );

        // 验证分页数据不为空
        Assertions.assertNotNull(page);
        Assertions.assertTrue(page.getTotal() > 0);

        // 查找我们创建的测试偏好是否在分页中
        boolean found = page.getRecords().stream()
                .anyMatch(p -> p.getPreferenceUuid().equals(setupPreference.getPreferenceUuid()));
        Assertions.assertTrue(found, "未找到测试创建的偏好记录");

        log.debug("删除缓存并再次获取");
        // 删除Redis缓存
        String cacheKey = StringConstant.Redis.TEACHER_PREFERENCES_PAGE + "1:10:false:" +
                testTeacher.getTeacherUuid() + ":" + testSemester.getSemesterUuid();
        redisson.getMap(cacheKey).delete();

        // 再次获取，这次应该从数据库获取并重新缓存
        Page<TeacherPreferencesDO> page2 = teacherPreferencesDAO.getTeacherPreferencesPage(
                1, 10, false,
                testTeacher.getTeacherUuid(),
                testSemester.getSemesterUuid()
        );

        // 验证仍然能获取到数据
        Assertions.assertNotNull(page2);
        Assertions.assertTrue(page2.getTotal() > 0);
    }

    /**
     * 测试更新教师课程偏好
     */
    @Test
    void testUpdateTeacherPreferences() {
        log.debug("测试更新教师课程偏好");

        // 修改偏好信息
        setupPreference.setPreferenceLevel(1);  // 修改为非常不喜欢
        setupPreference.setReason("测试更新原因");

        // 更新数据
        teacherPreferencesDAO.updateTeacherPreferences(setupPreference);

        // 从数据库重新获取
        TeacherPreferencesDO updatedPreference = teacherPreferencesDAO.getById(setupPreference.getPreferenceUuid());

        // 验证更新是否成功
        Assertions.assertNotNull(updatedPreference);
        Assertions.assertEquals(1, updatedPreference.getPreferenceLevel());
        Assertions.assertEquals("测试更新原因", updatedPreference.getReason());

        // 验证缓存是否被清除
        RMap<String, String> uuidCache = redisson.getMap(StringConstant.Redis.TEACHER_PREFERENCES_UUID + setupPreference.getPreferenceUuid());
        Assertions.assertFalse(uuidCache.isExists());
    }

    /**
     * 测试删除教师课程偏好
     */
    @Test
    void testDeleteTeacherPreference() {
        log.debug("测试删除教师课程偏好");

        // 删除偏好
        boolean result = teacherPreferencesDAO.deleteTeacherPreference(setupPreference);

        // 验证删除成功
        Assertions.assertTrue(result);

        // 尝试从数据库获取已删除的数据
        TeacherPreferencesDO deletedPreference = teacherPreferencesDAO.getById(setupPreference.getPreferenceUuid());

        // 验证数据已被删除
        Assertions.assertNull(deletedPreference);

        // 验证缓存是否被清除
        RMap<String, String> uuidCache = redisson.getMap(StringConstant.Redis.TEACHER_PREFERENCES_UUID + setupPreference.getPreferenceUuid());
        Assertions.assertFalse(uuidCache.isExists());

        // 为防止tearDown方法报错，将setupPreference设为null
        setupPreference = null;
    }
}
