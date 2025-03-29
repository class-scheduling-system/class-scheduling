package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.Map;

/**
 * 排课冲突数据传输对象
 * <p>
 * 用于在不同层之间传输排课冲突信息。该 DTO 包含冲突主键、学期主键、排课主键、
 * 冲突类型、冲突时间、冲突描述、解决状态、解决方法、解决备注、解决人、解决时间、
 * 创建时间及更新时间等字段。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SchedulingConflictDTO {
    /**
     * 冲突主键
     */
    private String conflictUuid;

    /**
     * 学期主键
     */
    private String semesterUuid;

    /**
     * 第一个排课主键
     */
    private String firstAssignmentUuid;

    /**
     * 第二个排课主键
     */
    private String secondAssignmentUuid;

    /**
     * 冲突类型: 1-教师冲突 2-教室冲突 3-班级冲突 4-其他冲突
     */
    private Integer conflictType;

    /**
     * 冲突时间
     */
    private Map<String, Object> conflictTime;

    /**
     * 冲突描述
     */
    private String description;

    /**
     * 解决状态: 0-未解决 1-已解决 2-忽略
     */
    private Integer resolutionStatus;

    /**
     * 解决方法: 1-调整第一个课程 2-调整第二个课程 3-同时调整 4-其他
     */
    private Integer resolutionMethod;

    /**
     * 解决备注
     */
    private String resolutionNotes;

    /**
     * 解决人
     */
    private String resolvedBy;

    /**
     * 解决时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp resolvedAt;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}