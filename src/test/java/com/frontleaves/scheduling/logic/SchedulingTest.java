package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.services.SchedulingService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 调度逻辑测试类
 *
 * @author FLASHLACK
 */
@Slf4j
@SpringBootTest
class SchedulingTest {

    @Resource
    private SchedulingService schedulingService;
    @Resource
    private SemesterDAO semesterDAO;


    @Test
    void testGetAutoClassSchedulingBaseDTO() {

    }
}