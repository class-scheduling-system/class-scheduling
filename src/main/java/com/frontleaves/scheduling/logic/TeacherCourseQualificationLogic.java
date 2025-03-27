package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.models.dto.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.services.TeacherCourseQualificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教师课程资格业务逻辑实现类
 * <p>
 * 该类实现了TeacherCourseQualificationService接口，提供教师课程资格相关的业务逻辑处理。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherCourseQualificationLogic implements TeacherCourseQualificationService {
    private final TeacherCourseQualificationDAO teacherCourseQualificationDAO;

    @Override
    public List<CourseLibraryAndTeacherCourseQualificationListDTO>
    getCourseLibraryDOAndTeacherCourseQualificationDO(List<CourseLibraryDO> courseLibraryDOList) {

        return List.of();
    }
}