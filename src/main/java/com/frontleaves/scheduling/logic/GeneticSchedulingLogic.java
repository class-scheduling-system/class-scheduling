package com.frontleaves.scheduling.logic;


import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomAndTypeDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.services.GeneticSchedulingService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
@RequiredArgsConstructor
public class GeneticSchedulingLogic implements GeneticSchedulingService {
    /**
     * 任务进度映射，记录每个排课任务的进度百分比
     */
    private final Map<String, Integer> taskProgress = new HashMap<>();

    /**
     * 任务状态映射，记录每个排课任务的当前状态描述
     */
    private final Map<String, String> taskStatus = new HashMap<>();


    /**
     * 执行遗传算法排课
     * <p>
     * 该方法是遗传算法排课的主入口，包括以下步骤：
     * 1. 初始化种群
     * 2. 评估初始种群适应度
     * 3. 进行多代进化（选择、交叉、变异）
     * 4. 记录最优解
     * 5. 构建排课结果
     * </p>
     *
     * @param taskId 排课任务ID，用于标识和跟踪排课进度
     * @param baseDTO 排课基础数据，包含课程、教师、教室等信息
     * @return 排课结果，包含课程安排、资源利用率、冲突信息等
     * @throws BusinessException 排课过程中的业务异常
     */
    @Override
    public ScheduleResultDTO executeGeneticAlgorithm(String taskId, AutomaticClassSchedulingBaseDTO baseDTO) {
        try {
            updateProgress(taskId, 0);
            taskStatus.put(taskId, "正在初始化种群...");

            // 生成初始种群
            List<ScheduleDTO> population = generateInitialPopulation(baseDTO);

            // 评估初始种群
            evaluatePopulation(population, baseDTO);

            int generation = 0;
            int maxGenerations = baseDTO.getAlgorithmParams().getMaxIterations();
            double bestFitness = 0.0;
            ScheduleDTO bestSchedule = null;

            while (generation < maxGenerations) {
                // 选择
                List<ScheduleDTO> selected = selection(population);

                // 交叉
                List<ScheduleDTO> offspring = crossover(selected, baseDTO.getAlgorithmParams().getCrossoverRate());

                // 变异
                mutation(offspring, baseDTO.getAlgorithmParams().getMutationRate(), baseDTO);

                // 评估新一代
                evaluatePopulation(offspring, baseDTO);

                // 更新种群
                population = offspring;

                // 更新最佳解
                Optional<ScheduleDTO> currentBest = population.stream()
                        .max(Comparator.comparingDouble(ScheduleDTO::getFitness));

                if (currentBest.isPresent() && currentBest.get().getFitness() > bestFitness) {
                    bestFitness = currentBest.get().getFitness();
                    bestSchedule = deepCopySchedule(currentBest.get());
                }

                // 更新进度
                int progress = (int) ((double) generation / maxGenerations * 100);
                updateProgress(taskId, progress);
                taskStatus.put(taskId, String.format("正在进行第 %d 代优化...", generation));

                generation++;
            }

            // 构建结果
            if (bestSchedule != null) {
                List<SchedulingConflictDTO> conflicts = findConflicts(bestSchedule);
                ScheduleResultDTO.ResourceUtilization utilization = calculateResourceUtilization(bestSchedule);
                List<ScheduleResultDTO.ClassAssignmentDTO> assignments = convertScheduleToAssignments(bestSchedule);

                updateProgress(taskId, 100);
                taskStatus.put(taskId, "排课完成");

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
            taskStatus.put(taskId, "排课失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 评估种群中所有个体的适应度
     *
     * @param population 待评估的种群
     * @param baseDTO 排课基础数据
     */
    private void evaluatePopulation(List<ScheduleDTO> population, AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO schedule : population) {
            double fitness = calculateFitness(schedule, baseDTO);
            schedule.setFitness(fitness);
        }
    }

    /**
     * 计算单个课程表的适应度得分
     * <p>
     * 适应度计算考虑多个因素：
     * 1. 冲突惩罚（教师、教室时间冲突）
     * 2. 连续课程偏好
     * 3. 时间槽偏好
     * 4. 教室优化（容量、类型匹配）
     * </p>
     *
     * @param schedule 待评估的课程表
     * @param baseDTO 排课基础数据，包含约束条件
     * @return 适应度得分，值越高表示排课方案越优
     */
    private double calculateFitness(ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        double fitness = 100.0;  // 基础分数

        // 减去冲突惩罚
        fitness -= calculateConflictPenalty(schedule);

        // 连续课程适应度
        if (baseDTO.getConstraints().getConsecutiveCoursesPreferred()) {
            fitness += calculateConsecutiveCoursesFitness(schedule);
        }

        // 时间偏好适应度
        fitness += calculateTimePreferenceFitness(schedule, baseDTO.getTimePreferences());

        // 教室优化适应度
        if (baseDTO.getConstraints().getRoomOptimization()) {
            fitness += calculateRoomOptimizationFitness(schedule);
        }

        return Math.max(0.0, fitness);  // 确保适应度不为负
    }

    /**
     * 计算冲突惩罚
     * <p>
     * 检查课程表中的冲突情况并计算惩罚值，包括：
     * - 教师在同一时间段被安排多门课程的冲突
     * - 教室在同一时间段被多门课程占用的冲突
     * </p>
     *
     * @param schedule 待检查的课程表
     * @return 冲突惩罚值，值越大表示冲突越严重
     */
    private double calculateConflictPenalty(ScheduleDTO schedule) {
        double penalty = 0.0;

        // 检查所有时间槽的安排
        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry1 : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slot1 = entry1.getKey();
            ScheduleItemDTO item1 = entry1.getValue();

            for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry2 : schedule.getAssignments().entrySet()) {
                if (entry1 == entry2) continue;

                TimeSlotDTO slot2 = entry2.getKey();
                ScheduleItemDTO item2 = entry2.getValue();

                // 同一时间段的冲突检查
                if (slot1.getWeek() == slot2.getWeek() &&
                        slot1.getDayOfWeek() == slot2.getDayOfWeek() &&
                        slot1.getPeriod() == slot2.getPeriod()) {

                    // 教师冲突
                    if (item1.getTeacher().getTeacher().getTeacherUuid().equals(item2.getTeacher().getTeacher().getTeacherUuid())) {
                        penalty += 100.0;
                    }

                    // 教室冲突
                    if (item1.getClassroom().getClassroom().getClassroomUuid().equals(item2.getClassroom().getClassroom().getClassroomUuid())) {
                        penalty += 100.0;
                    }
                }
            }
        }

        return penalty;
    }

    /**
     * 查找课程表中的冲突
     * <p>
     * 分析课程表中的冲突并生成详细的冲突信息，包括：
     * - 教师在同一时间段被安排多门课程的冲突
     * - 教室在同一时间段被多门课程占用的冲突
     * </p>
     *
     * @param schedule 待分析的课程表
     * @return 冲突列表，包含冲突类型和描述
     */
    private List<SchedulingConflictDTO> findConflicts(ScheduleDTO schedule) {
        List<SchedulingConflictDTO> conflicts = new ArrayList<>();

        // 检查所有时间槽的安排
        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry1 : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slot1 = entry1.getKey();
            ScheduleItemDTO item1 = entry1.getValue();

            for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry2 : schedule.getAssignments().entrySet()) {
                if (entry1 == entry2) continue;

                TimeSlotDTO slot2 = entry2.getKey();
                ScheduleItemDTO item2 = entry2.getValue();

                // 同一时间段的冲突检查
                if (slot1.getWeek() == slot2.getWeek() &&
                        slot1.getDayOfWeek() == slot2.getDayOfWeek() &&
                        slot1.getPeriod() == slot2.getPeriod()) {

                    // 教师冲突
                    if (item1.getTeacher().getTeacher().getTeacherUuid()
                            .equals(item2.getTeacher().getTeacher().getTeacherUuid())) {
                        conflicts.add(new SchedulingConflictDTO()
                                .setConflictType(1)
                                .setDescription(String.format(
                                        "教师 %s 在第%d周星期%d第%d节课有重复安排",
                                        item1.getTeacher().getTeacher().getName(),
                                        slot1.getWeek(),
                                        slot1.getDayOfWeek(),
                                        slot1.getPeriod()
                                )));
                    }
                    // 教室冲突
                    if (item1.getClassroom().getClassroom().getClassroomUuid()
                            .equals(item2.getClassroom().getClassroom().getClassroomUuid())) {
                        conflicts.add(new SchedulingConflictDTO()
                                .setConflictType(2)
                                .setDescription(String.format(
                                        "教室 %s 在第%d周星期%d第%d节课有重复安排",
                                        item1.getClassroom().getClassroom().getName(),
                                        slot1.getWeek(),
                                        slot1.getDayOfWeek(),
                                        slot1.getPeriod()
                                )));
                    }
                }
            }
        }

        return conflicts;
    }

    /**
     * 计算资源利用率
     * <p>
     * 评估课程表中各种资源的利用情况，包括：
     * - 教室利用率：衡量教室容量与实际使用情况的匹配度
     * - 教师工作量：评估教师课程分配的均衡性
     * - 时间槽使用率：评估时间资源的利用效率
     * - 总体利用率：综合上述三项指标的平均值
     * </p>
     *
     * @param schedule 待评估的课程表
     * @return 资源利用率指标
     */
    private ScheduleResultDTO.ResourceUtilization calculateResourceUtilization(ScheduleDTO schedule) {
        // 教室利用率
        Map<String, Integer> roomUsage = new HashMap<>();
        Map<String, Integer> roomCapacity = new HashMap<>();

        // 教师工作量
        Map<String, Integer> teacherWorkload = new HashMap<>();

        // 时间槽使用情况
        Set<String> usedTimeSlots = new HashSet<>();
        int totalTimeSlots = 0;

        // 统计使用情况
        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slot = entry.getKey();
            ScheduleItemDTO item = entry.getValue();

            // 教室使用统计
            String roomId = item.getClassroom().getClassroom().getClassroomUuid();
            roomUsage.merge(roomId, 1, Integer::sum);
            roomCapacity.putIfAbsent(roomId, item.getClassroom().getClassroom().getCapacity());

            // 教师工作量统计
            String teacherId = item.getTeacher().getTeacher().getTeacherUuid();
            teacherWorkload.merge(teacherId, 1, Integer::sum);

            // 时间槽使用统计
            String timeSlotKey = String.format("%d-%d-%d", slot.getWeek(), slot.getDayOfWeek(), slot.getPeriod());
            usedTimeSlots.add(timeSlotKey);
            totalTimeSlots++;
        }

        // 计算教室平均利用率
        double classroomUtilization = roomUsage.entrySet().stream()
                .mapToDouble(entry -> {
                    int usage = entry.getValue();
                    int capacity = roomCapacity.get(entry.getKey());
                    return (double) usage / capacity;
                })
                .average()
                .orElse(0.0);

        // 计算教师工作量平均值
        double teacherUtilization = teacherWorkload.values().stream()
                .mapToDouble(Integer::doubleValue)
                .average()
                .orElse(0.0) / totalTimeSlots;

        // 计算时间槽利用率
        double timeSlotUtilization = (double) usedTimeSlots.size() / totalTimeSlots;

        // 计算总体利用率
        double overallUtilization = (classroomUtilization + teacherUtilization + timeSlotUtilization) / 3;

        return new ScheduleResultDTO.ResourceUtilization()
                .setOverall(overallUtilization)
                .setClassroom(classroomUtilization)
                .setTeacher(teacherUtilization)
                .setTimeSlot(timeSlotUtilization);
    }

    /**
     * 深拷贝课程表
     * <p>
     * 创建课程表对象的深拷贝，包括时间槽和排课项目的完整复制，
     * 确保原对象和复制对象完全独立，避免引用共享导致的意外修改。
     * </p>
     *
     * @param schedule 源课程表对象
     * @return 深拷贝后的新课程表对象
     */
    private ScheduleDTO deepCopySchedule(ScheduleDTO schedule) {
        ScheduleDTO copy = new ScheduleDTO();
        Map<TimeSlotDTO, ScheduleItemDTO> assignments = new HashMap<>();

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slotCopy = new TimeSlotDTO(entry.getKey());
            ScheduleItemDTO itemCopy = new ScheduleItemDTO(entry.getValue());
            assignments.put(slotCopy, itemCopy);
        }

        copy.setAssignments(assignments);
        copy.setFitness(schedule.getFitness());
        return copy;
    }

    /**
     * 选择操作
     * <p>
     * 基于轮盘赌算法进行个体选择，适应度越高的个体被选中的概率越大。
     * 该方法是遗传算法中模拟自然选择的过程，使得优质个体有更多机会繁殖下一代。
     * </p>
     *
     * @param population 当前种群
     * @return 选择后的种群（通过深拷贝创建）
     */
    private List<ScheduleDTO> selection(List<ScheduleDTO> population) {
        List<ScheduleDTO> selected = new ArrayList<>();
        double totalFitness = population.stream()
                .mapToDouble(ScheduleDTO::getFitness)
                .sum();

        Random random = new Random();
        while (selected.size() < population.size()) {
            double point = random.nextDouble() * totalFitness;
            double sum = 0;
            for (ScheduleDTO schedule : population) {
                sum += schedule.getFitness();
                if (sum >= point) {
                    selected.add(deepCopySchedule(schedule));
                    break;
                }
            }
        }

        return selected;
    }

    /**
     * 交叉操作
     * <p>
     * 对选择后的个体按照交叉率进行交叉操作，生成新的后代。
     * 交叉操作是遗传算法中模拟基因重组的过程，通过将两个父代个体的部分特征组合，产生具有新特性的后代。
     * </p>
     *
     * @param selected 经过选择的个体
     * @param crossoverRate 交叉概率
     * @return 交叉后产生的后代
     */
    private List<ScheduleDTO> crossover(List<ScheduleDTO> selected, double crossoverRate) {
        List<ScheduleDTO> offspring = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < selected.size() - 1; i += 2) {
            ScheduleDTO parent1 = selected.get(i);
            ScheduleDTO parent2 = selected.get(i + 1);

            if (random.nextDouble() < crossoverRate) {
                // 执行交叉
                List<ScheduleDTO> children = crossoverSchedules(parent1, parent2);
                offspring.addAll(children);
            } else {
                // 直接复制父代
                offspring.add(deepCopySchedule(parent1));
                offspring.add(deepCopySchedule(parent2));
            }
        }

        // 如果是奇数，保留最后一个
        if (selected.size() % 2 != 0) {
            offspring.add(deepCopySchedule(selected.get(selected.size() - 1)));
        }

        return offspring;
    }

    /**
     * 交叉两个课程表
     * <p>
     * 实现两个父代课程表之间的交叉操作，通过随机选择交叉点，
     * 将两个父代的课程安排按照课程ID进行混合，生成两个新的子代课程表。
     * </p>
     *
     * @param parent1 第一个父代课程表
     * @param parent2 第二个父代课程表
     * @return 交叉后生成的两个子代课程表
     */
    private List<ScheduleDTO> crossoverSchedules(ScheduleDTO parent1, ScheduleDTO parent2) {
        List<ScheduleDTO> children = new ArrayList<>();
        Random random = new Random();

        // 创建两个子代
        ScheduleDTO child1 = new ScheduleDTO();
        ScheduleDTO child2 = new ScheduleDTO();

        Map<TimeSlotDTO, ScheduleItemDTO> assignments1 = new HashMap<>();
        Map<TimeSlotDTO, ScheduleItemDTO> assignments2 = new HashMap<>();

        // 获取所有课程
        Map<String, List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>>> courseAssignments1 = new HashMap<>();
        Map<String, List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>>> courseAssignments2 = new HashMap<>();

        // 按课程ID分组
        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : parent1.getAssignments().entrySet()) {
            String courseId = entry.getValue().getCourse().getCourseLibraryUuid();
            courseAssignments1.computeIfAbsent(courseId, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : parent2.getAssignments().entrySet()) {
            String courseId = entry.getValue().getCourse().getCourseLibraryUuid();
            courseAssignments2.computeIfAbsent(courseId, k -> new ArrayList<>()).add(entry);
        }

        Set<String> allCourses = new HashSet<>();
        allCourses.addAll(courseAssignments1.keySet());
        allCourses.addAll(courseAssignments2.keySet());

        // 随机选择交叉点
        int crossoverPoint = random.nextInt(allCourses.size());

        int count = 0;
        for (String courseId : allCourses) {
            if (count < crossoverPoint) {
                // 从父代1复制到子代1，从父代2复制到子代2
                copyAssignmentsFromParent(courseAssignments1.get(courseId), assignments1);
                copyAssignmentsFromParent(courseAssignments2.get(courseId), assignments2);
            } else {
                // 从父代2复制到子代1，从父代1复制到子代2
                copyAssignmentsFromParent(courseAssignments2.get(courseId), assignments1);
                copyAssignmentsFromParent(courseAssignments1.get(courseId), assignments2);
            }
            count++;
        }

        child1.setAssignments(assignments1);
        child2.setAssignments(assignments2);

        children.add(child1);
        children.add(child2);

        return children;
    }

    /**
     * 从父代复制课程安排到子代
     *
     * @param assignments 父代的课程安排列表
     * @param target 目标子代的课程安排映射
     */
    private void copyAssignmentsFromParent(
            List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> assignments,
            Map<TimeSlotDTO, ScheduleItemDTO> target
    ) {
        if (assignments != null) {
            for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : assignments) {
                TimeSlotDTO slotCopy = new TimeSlotDTO(entry.getKey());
                ScheduleItemDTO itemCopy = new ScheduleItemDTO(entry.getValue());
                target.put(slotCopy, itemCopy);
            }
        }
    }

    /**
     * 变异操作
     * <p>
     * 按照变异率对个体进行变异，通过随机改变课程安排的某些属性，
     * 增加种群的多样性，避免陷入局部最优解。
     * 包括三种变异策略：时间槽变异、教室变异和教师变异。
     * </p>
     *
     * @param population 当前种群
     * @param mutationRate 变异概率
     * @param baseDTO 排课基础数据
     */
    private void mutation(List<ScheduleDTO> population, double mutationRate, AutomaticClassSchedulingBaseDTO baseDTO) {
        Random random = new Random();

        for (ScheduleDTO schedule : population) {
            if (random.nextDouble() < mutationRate) {
                // 选择变异策略
                int strategy = random.nextInt(3);

                switch (strategy) {
                    case 0:
                        // 时间槽变异
                        timeSlotMutation(schedule, baseDTO);
                        break;
                    case 1:
                        // 教室变异
                        classroomMutation(schedule, baseDTO);
                        break;
                    case 2:
                        // 教师变异
                        teacherMutation(schedule, baseDTO);
                        break;
                }
            }
        }
    }

    /**
     * 时间槽变异
     * <p>
     * 随机选择一个课程安排，改变其时间槽，
     * 包括尝试找到新的合适时间槽或与其他课程交换时间。
     * </p>
     *
     * @param schedule 待变异的课程表
     * @param baseDTO 排课基础数据
     */
    private void timeSlotMutation(ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        Random random = new Random();
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());

        if (!entries.isEmpty()) {
            // 随机选择一个课程进行时间变异
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            schedule.getAssignments().remove(entry.getKey());

            // 尝试找到新的合适时间槽
            TimeSlotDTO newTimeSlot = findSuitableTimeSlot(
                    schedule.getAssignments(),
                    entry.getValue().getTeacher(),
                    entry.getValue().getClassroom(),
                    baseDTO
            );

            if (newTimeSlot != null) {
                schedule.getAssignments().put(newTimeSlot, entry.getValue());
            } else {
                // 如果找不到合适的新时间槽，尝试与其他课程交换时间
                for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> other : entries) {
                    if (other != entry && isSwapValid(entry, other, schedule)) {
                        swapTimeSlots(schedule, entry, other);
                        return;
                    }
                }
                // 如果无法交换，恢复原有安排
                schedule.getAssignments().put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 教室变异
     * <p>
     * 随机选择一个课程安排，尝试分配更合适的教室。
     * </p>
     *
     * @param schedule 待变异的课程表
     * @param baseDTO 排课基础数据
     */
    private void classroomMutation(ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        Random random = new Random();
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());

        if (!entries.isEmpty()) {
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseLibraryDTO course = entry.getValue().getCourse();

            // 尝试分配新教室
            ClassroomAndTypeDTO newClassroom = selectClassroomForCourse(course, baseDTO.getClassroomAndType());

            if (newClassroom != null && !newClassroom.getClassroom().getClassroomUuid()
                    .equals(entry.getValue().getClassroom().getClassroom().getClassroomUuid())) {
                ScheduleItemDTO newItem = new ScheduleItemDTO(
                        course,
                        entry.getValue().getTeacher(),
                        newClassroom,
                        entry.getValue().getPriority()
                );
                schedule.getAssignments().put(entry.getKey(), newItem);
            }
        }
    }

    /**
     * 教师变异
     * <p>
     * 随机选择一个课程安排，尝试分配其他合格的教师。
     * </p>
     *
     * @param schedule 待变异的课程表
     * @param baseDTO 排课基础数据
     */
    private void teacherMutation(@NotNull ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        Random random = new Random();
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());

        if (!entries.isEmpty()) {
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseLibraryDTO course = entry.getValue().getCourse();

            // 查找可以教授这门课程的教师列表
            List<TeacherCoursePreferencesDTO> suitableTeachers = baseDTO.getCourseAndTeacherList().stream()
                    .filter(ct -> ct.getCourse().getCourseUuid().equals(course.getCourseUuid()))
                    .flatMap(ct -> ct.getTeacherList().stream())
                    .collect(Collectors.toList());

            // 尝试分配新教师
            TeacherCoursePreferencesDTO newTeacher = selectTeacherForCourse(course, suitableTeachers);

            if (newTeacher != null && !newTeacher.getTeacherUuid().equals(entry.getValue().getTeacher().getTeacherUuid())) {
                ScheduleItemDTO newItem = new ScheduleItemDTO(
                        course,
                        newTeacher,
                        entry.getValue().getClassroom(),
                        entry.getValue().getPriority()
                );
                schedule.getAssignments().put(entry.getKey(), newItem);
            }
        }
    }

    /**
     * 检查时间槽交换是否有效
     * <p>
     * 通过创建临时映射来模拟交换后的状态，检查是否会产生冲突。
     * </p>
     *
     * @param entry1 第一个课程安排
     * @param entry2 第二个课程安排
     * @param schedule 当前课程表
     * @return 交换是否有效（无冲突）
     */
    private boolean isSwapValid(
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry1,
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry2,
            ScheduleDTO schedule
    ) {
        // 创建临时映射来测试交换
        Map<TimeSlotDTO, ScheduleItemDTO> tempAssignments = new HashMap<>(schedule.getAssignments());
        tempAssignments.remove(entry1.getKey());
        tempAssignments.remove(entry2.getKey());
        tempAssignments.put(entry1.getKey(), entry2.getValue());
        tempAssignments.put(entry2.getKey(), entry1.getValue());

        // 检查交换后是否有冲突
        return !hasConflicts(tempAssignments);
    }

    /**
     * 检查课程安排是否存在冲突
     *
     * @param assignments 课程安排映射
     * @return 是否存在冲突
     */
    private boolean hasConflicts(Map<TimeSlotDTO, ScheduleItemDTO> assignments) {
        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry1 : assignments.entrySet()) {
            for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry2 : assignments.entrySet()) {
                if (entry1 == entry2) continue;

                TimeSlotDTO slot1 = entry1.getKey();
                TimeSlotDTO slot2 = entry2.getKey();
                ScheduleItemDTO item1 = entry1.getValue();
                ScheduleItemDTO item2 = entry2.getValue();

                if (slot1.getWeek() == slot2.getWeek() &&
                        slot1.getDayOfWeek() == slot2.getDayOfWeek() &&
                        slot1.getPeriod() == slot2.getPeriod()) {

                    // 检查教师冲突
                    if (item1.getTeacher().getTeacher().getTeacherUuid()
                            .equals(item2.getTeacher().getTeacher().getTeacherUuid())) {
                        return true;
                    }

                    // 检查教室冲突
                    if (item1.getClassroom().getClassroom().getClassroomUuid()
                            .equals(item2.getClassroom().getClassroom().getClassroomUuid())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * 交换两个课程的时间槽
     *
     * @param schedule 课程表
     * @param entry1 第一个课程安排
     * @param entry2 第二个课程安排
     */
    private void swapTimeSlots(
            ScheduleDTO schedule,
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry1,
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry2
    ) {
        ScheduleItemDTO item1 = entry1.getValue();
        ScheduleItemDTO item2 = entry2.getValue();

        schedule.getAssignments().put(entry1.getKey(), item2);
        schedule.getAssignments().put(entry2.getKey(), item1);
    }

    /**
     * 转换课程表为课程安排列表
     * <p>
     * 将内部使用的课程表模型转换为前端展示所需的课程安排格式。
     * </p>
     *
     * @param schedule 内部课程表模型
     * @return 课程安排列表
     */
    private List<ScheduleResultDTO.ClassAssignmentDTO> convertScheduleToAssignments(ScheduleDTO schedule) {
        List<ScheduleResultDTO.ClassAssignmentDTO> assignments = new ArrayList<>();

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slot = entry.getKey();
            ScheduleItemDTO item = entry.getValue();

            ScheduleResultDTO.TimeSlot timeSlot = new ScheduleResultDTO.TimeSlot()
                    .setWeek(slot.getWeek())
                    .setDayOfWeek(slot.getDayOfWeek())
                    .setPeriod(slot.getPeriod());

            ScheduleResultDTO.ClassAssignmentDTO assignment = new ScheduleResultDTO.ClassAssignmentDTO()
                    .setCourse(item.getCourse())
                    .setTeacher(item.getTeacher())
                    .setClassroom(item.getClassroom())
                    .setTimeSlot(timeSlot)
                    .setPriority(item.getPriority());

            assignments.add(assignment);
        }

        return assignments;
    }

    /**
     * 获取排课任务的进度
     *
     * @param taskId 任务ID
     * @return 排课进度（0-100的整数）
     */
    @Override
    public int getSchedulingProgress(String taskId) {
        return taskProgress.getOrDefault(taskId, 0);
    }

    /**
     * 获取排课任务的状态
     *
     * @param taskId 任务ID
     * @return 排课状态描述
     */
    @Override
    public String getSchedulingStatus(String taskId) {
        return taskStatus.getOrDefault(taskId, "unknown");
    }

    /**
     * 更新排课任务的进度
     *
     * @param taskId 任务ID
     * @param progress 进度值（0-100）
     */
    private void updateProgress(String taskId, int progress) {
        taskProgress.put(taskId, progress);
    }

    /**
     * 生成初始种群
     * <p>
     * 根据排课基础数据生成初始种群，为每个课程分配合适的教师、教室和时间槽。
     * 初始种群的质量会影响算法的收敛速度和最终结果。
     * </p>
     *
     * @param baseDTO 排课基础数据
     * @return 初始种群
     */
    private List<ScheduleDTO> generateInitialPopulation(AutomaticClassSchedulingBaseDTO baseDTO) {
        List<ScheduleDTO> population = new ArrayList<>();
        int populationSize = baseDTO.getAlgorithmParams().getPopulationSize();

        for (int i = 0; i < populationSize; i++) {
            ScheduleDTO schedule = new ScheduleDTO();
            Map<TimeSlotDTO, ScheduleItemDTO> assignments = new HashMap<>();

            // 为每个课程分配时间槽
            for (CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers : baseDTO.getCourseAndTeacherList()) {
                CourseLibraryDTO course = courseAndTeachers.getCourse();
                TeacherCoursePreferencesDTO teacher = selectTeacherForCourse(course, courseAndTeachers.getTeacherList());
                ClassroomAndTypeDTO classroom = selectClassroomForCourse(course, baseDTO.getClassroomAndType());

                if (teacher != null && classroom != null) {
                    TimeSlotDTO timeSlot = findSuitableTimeSlot(assignments, teacher, classroom, baseDTO);

                    if (timeSlot != null) {
                        ScheduleItemDTO item = new ScheduleItemDTO(
                                course,
                                teacher,
                                classroom,
                                courseAndTeachers.getPriority()
                        );

                        assignments.put(timeSlot, item);
                    }
                }
            }

            schedule.setAssignments(assignments);
            population.add(schedule);
        }

        return population;
    }

    /**
     * 为课程选择合适的教师
     * <p>
     * 从符合课程学科要求的教师中随机选择一位。
     * </p>
     *
     * @param course 课程信息
     * @param teachers 候选教师列表
     * @return 选择的教师，如果没有合适教师则返回null
     */
    private @Nullable TeacherCoursePreferencesDTO selectTeacherForCourse(
            CourseLibraryDTO course,
            List<TeacherCoursePreferencesDTO> teachers
    ) {
        List<TeacherCoursePreferencesDTO> suitableTeachers = teachers.stream()
                .filter(teacher -> teacher.getSubjects().contains(course.getSubject()))
                .collect(Collectors.toList());

        if (!suitableTeachers.isEmpty()) {
            Random random = new Random();
            return suitableTeachers.get(random.nextInt(suitableTeachers.size()));
        }

        return null;
    }

    /**
     * 为课程选择合适的教室
     * <p>
     * 从符合课程容量和类型要求的教室中随机选择一间。
     * </p>
     *
     * @param course 课程信息
     * @param classrooms 候选教室列表
     * @return 选择的教室，如果没有合适教室则返回null
     */
    private @Nullable ClassroomAndTypeDTO selectClassroomForCourse(
            CourseLibraryDTO course,
            List<ClassroomAndTypeDTO> classrooms
    ) {
        List<ClassroomAndTypeDTO> suitableClassrooms = classrooms.stream()
                .filter(classroom ->
                        classroom.getClassroomCapacity() >= course.getStudentCount() &&
                                classroom.getClassroomType().equals(course.getRequiredClassroomType()))
                .collect(Collectors.toList());

        if (!suitableClassrooms.isEmpty()) {
            Random random = new Random();
            return suitableClassrooms.get(random.nextInt(suitableClassrooms.size()));
        }

        return null;
    }

    /**
     * 查找合适的时间槽
     * <p>
     * 从所有可能的时间槽中查找一个不会导致冲突的时间槽。
     * </p>
     *
     * @param assignments 已有的课程安排
     * @param teacher 教师信息
     * @param classroom 教室信息
     * @param baseDTO 排课基础数据
     * @return 合适的时间槽，如果没有找到则返回null
     */
    private @Nullable TimeSlotDTO findSuitableTimeSlot(
            Map<TimeSlotDTO, ScheduleItemDTO> assignments,
            TeacherCoursePreferencesDTO teacher,
            ClassroomAndTypeDTO classroom,
            AutomaticClassSchedulingBaseDTO baseDTO
    ) {
        Random random = new Random();
        List<TimeSlotDTO> allTimeSlots = generateAllTimeSlots(baseDTO);
        Collections.shuffle(allTimeSlots);

        for (TimeSlotDTO slot : allTimeSlots) {
            if (isTimeSlotSuitable(slot, assignments, teacher, classroom)) {
                return slot;
            }
        }

        return null;
    }

    /**
     * 生成所有可能的时间槽
     * <p>
     * 基于排课周数、每周天数和每天课时数生成所有可能的时间槽。
     * </p>
     *
     * @param baseDTO 排课基础数据
     * @return 所有可能的时间槽列表
     */
    private @NotNull List<TimeSlotDTO> generateAllTimeSlots(AutomaticClassSchedulingBaseDTO baseDTO) {
        List<TimeSlotDTO> slots = new ArrayList<>();

        for (int week = 1; week <= baseDTO.getEndWeek(); week++) {
            for (int day = 1; day <= 5; day++) {  // 假设一周5天
                for (int period = 1; period <= 8; period++) {  // 假设一天8节课
                    slots.add(new TimeSlotDTO(week, day, period));
                }
            }
        }

        return slots;
    }

    /**
     * 检查时间槽是否合适
     * <p>
     * 检查给定时间槽是否已被占用，包括教师和教室的时间冲突检查。
     * </p>
     *
     * @param slot 待检查的时间槽
     * @param assignments 已有的课程安排
     * @param teacher 教师信息
     * @param classroom 教室信息
     * @return 时间槽是否合适
     */
    private boolean isTimeSlotSuitable(
            TimeSlotDTO slot,
            Map<TimeSlotDTO, ScheduleItemDTO> assignments,
            TeacherCoursePreferencesDTO teacher,
            ClassroomAndTypeDTO classroom
    ) {
        // 检查该时间槽是否已被占用
        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : assignments.entrySet()) {
            TimeSlotDTO existingSlot = entry.getKey();
            ScheduleItemDTO existingItem = entry.getValue();

            // 如果是同一时间段
            if (slot.getWeek() == existingSlot.getWeek() &&
                    slot.getDayOfWeek() == existingSlot.getDayOfWeek() &&
                    slot.getPeriod() == existingSlot.getPeriod()) {

                // 检查教师是否已被安排
                if (existingItem.getTeacher().getTeacher().getTeacherUuid()
                        .equals(teacher.getTeacher().getTeacherUuid())) {
                    return false;
                }

                // 检查教室是否已被安排
                if (existingItem.getClassroom().getClassroom().getClassroomUuid()
                        .equals(classroom.getClassroom().getClassroomUuid())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 计算连续课程适应度
     * <p>
     * 评估课程表中连续安排的课程情况，并给予适当的奖励。
     * 连续课程通常更有利于学生学习和教师工作安排。
     * </p>
     *
     * @param schedule 待评估的课程表
     * @return 连续课程适应度得分
     */
    private double calculateConsecutiveCoursesFitness(ScheduleDTO schedule) {
        double fitness = 0.0;

        // 按课程分组
        Map<String, List<TimeSlotDTO>> courseSlots = new HashMap<>();
        schedule.getAssignments().forEach((slot, item) -> {
            String courseId = item.getCourse().getCourseLibraryUuid();
            courseSlots.computeIfAbsent(courseId, k -> new ArrayList<>()).add(slot);
        });

        // 检查每个课程的连续性
        for (List<TimeSlotDTO> slots : courseSlots.values()) {
            // 按周和天分组
            Map<Integer, Map<Integer, List<TimeSlotDTO>>> weekDaySlots = slots.stream()
                    .collect(Collectors.groupingBy(
                            TimeSlotDTO::getWeek,
                            Collectors.groupingBy(TimeSlotDTO::getDayOfWeek)
                    ));

            // 检查每天的连续课程
            for (Map<Integer, List<TimeSlotDTO>> daySlots : weekDaySlots.values()) {
                for (List<TimeSlotDTO> dailySlots : daySlots.values()) {
                    if (dailySlots.size() > 1) {
                        // 排序时间槽
                        dailySlots.sort(Comparator.comparingInt(TimeSlotDTO::getPeriod));

                        // 检查连续性
                        for (int i = 0; i < dailySlots.size() - 1; i++) {
                            if (dailySlots.get(i + 1).getPeriod() - dailySlots.get(i).getPeriod() == 1) {
                                fitness += 5.0;  // 连续课程奖励
                            }
                        }
                    }
                }
            }
        }

        return fitness;
    }

    /**
     * 计算时间偏好适应度
     * <p>
     * 评估课程安排与设定的时间偏好的匹配程度，
     * 包括偏好时间段和避免晚课等偏好。
     * </p>
     *
     * @param schedule 待评估的课程表
     * @param preferences 时间偏好设置
     * @return 时间偏好适应度得分
     */
    private double calculateTimePreferenceFitness(
            ScheduleDTO schedule,
            AutomaticClassSchedulingBaseDTO.TimePreferences preferences
    ) {
        double fitness = 0.0;

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slot = entry.getKey();

            // 检查是否在偏好时间段
            boolean inPreferredSlot = preferences.getPreferredTimeSlots().stream()
                    .anyMatch(preferred ->
                            preferred.getDay() == slot.getDayOfWeek() &&
                                    preferred.getPeriodStart() <= slot.getPeriod() &&
                                    preferred.getPeriodEnd() >= slot.getPeriod());

            if (inPreferredSlot) {
                fitness += 10.0;
            }

            // 如果不喜欢晚课但安排在晚上
            if (preferences.getAvoidEveningCourses() && slot.getPeriod() >= 5) {
                fitness -= 5.0;
            }
        }

        return fitness;
    }

    /**
     * 计算教室优化适应度
     * <p>
     * 评估教室分配的合理性，包括容量匹配度和教室类型匹配度。
     * 理想情况下，教室容量应略大于学生数量，教室类型应符合课程需求。
     * </p>
     *
     * @param schedule 待评估的课程表
     * @return 教室优化适应度得分
     */
    private double calculateRoomOptimizationFitness(ScheduleDTO schedule) {
        double fitness = 0.0;

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            ScheduleItemDTO item = entry.getValue();

            // 教室容量与学生数量的匹配度
            int capacity = item.getClassroom().getClassroom().getCapacity();
            int studentCount = item.getCourse().getC();

            if (capacity >= studentCount) {
                // 容量足够，但不要过大
                double utilizationRate = (double) studentCount / capacity;
                // 利用率达到70%以上
                if (utilizationRate >= 0.7) {
                    fitness += 5.0;
                }
            } else {
                // 容量不足，严重惩罚
                fitness -= 50.0;
            }

            // 教室类型匹配
            if (item.getClassroom().getClassroomType().equals(item.getCourse().getRequiredClassroomType())) {
                fitness += 10.0;
            }
        }

        return fitness;
    }
}
