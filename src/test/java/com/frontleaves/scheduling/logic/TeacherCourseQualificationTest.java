package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.TeacherCourseQualificationDO;
import com.frontleaves.scheduling.services.TeacherCourseQualificationService;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@Slf4j
class TeacherCourseQualificationTest {
    @Resource
    private TeacherCourseQualificationService teacherCourseQualificationService;
    @Resource
    private CourseLibraryDAO courseLibraryDAO;
    @Resource
    private TeacherCourseQualificationDAO teacherCourseQualificationDAO;


    @Test
    void testGetCourseLibraryAndTeacherCourseQualificationList (){
        TeacherCourseQualificationDO teacherCourseQualificationDO = teacherCourseQualificationDAO.lambdaQuery().list().get(0);
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery().eq(CourseLibraryDO::getCourseLibraryUuid,
                teacherCourseQualificationDO.getCourseUuid()).one();
        List<CourseLibraryDO> courseLibraryDOList =  new ArrayList<>();
        courseLibraryDOList.add(courseLibraryDO);
        Assertions.assertFalse(teacherCourseQualificationService
                .getCourseLibraryAndTeacherCourseQualificationList(courseLibraryDOList).isEmpty());
        courseLibraryDO.setCourseLibraryUuid(UuidUtil.generateUuidNoDash());
        List<CourseLibraryDO> courseLibraryDOList1 =  new ArrayList<>();
        courseLibraryDOList1.add(courseLibraryDO);
        Assertions.assertThrows(Exception.class, () -> teacherCourseQualificationService
                .getCourseLibraryAndTeacherCourseQualificationList(courseLibraryDOList1));
    }
}
