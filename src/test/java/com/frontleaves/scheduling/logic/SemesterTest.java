package com.frontleaves.scheduling.logic;


import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.models.dto.base.SemesterDTO;
import com.frontleaves.scheduling.models.entity.SemesterDO;
import com.frontleaves.scheduling.services.SemesterService;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;

@SpringBootTest
@Slf4j
class SemesterTest {
    @Resource
    private SemesterService semesterService;
    @Resource
    private SemesterDAO semesterDAO;
    @Resource
    private RedissonClient redisson;

    /**
     * 测试根据UUID获取并检查学期是否启用
     * 测试场景：
     * 1. 正常获取已启用的学期
     * 2. 获取未启用的学期时抛出异常
     * 3. 获取不存在的学期时抛出异常
     */
    @Test
    void testGetSemesterByUuidCheckEnabled() {
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

        // 测试场景1：正常获取已启用的学期
        SemesterDTO enabledSemester = semesterService.getSemesterByUuidCheckEnabled(semesterUuid);
        Assertions.assertNotNull(enabledSemester, "获取的学期信息不应为空");
        Assertions.assertEquals(semesterName, enabledSemester.getName(), "学期名称应该匹配");
        Assertions.assertEquals(semesterDescription, enabledSemester.getDescription(), "学期描述应该匹配");
        Assertions.assertEquals(startDate, enabledSemester.getStartDate(), "开始日期应该匹配");
        Assertions.assertEquals(endDate, enabledSemester.getEndDate(), "结束日期应该匹配");
        Assertions.assertEquals(isCurrent, enabledSemester.getIsCurrent(), "是否当前学期应该匹配");
        Assertions.assertEquals(isEnabled, enabledSemester.getIsEnabled(), "是否启用应该匹配");
        semesterDAO.removeById(testSemester.getSemesterUuid());
        redisson.getKeys().delete(StringConstant.Redis.SEMESTER_UUID + semesterUuid);
        // 测试场景2：获取未启用的学期时抛出异常
        testSemester.setIsEnabled(false);
        semesterDAO.save(testSemester);
        Assertions.assertThrows(
                BusinessException.class,
                () -> semesterService.getSemesterByUuidCheckEnabled(semesterUuid)
        );

        // 测试场景3：获取不存在的学期时抛出异常
        String nonExistentUuid = UuidUtil.generateUuidNoDash();
        Assertions.assertThrows(
                BusinessException.class,
                () -> semesterService.getSemesterByUuidCheckEnabled(nonExistentUuid)
        );
        // 清理测试数据
        semesterDAO.removeById(semesterUuid);
    }
}

