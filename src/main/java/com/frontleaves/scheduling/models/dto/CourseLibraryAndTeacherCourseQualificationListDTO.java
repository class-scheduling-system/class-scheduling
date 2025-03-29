package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 课程库和教师课程资格列表DTO
 * 内有课程库和班级DTO
 * 内有教师课程资格列表
 * 内有优先级
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseLibraryAndTeacherCourseQualificationListDTO {
    /**
     * 课程库已经学生DTO
     **/
    private CourseLibraryAndClassDTO libraryAndClass;
    /**
     * 教师课程资格列表
     */
    private List<TeacherCoursePreferencesDTO> teacherCoursePreferencesDTOList;
    /**
     * 课程优先级
     */
    private Short courseTypes;
}
