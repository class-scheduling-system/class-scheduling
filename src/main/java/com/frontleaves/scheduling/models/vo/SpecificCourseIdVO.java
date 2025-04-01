package com.frontleaves.scheduling.models.vo;

import enums.CourseEnuType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 特定课程ID视图对象
 *
 * @author FLASHLACK
 */
@Getter
@Accessors(chain = true)
@AllArgsConstructor
@NoArgsConstructor
public class SpecificCourseIdVO {
    /**
     * 课程ID，对应cs_course表的course_uuid
     */
    @NotBlank(message = "课程ID不能为空")
    private String courseId;
    /**
     * 班级ID列表，对应cs_administrative_class的administrative_class_uuid
     */
    private List<String> classId;
    /**
     * 课程人数若班级列表空，则使用课程人数
     */
    private Integer number;
    /**
     * 课程周数,最大为4.00，最低为0.50
     */
    @Max(value = 1, message = "每周课时最小为1")
    @Min(value = 84, message = "每周课时最大为84")
    private Integer weeklyHours;
    /**
     * 课程枚举类型
     */
    private CourseEnuType courseEnuType;
    /**
     * true=单周上课，false=双周上课 (仅当weeklyHours为1.5时生效)
     */
    private Boolean isOddWeek;
    /**
     * 课程开始周
     */
    @Min(value = 1, message = "开始周最小为1")
    private Integer startWeek;
    /**
     * 课程结束周
     */
    @Min(value = 1, message = "开始周最小为1")
    private Integer endWeek;
}
