package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;


/**
 * DTO类，用于表示课程时间安排
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class ClassTimeDTO {
    /**
     * 星期几 (例如，1代表周一, 2代表周二)
     */
    private Integer dayOfWeek;
    /**
     * 开始节次
     */
    private Integer periodStart;
    /**
     * 结束节次
     */
    private Integer periodEnd;
    /**
     * 生效周列表
     */
    private List<Integer> weekNumbers;
}
