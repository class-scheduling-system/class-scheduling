package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Objects;

/**
 * 时间槽 DTO
 */
@Data
@Accessors(chain = true)
public class TimeSlotDTO {
    private final int week;
    private final int dayOfWeek;
    private final int period;
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlotDTO timeSlot = (TimeSlotDTO) o;
        return week == timeSlot.week &&
                dayOfWeek == timeSlot.dayOfWeek &&
                period == timeSlot.period;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(week, dayOfWeek, period);
    }
}