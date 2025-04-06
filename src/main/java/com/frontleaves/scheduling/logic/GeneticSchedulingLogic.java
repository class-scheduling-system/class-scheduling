package com.frontleaves.scheduling.logic;


import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.services.scheduling.EvaluatePopulationService;
import com.frontleaves.scheduling.services.scheduling.GenerateInitialPopulationService;
import com.frontleaves.scheduling.services.scheduling.GeneticSchedulingService;
import com.frontleaves.scheduling.services.scheduling.IterateService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 遗传算法排课逻辑实现类
 * <p>
 * 该类实现了基于遗传算法的自动排课功能，通过进化算法对课程、教师、教室等资源进行优化分配。
 * 遗传算法主要包括初始化种群、选择、交叉、变异、评估等操作，通过多代进化寻找最优的课程安排方案。
 * </p>
 *
 * @author frontleaves
 * @version 1.0
 */
@Service
@Slf4j
public class GeneticSchedulingLogic extends BaseGeneticSchedulingLogic implements GeneticSchedulingService {

    /**
     * 遗传算法初始种群生成服务
     */
private final GenerateInitialPopulationService generateInitialPopulationService;
private final EvaluatePopulationService evaluatePopulationService;
private final IterateService iterateService;
    /**
     * 构造函数
     *
     * @param redisson Redis客户端，用于缓存和分布式锁
     */
    public GeneticSchedulingLogic(RedissonClient redisson,
                                  GenerateInitialPopulationService generateInitialPopulationService,
                                  EvaluatePopulationService evaluatePopulationService,
                                  IterateService iterateService) {
        super(redisson);
        this.generateInitialPopulationService = generateInitialPopulationService;
        this.evaluatePopulationService = evaluatePopulationService;
        this.iterateService = iterateService;
    }

    /**
     * 执行遗传算法排课
     * <p>
     * 该方法是遗传算法排课的主入口，包括以下步骤：
     * <ul>
     *  <ol>1. 初始化种群</ol>
     *  <ol>2. 评估初始种群适应度</ol>
     *  <ol>3. 进行多代进化（选择、交叉、变异）</ol>
     *  <ol>4. 记录最优解</ol>
     *  <ol>5. 构建排课结果</ol>
     * </ul>
     *
     * @param taskId 排课任务ID，用于标识和跟踪排课进度
     * @param baseDTO 排课基础数据，包含课程、教师、教室等信息
     * @return 排课结果，包含课程安排、资源利用率、冲突信息等
     * @throws BusinessException 排课过程中的业务异常
     */
    @Override
    public ScheduleResultDTO executeGeneticAlgorithm(String taskId, AutomaticClassSchedulingBaseDTO baseDTO) {
        try {
            this.updateProgress(taskId, 0);
            this.updateStatus(taskId, "正在初始化种群...");
            // 生成初始种群
            List<ScheduleDTO> allPopulation = generateInitialPopulationService.generateInitialPopulation(baseDTO);
            log.debug("初始种群生成完成，种群大小: {}", allPopulation.size());
            log.debug("获取第一个种群: {}", allPopulation.get(0));
            // 评估初始种群
            log.debug("开始评估初始种群...");
            evaluatePopulationService.evaluatePopulation(allPopulation, baseDTO);
            int generation = 0;
            int maxGenerations = baseDTO.getAlgorithmParams().getMaxIterations();
            double bestFitness = 0.0;
            ScheduleDTO bestSchedule = null;
            log.debug("进行迭代进化...");
            // 迭代进化
            while (generation < maxGenerations) {
                // 选择
                List<ScheduleDTO> selected = iterateService.selection(allPopulation);
                // 交叉
                List<ScheduleDTO> offspring = iterateService.crossover(selected, baseDTO.getAlgorithmParams().getCrossoverRate(), baseDTO);
                // 变异
                iterateService.mutation(offspring, baseDTO.getAlgorithmParams().getMutationRate(), baseDTO);
                // 评估新一代
                evaluatePopulationService.evaluatePopulation(offspring, baseDTO);
                // 更新种群
                allPopulation = offspring;
                // 更新最佳解
                Optional<ScheduleDTO> currentBest = allPopulation.stream()
                        .max(Comparator.comparingDouble(ScheduleDTO::getFitness));
                if (currentBest.isPresent() && currentBest.get().getFitness() > bestFitness) {
                    bestFitness = currentBest.get().getFitness();
                    bestSchedule = deepCopySchedule(currentBest.get());
                }
                log.debug("第 {} 代适应度: {}", generation, bestFitness);
                // 更新进度
                int progress = (int) ((double) generation / maxGenerations * 100);
                updateProgress(taskId, progress);
                updateStatus(taskId, String.format("正在进行第 %d 代优化...", generation));
                generation++;
            }
            // 构建结果
            if (bestSchedule != null) {
                log.debug("最终排课方案: {}", bestSchedule);
                List<SchedulingConflictDTO> conflicts = this.findConflicts(bestSchedule);
                ScheduleResultDTO.ResourceUtilization utilization = calculateResourceUtilization(bestSchedule);
                List<ScheduleResultDTO.CourseTeachingClassDTO> assignments = convertScheduleToAssignments(bestSchedule);
                updateProgress(taskId, 100);
                updateStatus(taskId, "排课完成");
                return new ScheduleResultDTO()
                        .setTaskId(taskId)
                        .setSemesterId(baseDTO.getSemester().getSemesterUuid())
                        .setDepartmentId(baseDTO.getDepartment().getDepartmentUuid())
                        .setStatus("completed")
                        .setProgress(100)
                        .setAssignments(assignments)
                        .setConflicts(conflicts)
                        .setResourceUtilization(utilization)
                        .setFitness(bestFitness);
            }
            throw new BusinessException("未能生成有效的课程表", ErrorCode.BODY_ERROR);

        } catch (Exception e) {
            log.error("排课过程发生错误", e);
            updateStatus(taskId, "排课失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 获取排课任务的进度
     *
     * @param taskId 任务ID
     * @return 排课进度（0-100的整数）
     */
    @Override
    public int getSchedulingProgress(String taskId) {
        String key = getProgressKey(taskId);
        Object value = redisson.getBucket(key).get();
        return value != null ? (int) value : 0;
    }

    /**
     * 获取排课任务的状态
     *
     * @param taskId 任务ID
     * @return 排课状态描述
     */
    @Override
    public String getSchedulingStatus(String taskId) {
        String key = getStatusKey(taskId);
        Object value = redisson.getBucket(key).get();
        return value != null ? value.toString() : "unknown";
    }
}
