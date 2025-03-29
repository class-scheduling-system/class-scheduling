package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 课程库和班级DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseLibraryAndClassDTO {
    /**
     * 课程库
     */
    private CourseLibraryDTO course;
    /**
     * 班级列表
     */
    private List<AdministrativeClassDTO> classDTOList;
    /**
     * 班级人数
     */
    private Integer number;
}
