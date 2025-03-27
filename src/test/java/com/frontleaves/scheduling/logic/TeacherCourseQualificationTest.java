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
        // 初始化课程库DTO列表，并将获取的课程库对象转换为DTO后添加到列表中
        List<CourseLibraryDTO> courseLibraryDTOList =  new ArrayList<>();
        courseLibraryDTOList.add(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
        // 断言：调用服务方法后不应返回空列表
        Assertions.assertFalse(teacherCourseQualificationService
                .getCourseLibraryAndTeacherCourseQualificationList(courseLibraryDTOList,false)
                .isEmpty());
        // 修改课程库对象的UUID，用于后续的异常测试
        courseLibraryDO.setCourseLibraryUuid(UuidUtil.generateUuidNoDash());
        // 初始化新的课程库DTO列表，并将修改后的课程库对象转换为DTO后添加到列表中
        List<CourseLibraryDTO> courseLibraryDTOList1 =  new ArrayList<>();
        courseLibraryDTOList1.add(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
        // 断言：调用服务方法后应抛出异常，因为课程库UUID已被修改，不再匹配任何教师课程资格
        Assertions.assertThrows(Exception.class, () -> teacherCourseQualificationService
                .getCourseLibraryAndTeacherCourseQualificationList(courseLibraryDTOList1,false));
    }
}
