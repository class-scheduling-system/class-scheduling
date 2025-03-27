package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;

import java.util.List;

/**
 * 教师课程资格服务接口
 * <p>
 * 该接口定义了处理教师课程资格相关业务的方法。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
public interface TeacherCourseQualificationService {

    List<CourseLibraryAndTeacherCourseQualificationListDTO>
    getCourseLibraryDOAndTeacherCourseQualificationDO(
            List<CourseLibraryDO> courseLibraryDOList
    );
}