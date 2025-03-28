package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 课程库和教师课程资格列表DTO
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseLibraryAndTeacherCourseQualificationListDTO {
    /**
     * 课程库DTO
     **/
    private CourseLibraryDTO courseLibraryDTO;
    /**
     * 教师课程资格列表
     */
    private List<TeacherCoursePreferencesDTO> teacherCoursePreferencesDTOList;
    /**
     * 课程优先级
     */
    private Short courseTypes;
}
