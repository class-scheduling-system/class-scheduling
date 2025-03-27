package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
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
        List<CourseLibraryDTO> courseLibraryDTOList =  new ArrayList<>();
        courseLibraryDTOList.add(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
        Assertions.assertFalse(teacherCourseQualificationService
                .getCourseLibraryAndTeacherCourseQualificationList(courseLibraryDTOList).isEmpty());
        courseLibraryDO.setCourseLibraryUuid(UuidUtil.generateUuidNoDash());
        List<CourseLibraryDTO> courseLibraryDTOList1 =  new ArrayList<>();
        courseLibraryDTOList1.add(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
        Assertions.assertThrows(Exception.class, () -> teacherCourseQualificationService
                .getCourseLibraryAndTeacherCourseQualificationList(courseLibraryDTOList1));
    }
}
