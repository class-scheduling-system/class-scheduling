package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
class CourseLibraryTest {

    @Resource
    private CourseLibraryDAO courseLibraryDAO;


    @Test
    void testGetListCourseLibraryByDepartmentAndSpecify() {
        //准备数据
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery().list().get(0);
        //执行
        log.debug("只获取这个部门的课程");
        List<CourseLibraryDO> courseLibraryDOS =
                courseLibraryDAO.getListCourseLibraryByDepartmentAndSpecify(
                        courseLibraryDO.getDepartment(), null);
        Assertions.assertFalse(courseLibraryDOS.isEmpty());
        log.debug("测试获取这一个课程");
        List<CourseLibraryDO> courseLibraryDOS1 = courseLibraryDAO.getListCourseLibraryByDepartmentAndSpecify(courseLibraryDO.getDepartment(),
                List.of(courseLibraryDO.getCourseLibraryUuid()));
        Assertions.assertFalse(courseLibraryDOS1.isEmpty());
    }
}
