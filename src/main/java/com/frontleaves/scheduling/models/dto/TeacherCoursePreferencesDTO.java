package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 教师课程偏好DTO
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class TeacherCoursePreferencesDTO {
    private TeacherDTO teacher;
    private TeacherCourseQualificationDTO courseQualification;
    private TeacherPreferencesDTO teacherPreferencesDTO;
}
