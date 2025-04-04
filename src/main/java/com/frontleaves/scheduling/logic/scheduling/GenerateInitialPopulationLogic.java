package com.frontleaves.scheduling.logic.scheduling;

import com.frontleaves.scheduling.services.scheduling.GenerateInitialPopulationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
/**
 * 遗传算法初始化种群逻辑实现类
 * <p>
 * 该类实现了遗传算法的初始化种群功能，通过随机生成初始种群，为后续的遗传算法进化提供基础。
 * </p>
 * @author FLASHLACK
 * @version 1.0
 */
public class GenerateInitialPopulationLogic implements GenerateInitialPopulationService {
}
