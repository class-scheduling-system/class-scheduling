package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 排课数据传输对象
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class ScheduleDTO {
    /**
     * 用于存放系统课程安排
     */
    private List<CourseScheduleDTO> schedule;
    /**
     * 用于存放数据库内已经排好的课程
     */
    private List<CourseScheduleDTO> data;
    /**
     * 用于存放课程安排的适应度
     */
    private double fitness = 0.0;
}
