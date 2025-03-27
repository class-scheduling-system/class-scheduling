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
    private CourseLibraryDTO courseLibraryDTO;
    private List<TeacherCoursePreferencesDTO> teacherCoursePreferencesDTOList;
}
