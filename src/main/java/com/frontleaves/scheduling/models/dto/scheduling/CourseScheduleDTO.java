package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程课程表 DTO 只是单个课的课程表
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseScheduleDTO {
    private Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = new HashMap<>();
    private double fitness = 0.0;
}