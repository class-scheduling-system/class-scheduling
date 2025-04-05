package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 排课任务DTO
 * @author  FLASHLACK
 */
@Data
@Accessors(chain = true)
public class SchedulingTaskDTO {
    /**
     * 排课任务ID
     */
    private String taskId;
    /**
     * 学期ID
     */
    private String semesterId;
    /**
     * 部门ID
     */
    private String departmentId;
    /**
     * 任务状态
     * processing: 处理中
     * completed: 已完成
     * failed: 失败
     */
    private String status;
    /**
     * 预计完成时间(秒)
     */
    private Integer estimatedTime;
    /**
     * 任务创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 创建用户ID
     */
    private String createdBy;
}
