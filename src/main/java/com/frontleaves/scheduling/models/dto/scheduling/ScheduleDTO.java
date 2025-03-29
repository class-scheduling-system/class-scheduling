package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * 课程表 DTO
 */
@Data
@Accessors(chain = true)
public class ScheduleDTO {
    private Map<TimeSlotDTO, ScheduleItemDTO> assignments = new HashMap<>();
    private double fitness = 0.0;
}