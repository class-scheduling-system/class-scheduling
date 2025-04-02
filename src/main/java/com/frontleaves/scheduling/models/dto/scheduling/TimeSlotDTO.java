package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * 时间槽 DTO
 */
@Data
@Accessors(chain = true)
public class TimeSlotDTO {
    /**
     * 周数
     */
    private int week;
    
    /**
     * 星期几 (1-5)
     */
    private int day;
    
    /**
     * 第几节课 (1-8)
     */
    private int period;
    
    /**
     * 是否为单周
     * true = 单周
     * false = 双周
     * null = 不区分单双周
     */
    private Boolean isOddWeek;

    public TimeSlotDTO() {
    }

    public TimeSlotDTO(int week, int day, int period) {
        this.week = week;
        this.day = day;
        this.period = period;
        // 默认不区分单双周
        this.isOddWeek = null;
    }
    
    public TimeSlotDTO(int week, int day, int period, Boolean isOddWeek) {
        this.week = week;
        this.day = day;
        this.period = period;
        this.isOddWeek = isOddWeek;
    }

    public TimeSlotDTO(@NotNull TimeSlotDTO other) {
        this.week = other.week;
        this.day = other.day;
        this.period = other.period;
        this.isOddWeek = other.isOddWeek;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TimeSlotDTO that = (TimeSlotDTO) o;
        return week == that.week &&
               day == that.day &&
               period == that.period &&
               (Objects.equals(isOddWeek, that.isOddWeek));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + week;
        result = 31 * result + day;
        result = 31 * result + period;
        result = 31 * result + (isOddWeek != null ? isOddWeek.hashCode() : 0);
        return result;
    }
}