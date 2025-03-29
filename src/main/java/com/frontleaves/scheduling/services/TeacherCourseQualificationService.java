package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CourseLibraryAndTeacherCourseQualificationListDTO;

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
    /**
     * 获取课程库和教师课程资格的所有关联信息
     *
     * @param courseLibraryDOList 课程库数据对象列表
     * @return 包含课程库和教师课程资格信息的DTO列表
     */
    List<CourseLibraryAndTeacherCourseQualificationListDTO>
    getCourseLibraryAndTeacherCourseQualificationList(
            List<CourseLibraryAndTeacherCourseQualificationListDTO> CourseLibraryAndClassDTOList,
            Boolean isTeacherPreferences
    );
}
