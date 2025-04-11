package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 排课任务状态数据传输对象
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class SchedulingTaskStatusDTO {
    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 学期ID
     * <p>关联学期表的唯一标识</p>
     */
    private String semesterId;

    /**
     * 院系ID
     * <p>关联院系表的唯一标识</p>
     */
    private String departmentId;


    /**
     * 任务状态
     * <p>可选值：</p>
     * <ul>
     *   <li>processing - 处理中</li>
     *   <li>completed - 已完成</li>
     *   <li>failed - 已失败</li>
     * </ul>
     */
    private String status;

    /**
     * 完成进度百分比
     * <p>取值范围：0-100整数</p>
     * <p>示例：75（表示75%）</p>
     */
    private Integer progress;
    /**
     * 预计剩余时间(秒)
     */
    private Integer estimatedTimeRemaining;

    /**
     * 开始时间
     */
    private Timestamp startTime;

    /**
     * 冲突统计信息
     * 包含教师、教室、班级的冲突数量
     */
    private ConflictsCount conflictsCount;
    /**
     * 当前处理阶段
     * 可能值：
     * - "initial_scheduling"：初始排课
     * - "conflict_resolution"：冲突解决
     * - "optimization"：优化处理
     * - "finalization"：最终确认
     */
    private String processingStage;

    /**
     * 当前状态详细信息
     * 示例："正在执行遗传算法第150代迭代..."
     */
    private String message;

    /**
     * 冲突统计详情
     */
    @Data
    @Accessors(chain = true)
    public static class ConflictsCount {
        /**
         * 教师时间冲突次数
         */
        private Integer teacher;

        /**
         * 教室资源冲突次数
         */
        private Integer classroom;

        /**
         * 班级课程冲突次数
         * （使用clazz避免Java关键字冲突）
         */
        private Integer clazz;
    }
}
