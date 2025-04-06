package com.frontleaves.scheduling.services.scheduling;


import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;

/**
 * 遗传算法排课服务接口
 * @author FLASHLACK
 */
public interface GeneticSchedulingService {
    /**
     * 执行遗传算法进行排课
     *
     * @param taskId 任务ID
     * @param baseDTO 排课基础数据
     * @return 排课结果
     */
    ScheduleResultDTO executeGeneticAlgorithm(String taskId, AutomaticClassSchedulingBaseDTO baseDTO);

    /**
     * 获取排课进度
     *
     * @param taskId 任务ID
     * @return 进度百分比（0-100）
     */
    int getSchedulingProgress(String taskId);

    /**
     * 获取排课状态
     *
     * @param taskId 任务ID
     * @return 状态描述
     */
    String getSchedulingStatus(String taskId);
}
