package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.daos.CourseTypeDAO;
import com.frontleaves.scheduling.models.entity.base.CourseTypeDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class CourseTypeTest {

    @Resource
    private CourseTypeDAO courseTypeDAO;


    @Test
    void testGetCourseTypeByUuid (){
        log.debug("测试获取课程类型信息");
        CourseTypeDO courseTypeDO = courseTypeDAO.lambdaQuery().list().get(0);
        CourseTypeDO courseTypeDO1 = courseTypeDAO.getCourseTypeByUuid(courseTypeDO.getCourseTypeUuid());
        Assertions.assertNotNull(courseTypeDO1);
    }
}
