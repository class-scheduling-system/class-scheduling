package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
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
     * 课程库
     */
    private CourseLibraryDTO course;
    /**
     * 班级列表
     */
    private List<AdministrativeClassDTO> classList;
    /**
     * 班级人数
     */
    private Integer number;
    /**
     * 对应教师课程资格
     */
    private List<TeacherCoursePreferencesDTO> teacherList;
    /**
     * 课程优先级
     */
    private Short courseTypes;
}
