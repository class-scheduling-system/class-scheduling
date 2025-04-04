package com.frontleaves.scheduling.services.scheduling;

import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleDTO;

import java.util.List;

public interface IterateService {
    /**
     * 选择操作
     *
     * @param allPopulation 所有种群
     * @return 选择后的种群
     */
    List<ScheduleDTO> selection(
            List<ScheduleDTO> allPopulation);

    /**
     * 交叉操作
     *
     * @param selected      选择后的种群
     * @param crossoverRate 交叉率
     * @param baseDTO       基础数据传输对象
     * @return 交叉后的种群
     */
    List<ScheduleDTO> crossover(
            List<ScheduleDTO> selected,
            Double crossoverRate,
            AutomaticClassSchedulingBaseDTO baseDTO);

    /**
     * 变异操作
     * @param offspring  交叉后的种群
     * @param mutationRate 变异率
     * @param baseDTO 基础数据传输对象
     */
    void mutation(
            List<ScheduleDTO> offspring,
            Double mutationRate,
            AutomaticClassSchedulingBaseDTO baseDTO);
}
