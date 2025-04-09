package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.AcademicAffairsPermissionDAO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingTaskDTO;
import com.frontleaves.scheduling.models.dto.base.SemesterDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.IterativeDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.models.dto.scheduling.SchedulingTaskStatusDTO;
import com.frontleaves.scheduling.models.entity.base.AcademicAffairsPermissionDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.services.SchedulingService;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.thread.ScheduleLessonsDataPreparationThread;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import enums.CourseEnuType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Objects;

/**
 * 调度逻辑
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingLogic implements SchedulingService {
    private final ScheduleLessonsDataPreparationThread scheduleLessonsDataPreparationThread;
    private final UserService userService;
    private final AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    private final RedissonClient redisson;

    /**
     * 检查结束周是否超过学期周
     *
     * @param endWeek     结束周
     * @param semesterDTO 学期信息
     * @throws BusinessException 当结束周超过学期周时抛出异常
     */
    public static void checkEndWeekExceedSemesterWeeks(Integer endWeek, @NotNull SemesterDTO semesterDTO) {
        if (semesterDTO.getEndDate() == null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(semesterDTO.getStartDate());
            calendar.add(Calendar.WEEK_OF_YEAR, 31);
            semesterDTO.setEndDate(calendar.getTime());
        }
        // 计算学期总周数
        long totalWeeks = (semesterDTO.getEndDate().getTime() - semesterDTO.getStartDate().getTime())
                / (7 * 24 * 60 * 60 * 1000) + 1;
        if (endWeek > totalWeeks) {
            throw new BusinessException("开始周或者结束周超过学期总周数", ErrorCode.BODY_ERROR);
        }
    }

    public static @NotNull CourseLibraryAndTeacherCourseQualificationListDTO copyAndSet(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO originalDto,
            CourseEnuType newType,
            BigDecimal hours) {
        CourseLibraryAndTeacherCourseQualificationListDTO newDto = new CourseLibraryAndTeacherCourseQualificationListDTO();
        // 复制原始DTO的属性
        return newDto.setCourse(originalDto.getCourse())
                .setClassList(originalDto.getClassList())
                .setNumber(originalDto.getNumber())
                .setTeacherList(originalDto.getTeacherList())
                .setWeeklyHours(originalDto.getWeeklyHours())
                // 使用新的类型
                .setCourseEnuType(newType)
                .setIsOddWeek(originalDto.getIsOddWeek())
                .setStartWeek(originalDto.getStartWeek())
                .setEndWeek(originalDto.getEndWeek())
                // 使用新的课时
                .setExpectedTotalHours(hours)
                .setPriority(originalDto.getPriority());
    }

    /**
     * 获取自动排课基础DTO
     *
     * @param automaticClassSchedulingVO 自动排课请求对象，包含排课所需的各种设置和参数
     * @param request                    HTTP请求对象，用于获取当前用户信息
     */
    @Override
    public SchedulingTaskDTO getAutoClassSchedulingBaseDTO(
            @NotNull AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request) {
        UserDO getUser = userService.getUserByRequest(request);
        AcademicAffairsPermissionDO academicAffairsPermission = academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(getUser.getUserUuid());

        // 生成任务ID
        String taskId = getUser.getUserUuid() + "_" + System.currentTimeMillis();

        SchedulingTaskDTO schedulingTask = new SchedulingTaskDTO()
                .setTaskId(taskId)
                .setSemesterUuid(automaticClassSchedulingVO.getSemesterUuid())
                .setDepartmentUuid(academicAffairsPermission.getDepartment())
                .setStatus("processing")
                .setEstimatedTime(1000)
                .setCreatedAt(new Timestamp(System.currentTimeMillis()))
                .setCreatedBy(getUser.getUserUuid());

        try {
            scheduleLessonsDataPreparationThread.startUp(automaticClassSchedulingVO, taskId, request);
        } catch (Exception e) {
            schedulingTask.setStatus("failed");
            throw new BusinessException("排课失败", ErrorCode.BODY_ERROR, e);
        }

        redisson.getBucket(StringConstant.Redis.SCHEDULING_TASK + taskId).set(schedulingTask);
        return schedulingTask;
    }


    @Override
    public @NotNull SchedulingTaskStatusDTO getSchedulingTaskStatus(@NotNull String taskId) {
        RBucket<SchedulingTaskDTO> bucket = redisson.getBucket(StringConstant.Redis.SCHEDULING_TASK + taskId);
        if (!bucket.isExists()) {
            throw new BusinessException("排课任务不存在", ErrorCode.BODY_ERROR);
        }
        SchedulingTaskDTO task = bucket.get();
        //获取排课任务状态
        RBucket<IterativeDTO> iterative = redisson.getBucket(
                StringConstant.Redis.SCHEDULING_ITERATIVE + task.getTaskId());
        if (!iterative.isExists()) {
            throw new BusinessException("排课任务迭代缓存数据不存在", ErrorCode.BODY_ERROR);
        }
        IterativeDTO iterativeDTO = iterative.get();
        return this.handleTheStatusScheduledTasks(task, iterativeDTO);
    }

    private @NotNull SchedulingTaskStatusDTO handleTheStatusScheduledTasks(
            SchedulingTaskDTO task,
            @NotNull IterativeDTO iterativeDTO) {
        SchedulingTaskStatusDTO taskStatus = new SchedulingTaskStatusDTO();
        //计算是否已经完成
        if (Objects.equals(iterativeDTO.getMaximumNumberOfIterations(), iterativeDTO.getNumber())) {
            taskStatus.setStatus("completed")
                    .setMessage("排课完成");
        } else {
            taskStatus.setStatus("processing")
                    .setMessage("正在执行遗传算法第" + iterativeDTO.getNumber() + "代");
        }
        //计算百分比
        log.debug("迭代次数: {}, 最大迭代次数: {}",
                iterativeDTO.getNumber(), iterativeDTO.getMaximumNumberOfIterations());
        double progress = ((double) iterativeDTO.getNumber() / iterativeDTO.getMaximumNumberOfIterations()) * 100;
        //获取排课结果
        RBucket<ScheduleResultDTO> bucket = redisson.getBucket(
                StringConstant.Redis.SCHEDULE_RESULT + task.getTaskId());
        if (bucket.isExists()) {
            progress = 100;
        }
        int remainingTime;
        SchedulingTaskStatusDTO.ConflictsCount conflict = new SchedulingTaskStatusDTO.ConflictsCount();
        //检查是否完成
        if (progress == 100) {
            remainingTime = 0;
            // 获取冲突数量
            ScheduleResultDTO scheduleResultDTO = bucket.get();
            conflict = this.getConflicts(scheduleResultDTO);
        } else {
            remainingTime = (int) (task.getEstimatedTime() - (task.getEstimatedTime() * (progress / 100)));
        }
        taskStatus
                .setProgress((int) progress)
                .setEstimatedTimeRemaining(remainingTime)
                .setTaskId(task.getTaskId())
                .setSemesterId(task.getSemesterUuid())
                .setDepartmentId(task.getDepartmentUuid())
                .setStartTime(task.getCreatedAt())
                .setConflictsCount(conflict);
        return taskStatus;
    }

    private SchedulingTaskStatusDTO.ConflictsCount getConflicts(@NotNull ScheduleResultDTO scheduleResultDTO) {
        // 初始化冲突计数器（使用链式调用）
        SchedulingTaskStatusDTO.ConflictsCount conflictsCount = new SchedulingTaskStatusDTO.ConflictsCount()
                .setTeacher(0)
                .setClassroom(0)
                .setClazz(0);
        // 遍历所有冲突并分类统计
        for (SchedulingConflictDTO conflictDTO : scheduleResultDTO.getConflicts()) {
            if (conflictDTO == null || conflictDTO.getConflictType() == null) {
                continue;
            }
            switch (conflictDTO.getConflictType()) {
                // 教师冲突
                case 1:
                    conflictsCount.setTeacher(conflictsCount.getTeacher() + 1);
                    break;
                // 教室冲突
                case 2:
                    conflictsCount.setClassroom(conflictsCount.getClassroom() + 1);
                    break;
                // 班级冲突
                case 3:
                    conflictsCount.setClazz(conflictsCount.getClazz() + 1);
                    break;
                // 其他冲突
                case 4:
                    log.debug("发现其他类型冲突: {}", conflictDTO);
                    break;
                default:
                    log.warn("未知冲突类型: {}", conflictDTO.getConflictType());
            }
        }
        return conflictsCount;
    }
}
