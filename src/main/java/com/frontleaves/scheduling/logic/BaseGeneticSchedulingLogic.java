/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.models.dto.base.*;
import com.frontleaves.scheduling.models.dto.merge.ClassroomAndTypeDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
class BaseGeneticSchedulingLogic {
    /**
     * Redis客户端，用于缓存和分布式锁
     */
    final RedissonClient redisson;

    /**
     * 安全随机数生成器
     * 使用 SecureRandom 而不是普通的 Random 来确保随机性的安全性和不可预测性
     * 在遗传算法中，这种不可预测性对于确保种群多样性和避免陷入局部最优解是很重要的
     */
    private final SecureRandom random = new SecureRandom();

    /**
     * 获取课程表中的所有所需教室ID
     *
     * @param course 课程表
     * @return 所需教室ID列表
     */
    private String getCourseType(@NotNull CourseLibraryDTO course) {
        String theoryType = course.getTheoryClassroomType();
        if (theoryType != null) {
            return theoryType;
        }
        String experimentType = course.getExperimentClassroomType();
        if (experimentType != null) {
            return experimentType;
        }
        String practiceType = course.getPracticeClassroomType();
        if (practiceType != null) {
            return practiceType;
        }
        return course.getComputerClassroomType();
    }

    /**
     * 获取任务进度对应的Redis键
     *
     * @param taskId 任务ID
     * @return Redis键名
     */
    protected String getProgressKey(String taskId) {
        return StringConstant.Redis.SCHEDULE_EXECUTE_PROGRESS + taskId;
    }

    /**
     * 获取任务状态对应的Redis键
     *
     * @param taskId 任务ID
     * @return Redis键名
     */
    protected String getStatusKey(String taskId) {
        return StringConstant.Redis.SCHEDULE_EXECUTE_STATUS + taskId;
    }

    /**
     * 评估种群中所有个体的适应度
     *
     * @param allPopulation 待评估的种群
     * @param baseDTO       排课基础数据
     */
    void evaluatePopulation(@NotNull List<ScheduleDTO> allPopulation, AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO population : allPopulation) {
            double fitness = this.calculateFitness(population, baseDTO);
            population.setFitness(fitness);
        }
    }

    /**
     * 计算整个课程表的适应度得分
     * 通过计算每个课程的适应度，然后取平均值
     *
     * @param schedule 待评估的课程表
     * @param baseDTO  排课基础数据，包含约束条件
     * @return 适应度得分，值越高表示排课方案越优
     */
    double calculateFitness(@NotNull ScheduleDTO schedule, @NotNull AutomaticClassSchedulingBaseDTO baseDTO) {
        if (schedule.getSchedule() == null || schedule.getSchedule().isEmpty()) {
            return 0.0;
        }
        double totalFitness = 0.0;
        int courseCount = schedule.getSchedule().size();
        // 遍历每个课程计算适应度
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 基础分数
            double courseFitness = 100.0;
            // 减去冲突惩罚
            courseFitness -= calculateConflictPenalty(courseSchedule);
            // 连续课程适应度
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getConsecutiveCoursesPreferred())) {
                courseFitness += calculateConsecutiveCoursesFitness(courseSchedule);
            }
            // 时间偏好适应度
            courseFitness += calculateTimePreferenceFitness(courseSchedule, baseDTO.getTimePreferences());
            // 教室优化适应度
            if (Boolean.TRUE.equals(baseDTO.getConstraints().getRoomOptimization())) {
                courseFitness += calculateRoomOptimizationFitness(courseSchedule);
            }
            // 确保单个课程的适应度不为负
            courseFitness = Math.max(0.0, courseFitness);
            // 将课程适应度添加到总适应度
            totalFitness += courseFitness;
            // 更新单个课程的适应度
            courseSchedule.setFitness(courseFitness);
        }
        // 计算平均适应度
        double averageFitness = totalFitness / courseCount;
        // 更新整个课程表的适应度
        schedule.setFitness(averageFitness);
        return averageFitness;
    }

    /**
     * 计算课程安排的冲突惩罚值
     *
     * @param schedule 课程表
     * @return 冲突惩罚值
     */
    private double calculateConflictPenalty(@NotNull CourseScheduleDTO schedule) {
        List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries =
                new ArrayList<>(schedule.getAssignments().entrySet());
        double totalPenalty = 0.0;
        // 遍历所有课程安排
        for (int i = 0; i < entries.size(); i++) {
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1 = entries.get(i);
            List<TimeSlotDTO> slots1 = entry1.getKey();
            CourseScheduleItemDTO item1 = entry1.getValue();
            // 遍历其他课程安排
            for (int j = i + 1; j < entries.size(); j++) {
                Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2 = entries.get(j);
                List<TimeSlotDTO> slots2 = entry2.getKey();
                CourseScheduleItemDTO item2 = entry2.getValue();
                // 检查时间冲突
                boolean hasTimeConflict = false;
                for (TimeSlotDTO slot1 : slots1) {
                    for (TimeSlotDTO slot2 : slots2) {
                        if (Objects.equals(slot1.getWeek(), slot2.getWeek())
                                && Objects.equals(slot1.getDay(), slot2.getDay())
                                && Objects.equals(slot1.getPeriod(), slot2.getPeriod())) {
                            hasTimeConflict = true;
                            break;
                        }
                    }
                    if (hasTimeConflict) {
                        break;
                    }
                }
                // 如果有时间冲突，检查教师和教室冲突
                if (hasTimeConflict) {
                    // 教师冲突检查
                    String teacher1Uuid = item1.getTeacher().getTeacher().getTeacherUuid();
                    String teacher2Uuid = item2.getTeacher().getTeacher().getTeacherUuid();
                    if (teacher1Uuid.equals(teacher2Uuid)) {
                        totalPenalty += 100.0;
                    }
                    // 教室冲突检查
                    String room1Uuid = item1.getClassroom().getClassroom().getClassroomUuid();
                    String room2Uuid = item2.getClassroom().getClassroom().getClassroomUuid();
                    if (room1Uuid.equals(room2Uuid)) {
                        totalPenalty += 100.0;
                    }
                }
            }
        }

        return totalPenalty;
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
    /**
     * 查找课程表中的冲突
     * 检查教师冲突和教室冲突
     *
     * @param schedule 待检查的课程表
     * @return 冲突列表
     */
    List<SchedulingConflictDTO> findConflicts(@NotNull ScheduleDTO schedule) {
        List<SchedulingConflictDTO> conflicts = new ArrayList<>();

        // 获取所有课程安排
        List<CourseScheduleDTO> courseSchedules = schedule.getSchedule();

        // 遍历每个课程安排
        for (int i = 0; i < courseSchedules.size(); i++) {
            CourseScheduleDTO courseSchedule1 = courseSchedules.get(i);
            List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries1 =
                    new ArrayList<>(courseSchedule1.getAssignments().entrySet());

            // 检查当前课程安排内部的冲突
            for (int j = 0; j < entries1.size(); j++) {
                Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1 = entries1.get(j);
                List<TimeSlotDTO> slots1 = entry1.getKey();
                CourseScheduleItemDTO item1 = entry1.getValue();

                // 与同一课程安排中的其他时间槽比较
                for (int k = j + 1; k < entries1.size(); k++) {
                    this.checkConflicts(conflicts, slots1, item1, entries1.get(k));
                }

                // 与其他课程安排比较
                for (int m = i + 1; m < courseSchedules.size(); m++) {
                    CourseScheduleDTO courseSchedule2 = courseSchedules.get(m);
                    for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2 :
                            courseSchedule2.getAssignments().entrySet()) {
                        this.checkConflicts(conflicts, slots1, item1, entry2);
                    }
                }
            }
        }

        return conflicts;
    }

    /**
     * 检查两个课程安排之间的冲突
     */
    private void checkConflicts(
            List<SchedulingConflictDTO> conflicts,
            @NotNull List<TimeSlotDTO> slots1,
            CourseScheduleItemDTO item1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2
    ) {
        List<TimeSlotDTO> slots2 = entry2.getKey();
        CourseScheduleItemDTO item2 = entry2.getValue();

        // 检查时间冲突
        for (TimeSlotDTO slot1 : slots1) {
            for (TimeSlotDTO slot2 : slots2) {
                if (this.isTimeSlotConflict(slot1, slot2)) {
                    // 检查教师冲突
                    if (this.isTeacherConflict(item1, item2)) {
                        conflicts.add(this.createTeacherConflict(item1, slot1));
                    }
                    // 检查教室冲突
                    if (this.isClassroomConflict(item1, item2)) {
                        conflicts.add(createClassroomConflict(item1, slot1));
                    }
                }
            }
        }
    }

    /**
     * 检查时间槽是否冲突
     */
    private boolean isTimeSlotConflict(@NotNull TimeSlotDTO slot1, @NotNull TimeSlotDTO slot2) {
        return slot1.getWeek().equals(slot2.getWeek()) &&
                slot1.getDay().equals(slot2.getDay()) &&
                slot1.getPeriod().equals(slot2.getPeriod());
    }

    /**
     * 检查教师是否冲突
     */
    private boolean isTeacherConflict(@NotNull CourseScheduleItemDTO item1, @NotNull CourseScheduleItemDTO item2) {
        return item1.getTeacher().getTeacher().getTeacherUuid()
                .equals(item2.getTeacher().getTeacher().getTeacherUuid());
    }

    /**
     * 检查教室是否冲突
     */
    private boolean isClassroomConflict(@NotNull CourseScheduleItemDTO item1, @NotNull CourseScheduleItemDTO item2) {
        return item1.getClassroom().getClassroom().getClassroomUuid()
                .equals(item2.getClassroom().getClassroom().getClassroomUuid());
    }

    /**
     * 创建教师冲突记录
     */
    private SchedulingConflictDTO createTeacherConflict(@NotNull CourseScheduleItemDTO item, @NotNull TimeSlotDTO slot) {
        return new SchedulingConflictDTO()
                .setConflictType(1)
                .setDescription(String.format(
                        "教师 %s 在第%d周星期%d第%d节课有重复安排",
                        item.getTeacher().getTeacher().getName(),
                        slot.getWeek(),
                        slot.getDay(),
                        slot.getPeriod()
                ));
    }

    /**
     * 创建教室冲突记录
     */
    private SchedulingConflictDTO createClassroomConflict(@NotNull CourseScheduleItemDTO item, @NotNull TimeSlotDTO slot) {
        return new SchedulingConflictDTO()
                .setConflictType(2)
                .setDescription(String.format(
                        "教室 %s 在第%d周星期%d第%d节课有重复安排",
                        item.getClassroom().getClassroom().getName(),
                        slot.getWeek(),
                        slot.getDay(),
                        slot.getPeriod()
                ));
    }

    /**
     * 计算资源利用率
     * <p>
     * 评估课程表中各种资源的利用情况，包括：
     * - 教室利用率：衡量教室容量与实际使用情况的匹配度
     * - 教师工作量：评估教师课程分配的均衡性
     * - 时间槽使用率：评估时间资源的利用效率
     * - 总体利用率：综合上述三项指标的平均值
     *
     * @param schedule 待评估的课程表
     * @return 资源利用率指标
     */
    ScheduleResultDTO.ResourceUtilization calculateResourceUtilization(@NotNull ScheduleDTO schedule) {
        // 教室利用率
        Map<String, Integer> roomUsage = new HashMap<>();
        Map<String, Integer> roomCapacity = new HashMap<>();

        // 教师工作量
        Map<String, Integer> teacherWorkload = new HashMap<>();

        // 时间槽使用情况
        Set<String> usedTimeSlots = new HashSet<>();
        int totalTimeSlots = 0;

        // 遍历所有课程安排
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 遍历每个课程的时间槽和排课项
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : courseSchedule.getAssignments().entrySet()) {
                List<TimeSlotDTO> slots = entry.getKey();
                CourseScheduleItemDTO item = entry.getValue();

                // 教室使用统计
                String roomId = item.getClassroom().getClassroom().getClassroomUuid();
                roomUsage.merge(roomId, slots.size(), Integer::sum);
                roomCapacity.putIfAbsent(roomId, item.getClassroom().getClassroom().getCapacity());

                // 教师工作量统计
                String teacherId = item.getTeacher().getTeacher().getTeacherUuid();
                teacherWorkload.merge(teacherId, slots.size(), Integer::sum);

                // 时间槽使用统计
                for (TimeSlotDTO slot : slots) {
                    String timeSlotKey = String.format("%d-%d-%d",
                            slot.getWeek(), slot.getDay(), slot.getPeriod());
                    usedTimeSlots.add(timeSlotKey);
                    totalTimeSlots++;
                }
            }
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
                .orElse(0.0) / (totalTimeSlots > 0 ? totalTimeSlots : 1);

        // 计算时间槽利用率
        double timeSlotUtilization = totalTimeSlots > 0 ?
                (double) usedTimeSlots.size() / totalTimeSlots : 0.0;

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
     * 创建课程表对象的深拷贝，包括所有课程安排的完整复制，
     * 确保原对象和复制对象完全独立，避免引用共享导致的意外修改。
     *
     * @param schedule 源课程表对象
     * @return 深拷贝后的新课程表对象
     */
    @NotNull ScheduleDTO deepCopySchedule(@NotNull ScheduleDTO schedule) {
        ScheduleDTO copy = new ScheduleDTO();
        List<CourseScheduleDTO> courseSchedules = new ArrayList<>();
        // 复制每个课程安排
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            CourseScheduleDTO courseScheduleCopy = new CourseScheduleDTO();
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = new HashMap<>();
            // 复制每个时间槽列表和排课项
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : courseSchedule.getAssignments().entrySet()) {
                // 复制时间槽列表
                List<TimeSlotDTO> timeSlotsCopy = entry.getKey().stream()
                        .map(slot -> new TimeSlotDTO(
                                slot.getWeek(),
                                slot.getDay(),
                                slot.getPeriod()
                        ))
                        .toList();
                // 复制排课项
                CourseScheduleItemDTO itemCopy = new CourseScheduleItemDTO(entry.getValue());
                // 将复制的对象添加到新的映射中
                assignments.put(timeSlotsCopy, itemCopy);
            }

            // 设置课程安排的属性
            courseScheduleCopy.setAssignments(assignments);
            courseScheduleCopy.setFitness(courseSchedule.getFitness());
            // 添加到课程安排列表
            courseSchedules.add(courseScheduleCopy);
        }
        // 设置课程表的属性
        copy.setSchedule(courseSchedules);
        copy.setFitness(schedule.getFitness());
        return copy;
    }

    /**
     * 选择操作
     * 基于轮盘赌算法进行个体选择，适应度越高的个体被选中的概率越大。
     * 该方法是遗传算法中模拟自然选择的过程，使得优质个体有更多机会繁殖下一代。
     *
     * @param population 当前种群
     * @return 选择后的种群（通过深拷贝创建）
     */
    @NotNull List<ScheduleDTO> selection(@NotNull List<ScheduleDTO> population) {
        List<ScheduleDTO> selected = new ArrayList<>();

        // 计算总适应度
        double totalFitness = population.stream()
                .mapToDouble(ScheduleDTO::getFitness)
                .sum();
        // 使用轮盘赌选择算法选择个体
        while (selected.size() < population.size()) {
            // 生成随机点
            double point = random.nextDouble() * totalFitness;
            double sum = 0;

            // 轮盘赌选择
            for (ScheduleDTO schedule : population) {
                sum += schedule.getFitness();
                if (sum >= point) {
                    // 选中当前个体，进行深拷贝
                    selected.add(deepCopySchedule(schedule));
                    break;
                }
            }
        }

        return selected;
    }

    /**
     * 交叉操作
     * 对选择后的个体按照交叉率进行交叉操作，生成新的后代。
     * 交叉操作是遗传算法中模拟基因重组的过程，通过将两个父代个体的部分特征组合，产生具有新特性的后代。
     *
     * @param selected      经过选择的个体
     * @param crossoverRate 交叉概率
     * @return 交叉后产生的后代
     */
    List<ScheduleDTO> crossover(@NotNull List<ScheduleDTO> selected, double crossoverRate,
                                AutomaticClassSchedulingBaseDTO baseDTO) {
        List<ScheduleDTO> offspring = new ArrayList<>();
        // 两两配对进行交叉
        for (int i = 0; i < selected.size() - 1; i += 2) {
            ScheduleDTO parent1 = selected.get(i);
            ScheduleDTO parent2 = selected.get(i + 1);
            if (random.nextDouble() < crossoverRate) {
                // 执行交叉
                List<ScheduleDTO> children = crossoverSchedules(parent1, parent2, baseDTO);
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
     * 实现两个父代课程表之间的交叉操作，通过随机选择交叉点，
     * 将两个父代的课程安排按照课程ID进行混合，生成两个新的子代课程表。
     *
     * @param parent1 第一个父代课程表
     * @param parent2 第二个父代课程表
     * @return 交叉后生成的两个子代课程表
     */
    private @NotNull List<ScheduleDTO> crossoverSchedules(@NotNull ScheduleDTO parent1, ScheduleDTO parent2,
                                                          AutomaticClassSchedulingBaseDTO baseDTO) {
        List<ScheduleDTO> children = new ArrayList<>();

        // 创建两个子代
        ScheduleDTO child1 = new ScheduleDTO();
        ScheduleDTO child2 = new ScheduleDTO();
        List<CourseScheduleDTO> child1Schedules = new ArrayList<>();
        List<CourseScheduleDTO> child2Schedules = new ArrayList<>();

        // 获取父代的课程安排
        List<CourseScheduleDTO> parent1Schedules = parent1.getSchedule();
        List<CourseScheduleDTO> parent2Schedules = parent2.getSchedule();

        // 按课程ID分组
        Map<String, CourseScheduleDTO> courseSchedules1 = new HashMap<>();
        Map<String, CourseScheduleDTO> courseSchedules2 = new HashMap<>();

        for (CourseScheduleDTO schedule : parent1Schedules) {
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
                String courseId = entry.getValue().getCourse().getCourseLibraryUuid();
                courseSchedules1.put(courseId, schedule);
            }
        }

        for (CourseScheduleDTO schedule : parent2Schedules) {
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
                String courseId = entry.getValue().getCourse().getCourseLibraryUuid();
                courseSchedules2.put(courseId, schedule);
            }
        }

        // 获取所有课程ID
        Set<String> allCourses = new HashSet<>();
        allCourses.addAll(courseSchedules1.keySet());
        allCourses.addAll(courseSchedules2.keySet());

        // 随机选择交叉点
        int crossoverPoint = random.nextInt(!allCourses.isEmpty() ? allCourses.size() : 1);

        // 执行交叉
        int count = 0;
        for (String courseId : allCourses) {
            if (count < crossoverPoint) {
                // 从父代1复制到子代1，从父代2复制到子代2
                if (courseSchedules1.containsKey(courseId)) {
                    child1Schedules.add(deepCopyCourseSchedule(courseSchedules1.get(courseId)));
                }
                if (courseSchedules2.containsKey(courseId)) {
                    child2Schedules.add(deepCopyCourseSchedule(courseSchedules2.get(courseId)));
                }
            } else {
                // 从父代2复制到子代1，从父代1复制到子代2
                if (courseSchedules2.containsKey(courseId)) {
                    child1Schedules.add(deepCopyCourseSchedule(courseSchedules2.get(courseId)));
                }
                if (courseSchedules1.containsKey(courseId)) {
                    child2Schedules.add(deepCopyCourseSchedule(courseSchedules1.get(courseId)));
                }
            }
            count++;
        }
        // 设置子代的课程安排
        child1.setSchedule(child1Schedules);
        child2.setSchedule(child2Schedules);
        // 计算子代的适应度
        child1.setFitness(calculateFitness(child1, baseDTO));
        child2.setFitness(calculateFitness(child2, baseDTO));
        children.add(child1);
        children.add(child2);

        return children;
    }

    /**
     * 从父代复制课程安排到子代
     *
     * @param sourceSchedule 父代的课程安排
     * @return 复制的课程安排
     */
    private @NotNull CourseScheduleDTO deepCopyCourseSchedule(@NotNull CourseScheduleDTO sourceSchedule) {
        CourseScheduleDTO targetSchedule = new CourseScheduleDTO();
        Map<List<TimeSlotDTO>, CourseScheduleItemDTO> targetAssignments = new HashMap<>();

        // 复制时间槽列表和排课项
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : sourceSchedule.getAssignments().entrySet()) {
            // 复制时间槽列表
            List<TimeSlotDTO> timeSlotsCopy = entry.getKey().stream()
                    .map(slot -> new TimeSlotDTO(
                            slot.getWeek(),
                            slot.getDay(),
                            slot.getPeriod()
                    ))
                    .toList();
            // 复制排课项
            CourseScheduleItemDTO itemCopy = new CourseScheduleItemDTO(entry.getValue());
            targetAssignments.put(timeSlotsCopy, itemCopy);
        }
        // 设置复制的课程安排
        targetSchedule.setAssignments(targetAssignments);
        targetSchedule.setFitness(sourceSchedule.getFitness());
        return targetSchedule;
    }

    /**
     * 变异操作
     * <p>
     * 按照变异率对个体进行变异，通过随机改变课程安排的某些属性，
     * 增加种群的多样性，避免陷入局部最优解。
     * 包括三种变异策略：时间槽变异、教室变异和教师变异。
     * </p>
     *
     * @param population   当前种群
     * @param mutationRate 变异概率
     * @param baseDTO      排课基础数据
     */
    void mutation(@NotNull List<ScheduleDTO> population, double mutationRate, AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO schedule : population) {
            List<CourseScheduleDTO> schedules = schedule.getSchedule();
            if (schedules == null || schedules.isEmpty()) {
                continue; // 跳过空的课程表
            }
            // 随机选择一门课程进行变异
            CourseScheduleDTO courseSchedule = schedules.get(random.nextInt(schedules.size()));
            if (random.nextDouble() < mutationRate) {
                // 选择变异策略
                int strategy = random.nextInt(3);
                try {
                    switch (strategy) {
                        case 0:
                            // 时间槽变异
                            this.timeSlotMutation(courseSchedule, baseDTO.getCourseList(), baseDTO, schedules);
                            break;
                        case 1:
                            // 教室变异
                            classroomMutation(courseSchedule, baseDTO);
                            break;
                        case 2:
                            // 教师变异
                            teacherMutation(courseSchedule, baseDTO);
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    log.error("变异操作失败: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 根据课程ID在 基础排课数据 中查找老师资格表
     *
     * @param courseUuid 课程ID
     * @param courseList 课程列表
     * @return 匹配的课程，若找不到则返回null
     */
    private @Nullable CourseLibraryAndTeacherCourseQualificationListDTO findCourseById(String courseUuid, @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList) {
        for (CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeacher : courseList) {
            if (courseAndTeacher.getCourse().getCourseLibraryUuid().equals(courseUuid)) {
                return courseAndTeacher;
            }
        }
        return null;
    }


    /**
     * 教室变异操作
     * 随机选择一个课程安排，尝试为其分配新的教室
     *
     * @param schedule 课程表
     * @param baseDTO  基础数据
     */
    private void classroomMutation(@NotNull CourseScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        // 获取所有课程安排
        List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries =
                new ArrayList<>(schedule.getAssignments().entrySet());
        if (!entries.isEmpty()) {
            // 随机选择一个课程安排
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseScheduleItemDTO currentItem = entry.getValue();
            CourseLibraryDTO course = currentItem.getCourse();
            // 查找可以教授这门课程的教室列表
            CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeacher = this.findCourseById(
                    course.getCourseLibraryUuid(),
                    baseDTO.getCourseList()
            );
            if (courseAndTeacher != null) {
                // 获取可用的教室列表
                Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> classroomList =
                        this.selectClassroomsForCourse(courseAndTeacher, baseDTO.getClassroomList());
                if (classroomList != null && !classroomList.isEmpty()) {
                    // 过滤掉当前教室
                    List<Map.Entry<List<AdministrativeClassDTO>, ClassroomInfoDTO>> availableClassrooms =
                            classroomList.entrySet().stream()
                                    .filter(classroomEntry ->
                                            !classroomEntry.getValue().getClassroom().getClassroomUuid()
                                                    .equals(currentItem.getClassroom().getClassroom().getClassroomUuid()))
                                    .toList();
                    if (!availableClassrooms.isEmpty()) {
                        // 随机选择一个新教室
                        Map.Entry<List<AdministrativeClassDTO>, ClassroomInfoDTO> newClassroomEntry =
                                availableClassrooms.get(random.nextInt(availableClassrooms.size()));
                        // 创建新的排课项
                        CourseScheduleItemDTO newItem = new CourseScheduleItemDTO(
                                course,
                                currentItem.getTeacher(),
                                newClassroomEntry.getValue(),
                                //使用原来的班级
                                currentItem.getClassGroup(),
                                currentItem.getCourseType(),
                                currentItem.getPriority()
                        );
                        // 更新课程安排
                        schedule.getAssignments().put(entry.getKey(), newItem);
                    }
                }
            }
        }
    }

    /**
     * 教师变异操作
     * 随机选择一个课程安排，尝试为其分配新的教师
     *
     * @param schedule 课程表
     * @param baseDTO  基础数据
     */
    private void teacherMutation(@NotNull CourseScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        // 获取所有课程安排
        List<Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO>> entries =
                new ArrayList<>(schedule.getAssignments().entrySet());

        if (!entries.isEmpty()) {
            // 随机选择一个课程安排
            Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseScheduleItemDTO currentItem = entry.getValue();
            CourseLibraryDTO course = currentItem.getCourse();
            // 查找可以教授这门课程的教师列表
            List<TeacherCoursePreferencesDTO> suitableTeachers = baseDTO.getCourseList().stream()
                    .filter(ct -> ct.getCourse().getCourseLibraryUuid().equals(course.getCourseLibraryUuid()))
                    .flatMap(ct -> ct.getTeacherList().stream())
                    .filter(teacher -> !teacher.getTeacher().getTeacherUuid()
                            .equals(currentItem.getTeacher().getTeacher().getTeacherUuid()))
                    .toList();
            if (!suitableTeachers.isEmpty()) {
                // 随机选择一个新教师
                TeacherCoursePreferencesDTO newTeacher = suitableTeachers.get(random.nextInt(suitableTeachers.size()));
                // 创建新的排课项
                CourseScheduleItemDTO newItem = new CourseScheduleItemDTO(
                        course,
                        newTeacher,
                        currentItem.getClassroom(),
                        currentItem.getClassGroup(),
                        currentItem.getCourseType(),
                        currentItem.getPriority()
                );
                // 更新课程安排
                schedule.getAssignments().put(entry.getKey(), newItem);
            }
        }
    }

    /**
     * 检查课程安排是否存在冲突
     *
     * @param assignments 课程安排映射
     * @return 是否存在冲突
     */
    boolean hasConflicts(@NotNull Map<TimeSlotDTO, CourseScheduleItemDTO> assignments) {
        return assignments.entrySet().stream()
                .anyMatch(entryFirst ->
                        assignments.entrySet().stream()
                                .filter(entrySecond -> entryFirst != entrySecond)
                                .anyMatch(entrySecond -> {
                                    TimeSlotDTO slotFirst = entryFirst.getKey();
                                    TimeSlotDTO slotSecond = entrySecond.getKey();
                                    CourseScheduleItemDTO item1 = entryFirst.getValue();
                                    CourseScheduleItemDTO item2 = entrySecond.getValue();

                                    // 如果不在同一时间段，则无冲突
                                    if (slotFirst.getWeek() != slotSecond.getWeek() ||
                                            slotFirst.getDay() != slotSecond.getDay() ||
                                            slotFirst.getPeriod() != slotSecond.getPeriod()) {
                                        return false;
                                    }

                                    // 检查教师冲突
                                    boolean teacherConflict = item1.getTeacher().getTeacher().getTeacherUuid()
                                            .equals(item2.getTeacher().getTeacher().getTeacherUuid());

                                    // 检查教室冲突
                                    boolean roomConflict = item1.getClassroom().getClassroom().getClassroomUuid()
                                            .equals(item2.getClassroom().getClassroom().getClassroomUuid());

                                    return teacherConflict || roomConflict;
                                })
                );
    }


    /**
     * 转换课程表为课程安排列表
     * 将内部使用的课程表模型转换为前端展示所需的课程安排格式。
     *
     * @param schedule 内部课程表模型
     * @return 课程安排列表
     */
    List<ScheduleResultDTO.ClassAssignmentDTO> convertScheduleToAssignments(@NotNull ScheduleDTO schedule) {
        List<ScheduleResultDTO.ClassAssignmentDTO> assignments = new ArrayList<>();
        // 遍历所有课程安排
        for (CourseScheduleDTO courseSchedule : schedule.getSchedule()) {
            // 遍历每个课程的时间槽和排课项
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : courseSchedule.getAssignments().entrySet()) {
                List<TimeSlotDTO> slots = entry.getKey();
                CourseScheduleItemDTO item = entry.getValue();
                // 为每个时间槽创建课程安排
                for (TimeSlotDTO slot : slots) {
                    ScheduleResultDTO.TimeSlot timeSlot = new ScheduleResultDTO.TimeSlot()
                            .setWeek(slot.getWeek())
                            .setDayOfWeek(slot.getDay())
                            .setPeriod(slot.getPeriod());
                    ScheduleResultDTO.ClassAssignmentDTO assignment = new ScheduleResultDTO.ClassAssignmentDTO()
                            .setCourse(item.getCourse())
                            .setTeacher(item.getTeacher())
                            .setClassroom(item.getClassroom())
                            .setClassGroup(item.getClassGroup())
                            .setTimeSlot(timeSlot)
                            .setPriority(item.getPriority());

                    assignments.add(assignment);
                }
            }
        }

        return assignments;
    }

    /**
     * 更新排课任务的进度
     *
     * @param taskId   任务ID
     * @param progress 进度值（0-100）
     */
    void updateProgress(String taskId, int progress) {
        String key = getProgressKey(taskId);
        RBucket<Integer> processList = redisson.getBucket(key);
        processList.set(progress);
    }

    /**
     * 更新排课任务的状态
     *
     * @param taskId 任务ID
     * @param status 状态描述
     */
    void updateStatus(String taskId, String status) {
        String key = getStatusKey(taskId);
        RList<String> getBuket = redisson.getList(key);
        getBuket.add(status);
    }

    /**
     * 生成初始种群
     * <p>
     * 根据排课基础数据生成初始种群，为每个课程分配合适的教师、教室和时间槽。
     * 初始种群的质量会影响算法的收敛速度和最终结果。
     * </p>
     *
     * @param baseData 排课基础数据
     * @return 初始种群
     */
    List<ScheduleDTO> generateInitialPopulation(@NotNull AutomaticClassSchedulingBaseDTO baseData) {
        // 初始化种群列表
        List<ScheduleDTO> allPopulation = new ArrayList<>();
        int populationSize = baseData.getAlgorithmParams().getPopulationSize();
        for (int i = 0; i < populationSize; i++) {
            CourseScheduleDTO schedule = new CourseScheduleDTO();
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = new HashMap<>();
            log.debug("生成第 {} 个个体", i + 1);
            List<CourseScheduleDTO> population = new ArrayList<>();
            // 为每个课程分配时间槽
            for (CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers : baseData.getCourseList()) {
                CourseLibraryDTO course = courseAndTeachers.getCourse();
                // 按照课程和班级进行教师选择
                Map<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> teacherAssignments = new HashMap<>();
                Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> classroomAssignments =
                        this.selectClassroomsForCourse(courseAndTeachers, baseData.getClassroomList());
                // 为每个班级选择随机的教师
                if (classroomAssignments != null) {
                    for (Map.Entry<List<AdministrativeClassDTO>, ClassroomInfoDTO> entry : classroomAssignments.entrySet()) {
                        List<AdministrativeClassDTO> classGroup = entry.getKey();
                        TeacherCoursePreferencesDTO teacher = this.selectTeacherForCourse(
                                course, courseAndTeachers.getTeacherList());
                        // 记录教师分配
                        if (teacher != null) {
                            teacherAssignments.put(classGroup, teacher);
                        }
                    }
                }
                // 为每门课程分配时间槽，这一步分配了时间，班级，教室，老师
                for (Map.Entry<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> entry : teacherAssignments.entrySet()) {
                    List<AdministrativeClassDTO> classGroup = entry.getKey();
                    TeacherCoursePreferencesDTO assignedTeacher = entry.getValue();
                    ClassroomInfoDTO assignedClassroom = classroomAssignments.get(classGroup);
                    // 寻找合适的时间槽
                    List<TimeSlotDTO> timeSlot = findSuitableTimeSlot(
                            null,
                            null,
                            assignedTeacher,
                            assignedClassroom,
                            courseAndTeachers,
                            baseData);
                    if (timeSlot != null) {
                        CourseScheduleItemDTO item = new CourseScheduleItemDTO(
                                course,
                                assignedTeacher,
                                assignedClassroom,
                                classGroup,
                                new CreditHourTypeEnuDTO(),
                                courseAndTeachers.getPriority()
                        );
                        //将时间槽分配到课程安排中
                        assignments.put(timeSlot, item);
                        // 更新课程表
                        schedule.setAssignments(assignments);
                    } else {
                        log.warn("无法为课程 {} 找到合适的时间槽", course.getName());
                    }
                }
            }
            population.add(schedule);
            ScheduleDTO scheduleDTO = new ScheduleDTO();
            scheduleDTO.setSchedule(population);
            allPopulation.add(scheduleDTO);
        }
        log.debug("生成初始种群完成，种群大小: {}", allPopulation.size());
        return allPopulation;
    }

    /**
     * 为课程选择合适的教师
     * <p>
     * 本方法根据课程的学科要求，从候选教师列表中选择一名合适的教师进行课程教学。
     * 选择过程仅考虑教师对课程学科的适应性，确保教师具备教授该课程的资格。
     * </p>
     *
     * @param course   课程信息对象，包含课程学科、课程类型等基本属性
     * @param teachers 候选教师列表，包含所有可能分配给该课程的教师
     * @return 选择的教师对象；如果没有合适的教师，则返回null
     */
    @Nullable
    TeacherCoursePreferencesDTO selectTeacherForCourse(
            CourseLibraryDTO course,
            @NotNull List<TeacherCoursePreferencesDTO> teachers
    ) {
        // 筛选出能够教授该课程学科的教师列表
        List<TeacherCoursePreferencesDTO> suitableTeachers = new ArrayList<>(teachers.stream()
                .filter(teacher -> teacher.getQualification() != null
                        && teacher.getQualification().getCourseUuid().equals(course.getCourseLibraryUuid()))
                .toList());

        // 如果没有找到符合条件的教师，返回null
        if (suitableTeachers.isEmpty()) {
            log.warn("没有找到合适的教师来教授课程: {}", course.getName());
            return null;
        }

        // 随机选择一个符合条件的教师
        Collections.shuffle(suitableTeachers, random);

        return suitableTeachers.get(0);
    }


    /**
     * 为课程选择合适的教室
     * 根据课程资格列表和可用教室列表，为每个班级分配最合适的教室
     * 如果无法为所有班级找到合适的教室，则返回null
     *
     * @param courseQualificationList 课程资格列表，包含课程信息和班级列表
     * @param classrooms              可用的教室列表
     * @return 分配结果，以班级列表为键，分配的教室为值如果无法为所有班级找到合适的教室，则返回null
     */
    Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> selectClassroomsForCourse(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualificationList,
            @Nonnull List<ClassroomInfoDTO> classrooms) {
        List<AdministrativeClassDTO> classList = courseQualificationList.getClassList();
        Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> allocationMap = new HashMap<>();
        ArrayList<ClassroomInfoDTO> remainingClassrooms = new ArrayList<>(classrooms);
        if (classList == null || classList.isEmpty()) {
            this.allocateVirtualClasses(courseQualificationList, courseQualificationList.getNumber(), remainingClassrooms, allocationMap);
        } else {
            this.allocateClassesByMajor(classList, remainingClassrooms, allocationMap, courseQualificationList);
        }
        return allocationMap;
    }

    /**
     * 为课程分配虚拟班级和教室
     * 此方法根据剩余教室的容量和课程类型，为一定数量的学生分配虚拟班级和合适的教室
     */
    private void allocateVirtualClasses(@NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualificationList,
                                        int number,
                                        @NotNull List<ClassroomInfoDTO> remainingClassrooms,
                                        Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> allocationMap) {
        CourseLibraryDTO course = courseQualificationList.getCourse();
        int studentCounter = 0;
        int virtualClassIndex = 1;

        // 计算所有教室的总容量
        int totalCapacity = remainingClassrooms.stream()
                .mapToInt(c -> c.getClassroom().getCapacity())
                .sum();
        if (number > totalCapacity) {
            log.warn("总人数 {} 超过所有教室总容量 {}，无法完成分配", number, totalCapacity);
            return;
        }
        // 循环分配学生，直到所有学生都被分配
        while (studentCounter < number) {
            int remainingStudents = number - studentCounter;
            // 尝试找到能容纳所有剩余学生的教室
            ClassroomInfoDTO classroom = this.findBestClassroom(
                    remainingClassrooms,
                    remainingStudents,
                    course,
                    courseQualificationList
            );
            if (classroom != null) {
                // 如果找到合适的教室，直接分配所有剩余学生
                allocateStudentsToClassroom(
                        course,
                        remainingStudents,
                        virtualClassIndex,
                        classroom,
                        allocationMap
                );
                studentCounter += remainingStudents;
                virtualClassIndex++;
                remainingClassrooms.remove(classroom);
            } else {
                // 如果找不到合适的教室，进行分班
                int splitSize = remainingStudents / 2;
                if (splitSize < 20) {
                    // 确保每个班级至少20人
                    splitSize = 20;
                }
                // 尝试为分班后的学生找到教室
                classroom = this.findBestClassroom(
                        remainingClassrooms,
                        splitSize,
                        course,
                        courseQualificationList
                );
                if (classroom == null) {
                    log.debug("无法为 {} 名学生找到合适的教室", splitSize);
                    return;
                }
                // 分配分班后的学生
                allocateStudentsToClassroom(
                        course,
                        splitSize,
                        virtualClassIndex,
                        classroom,
                        allocationMap
                );
                studentCounter += splitSize;
                virtualClassIndex++;
                remainingClassrooms.remove(classroom);
            }
        }
    }

    /**
     * 将学生分配到教室并创建虚拟班级
     */
    private void allocateStudentsToClassroom(
            @NotNull CourseLibraryDTO course,
            int studentCount,
            int classIndex,
            ClassroomInfoDTO classroom,
            @NotNull Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> allocationMap) {

        String classKey = course.getCourseLibraryUuid() + "-" + classIndex;
        AdministrativeClassDTO classDTO = new AdministrativeClassDTO()
                .setAdministrativeClassUuid(classKey)
                .setClassName(course.getName() + classIndex)
                .setStudentCount(studentCount);

        List<AdministrativeClassDTO> virtualClass = List.of(classDTO);
        allocationMap.put(virtualClass, classroom);
    }

    /**
     * 根据专业分配班级到教室
     * 该方法旨在根据班级的学生人数和课程类型，尽可能高效地利用剩余教室资源进行班级分配
     *
     * @param classList               待分配的行政班列表，不能为空
     * @param remainingClassrooms     剩余可用的教室及其类型列表
     * @param allocationMap           班级到教室的分配映射
     * @param courseQualificationList 课程资格列表，包含课程信息和班级列表
     */
    void allocateClassesByMajor(
            @NotNull List<AdministrativeClassDTO> classList,
            List<ClassroomInfoDTO> remainingClassrooms,
            Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> allocationMap,
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualificationList) {
        CourseLibraryDTO course = courseQualificationList.getCourse();
        // 1. 首先尝试将所有班级放在一个教室
        int totalStudents = classList.stream().mapToInt(AdministrativeClassDTO::getStudentCount).sum();
        ClassroomInfoDTO selectedClassroom = this.findBestClassroom(remainingClassrooms,
                totalStudents, course, courseQualificationList);
        if (selectedClassroom != null) {
            // 如果找到足够大的教室，直接分配所有班级
            allocationMap.put(new ArrayList<>(classList), selectedClassroom);
            remainingClassrooms.remove(selectedClassroom);
            return;
        }
        // 2. 如果找不到足够大的教室，再按专业分组
        Map<String, List<AdministrativeClassDTO>> classesByMajor = classList.stream()
                .collect(Collectors.groupingBy(AdministrativeClassDTO::getMajorUuid));
        // 遍历每个专业的班级
        for (List<AdministrativeClassDTO> majorClasses : classesByMajor.values()) {
            List<AdministrativeClassDTO> pendingClasses = new ArrayList<>();
            int majorTotalStudents = majorClasses.stream().mapToInt(AdministrativeClassDTO::getStudentCount).sum();

            // 先尝试将整个专业的班级放在一个教室
            selectedClassroom = this.findBestClassroom(remainingClassrooms,
                    majorTotalStudents, course, courseQualificationList);

            if (selectedClassroom != null) {
                allocationMap.put(new ArrayList<>(majorClasses), selectedClassroom);
                remainingClassrooms.remove(selectedClassroom);
                continue;
            }

            // 如果专业班级也放不下，则逐个班级分配
            for (AdministrativeClassDTO adminClass : majorClasses) {
                pendingClasses.add(adminClass);
                int classStudents = adminClass.getStudentCount();

                selectedClassroom = this.findBestClassroom(remainingClassrooms,
                        classStudents, course, courseQualificationList);

                if (selectedClassroom != null) {
                    allocationMap.put(new ArrayList<>(pendingClasses), selectedClassroom);
                    remainingClassrooms.remove(selectedClassroom);
                    pendingClasses.clear();
                }
            }

            // 处理未分配的班级
            if (!pendingClasses.isEmpty()) {
                this.allocateUnassignedClasses(pendingClasses, remainingClassrooms, allocationMap);
            }
        }
    }

    /**
     * 寻找下一个未处理的行政班级
     * 该方法用于在给定的专业班级列表中，找到下一个不在待处理列表中的班级
     * 主要用于班级处理流程中，确定下一个需要处理的班级
     *
     * @param majorClasses   专业班级列表，不能为空
     * @param pendingClasses 待处理班级列表，可能为空
     * @return 如果找到下一个未处理的班级，则返回该班级；否则返回null
     */
    private @Nullable AdministrativeClassDTO findNextClass(@NotNull List<AdministrativeClassDTO> majorClasses,
                                                           List<AdministrativeClassDTO> pendingClasses) {
        // 使用流处理，过滤出下一个未处理的班级
        return majorClasses.stream()
                .filter(cls -> !pendingClasses.contains(cls))
                .findFirst()
                .orElse(null);
    }

    /**
     * 将未分配的班级随机分配到剩余的教室
     * 当没有找到完全匹配的教室时，此方法旨在确保所有班级都能被分配一个教室，以避免空班情况
     *
     * @param pendingClasses      待分配的班级列表
     * @param remainingClassrooms 剩余可用的教室列表
     * @param allocationMap       班级到教室的分配映射
     */
    private void allocateUnassignedClasses(@NotNull List<AdministrativeClassDTO> pendingClasses,
                                           List<ClassroomInfoDTO> remainingClassrooms,
                                           Map<List<AdministrativeClassDTO>, ClassroomInfoDTO> allocationMap) {
        // 当没有找到合适的教室来教授班级时，记录待分配的班级名称
        log.debug("没有找到合适的教室来教授班级: {}", pendingClasses.stream().map(AdministrativeClassDTO::getClassName).toList());
        // 从剩余的教室中随机选择一个教室
        ClassroomInfoDTO randomClassroom = this.selectRandomClassroom(remainingClassrooms);
        // 如果随机选中的教室不为空，则将所有待分配的班级都分配到这个教室，并记录分配信息
        if (randomClassroom != null) {
            allocationMap.put(new ArrayList<>(pendingClasses), randomClassroom);
        }
    }

    /**
     * 查找最佳教室
     *
     * @param classrooms   教室列表
     * @param studentCount 学生人数
     * @param course       课程信息
     * @return 最佳教室，如果没有找到合适的教室，则返回null
     */
    private ClassroomInfoDTO findBestClassroom(
            @NotNull List<ClassroomInfoDTO> classrooms,
            int studentCount,
            CourseLibraryDTO course,
            CourseLibraryAndTeacherCourseQualificationListDTO courseQualificationList) {
        // 获取课程对应的教室类型
        String requiredClassroomType = null;
        if (course != null) {
            switch (courseQualificationList.getCourseEnuType()) {
                case THEORY:
                    requiredClassroomType = course.getTheoryClassroomType();
                    break;
                case EXPERIMENT:
                    requiredClassroomType = course.getExperimentClassroomType();
                    break;
                case PRACTICE:
                    requiredClassroomType = course.getPracticeClassroomType();
                    break;
                case COMPUTER:
                    requiredClassroomType = course.getComputerClassroomType();
                    break;
            }
        }
        // 先尝试找到指定类型的教室
        String finalRequiredClassroomType = requiredClassroomType;
        return classrooms.stream()
                .filter(c -> {
                    // 首先检查容量是否满足
                    if (c.getClassroom().getCapacity() < studentCount) {
                        return false;
                    }
                    // 如果课程类型为空，则匹配任何教室
                    if (finalRequiredClassroomType == null) {
                        return true;
                    }
                    // 如果课程类型不为空，则必须匹配
                    return finalRequiredClassroomType.equals(c.getType().getClassTypeUuid());
                })
                .min(Comparator.comparingInt(c -> c.getClassroom().getCapacity()))
                .orElseGet(() -> {
                    // 如果找不到指定类型的教室，尝试找普通教室
                    return classrooms.stream()
                            .filter(c -> {
                                // 检查容量是否满足
                                if (c.getClassroom().getCapacity() < studentCount) {
                                    return false;
                                }
                                // 返回普通教室
                                return c.getType().getClassTypeUuid() == SystemConstant.getClassroomTypeCommon();
                            })
                            .min(Comparator.comparingInt(c -> c.getClassroom().getCapacity()))
                            .orElse(null);
                });
    }

    /**
     * 随机选择一个教室
     * 从给定的教室列表中随机选择一个教室如果列表为空，则返回null
     *
     * @param classrooms 一个包含教室信息的列表，不能为null
     * @return 随机选中的教室信息，如果列表为空则返回null
     */
    private @Nullable ClassroomInfoDTO selectRandomClassroom(@NotNull List<ClassroomInfoDTO> classrooms) {
        return classrooms.isEmpty() ? null : classrooms.get(new Random().nextInt(classrooms.size()));
    }

    /**
     * 计算连续课程的适应度值
     *
     * @param schedule 课程表
     * @return 连续课程的适应度值
     */
    private double calculateConsecutiveCoursesFitness(@NotNull CourseScheduleDTO schedule) {
        // 按课程分组，获取每个课程的所有时间槽
        Map<String, List<List<TimeSlotDTO>>> courseSlots = schedule.getAssignments().entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue().getCourse().getCourseLibraryUuid(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));

        double totalFitness = 0.0;

        // 遍历每个课程
        for (Map.Entry<String, List<List<TimeSlotDTO>>> courseEntry : courseSlots.entrySet()) {
            // 展平所有时间槽
            List<TimeSlotDTO> allSlots = courseEntry.getValue().stream()
                    .flatMap(List::stream)
                    .toList();

            // 按周和天分组
            Map<Integer, Map<Integer, List<TimeSlotDTO>>> weekDaySlots = allSlots.stream()
                    .collect(Collectors.groupingBy(
                            TimeSlotDTO::getWeek,
                            Collectors.groupingBy(TimeSlotDTO::getDay)
                    ));

            // 遍历每周
            for (Map<Integer, List<TimeSlotDTO>> daySlots : weekDaySlots.values()) {
                // 遍历每天
                for (List<TimeSlotDTO> dailySlots : daySlots.values()) {
                    if (dailySlots.size() > 1) {
                        // 按节次排序
                        List<TimeSlotDTO> sortedSlots = dailySlots.stream()
                                .sorted(Comparator.comparingInt(TimeSlotDTO::getPeriod))
                                .toList();
                        // 计算连续课程数
                        int consecutiveCount = 1;
                        for (int i = 0; i < sortedSlots.size() - 1; i++) {
                            if (sortedSlots.get(i + 1).getPeriod() - sortedSlots.get(i).getPeriod() == 1) {
                                consecutiveCount++;
                            } else {
                                // 如果有连续课程，给予奖励
                                if (consecutiveCount > 1) {
                                    totalFitness += (consecutiveCount - 1) * 5.0;
                                }
                                consecutiveCount = 1;
                            }
                        }

                        // 处理最后一组连续课程
                        if (consecutiveCount > 1) {
                            totalFitness += (consecutiveCount - 1) * 5.0;
                        }
                    }
                }
            }
        }

        return totalFitness;
    }

    /**
     * 计算时间偏好适应度值
     *
     * @param schedule    课程表
     * @param preferences 时间偏好设置
     * @return 时间偏好适应度值
     */
    private double calculateTimePreferenceFitness(
            @NotNull CourseScheduleDTO schedule,
            AutomaticClassSchedulingBaseDTO.TimePreferences preferences
    ) {
        double fitness = 0.0;
        // 遍历所有课程安排
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            List<TimeSlotDTO> slots = entry.getKey();
            // 遍历每个时间槽
            for (TimeSlotDTO slot : slots) {
                // 检查是否在偏好时间段
                boolean inPreferredSlot = preferences.getPreferredTimeSlots().stream()
                        .anyMatch(preferred ->
                                Objects.equals(preferred.getDay(), slot.getDay()) &&
                                        preferred.getPeriodStart() <= slot.getPeriod() &&
                                        preferred.getPeriodEnd() >= slot.getPeriod());

                if (inPreferredSlot) {
                    fitness += 10.0;
                }

                // 如果不喜欢晚课但安排在晚上
                if (Boolean.TRUE.equals(preferences.getEveningCourses()) && slot.getPeriod() >= 5) {
                    fitness -= 5.0;
                }
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
    private double calculateRoomOptimizationFitness(@NotNull CourseScheduleDTO schedule) {
        double fitness = 0.0;
        // 遍历所有课程安排
        for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            CourseScheduleItemDTO item = entry.getValue();
            // 获取教室容量
            int capacity = item.getClassroom().getClassroom().getCapacity();
            // 获取课程所需学生数，使用totalHours作为替代指标
            int studentCount = item.getCourse().getTotalHours() != null ?
                    item.getCourse().getTotalHours().intValue() : 30;
            // 计算容量匹配度
            if (capacity >= studentCount) {
                // 容量足够，计算利用率
                double utilizationRate = (double) studentCount / capacity;
                // 利用率达到70%以上给予奖励
                if (utilizationRate >= 0.7) {
                    fitness += 5.0;
                }
                // 利用率过高（超过90%）给予惩罚
            } else {
                // 容量不足，严重惩罚
                fitness -= 50.0;
            }
            // 教室类型匹配度
            String courseType = item.getCourse().getType();
            String classroomType = item.getClassroom().getType().getClassTypeUuid();
            if (classroomType != null && classroomType.equals(courseType)) {
                fitness += 10.0;
            } else if (classroomType != null && courseType != null) {
                // 类型不匹配，给予惩罚
                fitness -= 5.0;
            }
        }
        return fitness;
    }

    /**
     * 执行时间槽变异
     */
    public void timeSlotMutation(
            @NotNull CourseScheduleDTO schedule,
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courses,
            @NotNull AutomaticClassSchedulingBaseDTO baseDTO,
            //总排课安排
            @NotNull List<CourseScheduleDTO> population
    ) {
        // 获取当前课程安排
        Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry = schedule.getAssignments().entrySet().iterator().next();
        // 获取对应的课程信息
        CourseLibraryAndTeacherCourseQualificationListDTO course = findCourseByScheduleItem(entry.getValue(), courses);
        if (course != null) {
            // 尝试找到新的合适时间槽
            List<TimeSlotDTO> newTimeSlot = this.findSuitableTimeSlot(
                    population,
                    schedule.getAssignments(),
                    entry.getValue().getTeacher(),
                    entry.getValue().getClassroom(),
                    course,
                    baseDTO
            );
            if (newTimeSlot != null) {
                // 找到新时间槽才进行替换
                schedule.getAssignments().remove(entry.getKey());
                schedule.getAssignments().put(newTimeSlot, entry.getValue());
            } else {
                // 如果找不到合适的新时间槽，尝试与其他课程交换时间
                for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> other : schedule.getAssignments().entrySet()) {
                    if (!other.equals(entry) && this.isSwapValid(entry, other, schedule, courses, baseDTO, population)) {
                        swapTimeSlots(schedule, entry, other);
                        break;
                    }
                }
            }
        }
    }

    /**
     * 根据ScheduleItem查找对应的课程信息
     */
    private CourseLibraryAndTeacherCourseQualificationListDTO findCourseByScheduleItem(
            CourseScheduleItemDTO item,
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courses
    ) {
        return courses.stream()
                .filter(course -> course.getCourse().getId().equals(item.getCourse().getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 查找合适的时间槽
     * <p>
     * 该方法用于在给定的排课安排中查找合适的时间槽，
     * 以便为指定的课程、教师和教室分配新的时间槽。
     * 它会检查现有的排课安排，确保新的时间槽不会与已有的时间槽冲突。
     * 如果找到合适的时间槽，则返回该时间槽列表；
     * 如果没有找到合适的时间槽，则返回null。
     * </p>
     */
    @Nullable
    private List<TimeSlotDTO> findSuitableTimeSlot(
            List<CourseScheduleDTO> schedules,
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments,
            TeacherCoursePreferencesDTO teacher,
            ClassroomInfoDTO classroom,
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO course,
            AutomaticClassSchedulingBaseDTO baseDTO
    ) {
        // 寻找合适的时间槽,重新获得一个排课
        List<TimeSlotDTO> allTimeSlots = generateTimeSlotsByCourse(course, baseDTO);
        //时间槽为空，则就是初始化种群的情况
        if (assignments == null || assignments.isEmpty()) {
            log.debug("初始化种群，直接返回随机生成的时间槽");
            return allTimeSlots;
        }
        // 如果有排课安排，检查新的时间槽是否与现有安排冲突
        if (schedules != null && !schedules.isEmpty()) {
            if (isTimeSlotSuitable(allTimeSlots, teacher, classroom, course, baseDTO, schedules)) {
                log.debug("有排课安排，检查新的时间槽是否与现有安排冲突");
                return allTimeSlots;
            }
        }
        return null;
    }

    /**
     * 生成时间槽
     *
     * @param course  课程信息
     * @param baseDTO 基础信息
     * @return 生成的时间槽列表
     */
    private @NotNull List<TimeSlotDTO> generateTimeSlotsByCourse(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO course,
            @NotNull AutomaticClassSchedulingBaseDTO baseDTO
    ) {
        List<TimeSlotDTO> slots = new ArrayList<>();
        // 根据是否开设晚上课程确定每天最多节次
        int maxPeriodsPerDay = Boolean.TRUE.equals(baseDTO.getTimePreferences().getEveningCourses()) ? 12 : 8;
        int totalScheduledHours = 0;
        int targetTotalHours = course.getExpectedTotalHours().intValue();
        // 创建每天可用的连续时间段
        Map<Integer, List<List<Integer>>> availableBlocksByDay = new HashMap<>();
        for (int day = 1; day <= 5; day++) {
            List<List<Integer>> dayBlocks = new ArrayList<>();
            // 上午时间段（1-4节）
            dayBlocks.add(Arrays.asList(1, 2, 3, 4));
            // 下午时间段（5-8节）
            dayBlocks.add(Arrays.asList(5, 6, 7, 8));
            if (maxPeriodsPerDay > 8) {
                // 晚上时间段（9-12节）
                dayBlocks.add(Arrays.asList(9, 10, 11, 12));
            }
            availableBlocksByDay.put(day, dayBlocks);
        }
        // 根据每周课时数决定分配策略
        List<Integer> distribution = this.determineDistribution(course.getWeeklyHours());
        // 随机选择天数
        List<Integer> days = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Collections.shuffle(days);
        // 记录每个时间块对应的节次
        Map<Integer, List<Integer>> dayTimeSlots = new HashMap<>();
        // 为每个选中的天数分配时间块
        for (Integer blockSize : distribution) {
            if (days.isEmpty()) {
                break;
            }
            int day = days.remove(0);
            // 获取该天的可用时间块
            List<List<Integer>> dayBlocks = new ArrayList<>(availableBlocksByDay.get(day));
            // 收集所有可能的时间段
            List<List<Integer>> possibleTimeSlots = new ArrayList<>();
            // 遍历每个时间块，生成所有可能的时间段
            for (List<Integer> block : dayBlocks) {
                for (int i = 0; i <= block.size() - blockSize; i++) {
                    List<Integer> timeSlot = new ArrayList<>();
                    for (int j = 0; j < blockSize; j++) {
                        timeSlot.add(block.get(i + j));
                    }
                    possibleTimeSlots.add(timeSlot);
                }
            }
            // 随机选择一个时间段
            if (!possibleTimeSlots.isEmpty()) {
                Collections.shuffle(possibleTimeSlots);
                dayTimeSlots.put(day, possibleTimeSlots.get(0));
            }
        }
        // 遍历周，生成时间槽
        for (int week = course.getStartWeek(); week <= course.getEndWeek()
                && totalScheduledHours < targetTotalHours; week++) {
            // 为每个选中的天数生成时间槽
            for (Map.Entry<Integer, List<Integer>> entry : dayTimeSlots.entrySet()) {
                int day = entry.getKey();
                List<Integer> timeSlots = entry.getValue();
                for (Integer period : timeSlots) {
                    TimeSlotDTO timeSlot = new TimeSlotDTO(week, day, period);
                    slots.add(timeSlot);
                    totalScheduledHours++;
                }
            }
        }
        return slots;
    }
    /**
     * 根据每周课时数确定分配策略
     *
     * @param weeklyHours 每周总课时数
     * @return 返回课时分配列表，每个数字代表一天要安排的连续课时数
     */
    private @NotNull List<Integer> determineDistribution(int weeklyHours) {
        List<Integer> distribution = new ArrayList<>();
        // 处理所有2节课的部分
        while (weeklyHours >= 2) {
            distribution.add(2);
            weeklyHours -= 2;
        }
        // 如果还剩1节课
        if (weeklyHours == 1) {
            distribution.add(1);
        }
        // 打乱分配顺序
        Collections.shuffle(distribution);
        return distribution;
    }

    /**
     * 检查时间槽是否合适
     */
    private boolean isTimeSlotSuitable(
            @NotNull List<TimeSlotDTO> timeSlots,
            TeacherCoursePreferencesDTO teacher,
            ClassroomInfoDTO classroom,
            CourseLibraryAndTeacherCourseQualificationListDTO course,
            AutomaticClassSchedulingBaseDTO baseDTO,
            List<CourseScheduleDTO> population
    ) {
        // 1. 检查是否是晚课时段且不允许晚课
        for (TimeSlotDTO time : timeSlots) {
            if (!Boolean.TRUE.equals(baseDTO.getTimePreferences().getEveningCourses())
                    && time.getPeriod() > 8) {
                return false;
            }
        }
        // 2. 检查教师在该时间段是否有其他课程
        if (!isTeacherAvailable(teacher, timeSlots, population)) {
            return false;
        }
        // 3. 检查教室在该时间段是否可用
        if (!isClassroomAvailable(classroom, timeSlots, population)) {
            return false;
        }
        // 4. 检查是否在课程的周数范围内
        for (TimeSlotDTO time : timeSlots) {
            if (time.getWeek() < course.getStartWeek() || time.getWeek() > course.getEndWeek()) {
                return false;
            }
        }
        // 5. 检查本周的课时数是否已达到限制
        for (TimeSlotDTO time : timeSlots) {
            int currentWeekSlots = countWeeklySlots(
                    population, course.getCourse().getCourseLibraryUuid(), time.getWeek());
            if (currentWeekSlots >= course.getWeeklyHours()) {
                return false;
            }
        }
        // 6.按照这样子排课是否学时能够修满
        int totalScheduledHours = countTotalScheduledHours(population, course.getCourse().getCourseLibraryUuid());
        return totalScheduledHours < course.getExpectedTotalHours().intValue();
    }

    /**
     * 计算课程的总排课时长
     */
    private int countTotalScheduledHours(
            @NotNull List<CourseScheduleDTO> population,
            String courseId
    ) {
        return population.stream()
                .flatMap(schedule -> schedule.getAssignments().entrySet().stream())
                .filter(entry -> entry.getValue().getCourse().getCourseLibraryUuid().equals(courseId))
                .mapToInt(entry -> entry.getKey().size())
                .sum();
    }

    /**
     * 计算指定课程在特定周的课时数
     * <p>
     * 统计指定课程在给定周内已经安排的课时总数。
     * 这个方法用于确保课程不会超过每周课时限制。
     * </p>
     *
     * @param population 当前的课程安排映射
     * @param courseId   课程ID
     * @param week       要检查的周数
     * @return 该课程在指定周的课时数
     */
    private int countWeeklySlots(
            @NotNull List<CourseScheduleDTO> population,
            String courseId,
            Integer week
    ) {
        return population.stream()
                .flatMap(schedule -> schedule.getAssignments().entrySet().stream())
                .filter(entry -> entry.getValue().getCourse().getCourseLibraryUuid().equals(courseId))
                .flatMap(entry -> entry.getKey().stream())
                .filter(slot -> slot.getWeek().equals(week))
                .mapToInt(slot -> 1)
                .sum();
    }

    /**
     * 检查教师在指定时间槽是否可用
     * <p>
     * 验证教师在特定时间段是否已有其他课程安排。
     * 通过检查所有现有课程安排，确保不会出现教师在同一时间段被重复安排的情况。
     * </p>
     *
     * @param teacher    要检查的教师
     * @param slot       要检查的时间槽
     * @param population 当前的课程安排映射
     * @return 如果教师在该时间段可用返回true，否则返回false
     */
    private boolean isTeacherAvailable(
            TeacherCoursePreferencesDTO teacher,
            List<TimeSlotDTO> slot,
            @NotNull List<CourseScheduleDTO> population
    ) {
        // 遍历所有现有的课程安排
        for (CourseScheduleDTO schedule : population) {
            // 遍历每个具体的课程安排
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
                // 检查是否是同一个教师
                if (entry.getValue().getTeacher().getTeacher().getTeacherUuid()
                        .equals(teacher.getTeacher().getTeacherUuid())) {
                    // 检查时间是否冲突
                    List<TimeSlotDTO> existingSlots = entry.getKey();
                    // 检查两个时间槽列表是否有重叠
                    for (TimeSlotDTO newSlot : slot) {
                        for (TimeSlotDTO existingSlot : existingSlots) {
                            if (Objects.equals(newSlot.getWeek(), existingSlot.getWeek())
                                    && Objects.equals(newSlot.getDay(), existingSlot.getDay())
                                    && Objects.equals(newSlot.getPeriod(), existingSlot.getPeriod())) {
                                // 发现时间冲突
                                return false;
                            }
                        }
                    }
                }
            }
        }
        // 没有发现冲突
        return true;
    }

    /**
     * 检查教室在指定时间槽是否可用
     * <p>
     * 验证教室在特定时间段是否已被占用。
     * 通过检查所有现有课程安排，确保不会出现教室在同一时间段被多门课程使用的情况。
     * </p>
     *
     * @param classroom  要检查的教室
     * @param slot       要检查的时间槽
     * @param population 当前的所有课程安排映射
     * @return 如果教室在该时间段可用返回true，否则返回false
     */
    private boolean isClassroomAvailable(
            ClassroomInfoDTO classroom,
            List<TimeSlotDTO> slot,
            @NotNull List<CourseScheduleDTO> population
    ) {
        for (CourseScheduleDTO schedule : population) {
            // 遍历每个课程安排
            for (Map.Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
                // 检查是否是同一个教室
                if (entry.getValue().getClassroom().getClassroom().getClassroomUuid()
                        .equals(classroom.getClassroom().getClassroomUuid())) {
                    // 获取现有的时间槽列表
                    List<TimeSlotDTO> existingSlots = entry.getKey();
                    // 检查新的时间槽列表是否与现有的时间槽列表有重叠
                    for (TimeSlotDTO newSlot : slot) {
                        for (TimeSlotDTO existingSlot : existingSlots) {
                            // 使用Objects.equals进行安全的空值比较
                            if (Objects.equals(newSlot.getWeek(), existingSlot.getWeek())
                                    && Objects.equals(newSlot.getDay(), existingSlot.getDay())
                                    && Objects.equals(newSlot.getPeriod(), existingSlot.getPeriod())) {
                                // 发现冲突
                                return false;
                            }
                        }
                    }
                }
            }
        }
        // 没有冲突
        return true;
    }

    /**
     * 检查交换是否有效
     */
    private boolean isSwapValid(
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2,
            CourseScheduleDTO schedule,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> courses,
            AutomaticClassSchedulingBaseDTO baseDTO,
            List<CourseScheduleDTO> population
    ) {
        CourseLibraryAndTeacherCourseQualificationListDTO course1 = findCourseByScheduleItem(entry1.getValue(), courses);
        CourseLibraryAndTeacherCourseQualificationListDTO course2 = findCourseByScheduleItem(entry2.getValue(), courses);

        if (course1 == null || course2 == null) {
            return false;
        }
        // 检查交换后的时间槽是否适合各自的课程
        boolean slot1Valid = this.isTimeSlotSuitable(
                // 注意这里用entry2的时间槽给entry1的课程
                entry2.getKey(),
                entry1.getValue().getTeacher(),
                entry1.getValue().getClassroom(),
                course1,
                baseDTO,
                population
        );
        boolean slot2Valid = this.isTimeSlotSuitable(
                // 注意这里用entry1的时间槽给entry2的课程
                entry1.getKey(),
                entry2.getValue().getTeacher(),
                entry2.getValue().getClassroom(),
                course2,
                baseDTO,
                population
        );
        return true;
    }

    /**
     * 交换两个课程的时间槽
     *
     * @param schedule 课程表
     * @param entry1   第一个课程的时间槽和课程信息
     * @param entry2   第二个课程的时间槽和课程信息
     */
    private void swapTimeSlots(
            @NotNull CourseScheduleDTO schedule,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry1,
            Map.@NotNull Entry<List<TimeSlotDTO>, CourseScheduleItemDTO> entry2) {
        // 先保存原始的时间槽和课程信息
        List<TimeSlotDTO> timeSlot1 = new ArrayList<>(entry1.getKey());
        List<TimeSlotDTO> timeSlot2 = new ArrayList<>(entry2.getKey());
        CourseScheduleItemDTO item1 = entry1.getValue();
        CourseScheduleItemDTO item2 = entry2.getValue();
        // 从Map中移除原有的安排
        schedule.getAssignments().remove(entry1.getKey());
        schedule.getAssignments().remove(entry2.getKey());
        // 重新放入交换后的安排
        schedule.getAssignments().put(timeSlot1, item2);
        schedule.getAssignments().put(timeSlot2, item1);
    }
}

