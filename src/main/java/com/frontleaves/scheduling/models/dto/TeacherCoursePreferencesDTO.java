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
    /**
     * 教师信息
     */
    private TeacherDTO teacher;
    /**
     * 教师课程资格
     */
    private TeacherCourseQualificationDTO courseQualification;
    /**
     * 教师课程资格列表
     */
    private TeacherPreferencesDTO teacherPreferencesDTO;
}
