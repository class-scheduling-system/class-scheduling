package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 时间槽 DTO
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class TimeSlotDTO {
    /**
     * 周数
     */
    private Integer week;

    /**
     * 星期几 (1-5)
     */
    private Integer day;

    /**
     * 第几节课 (1-8)
     */
    private Integer period;
}