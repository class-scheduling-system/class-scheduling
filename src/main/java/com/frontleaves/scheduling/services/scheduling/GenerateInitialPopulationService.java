package com.frontleaves.scheduling.services.scheduling;

import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleDTO;

import java.util.List;

/**
 * 初始化接口
 * @author FLASHLACK
 */
public interface GenerateInitialPopulationService {
    /**
     * 生成初始种群
     * @param baseDTO 基础数据传输对象
     * @return 初始种群列表
     */
    List<ScheduleDTO> generateInitialPopulation(
            AutomaticClassSchedulingBaseDTO baseDTO);
}
