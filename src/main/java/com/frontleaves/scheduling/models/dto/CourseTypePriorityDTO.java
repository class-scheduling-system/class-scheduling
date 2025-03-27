package com.frontleaves.scheduling.models.dto;


import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 课程类型优先级DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseTypePriorityDTO {
    private CourseTypeDTO courseTypeDTO;
    private short priority;
}
