package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.models.entity.base.TeacherCourseQualificationDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class TeacherCourseQualificationTest {

    @Resource
    private TeacherCourseQualificationDAO teacherCourseQualificationDAO;

    @Test
    void testGetTeacherCourseQualificationByUuid() {
        TeacherCourseQualificationDO teacherCourseQualificationDO =
                teacherCourseQualificationDAO.lambdaQuery()
                        .eq(TeacherCourseQualificationDO::getStatus, 1)
                        .list()
                        .get(0);
        TeacherCourseQualificationDO teacherCourseQualificationDO1 = teacherCourseQualificationDAO.getTeacherCourseQualificationByUuid(
                teacherCourseQualificationDO.getQualificationUuid());
        Assertions.assertNotNull(teacherCourseQualificationDO1);
    }

    @Test
    void testGetTeacherCourseQualificationByCourseLibraryUuid() {
        TeacherCourseQualificationDO teacherCourseQualificationDO = teacherCourseQualificationDAO.lambdaQuery().list()
                .get(0);
        List<TeacherCourseQualificationDO> teacherCourseQualificationDO1 = teacherCourseQualificationDAO.getTeacherCourseQualificationStatusByCourseLibraryUuid(
                teacherCourseQualificationDO.getCourseUuid());
        Assertions.assertFalse(teacherCourseQualificationDO1.isEmpty());
    }

}
