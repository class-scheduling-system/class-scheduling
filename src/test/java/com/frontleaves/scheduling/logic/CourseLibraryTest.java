package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class CourseLibraryTest {
    @Resource
    private CourseLibraryDAO courseLibraryDAO;
    @Resource
    private CourseLibraryService courseLibraryService;

    @Test
    void testListCourseLibraryByDepartmentAndSpecifyWithThrow() {
        log.debug("测试获取课程库列表");
        // 准备数据
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery().list().get(0);
        // 调用方法
        Assertions.assertFalse(
                courseLibraryService.listCourseLibraryByDepartmentAndSpecifyWithThrow(
                        courseLibraryDO.getDepartment(), null).isEmpty());
        //报错
        String departmentUuid = UuidUtil.generateUuidNoDash();
        Assertions.assertThrows(BusinessException.class,() ->
            courseLibraryService.listCourseLibraryByDepartmentAndSpecifyWithThrow(
                    departmentUuid , null)
        );
    }
}
