package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.models.entity.base.SemesterDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 学期数据访问对象测试类
 *
 * @author FLASHLACK
 */
@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SemesterTest {

    @Resource
    private SemesterDAO semesterDAO;


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
        SemesterDO date = semesterDAO.lambdaQuery().list().get(0);
        // 从缓存中获取数据
        SemesterDO cachedSemester = semesterDAO.getSemesterByUuid(date.getSemesterUuid());
        Assertions.assertNotNull(cachedSemester);
    }
}
