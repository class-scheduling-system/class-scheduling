package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CourseTypeDAO;
import com.frontleaves.scheduling.models.dto.CourseTypeDTO;
import com.frontleaves.scheduling.models.entity.CourseTypeDO;
import com.frontleaves.scheduling.services.CourseTypeService;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class CourseTypeTest {
    @Resource
    private CourseTypeService courseTypeService;
    @Resource
    private CourseTypeDAO courseTypeDAO;


    @Test
    void testGetCourseTypeByUuidWithError() {
        log.debug("测试获取课程类型信息");
        CourseTypeDO courseTypeDO = courseTypeDAO.lambdaQuery().list().get(0);
        CourseTypeDTO courseTypeDTO = courseTypeService.getCourseTypeByUuidWithError(courseTypeDO.getCourseTypeUuid());
        Assertions.assertNotNull(courseTypeDTO);
        String uuid = UuidUtil.generateUuidNoDash();
        Assertions.assertThrows(BusinessException.class, () -> courseTypeService.getCourseTypeByUuidWithError(uuid));

    }
}
