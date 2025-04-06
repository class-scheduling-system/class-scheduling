package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.models.entity.base.SemesterDO;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.time.Duration;

/**
 * 学期数据访问对象测试类
 *
 * @author FLASHLACK
 */
@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class SemesterTest {

    @Resource
    private SemesterDAO semesterDAO;

    @Resource
    private RedissonClient redisson;

    /**
     * 测试根据UUID获取学期信息
     * 测试场景：
     * 1. 从Redis缓存中获取数据
     * 2. 从数据库中获取数据并更新缓存
     * 3. 数据不存在时返回null
     */
    @Test
    void testGetSemesterByUuid() {
        // 准备测试数据
        String semesterUuid = UuidUtil.generateUuidNoDash();
        String semesterName = "测试学期";
        String semesterDescription = "这是一个测试学期";
        Timestamp startDate = Timestamp.valueOf("2024-02-26 00:00:00");
        Timestamp endDate = Timestamp.valueOf("2024-07-12 23:59:59");
        Boolean isCurrent = true;
        Boolean isEnabled = true;

        // 创建测试数据
        SemesterDO testSemester = new SemesterDO();
        testSemester.setSemesterUuid(semesterUuid);
        testSemester.setName(semesterName);
        testSemester.setDescription(semesterDescription);
        testSemester.setStartDate(startDate);
        testSemester.setEndDate(endDate);
        testSemester.setIsCurrent(isCurrent);
        testSemester.setIsEnabled(isEnabled);

        // 保存测试数据到数据库
        semesterDAO.save(testSemester);

        // 测试场景1：从Redis缓存中获取数据
        // 先将数据存入Redis缓存
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.SEMESTER_UUID + semesterUuid);
        rMap.putAll(ConvertUtil.convertObjectToMapString(testSemester));
        rMap.expire(Duration.ofSeconds(86400));

        // 从缓存中获取数据
        SemesterDO cachedSemester = semesterDAO.getSemesterByUuid(semesterUuid);
        Assertions.assertNotNull(cachedSemester, "从缓存中获取的学期信息不应为空");
        Assertions.assertEquals(semesterName, cachedSemester.getName(), "学期名称应该匹配");
        Assertions.assertEquals(semesterDescription, cachedSemester.getDescription(), "学期描述应该匹配");
        Assertions.assertEquals(startDate, cachedSemester.getStartDate(), "开始日期应该匹配");
        Assertions.assertEquals(endDate, cachedSemester.getEndDate(), "结束日期应该匹配");
        Assertions.assertEquals(isCurrent, cachedSemester.getIsCurrent(), "是否当前学期应该匹配");
        Assertions.assertEquals(isEnabled, cachedSemester.getIsEnabled(), "是否启用应该匹配");

        // 测试场景2：从数据库中获取数据并更新缓存
        // 删除Redis缓存
        rMap.delete();
        // 从数据库获取数据
        SemesterDO dbSemester = semesterDAO.getSemesterByUuid(semesterUuid);
        Assertions.assertNotNull(dbSemester, "从数据库获取的学期信息不应为空");
        Assertions.assertEquals(semesterName, dbSemester.getName(), "学期名称应该匹配");
        Assertions.assertEquals(semesterDescription, dbSemester.getDescription(), "学期描述应该匹配");
        Assertions.assertEquals(startDate, dbSemester.getStartDate(), "开始日期应该匹配");
        Assertions.assertEquals(endDate, dbSemester.getEndDate(), "结束日期应该匹配");
        Assertions.assertEquals(isCurrent, dbSemester.getIsCurrent(), "是否当前学期应该匹配");
        Assertions.assertEquals(isEnabled, dbSemester.getIsEnabled(), "是否启用应该匹配");

        // 验证数据已被缓存
        Assertions.assertTrue(rMap.isExists(), "数据应该已被缓存");

        // 测试场景3：数据不存在时返回null
        String nonExistentUuid = UuidUtil.generateUuidNoDash();
        SemesterDO nonExistentSemester = semesterDAO.getSemesterByUuid(nonExistentUuid);
        Assertions.assertNull(nonExistentSemester, "不存在的学期UUID应该返回null");

        // 清理测试数据
        semesterDAO.removeById(semesterUuid);
        rMap.delete();
    }
}
