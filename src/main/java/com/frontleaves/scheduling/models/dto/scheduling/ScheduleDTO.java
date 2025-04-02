package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程表 DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class ScheduleDTO {
    private Map<List<TimeSlotDTO>, ScheduleItemDTO> assignments = new HashMap<>();
    private double fitness = 0.0;
}