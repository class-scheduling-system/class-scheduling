package com.frontleaves.scheduling.logic;


import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleItemDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.models.dto.scheduling.TimeSlotDTO;
import com.frontleaves.scheduling.services.GeneticSchedulingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class GeneticSchedulingLogic implements GeneticSchedulingService {
    private final Map<String, Integer> taskProgress = new HashMap<>();
    private final Map<String, String> taskStatus = new HashMap<>();

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
                        .setDepartmentId(baseDTO.getDepartment().getDepartmentId())
                        .setStatus("completed")
                        .setProgress(100)
                        .setAssignments(assignments)
                        .setConflicts(conflicts)
                        .setResourceUtilization(utilization)
                        .setFitness(bestFitness);
            }

            throw new RuntimeException("未能生成有效的课程表");

        } catch (Exception e) {
            log.error("排课过程发生错误", e);
            taskStatus.put(taskId, "排课失败: " + e.getMessage());
            throw e;
        }
    }

    private void evaluatePopulation(List<ScheduleDTO> population, AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO schedule : population) {
            double fitness = calculateFitness(schedule, baseDTO);
            schedule.setFitness(fitness);
        }
    }

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
                    if (item1.getTeacher().getTeacherUuid().equals(item2.getTeacher().getTeacherUuid())) {
                        penalty += 100.0;
                    }

                    // 教室冲突
                    if (item1.getClassroom().getClassroomUuid().equals(item2.getClassroom().getClassroomUuid())) {
                        penalty += 100.0;
                    }
                }
            }
        }

        return penalty;
    }

    /**
     * 查找课程表中的冲突
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
                    if (item1.getTeacher().getTeacherUuid().equals(item2.getTeacher().getTeacherUuid())) {
                        conflicts.add(new ScheduleResultDTO.SchedulingConflictDTO()
                                .setType("teacher")
                                .setDescription(String.format(
                                        "教师 %s 在第%d周星期%d第%d节课有重复安排",
                                        item1.getTeacher().getTeacherName(),
                                        slot1.getWeek(),
                                        slot1.getDayOfWeek(),
                                        slot1.getPeriod()
                                ))
                                .setDetails(Map.of(
                                        "teacherId", item1.getTeacher().getTeacher().getTeacherUuid(),
                                        "week", String.valueOf(slot1.getWeek()),
                                        "day", String.valueOf(slot1.getDayOfWeek()),
                                        "period", String.valueOf(slot1.getPeriod())
                                )));
                    }

                    // 教室冲突
                    if (item1.getClassroom().getClassroomUuid().equals(item2.getClassroom().getClassroomUuid())) {
                        conflicts.add(new ScheduleResultDTO.SchedulingConflictDTO()
                                .setType("classroom")
                                .setDescription(String.format(
                                        "教室 %s 在第%d周星期%d第%d节课有重复安排",
                                        item1.getClassroom().getClassroomName(),
                                        slot1.getWeek(),
                                        slot1.getDayOfWeek(),
                                        slot1.getPeriod()
                                ))
                                .setDetails(Map.of(
                                        "classroomId", item1.getClassroom().getClassroomUuid(),
                                        "week", String.valueOf(slot1.getWeek()),
                                        "day", String.valueOf(slot1.getDayOfWeek()),
                                        "period", String.valueOf(slot1.getPeriod())
                                )));
                    }
                }
            }
        }

        return conflicts;
    }

    /**
     * 计算资源利用率
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
            String roomId = item.getClassroom().getClassroomUuid();
            roomUsage.merge(roomId, 1, Integer::sum);
            roomCapacity.putIfAbsent(roomId, item.getClassroom().getClassroomCapacity());

            // 教师工作量统计
            String teacherId = item.getTeacher().getTeacherUuid();
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
            String courseId = entry.getValue().getCourse().getCourseUuid();
            courseAssignments1.computeIfAbsent(courseId, k -> new ArrayList<>()).add(entry);
        }

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : parent2.getAssignments().entrySet()) {
            String courseId = entry.getValue().getCourse().getCourseUuid();
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

    private void classroomMutation(ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        Random random = new Random();
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());

        if (!entries.isEmpty()) {
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseLibraryDTO course = entry.getValue().getCourse();

            // 尝试分配新教室
            ClassroomAndTypeDTO newClassroom = selectClassroomForCourse(course, baseDTO.getClassroomAndType());

            if (newClassroom != null && !newClassroom.getClassroomUuid().equals(entry.getValue().getClassroom().getClassroomUuid())) {
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

    private void teacherMutation(ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
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
                    if (item1.getTeacher().getTeacherUuid().equals(item2.getTeacher().getTeacherUuid())) {
                        return true;
                    }

                    // 检查教室冲突
                    if (item1.getClassroom().getClassroomUuid().equals(item2.getClassroom().getClassroomUuid())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

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

    @Override
    public int getSchedulingProgress(String taskId) {
        return taskProgress.getOrDefault(taskId, 0);
    }

    @Override
    public String getSchedulingStatus(String taskId) {
        return taskStatus.getOrDefault(taskId, "unknown");
    }

    private void updateProgress(String taskId, int progress) {
        taskProgress.put(taskId, progress);
    }

    /**
     * 生成初始种群
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
     */
    private TeacherCoursePreferencesDTO selectTeacherForCourse(
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
     */
    private ClassroomAndTypeDTO selectClassroomForCourse(
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
     */
    private TimeSlotDTO findSuitableTimeSlot(
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
     */
    private List<TimeSlotDTO> generateAllTimeSlots(AutomaticClassSchedulingBaseDTO baseDTO) {
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
                if (existingItem.getTeacher().getTeacherUuid().equals(teacher.getTeacherUuid())) {
                    return false;
                }

                // 检查教室是否已被安排
                if (existingItem.getClassroom().getClassroomUuid().equals(classroom.getClassroomUuid())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 计算连续课程适应度
     */
    private double calculateConsecutiveCoursesFitness(ScheduleDTO schedule) {
        double fitness = 0.0;

        // 按课程分组
        Map<String, List<TimeSlotDTO>> courseSlots = new HashMap<>();
        schedule.getAssignments().forEach((slot, item) -> {
            String courseId = item.getCourse().getCourseUuid();
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
     */
    private double calculateRoomOptimizationFitness(ScheduleDTO schedule) {
        double fitness = 0.0;

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            ScheduleItemDTO item = entry.getValue();

            // 教室容量与学生数量的匹配度
            int capacity = item.getClassroom().getClassroomCapacity();
            int studentCount = item.getCourse().getStudentCount();

            if (capacity >= studentCount) {
                // 容量足够，但不要过大
                double utilizationRate = (double) studentCount / capacity;
                if (utilizationRate >= 0.7) {  // 利用率达到70%以上
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