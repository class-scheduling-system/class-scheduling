package com.frontleaves.scheduling.models.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 调整课程表的VO类
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentDetailsVO {
    /**
     * 可选，新教室ID
     */
    private String classroomId;

    /**
     * 可选，新教师ID
     */
    private String teacherId;

    /**
     * 可选，新时间安排
     */
    private List<ClassTimeVO> classTime;

    /**
     * 可选，连堂节数
     */
    private Integer consecutiveSessions;

    /**
     * 可选，排课优先级
     */
    private Integer schedulingPriority;
}