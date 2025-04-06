package com.frontleaves.scheduling.logic.scheduling;

import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleDTO;
import com.frontleaves.scheduling.services.scheduling.EvaluatePopulationService;
import com.frontleaves.scheduling.utils.ScheduleFitnessCalculator;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 遗传算法评估种群逻辑实现类
 *
 * @author FLASHLACK
 */
@Service
@Slf4j
public class EvaluatePopulationLogic implements EvaluatePopulationService {

    @Override
    public void evaluatePopulation(@NotNull List<ScheduleDTO> allPopulation, AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO population : allPopulation) {
            double fitness = ScheduleFitnessCalculator.calculateFitness(population, baseDTO);
            population.setFitness(fitness);
        }
    }
}