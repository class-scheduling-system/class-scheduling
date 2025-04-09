package com.frontleaves.scheduling.thread;

import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.SchedulingConflictDAO;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;
import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.CreditHourTypeEnuDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.models.dto.scheduling.TimeSlotDTO;
import com.frontleaves.scheduling.models.entity.base.ClassAssignmentDO;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.services.ClassAssignmentService;
import com.frontleaves.scheduling.services.CreditHourTypeService;
import com.frontleaves.scheduling.services.TeachingClassService;
import com.frontleaves.scheduling.services.scheduling.GeneticSchedulingService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import enums.CourseEnuType;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * 自动排课线程
 * 使用遗传算法进行智能排课
 *
 * @author xiao_lfeng
 */
@Slf4j
public class AutomaticClassSchedulingThread extends Thread {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    @Resource
    private RedissonClient redisson;
    @Resource
    private GeneticSchedulingService geneticSchedulingService;
    @Resource
    private CreditHourTypeService creditHourTypeService;
    @Resource
    private ClassAssignmentService classAssignmentService;
    @Resource
    private TeachingClassService teachingClassService;
    @Resource
    private SchedulingConflictDAO schedulingConflictDAO;

    private final SecureRandom random = new SecureRandom();
    private boolean hasTask = false;

    private UserDO user;
    private String taskId;
    public AutomaticClassSchedulingThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        log.info("自动排课线程启动，等待任务...");

        while (true) {
            lock.lock();
            try {
                while (!hasTask) {
                    log.info(LogConstant.THREAD + "线程进入等待状态");
                    condition.await();
                }
                // 从Redis获取排课基础数据
                RBucket<AutomaticClassSchedulingBaseDTO> cacheData = redisson
                        .getBucket(StringConstant.Redis.SCHEDULE_LESSONS + user.getUserUuid());
                if (!cacheData.isExists()) {
                    throw new BusinessException("缓存数据不存在", ErrorCode.BODY_ERROR);
                }
                AutomaticClassSchedulingBaseDTO baseData = cacheData.get();
                // 执行遗传算法排课
                log.info("开始执行遗传算法排课，任务ID：{}", taskId);
                ScheduleResultDTO result = geneticSchedulingService.executeGeneticAlgorithm(taskId, baseData);
                this.getSaveScheduleDTO(result);
                // 保存排课冲突信息
                this.saveSchedulingConflicts(result);
                // 将排课结果保存到Redis
                RBucket<ScheduleResultDTO> resultCache = redisson
                        .getBucket(StringConstant.Redis.SCHEDULE_RESULT + taskId);
                resultCache.set(result);
                resultCache.expire(Duration.ofHours(24));
                // 删除任务有关的缓存数据
                RKeys rKeys = redisson.getKeys();
                rKeys.deleteByPattern(StringConstant.Redis.SCHEDULE_LESSONS + user.getUserUuid());
                rKeys.deleteByPattern(StringConstant.Redis.SCHEDULE_EXECUTE_STATUS + user.getUserUuid());
                rKeys.deleteByPattern(StringConstant.Redis.SCHEDULE_EXECUTE_PROGRESS + user.getUserUuid());

                log.info("排课完成，适应度：{}", result.getFitness());
                hasTask = false;
            } catch (Exception e) {
                log.error("排课过程发生错误：", e);
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
        }

        log.info("排课线程结束运行");
    }

    /**
     * 保存排课冲突信息到数据库
     *
     * @param result 排课结果，包含冲突信息列表
     */
    private void saveSchedulingConflicts(@NotNull ScheduleResultDTO result) {
        List<SchedulingConflictDTO> conflicts = result.getConflicts();
        if (conflicts == null || conflicts.isEmpty()) {
            log.info("没有检测到排课冲突，无需保存冲突信息");
            return;
        }

        log.info("检测到{}个排课冲突，开始保存冲突信息", conflicts.size());

        // 设置学期ID
        for (SchedulingConflictDTO conflict : conflicts) {
            conflict
                    .setSemesterUuid(result.getSemesterUuid())
                    // 设置为未解决状态
                    .setResolutionStatus(0);
        }

        // 批量保存冲突信息
        int savedCount = schedulingConflictDAO.batchSaveConflicts(conflicts, result.getSemesterUuid());
        log.info("成功保存{}个排课冲突信息", savedCount);
    }

    /**
     * 保存排课结果到数据库
     * <p>
     * 将排课结果中的教学班和课程安排信息保存到数据库
     * 同时将冲突信息保存到cs_scheduling_conflict表
     * </p>
     * 
     * @param result 排课结果
     */
    private void getSaveScheduleDTO(@NotNull ScheduleResultDTO result) {
        List<ScheduleResultDTO.CourseTeachingClassDTO> assignments = result.getAssignments();

        // 获取学时类型list
        List<CreditHourTypeEnuDTO> creditHourTypeList = creditHourTypeService.getList();
        Map<CourseEnuType, String> creditHourTypeUuidMapping = this.getAllCreditHourTypeUuidMapping(creditHourTypeList);
        for (ScheduleResultDTO.CourseTeachingClassDTO assignment : assignments) {
            log.debug("教学班UUID：{}", assignment.getTeachingClass().getTeachingClassUuid());
            // 新建教学班
            TeachingClassDO teachingClassDO = new TeachingClassDO();
            //检测是否存在此教学班
            TeachingClassDTO existingTeachingClass = teachingClassService.getTeachingClassByUuidNoError(
                    assignment.getTeachingClass().getTeachingClassUuid());
            log.debug("是否存在教学班：{}", existingTeachingClass != null);
            if (existingTeachingClass == null) {
                teachingClassDO
                        .setTeachingClassUuid(assignment.getTeachingClass().getTeachingClassUuid())
                        .setSemesterUuid(result.getSemesterUuid())
                        .setCourseUuid(assignment.getCourse().getCourseLibraryUuid())
                        .setTeachingClassCode(UuidUtil.generateUuidNoDash())
                        .setTeachingClassName(assignment.getCourse().getName() + random.nextInt(900000) + 100000)
                        .setAdministrativeClasses(this.checkClass(assignment.getClassGroup()))
                        .setIsAdministrative(this.checkIfItIsAnAdministrativeClass(assignment.getClassGroup()))
                        .setClassSize(this.detectClassSize(assignment.getClassGroup()))
                        .setActualStudentCount(assignment.getNumber())
                        .setCourseDepartmentUuid(assignment.getCourse().getDepartment())
                        .setIsEnabled(true);
                 teachingClassService.save(teachingClassDO);
            }
            ClassAssignmentDO classAssignmentDO = new ClassAssignmentDO();
            classAssignmentDO
                    .setClassAssignmentUuid(assignment.getCourseScheduleItemUuid())
                    .setSemesterUuid(result.getSemesterUuid())
                    .setCourseUuid(assignment.getCourse().getCourseLibraryUuid())
                    .setTeacherUuid(assignment.getTeacher().getTeacher().getTeacherUuid())
                    .setCampusUuid(assignment.getClassroom().getCampus().getCampusUuid())
                    .setBuildingUuid(assignment.getClassroom().getBuilding().getBuildingUuid())
                    .setClassroomUuid(assignment.getClassroom().getClassroom().getClassroomUuid())
                    .setTeachingClassUuid(assignment.getTeachingClass().getTeachingClassUuid())
                    .setCourseOwnership("未定义")
                    .setCreditHourType(creditHourTypeUuidMapping.get(assignment.getCourseType().getCourseEnuType()))
                    .setScheduledHours(this.getscheduleClassHours(assignment.getTimeSlot()))
                    .setTotalHours(this.getHoursByCourseType(assignment.getCourseType().getCourseEnuType(),
                            assignment.getCourse()))
                    .setSchedulingPriority(assignment.getPriority())
                    .setTeachingCampus(assignment.getClassroom().getCampus().getCampusUuid())
                    .setClassTime(JSONUtil.toJsonStr(assignment.getTimeSlot()))
                    .setSpecifiedTime(this.calculateContinuousClassHours(assignment.getTimeSlot()))
                    .setClassroomType(assignment.getClassroom().getType().getClassTypeUuid());
            // 新建教学班
            classAssignmentService.save(classAssignmentDO);
        }
    }

    /**
     * 检查并转换行政班级UUID列表为JSON字符串
     *
     * @param classGroup 行政班级列表
     * @return JSON字符串，如果列表为空则返回"[]"
     */
    private String checkClass(List<AdministrativeClassDTO> classGroup) {
        if (classGroup == null || classGroup.isEmpty()) {
            return "[]";
        }
        List<String> classUuids = classGroup.stream()
                .filter(Objects::nonNull)
                .map(AdministrativeClassDTO::getAdministrativeClassUuid)
                .filter(StringUtils::isNotBlank)
                .toList();
        return JSONUtil.toJsonStr(classUuids);
    }

    private @NotNull Integer detectClassSize(List<AdministrativeClassDTO> classGroup) {
        if (classGroup == null || classGroup.isEmpty()) {
            return 0;
        }
        // 计算班级人数
        return classGroup.size();
    }

    @Contract(pure = true)
    private @NotNull Boolean checkIfItIsAnAdministrativeClass(List<AdministrativeClassDTO> classGroup) {
        return classGroup != null && !classGroup.isEmpty();
    }

    /**
     * 计算连堂课的课时数并返回字符串形式
     *
     * @param timeSlots 时间槽列表
     * @return 课时数的字符串表示
     */
    private @NotNull String calculateContinuousClassHours(List<TimeSlotDTO> timeSlots) {
        if (timeSlots == null || timeSlots.isEmpty()) {
            return "0";
        }
        // 按周和天分组
        Map<String, List<TimeSlotDTO>> groupedSlots = timeSlots.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(slot -> slot.getWeek() + "-" + slot.getDay()));
        // 计算每组中连续的课时数
        BigDecimal totalHours = BigDecimal.ZERO;
        for (List<TimeSlotDTO> daySlots : groupedSlots.values()) {
            // 按照period排序
            daySlots.sort(Comparator.comparing(TimeSlotDTO::getPeriod));
            int continuousCount = 1;
            for (int i = 1; i < daySlots.size(); i++) {
                if (daySlots.get(i).getPeriod() == daySlots.get(i - 1).getPeriod() + 1) {
                    // 如果是连续的
                    continuousCount++;
                } else {
                    // 不连续，计算之前的连续课时
                    totalHours = totalHours.add(new BigDecimal(continuousCount / 2));
                    continuousCount = 1;
                }
            }
            // 处理最后一组连续课时
            totalHours = totalHours.add(new BigDecimal(continuousCount / 2));
        }

        // 转换为字符串，去掉小数点后的.0
        return totalHours.stripTrailingZeros().toPlainString();
    }

    /**
     * 根据课程类型获取对应的学时数
     *
     * @param courseEnuType 课程类型枚举
     * @param course        课程信息
     * @return 对应类型的学时数，如果是混排类型或无效类型则返回0
     */
    private BigDecimal getHoursByCourseType(CourseEnuType courseEnuType, CourseLibraryDTO course) {
        if (courseEnuType == null || course == null) {
            return BigDecimal.ZERO;
        }
        return switch (courseEnuType) {
            case THEORY -> course.getTheoryHours() != null ? course.getTheoryHours() : BigDecimal.ZERO;
            case EXPERIMENT -> course.getExperimentHours() != null ? course.getExperimentHours() : BigDecimal.ZERO;
            case PRACTICE -> course.getPracticeHours() != null ? course.getPracticeHours() : BigDecimal.ZERO;
            case COMPUTER -> course.getComputerHours() != null ? course.getComputerHours() : BigDecimal.ZERO;
            case OTHER -> course.getOtherHours() != null ? course.getOtherHours() : BigDecimal.ZERO;
            // 混排课程特殊处
            case MIXED -> BigDecimal.ZERO;
        };
    }

    /**
     * 计算调度课程的总学时
     *
     * @param timeSlot 时间段列表，表示课程安排的时间
     * @return 返回总学时如果时间段列表为空或null，则返回0
     */
    private BigDecimal getscheduleClassHours(List<TimeSlotDTO> timeSlot) {
        // 检查时间段列表是否为空或null，如果是，则返回0
        if (timeSlot == null || timeSlot.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // 返回时间段列表的大小作为总学时
        return new BigDecimal(timeSlot.size());
    }

    /**
     * 获取所有课程类型与UUID的映射关系
     */
    private @NotNull Map<CourseEnuType, String> getAllCreditHourTypeUuidMapping(
            List<CreditHourTypeEnuDTO> creditHourTypeList) {
        Map<CourseEnuType, String> mapping = new EnumMap<>(CourseEnuType.class);
        for (CourseEnuType type : CourseEnuType.values()) {
            // 排除混排课程
            if (type != CourseEnuType.MIXED) {
                String uuid = getCreditHourTypeUuid(type, creditHourTypeList);
                if (uuid != null) {
                    mapping.put(type, uuid);
                }
            }
        }

        return mapping;
    }

    /**
     * 通过课程类型枚举获取对应的学时类型UUID
     */
    private String getCreditHourTypeUuid(CourseEnuType courseEnuType, List<CreditHourTypeEnuDTO> creditHourTypeList) {
        if (courseEnuType == null || creditHourTypeList == null || creditHourTypeList.isEmpty()) {
            return null;
        }
        // 如果是混排课程，直接返回null或特殊处理
        if (courseEnuType == CourseEnuType.MIXED) {
            return null;
        }
        // 根据枚举值的name匹配CreditHourTypeEnuDTO的name
        return creditHourTypeList.stream()
                .filter(type -> type.getName() != null && matchCreditHourType(courseEnuType, type.getName()))
                .findFirst()
                .map(CreditHourTypeEnuDTO::getCreditHourTypeUuid)
                .orElse(null);
    }

    /**
     * 匹配课程类型和学时类型名称
     */
    private boolean matchCreditHourType(@NotNull CourseEnuType courseEnuType, String creditHourTypeName) {
        return switch (courseEnuType) {
            case THEORY -> creditHourTypeName.contains("理论") || "理论学时".equals(creditHourTypeName);
            case EXPERIMENT -> creditHourTypeName.contains("实验") || "实验学时".equals(creditHourTypeName);
            case PRACTICE -> creditHourTypeName.contains("实践") || "实践学时".equals(creditHourTypeName);
            case COMPUTER -> creditHourTypeName.contains("上机") || "上机学时".equals(creditHourTypeName);
            case OTHER -> creditHourTypeName.contains("其他") || "其他学时".equals(creditHourTypeName);
            default -> false;
        };
    }

    /**
     * 启动排课任务
     */
    public void startUp(UserDO user, String taskId) {
        lock.lock();
        try {
            this.user = user;
            this.taskId = taskId;
            condition.signal();
            hasTask = true;
            log.info("已通知线程执行排课任务");
        } finally {
            lock.unlock();
        }
    }
}
