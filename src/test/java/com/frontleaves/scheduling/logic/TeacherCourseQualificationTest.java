package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.models.entity.base.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.base.TeacherCourseQualificationDO;
import com.frontleaves.scheduling.services.TeacherCourseQualificationService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class TeacherCourseQualificationTest {
    @Resource
    private TeacherCourseQualificationService teacherCourseQualificationService;
    @Resource
    private CourseLibraryDAO courseLibraryDAO;
    @Resource
    private TeacherCourseQualificationDAO teacherCourseQualificationDAO;


    /**
     * 测试获取课程库和教师课程资格列表的功能
     * 此测试用例旨在验证教师课程资格与课程库关联的正确性
     */
    @Test
    void testGetCourseLibraryAndTeacherCourseQualificationList (){
        // 获取第一个教师课程资格对象
        TeacherCourseQualificationDO teacherCourseQualificationDO = teacherCourseQualificationDAO.lambdaQuery().list().get(0);
        // 根据课程UUID获取对应的课程库对象
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery().eq(CourseLibraryDO::getCourseLibraryUuid,
                teacherCourseQualificationDO.getCourseUuid()).one();

    }
}
