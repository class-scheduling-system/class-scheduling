package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 特定课程ID视图对象
 *
 * @author 26473
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
     * 课程人数可以选择
     */
    private Integer number;
}
