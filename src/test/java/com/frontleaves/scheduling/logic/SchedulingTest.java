package com.frontleaves.scheduling.logic;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
    private SchedulingLogic schedulingLogic;

}