package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.Map;

/**
 * 排课冲突实体类
 * <p>
 * 对应数据库表：`cs_scheduling_conflict`
 * 本类用于封装排课冲突的详细信息，主键为 conflict_uuid，采用 UUID 自动生成。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName(value = "cs_scheduling_conflict", autoResultMap = true)
@NoArgsConstructor
@Accessors(chain = true)
public class SchedulingConflictDO {
    /**
     * 冲突主键，采用 UUID 自动生成
     */
    @TableId(value = "conflict_uuid", type = IdType.ASSIGN_UUID)
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
    @TableField(typeHandler = JacksonTypeHandler.class)
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
    private Timestamp resolvedAt;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}