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
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomAndTypeDTO;
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
     * @param population 待评估的种群
     * @param baseDTO    排课基础数据
     */
    void evaluatePopulation(@NotNull List<ScheduleDTO> population, AutomaticClassSchedulingBaseDTO baseDTO) {
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
     * @param baseDTO  排课基础数据，包含约束条件
     * @return 适应度得分，值越高表示排课方案越优
     */
    double calculateFitness(ScheduleDTO schedule, @NotNull AutomaticClassSchedulingBaseDTO baseDTO) {
        // 基础分数
        double fitness = 100.0;

        // 减去冲突惩罚
        fitness -= calculateConflictPenalty(schedule);

        // 连续课程适应度
        if (Boolean.TRUE.equals(baseDTO.getConstraints().getConsecutiveCoursesPreferred())) {
            fitness += calculateConsecutiveCoursesFitness(schedule);
        }

        // 时间偏好适应度
        fitness += calculateTimePreferenceFitness(schedule, baseDTO.getTimePreferences());

        // 教室优化适应度
        if (Boolean.TRUE.equals(baseDTO.getConstraints().getRoomOptimization())) {
            fitness += calculateRoomOptimizationFitness(schedule);
        }

        // 确保适应度不为负
        return Math.max(0.0, fitness);
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
    double calculateConflictPenalty(@NotNull ScheduleDTO schedule) {
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());

        // 使用流处理所有课程安排对
        return entries.stream().mapToDouble(entry -> {
                    TimeSlotDTO slot1 = entry.getKey();
                    ScheduleItemDTO item1 = entry.getValue();

                    // 统计与当前课程安排冲突的其他安排产生的惩罚
                    return entries.stream()
                            .filter(entry2 -> entry != entry2) // 排除自身
                            .filter(entry2 -> {
                                // 检查是否在同一时间段
                                TimeSlotDTO slot2 = entry2.getKey();
                                return slot1.getWeek() == slot2.getWeek() &&
                                        slot1.getDay() == slot2.getDay() &&
                                        slot1.getPeriod() == slot2.getPeriod();
                            })
                            .mapToDouble(entry2 -> {
                                ScheduleItemDTO item2 = entry2.getValue();
                                double itemPenalty = 0.0;

                                // 教师冲突检查
                                String teacher1Uuid = item1.getTeacher().getTeacher().getTeacherUuid();
                                String teacher2Uuid = item2.getTeacher().getTeacher().getTeacherUuid();
                                if (teacher1Uuid.equals(teacher2Uuid)) {
                                    itemPenalty += 100.0;
                                }

                                // 教室冲突检查
                                String room1Uuid = item1.getClassroom().getClassroom().getClassroomUuid();
                                String room2Uuid = item2.getClassroom().getClassroom().getClassroomUuid();
                                if (room1Uuid.equals(room2Uuid)) {
                                    itemPenalty += 100.0;
                                }

                                return itemPenalty;
                            })
                            .sum();
                })
                // 除以2是因为每个冲突会被计算两次
                .sum() / 2;
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
    List<SchedulingConflictDTO> findConflicts(@NotNull ScheduleDTO schedule) {
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());
        List<SchedulingConflictDTO> conflicts = new ArrayList<>();

        // 使用流处理所有可能的课程安排对
        for (int i = 0; i < entries.size(); i++) {
            final int finalI = i;

            // 当前课程安排
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry1 = entries.get(i);
            TimeSlotDTO slot1 = entry1.getKey();
            ScheduleItemDTO item1 = entry1.getValue();

            // 查找与当前课程安排冲突的其他安排
            entries.stream()
                    // 避免重复检查
                    .skip(finalI + 1L)
                    .filter(entry2 -> {
                        TimeSlotDTO slot2 = entry2.getKey();

                        // 检查是否在同一时间段
                        return slot1.getWeek() == slot2.getWeek() &&
                                slot1.getDay() == slot2.getDay() &&
                                slot1.getPeriod() == slot2.getPeriod();
                    })
                    .forEach(entry2 -> {
                        ScheduleItemDTO item2 = entry2.getValue();

                        // 检查教师冲突
                        String teacher1Uuid = item1.getTeacher().getTeacher().getTeacherUuid();
                        String teacher2Uuid = item2.getTeacher().getTeacher().getTeacherUuid();

                        if (teacher1Uuid.equals(teacher2Uuid)) {
                            // 添加教师冲突
                            conflicts.add(new SchedulingConflictDTO()
                                    .setConflictType(1)
                                    .setDescription(String.format(
                                            "教师 %s 在第%d周星期%d第%d节课有重复安排",
                                            item1.getTeacher().getTeacher().getName(),
                                            slot1.getWeek(),
                                            slot1.getDay(),
                                            slot1.getPeriod()
                                    )));
                        }

                        // 检查教室冲突
                        String room1Uuid = item1.getClassroom().getClassroom().getClassroomUuid();
                        String room2Uuid = item2.getClassroom().getClassroom().getClassroomUuid();

                        if (room1Uuid.equals(room2Uuid)) {
                            // 添加教室冲突
                            conflicts.add(new SchedulingConflictDTO()
                                    .setConflictType(2)
                                    .setDescription(String.format(
                                            "教室 %s 在第%d周星期%d第%d节课有重复安排",
                                            item1.getClassroom().getClassroom().getName(),
                                            slot1.getWeek(),
                                            slot1.getDay(),
                                            slot1.getPeriod()
                                    )));
                        }
                    });
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
    ScheduleResultDTO.ResourceUtilization calculateResourceUtilization(@NotNull ScheduleDTO schedule) {
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
            String timeSlotKey = String.format("%d-%d-%d", slot.getWeek(), slot.getDay(), slot.getPeriod());
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
    ScheduleDTO deepCopySchedule(@NotNull ScheduleDTO schedule) {
        ScheduleDTO copy = new ScheduleDTO();
        Map<TimeSlotDTO, ScheduleItemDTO> assignments = new HashMap<>();

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slotCopy = new TimeSlotDTO(
                    entry.getKey().getWeek(),
                    entry.getKey().getDay(),
                    entry.getKey().getPeriod()
            );
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
    List<ScheduleDTO> selection(@NotNull List<ScheduleDTO> population) {
        List<ScheduleDTO> selected = new ArrayList<>();
        double totalFitness = population.stream()
                .mapToDouble(ScheduleDTO::getFitness)
                .sum();

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
     * @param selected      经过选择的个体
     * @param crossoverRate 交叉概率
     * @return 交叉后产生的后代
     */
    List<ScheduleDTO> crossover(@NotNull List<ScheduleDTO> selected, double crossoverRate) {
        List<ScheduleDTO> offspring = new ArrayList<>();

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
    List<ScheduleDTO> crossoverSchedules(@NotNull ScheduleDTO parent1, ScheduleDTO parent2) {
        List<ScheduleDTO> children = new ArrayList<>();

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
        int crossoverPoint = random.nextInt(!allCourses.isEmpty() ? allCourses.size() : 1);

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
     * @param target      目标子代的课程安排映射
     */
    void copyAssignmentsFromParent(
            List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> assignments,
            Map<TimeSlotDTO, ScheduleItemDTO> target
    ) {
        if (assignments != null) {
            for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : assignments) {
                TimeSlotDTO slotCopy = new TimeSlotDTO(
                        entry.getKey().getWeek(),
                        entry.getKey().getDay(),
                        entry.getKey().getPeriod()
                );
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
     * @param population   当前种群
     * @param mutationRate 变异概率
     * @param baseDTO      排课基础数据
     */
    void mutation(@NotNull List<ScheduleDTO> population, double mutationRate, AutomaticClassSchedulingBaseDTO baseDTO) {
        for (ScheduleDTO schedule : population) {
            if (random.nextDouble() < mutationRate) {
                // 选择变异策略
                int strategy = random.nextInt(3);

                switch (strategy) {
                    case 0:
                        // 时间槽变异
                        timeSlotMutation(schedule, baseDTO.getCourseList());
                        break;
                    case 1:
                        // 教室变异
                        classroomMutation(schedule, baseDTO);
                        break;
                    case 2:
                        // 教师变异
                        teacherMutation(schedule, baseDTO);
                        break;
                    default:
                        break;
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
    private CourseLibraryAndTeacherCourseQualificationListDTO findCourseById(String courseUuid, List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList) {
        for (CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeacher : courseList) {
            if (courseAndTeacher.getCourse().getCourseLibraryUuid().equals(courseUuid)) {
                return courseAndTeacher;
            }
        }
        return null;
    }


    /**
     * 教室变异
     * <p>
     * 随机选择一个课程安排，尝试分配更合适的教室。
     * </p>
     *
     * @param schedule 待变异的课程表
     * @param baseDTO  排课基础数据
     */
    void classroomMutation(@NotNull ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());
        if (!entries.isEmpty()) {
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseLibraryDTO course = entry.getValue().getCourse();
            // 查找可以教授这门课程的教室列表
            CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeacher = this.findCourseById(
                    course.getCourseLibraryUuid(),
                    baseDTO.getCourseList()
            );
            // 尝试分配新教室
            Map<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> classroomList =
                    null;
            if (courseAndTeacher != null) {
                classroomList = this.selectClassroomsForCourse(courseAndTeacher, baseDTO.getClassroomList());
            }
            // 遍历所有教室并进行更新
            if (classroomList != null) {
                for (Map.Entry<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> classroomEntry : classroomList.entrySet()) {
                    ClassroomAndTypeDTO newClassroom = classroomEntry.getValue();
                    // 确保教室不同于当前分配的教室
                    if (newClassroom != null && !newClassroom.getClassroom().getClassroomUuid()
                            .equals(entry.getValue().getClassroom().getClassroom().getClassroomUuid())) {
                        // 创建新的排课项并更新
                        ScheduleItemDTO newItem = new ScheduleItemDTO(
                                course,
                                entry.getValue().getTeacher(),
                                newClassroom,
                                // 使用 classroomList 的键
                                classroomEntry.getKey(),
                                entry.getValue().getPriority()
                        );
                        schedule.getAssignments().put(entry.getKey(), newItem);
                        break; // 假设只需要更新一次
                    }
                }
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
     * @param baseDTO  排课基础数据
     */
    void teacherMutation(@NotNull ScheduleDTO schedule, AutomaticClassSchedulingBaseDTO baseDTO) {
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());

        if (!entries.isEmpty()) {
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            CourseLibraryDTO course = entry.getValue().getCourse();
            // 查找可以教授这门课程的教师列表
            List<TeacherCoursePreferencesDTO> suitableTeachers = baseDTO.getCourseList().stream()
                    .filter(ct -> ct.getCourse().getCourseLibraryUuid().equals(course.getCourseLibraryUuid()))
                    .flatMap(ct -> ct.getTeacherList().stream())
                    .toList();
            // 尝试分配新教师
            TeacherCoursePreferencesDTO newTeacher = selectTeacherForCourse(course, suitableTeachers);
            if (newTeacher != null && !newTeacher.getTeacher().getTeacherUuid().equals(entry.getValue().getTeacher().getTeacher().getTeacherUuid())) {
                ScheduleItemDTO newItem = new ScheduleItemDTO(
                        course,
                        newTeacher,
                        entry.getValue().getClassroom(),
                        entry.getValue().getClassGroup(),
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
     * @param entry1   第一个课程安排
     * @param entry2   第二个课程安排
     * @param schedule 当前课程表
     * @return 交换是否有效（无冲突）
     */
    boolean isSwapValid(
            Map.@NotNull Entry<TimeSlotDTO, ScheduleItemDTO> entry1,
            Map.@NotNull Entry<TimeSlotDTO, ScheduleItemDTO> entry2,
            @NotNull ScheduleDTO schedule
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
    boolean hasConflicts(@NotNull Map<TimeSlotDTO, ScheduleItemDTO> assignments) {
        return assignments.entrySet().stream()
                .anyMatch(entryFirst ->
                        assignments.entrySet().stream()
                                .filter(entrySecond -> entryFirst != entrySecond)
                                .anyMatch(entrySecond -> {
                                    TimeSlotDTO slotFirst = entryFirst.getKey();
                                    TimeSlotDTO slotSecond = entrySecond.getKey();
                                    ScheduleItemDTO item1 = entryFirst.getValue();
                                    ScheduleItemDTO item2 = entrySecond.getValue();

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
     * <p>
     * 将内部使用的课程表模型转换为前端展示所需的课程安排格式。
     * </p>
     *
     * @param schedule 内部课程表模型
     * @return 课程安排列表
     */
    List<ScheduleResultDTO.ClassAssignmentDTO> convertScheduleToAssignments(@NotNull ScheduleDTO schedule) {
        List<ScheduleResultDTO.ClassAssignmentDTO> assignments = new ArrayList<>();

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slot = entry.getKey();
            ScheduleItemDTO item = entry.getValue();

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
        List<ScheduleDTO> population = new ArrayList<>();
        int populationSize = baseData.getAlgorithmParams().getPopulationSize();

        for (int i = 0; i < populationSize; i++) {
            ScheduleDTO schedule = new ScheduleDTO();
            Map<TimeSlotDTO, ScheduleItemDTO> assignments = new HashMap<>();
            log.debug("生成第 {} 个个体", i + 1);
            // 为每个课程分配时间槽
            for (CourseLibraryAndTeacherCourseQualificationListDTO courseAndTeachers : baseData.getCourseList()) {
                CourseLibraryDTO course = courseAndTeachers.getCourse();
                // 按照课程和班级进行教师选择
                Map<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> teacherAssignments = new HashMap<>();
                Map<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> classroomAssignments =
                        this.selectClassroomsForCourse(courseAndTeachers, baseData.getClassroomList());
                // 为每个班级选择合适的教师
                if (classroomAssignments != null) {
                    log.debug("教室map,{}",classroomAssignments);
                    for (Map.Entry<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> entry : classroomAssignments.entrySet()) {
                        List<AdministrativeClassDTO> classGroup = entry.getKey();
                        TeacherCoursePreferencesDTO teacher = this.selectTeacherForCourse(
                                course, courseAndTeachers.getTeacherList());
                        // 记录教师分配
                        if (teacher != null) {
                            teacherAssignments.put(classGroup, teacher);
                        }
                    }
                }
                // 为每个班级分配时间槽
                for (Map.Entry<List<AdministrativeClassDTO>, TeacherCoursePreferencesDTO> entry : teacherAssignments.entrySet()) {
                    List<AdministrativeClassDTO> classGroup = entry.getKey();
                    TeacherCoursePreferencesDTO assignedTeacher = entry.getValue();
                    ClassroomAndTypeDTO assignedClassroom = classroomAssignments.get(classGroup);
                    // 寻找合适的时间槽
                    TimeSlotDTO timeSlot = findSuitableTimeSlot(assignments, assignedTeacher, assignedClassroom, courseAndTeachers);
                    if (timeSlot != null) {
                        ScheduleItemDTO item = new ScheduleItemDTO(
                                course,
                                assignedTeacher,
                                assignedClassroom,
                                classGroup,
                                courseAndTeachers.getPriority()
                        );
                        assignments.put(timeSlot, item);
                        log.debug("为课程 {} 的班级 {} 分配了时间槽 {}，教师: {}",
                                course.getName(),
                                classGroup.stream().map(AdministrativeClassDTO::getClassName).toList(),
                                timeSlot.getPeriod(),
                                assignedTeacher.getTeacher().getName());
                    } else {
                        log.warn("无法为课程 {} 找到合适的时间槽", course.getName());
                    }
                }
            }

            schedule.setAssignments(assignments);
            population.add(schedule);
        }

        log.debug("生成初始种群完成，种群大小: {}", population.size());
        return population;
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
        log.debug("是否存在合适的教师: {}", suitableTeachers);

        // 如果没有找到符合条件的教师，返回null
        if (suitableTeachers.isEmpty()) {
            log.warn("没有找到合适的教师来教授课程: {}", course.getName());
            return null;
        }

        // 随机选择一个符合条件的教师
        Collections.shuffle(suitableTeachers, random);
        TeacherCoursePreferencesDTO selectedTeacher = suitableTeachers.get(0);
        log.debug("为课程 {} 随机选择的教师: {}", course.getName(), selectedTeacher);

        return selectedTeacher;
    }


    /**
     * 为课程选择合适的教室
     * 根据课程资格列表和可用教室列表，为每个班级分配最合适的教室
     * 如果无法为所有班级找到合适的教室，则返回null
     * @param courseQualificationList 课程资格列表，包含课程信息和班级列表
     * @param classrooms              可用的教室列表
     * @return 分配结果，以班级列表为键，分配的教室为值如果无法为所有班级找到合适的教室，则返回null
     */
    Map<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> selectClassroomsForCourse(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO courseQualificationList,
            @Nonnull List<ClassroomAndTypeDTO> classrooms) {
        CourseLibraryDTO course = courseQualificationList.getCourse();
        List<AdministrativeClassDTO> classList = courseQualificationList.getClassList();
        String courseType = getCourseType(course);
        log.debug("课程 {} 类型: {}", course.getName(), courseType);
        Map<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> allocationMap = new HashMap<>();
        List<ClassroomAndTypeDTO> remainingClassrooms = new ArrayList<>(classrooms);
        log.debug("课程人数,{}",courseQualificationList.getNumber());
        if (classList == null || classList.isEmpty()) {
            log.debug("只限定了人数");
            allocateVirtualClasses(course, courseQualificationList.getNumber(), remainingClassrooms, allocationMap, courseType);
        } else {
            allocateClassesByMajor(classList, remainingClassrooms, allocationMap, courseType);
        }
        return allocationMap;
    }
    /**
     * 为课程分配虚拟班级和教室
     * 此方法根据剩余教室的容量和课程类型，为一定数量的学生分配虚拟班级和合适的教室
     * @param course 课程库DTO，包含课程信息
     * @param number 需要分配虚拟班级的学生人数
     * @param remainingClassrooms 剩余可用的教室和类型列表
     * @param allocationMap 分配结果映射，键为虚拟班级列表，值为分配的教室
     * @param courseType 课程类型，用于筛选合适的教室
     */
    private void allocateVirtualClasses(CourseLibraryDTO course, int number,
                                        List<ClassroomAndTypeDTO> remainingClassrooms,
                                        Map<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> allocationMap,
                                        String courseType) {
        // 初始化学生计数器和虚拟班级索引
        int studentCounter = 0;
        int virtualClassIndex = 1;
        log.debug("进行循环前");
        // 循环直到所有学生都被分配到虚拟班级
        while (studentCounter < number) {
            log.debug("计算为分配的学生人数");
            // 计算剩余未分配的学生人数
            int remainingStudents = number - studentCounter;
            // 寻找最适合当前剩余学生数的教室
            ClassroomAndTypeDTO selectedClassroom = findBestClassroom(remainingClassrooms, remainingStudents, courseType);
            // 如果没有合适的教室，则记录日志并退出分配过程
            if (selectedClassroom == null) {
                log.debug("没有足够的教室容纳剩余的 {} 名学生", remainingStudents);
                return;
            }
            log.debug("有足够多的教室");
            // 根据选定的教室容量和剩余学生数，确定本次分配的学生人数
            int assignedStudents = Math.min(selectedClassroom.getClassroom().getCapacity(), remainingStudents);
            // 生成虚拟班级的唯一标识
            String classKey = course.getCourseLibraryUuid() + "-" + virtualClassIndex;
            // 创建并初始化虚拟班级DTO
            AdministrativeClassDTO classDTO = new AdministrativeClassDTO();
            classDTO.setAdministrativeClassUuid(classKey);
            // 将虚拟班级DTO封装为列表，作为映射的键
            List<AdministrativeClassDTO> virtualClass = List.of(classDTO);
            // 将虚拟班级和选定的教室添加到分配结果映射中
            allocationMap.put(virtualClass, selectedClassroom);
            // 记录分配结果的日志信息
            log.debug("虚拟班级 {} ({} 人) 分配到教室 {}", classKey, assignedStudents, selectedClassroom.getClassroom().getName());
            // 更新学生计数器和虚拟班级索引
            studentCounter += assignedStudents;
            virtualClassIndex++;
            // 从剩余教室列表中移除已分配的教室
            remainingClassrooms.remove(selectedClassroom);
        }
    }

    /**
     * 根据专业分配班级到教室
     * 该方法旨在根据班级的学生人数和课程类型，尽可能高效地利用剩余教室资源进行班级分配
     * @param classList 待分配的行政班列表，不能为空
     * @param remainingClassrooms 剩余可用的教室及其类型列表
     * @param allocationMap 班级到教室的分配映射
     * @param courseType 课程类型，用于辅助教室分配决策
     */
    private void allocateClassesByMajor(@NotNull List<AdministrativeClassDTO> classList,
                                        List<ClassroomAndTypeDTO> remainingClassrooms,
                                        Map<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> allocationMap,
                                        String courseType) {
        // 按专业分组班级，以便后续处理
        Map<String, List<AdministrativeClassDTO>> classesByMajor = classList.stream()
                .collect(Collectors.groupingBy(AdministrativeClassDTO::getMajorUuid));
        // 遍历每个专业的班级
        for (List<AdministrativeClassDTO> majorClasses : classesByMajor.values()) {
            List<AdministrativeClassDTO> pendingClasses = new ArrayList<>();
            // 遍历当前专业的所有班级，尝试分配教室
            for (AdministrativeClassDTO adminClass : majorClasses) {
                pendingClasses.add(adminClass);
                int totalStudents = pendingClasses.stream().mapToInt(AdministrativeClassDTO::getStudentCount).sum();
                // 先尝试分配教室，不立即清空 pendingClasses
                ClassroomAndTypeDTO selectedClassroom = findBestClassroom(remainingClassrooms, totalStudents, courseType);
                if (selectedClassroom != null) {
                    int remainingCapacity = selectedClassroom.getClassroom().getCapacity() - totalStudents;
                    // **如果教室还有多余容量，尝试添加更多班级**
                    while (!remainingClassrooms.isEmpty() && remainingCapacity > 0) {
                        AdministrativeClassDTO nextClass = findNextClass(majorClasses, pendingClasses);
                        if (nextClass == null) {
                            break;
                        }
                        int nextClassSize = nextClass.getStudentCount();
                        if (remainingCapacity >= nextClassSize) {
                            pendingClasses.add(nextClass);
                            remainingCapacity -= nextClassSize;
                        } else {
                            break;
                        }
                    }
                    // **最终确认分配**
                    allocationMap.put(new ArrayList<>(pendingClasses), selectedClassroom);
                    log.debug("班级 {} 共享教室 {}", pendingClasses
                            .stream()
                            .map(AdministrativeClassDTO::getClassName).toList(), selectedClassroom.getClassroom().getName());
                    remainingClassrooms.remove(selectedClassroom);
                    pendingClasses.clear();
                }
            }
            // **仍然有未分配的班级，尝试随机分配**
            if (!pendingClasses.isEmpty()) {
                allocateUnassignedClasses(pendingClasses, remainingClassrooms, allocationMap);
            }
        }
    }
    /**
     * 寻找下一个未处理的行政班级
     * 该方法用于在给定的专业班级列表中，找到下一个不在待处理列表中的班级
     * 主要用于班级处理流程中，确定下一个需要处理的班级
     * @param majorClasses    专业班级列表，不能为空
     * @param pendingClasses  待处理班级列表，可能为空
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
     * @param pendingClasses 待分配的班级列表
     * @param remainingClassrooms 剩余可用的教室列表
     * @param allocationMap 班级到教室的分配映射
     */
    private void allocateUnassignedClasses(@NotNull List<AdministrativeClassDTO> pendingClasses,
                                           List<ClassroomAndTypeDTO> remainingClassrooms,
                                           Map<List<AdministrativeClassDTO>, ClassroomAndTypeDTO> allocationMap) {
        // 当没有找到合适的教室来教授班级时，记录待分配的班级名称
        log.debug("没有找到合适的教室来教授班级: {}", pendingClasses.stream().map(AdministrativeClassDTO::getClassName).toList());

        // 从剩余的教室中随机选择一个教室
        ClassroomAndTypeDTO randomClassroom = selectRandomClassroom(remainingClassrooms);

        // 如果随机选中的教室不为空，则将所有待分配的班级都分配到这个教室，并记录分配信息
        if (randomClassroom != null) {
            allocationMap.put(new ArrayList<>(pendingClasses), randomClassroom);
            log.debug("为班级 {} 随机分配了教室 {}", pendingClasses.stream().map(AdministrativeClassDTO::getClassName).toList(), randomClassroom.getClassroom().getName());
        }
    }
    private @Nullable ClassroomAndTypeDTO findBestClassroom(@NotNull List<ClassroomAndTypeDTO> classrooms,
                                                            int studentCount, String courseType) {
        return classrooms.stream()
                .filter(c -> c.getClassroom().getCapacity() >= studentCount &&
                        (courseType == null || courseType.equals(c.getClassroomType().getClassTypeUuid())))
                .min(Comparator.comparingInt(c -> c.getClassroom().getCapacity()))
                .or(() -> classrooms.stream()
                        .filter(c -> c.getClassroom().getCapacity() >= studentCount && c.getClassroomType().getClassTypeUuid() == null)
                        .min(Comparator.comparingInt(c -> c.getClassroom().getCapacity())))
                .orElse(null);
    }

    /**
     * 随机选择一个教室
     * 从给定的教室列表中随机选择一个教室如果列表为空，则返回null
     * @param classrooms 一个包含教室信息的列表，不能为null
     * @return 随机选中的教室信息，如果列表为空则返回null
     */
    private @Nullable ClassroomAndTypeDTO selectRandomClassroom(@NotNull List<ClassroomAndTypeDTO> classrooms) {
        return classrooms.isEmpty() ? null : classrooms.get(new Random().nextInt(classrooms.size()));
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
    double calculateConsecutiveCoursesFitness(@NotNull ScheduleDTO schedule) {
        // 按课程分组
        Map<String, List<TimeSlotDTO>> courseSlots = schedule.getAssignments().entrySet().stream()
                .collect(Collectors.groupingBy(
                        entry -> entry.getValue().getCourse().getCourseLibraryUuid(),
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())
                ));

        // 使用流处理所有课程时间槽
        return courseSlots.values().stream()
                .flatMap(slots -> slots.stream()
                        // 按周和天分组
                        .collect(Collectors.groupingBy(
                                TimeSlotDTO::getWeek,
                                Collectors.groupingBy(TimeSlotDTO::getDay)
                        )).values().stream())
                .flatMap(dayMap -> dayMap.values().stream())
                // 只处理有多个时间槽的天
                .filter(dailySlots -> dailySlots.size() > 1)
                .mapToDouble(dailySlots -> {
                    // 排序并计算连续课程数
                    List<TimeSlotDTO> sortedSlots = dailySlots.stream()
                            .sorted(Comparator.comparingInt(TimeSlotDTO::getPeriod))
                            .toList();

                    // 计算连续课程的奖励
                    double consecutiveReward = 0.0;
                    for (int i = 0; i < sortedSlots.size() - 1; i++) {
                        if (sortedSlots.get(i + 1).getPeriod() - sortedSlots.get(i).getPeriod() == 1) {
                            // 连续课程奖励
                            consecutiveReward += 5.0;
                        }
                    }
                    return consecutiveReward;
                })
                .sum();
    }

    /**
     * 计算时间偏好适应度
     * <p>
     * 评估课程安排与设定的时间偏好的匹配程度，
     * 包括偏好时间段和避免晚课等偏好。
     * </p>
     *
     * @param schedule    待评估的课程表
     * @param preferences 时间偏好设置
     * @return 时间偏好适应度得分
     */
    double calculateTimePreferenceFitness(
            @NotNull ScheduleDTO schedule,
            AutomaticClassSchedulingBaseDTO.TimePreferences preferences
    ) {
        double fitness = 0.0;

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            TimeSlotDTO slot = entry.getKey();

            // 检查是否在偏好时间段
            boolean inPreferredSlot = preferences.getPreferredTimeSlots().stream()
                    .anyMatch(preferred ->
                            preferred.getDay() == slot.getDay() &&
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
    double calculateRoomOptimizationFitness(@NotNull ScheduleDTO schedule) {
        double fitness = 0.0;

        for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry : schedule.getAssignments().entrySet()) {
            ScheduleItemDTO item = entry.getValue();

            // 教室容量与学生数量的匹配度
            int capacity = item.getClassroom().getClassroom().getCapacity();

            // 获取课程所需学生数，使用totalHours作为替代指标
            int studentCount = item.getCourse().getTotalHours() != null ?
                    item.getCourse().getTotalHours().intValue() : 30;

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
            // 使用课程类型与教室类型的比较
            String courseType = item.getCourse().getType();
            String classroomType = item.getClassroom().getClassroomType().getName();

            if (classroomType != null && classroomType.equals(courseType)) {
                fitness += 10.0;
            }
        }

        return fitness;
    }
    /**
     * 执行时间槽变异
     */
    public void timeSlotMutation(
            @NotNull ScheduleDTO schedule,
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courses
    ) {
        List<Map.Entry<TimeSlotDTO, ScheduleItemDTO>> entries = new ArrayList<>(schedule.getAssignments().entrySet());
        if (!entries.isEmpty()) {
            // 随机选择一个课程进行时间变异
            Map.Entry<TimeSlotDTO, ScheduleItemDTO> entry = entries.get(random.nextInt(entries.size()));
            // 获取对应的课程信息
            CourseLibraryAndTeacherCourseQualificationListDTO course = findCourseByScheduleItem(entry.getValue(), courses);
            if (course != null) {
                schedule.getAssignments().remove(entry.getKey());
                // 尝试找到新的合适时间槽
                TimeSlotDTO newTimeSlot = findSuitableTimeSlot(
                        schedule.getAssignments(),
                        entry.getValue().getTeacher(),
                        entry.getValue().getClassroom(),
                        course
                );
                if (newTimeSlot != null) {
                    schedule.getAssignments().put(newTimeSlot, entry.getValue());
                } else {
                    // 如果找不到合适的新时间槽，尝试与其他课程交换时间
                    for (Map.Entry<TimeSlotDTO, ScheduleItemDTO> other : entries) {
                        if (other != entry && isSwapValid(entry, other, schedule, courses)) {
                            swapTimeSlots(schedule, entry, other);
                            return;
                        }
                    }
                    // 如果无法交换，恢复原有安排
                    schedule.getAssignments().put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * 查找合适的时间槽
     */
    @Nullable
    private TimeSlotDTO findSuitableTimeSlot(
            Map<TimeSlotDTO, ScheduleItemDTO> assignments,
            TeacherCoursePreferencesDTO teacher,
            ClassroomAndTypeDTO classroom,
            CourseLibraryAndTeacherCourseQualificationListDTO course
    ) {
        List<TimeSlotDTO> allTimeSlots = generateTimeSlotsByCourse(course);
        Collections.shuffle(allTimeSlots);

        for (TimeSlotDTO slot : allTimeSlots) {
            if (isTimeSlotSuitable(slot, assignments, teacher, classroom, course)) {
                return slot;
            }
        }

        return null;
    }

    /**
     * 根据课程信息生成合适的时间槽
     */
    private @NotNull List<TimeSlotDTO> generateTimeSlotsByCourse(@NotNull CourseLibraryAndTeacherCourseQualificationListDTO course) {
        List<TimeSlotDTO> slots = new ArrayList<>();

        for (int week = course.getStartWeek(); week <= course.getEndWeek(); week++) {
            boolean isOddWeek = (week % 2 == 1);
            // 检查是否符合单双周要求
            if (course.getWeeklyHours() % 2 == 1 && course.getIsOddWeek() != null) {
                // 如果是单双周课程，只添加符合要求的周
                if (Boolean.TRUE.equals(course.getIsOddWeek()) != isOddWeek) {
                    continue;
                }
            }
            // 一周5天
            for (int day = 1; day <= 5; day++) {
                // 一天8节课
                for (int period = 1; period <= 8; period++) {
                    if (course.getWeeklyHours() % 2 == 1 && course.getIsOddWeek() != null) {
                        // 单双周课程
                        slots.add(new TimeSlotDTO(week, day, period, isOddWeek));
                    } else {
                        // 普通课程
                        slots.add(new TimeSlotDTO(week, day, period, null));
                    }
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
            @NotNull Map<TimeSlotDTO, ScheduleItemDTO> assignments,
            TeacherCoursePreferencesDTO teacher,
            ClassroomAndTypeDTO classroom,
            CourseLibraryAndTeacherCourseQualificationListDTO course
    ) {
        // 1. 检查时间槽是否已被占用
        if (assignments.containsKey(slot)) {
            return false;
        }
        // 2. 检查教师在该时间段是否有其他课程
        if (!isTeacherAvailable(teacher, slot, assignments)) {
            return false;
        }
        // 3. 检查教室在该时间段是否可用
        if (!isClassroomAvailable(classroom, slot, assignments)) {
            return false;
        }
        // 4. 检查是否符合课程的单双周要求
        if (course.getWeeklyHours() % 2 == 1 && course.getIsOddWeek() != null) {
            if (slot.getIsOddWeek() == null || !Objects.equals(slot.getIsOddWeek(), course.getIsOddWeek())) {
                return false;
            }
        }
        // 5. 检查是否在课程的周数范围内
        return slot.getWeek() >= course.getStartWeek() && slot.getWeek() <= course.getEndWeek();
    }

    /**
     * 检查教师是否在指定时间槽可用
     */
    private boolean isTeacherAvailable(
            TeacherCoursePreferencesDTO teacher,
            TimeSlotDTO slot,
            @NotNull Map<TimeSlotDTO, ScheduleItemDTO> assignments
    ) {
        return assignments.entrySet().stream()
                .noneMatch(entry -> entry.getKey().getWeek() == slot.getWeek() &&
                        entry.getKey().getDay() == slot.getDay() &&
                        entry.getKey().getPeriod() == slot.getPeriod() &&
                        entry.getValue().getTeacher().equals(teacher));
    }

    /**
     * 检查教室是否在指定时间槽可用
     */
    private boolean isClassroomAvailable(
            ClassroomAndTypeDTO classroom,
            TimeSlotDTO slot,
            @NotNull Map<TimeSlotDTO, ScheduleItemDTO> assignments
    ) {
        return assignments.entrySet().stream()
                .noneMatch(entry -> entry.getKey().getWeek() == slot.getWeek() &&
                        entry.getKey().getDay() == slot.getDay() &&
                        entry.getKey().getPeriod() == slot.getPeriod() &&
                        entry.getValue().getClassroom().equals(classroom));
    }

    /**
     * 根据ScheduleItem查找对应的课程信息
     */
    private CourseLibraryAndTeacherCourseQualificationListDTO findCourseByScheduleItem(
            ScheduleItemDTO item,
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courses
    ) {
        return courses.stream()
                .filter(course -> course.getCourse().getId().equals(item.getCourse().getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 检查交换是否有效
     */
    private boolean isSwapValid(
            Map.@NotNull Entry<TimeSlotDTO, ScheduleItemDTO> entry1,
            Map.@NotNull Entry<TimeSlotDTO, ScheduleItemDTO> entry2,
            ScheduleDTO schedule,
            List<CourseLibraryAndTeacherCourseQualificationListDTO> courses
    ) {
        CourseLibraryAndTeacherCourseQualificationListDTO course1 = findCourseByScheduleItem(entry1.getValue(), courses);
        CourseLibraryAndTeacherCourseQualificationListDTO course2 = findCourseByScheduleItem(entry2.getValue(), courses);

        if (course1 == null || course2 == null) {
            return false;
        }

        // 创建临时的assignments来验证交换是否有效
        Map<TimeSlotDTO, ScheduleItemDTO> tempAssignments = new HashMap<>(schedule.getAssignments());
        tempAssignments.remove(entry1.getKey());
        tempAssignments.remove(entry2.getKey());

        // 检查交换后的时间槽是否适合各自的课程
        boolean slot1Valid = isTimeSlotSuitable(
                entry2.getKey(),
                tempAssignments,
                entry1.getValue().getTeacher(),
                entry1.getValue().getClassroom(),
                course1
        );

        boolean slot2Valid = isTimeSlotSuitable(
                entry1.getKey(),
                tempAssignments,
                entry2.getValue().getTeacher(),
                entry2.getValue().getClassroom(),
                course2
        );

        return slot1Valid && slot2Valid;
    }

    /**
     * 交换时间槽
     */
    private void swapTimeSlots(
            @NotNull ScheduleDTO schedule,
            Map.@NotNull Entry<TimeSlotDTO, ScheduleItemDTO> entry1,
            Map.@NotNull Entry<TimeSlotDTO, ScheduleItemDTO> entry2
    ) {
        ScheduleItemDTO temp = entry1.getValue();
        schedule.getAssignments().put(entry1.getKey(), entry2.getValue());
        schedule.getAssignments().put(entry2.getKey(), temp);
    }
}
