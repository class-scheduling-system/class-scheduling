package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
/**
 * 后台自动排课DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class BackAutomaticClassSchedulingDTO {
    /**
     * 排课任务ID
     */
    private String taskId;
    /**
     * 学期ID
     */
    private String semesterId;
    /**
     * 部门ID，若有
     */
    private String departmentId;
    /**
     * 任务状态: processing, completed, failed
     */
    private String status;
    /**
     * 预计完成时间(秒)
     */
    private Integer estimatedTime;
    /**
     * 任务创建时间
     */
    private Timestamp createdAt;
    /**
     * 创建用户ID-UserUuid
     */
    private String createdBy;
}