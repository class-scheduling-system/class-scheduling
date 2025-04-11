package com.frontleaves.scheduling.services.scheduling;

import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleDTO;

import java.util.List;

/**
 * 遗传算法评估种群服务接口
 * @author FLASHLACK
 */
public interface EvaluatePopulationService {
    /**
     * 评估种群
     * @param allPopulation 所有种群
     * @param baseDTO 排课基础数据
     */
    void evaluatePopulation(
            List<ScheduleDTO> allPopulation,
            AutomaticClassSchedulingBaseDTO baseDTO);
}
