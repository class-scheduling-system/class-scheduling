package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.AcademicAffairsPermissionDAO;
import com.frontleaves.scheduling.daos.ClassAssignmentDAO;
import com.frontleaves.scheduling.daos.SchedulingConflictDAO;
import com.frontleaves.scheduling.models.dto.base.*;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.models.entity.base.AcademicAffairsPermissionDO;
import com.frontleaves.scheduling.models.entity.base.ClassAssignmentDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.models.vo.AdjustmentDetailsVO;
import com.frontleaves.scheduling.models.vo.AdjustmentsVO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.models.vo.ClassTimeVO;
import com.frontleaves.scheduling.services.*;
import com.frontleaves.scheduling.thread.ScheduleLessonsDataPreparationThread;
import com.frontleaves.scheduling.utils.CheckConflicts;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import enums.CourseEnuType;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
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
    private final ClassAssignmentService classAssignmentService;
    private final ClassroomService classroomService;
    private final TeacherService teacherService;
    private final SchedulingConflictDAO schedulingConflictDAO;
    private final SchedulingConflictService schedulingConflictService;
    private final ClassAssignmentDAO classAssignmentDAO;
    private final CourseLibraryService courseLibraryService;


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
        RList<String> taskList = redisson.getList(StringConstant.Redis.SCHEDULING_TASK_LIST + getUser.getUserUuid());
        if (!taskList.contains(taskId)) {
            taskList.add(taskId);
        }
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

    @Override
    public BackAdjustCourseScheduleDTO adjustCourseSchedule(
            String assignmentId,
            @NotNull AdjustmentsVO adjustmentsVO,
            HttpServletRequest request) {
        //获取排课分配
        ClassAssignmentDTO classAssignment = classAssignmentService.getById(assignmentId);
        ClassAssignmentDTO classAssignment1 =  BeanUtil.toBean(classAssignment,ClassAssignmentDTO.class);
        AdjustmentDetailsVO details = adjustmentsVO.getAdjustments();
        this.exchangeClassroom(classAssignment1, details);
        this.exchangeTeacher(classAssignment1, details);
        this.exchangeTimeSlot(classAssignment1, details);
        this.exchangeOtherDetails(classAssignment1, details);
        List<SchedulingConflictDTO> conflict =
                this.detectConflicts(classAssignment1, adjustmentsVO.getIgnoreConflicts());
        //更新排课安排
        classAssignmentDAO.updateClassAssignment(BeanUtil.toBean(classAssignment1, ClassAssignmentDO.class));
        //创建返回值
        return this.createdBackDate(classAssignment,
                classAssignment1, adjustmentsVO,conflict, request);
    }

    @Override
    public List<String> getSchedulingTasks(HttpServletRequest request) {
        UserDO getUser = userService.getUserByRequest(request);
        RList<String> taskList = redisson.getList(
                StringConstant.Redis.SCHEDULING_TASK_LIST + getUser.getUserUuid());
        if (taskList.isExists()){
            return taskList.readAll();
        }
        return List.of();
    }

    private BackAdjustCourseScheduleDTO createdBackDate(
            @NotNull ClassAssignmentDTO classAssignment,
            @NotNull ClassAssignmentDTO classAssignment1,
            @NotNull AdjustmentsVO adjustmentsVO,
            List<SchedulingConflictDTO> conflict,
            HttpServletRequest request) {
        //获取课程信息
        CourseLibraryDTO courseLibraryDTO = courseLibraryService.getCourseLibraryByUuid(classAssignment.getCourseUuid());
        //获取教室信息
        ClassroomInfoDTO classroom = classroomService.getClassroomByUuid(classAssignment.getClassroomUuid());
        //获取教室信息
        ClassroomInfoDTO classroom1 =  classroomService.getClassroomByUuid(classAssignment1.getClassroomUuid());
        //获取教师信息
        TeacherDTO teacher = teacherService.getTeacher(classAssignment.getTeacherUuid());
        TeacherDTO teacher1 = teacherService.getTeacher(classAssignment1.getTeacherUuid());
        // 将时间槽转换为classTime
        List<ClassTimeDTO> classTime = ProjectUtil.convertToClassTimeDTOList(JSONUtil.toList(
                classAssignment.getClassTime(), TimeSlotDTO.class));
        List<ClassTimeDTO> classTime1 = ProjectUtil.convertToClassTimeDTOList(JSONUtil.toList(
                classAssignment1.getClassTime(), TimeSlotDTO.class));
        //或许当前更改用户数据
        UserDO getUser = userService.getUserByRequest(request);
        //创建返回值
        if (classroom1 != null && classroom != null) {
                return new BackAdjustCourseScheduleDTO()
                        .setAssignmentId(classAssignment.getClassAssignmentUuid())
                        .setCourseCode(courseLibraryDTO.getId())
                        .setCourseName(courseLibraryDTO.getName())
                        .setBefore(new CourseDetailsDTO()
                                        .setClassroomId(classroom.getClassroom().getClassroomUuid())
                                        .setClassroomName(classroom.getClassroom().getName())
                                        .setClassTime(classTime)
                                        .setTeacherId(teacher.getTeacherUuid())
                                        .setTeacherName(teacher.getName())
                                        .setSchedulingPriority(classAssignment.getSchedulingPriority())
                                        .setConsecutiveSessions(classAssignment.getConsecutiveSessions())
                                )
                        .setAfter(new CourseDetailsDTO()
                                .setClassroomId(classroom1.getClassroom().getClassroomUuid())
                                .setClassroomName(classroom1.getClassroom().getName())
                                .setClassTime(classTime1)
                                .setTeacherId(teacher1.getTeacherUuid())
                                .setTeacherName(teacher1.getName())
                                .setSchedulingPriority(classAssignment1.getSchedulingPriority())
                                .setConsecutiveSessions(classAssignment1.getConsecutiveSessions()))
                        .setAdjustedAt(new Timestamp(System.currentTimeMillis()))
                        .setAdjustedBy(getUser.getUserUuid())
                        .setAdjustedByName(getUser.getName())
                        .setNewConflicts(conflict)
                        .setReason(adjustmentsVO.getReason());
            }
        throw new BusinessException("教室不存在", ErrorCode.BODY_ERROR);
    }

    private List<SchedulingConflictDTO> detectConflicts(ClassAssignmentDTO classAssignment1, Boolean ignoreConflicts) {
        schedulingConflictService.checkForConflictResolution(classAssignment1);
        List<SchedulingConflictDTO> conflicts = new ArrayList<>();
        if (Boolean.FALSE.equals(ignoreConflicts)){
            // 检查冲突
             conflicts = this.findConflicts(classAssignment1);
            //批量保存
            schedulingConflictDAO.batchSaveConflicts(conflicts, classAssignment1.getSemesterUuid());
        }
        return conflicts;
    }
    private @NotNull List<SchedulingConflictDTO> findConflicts(ClassAssignmentDTO classAssignment1) {
        // 获取有可能与之相关的所有课程安排
        List<ClassAssignmentDTO> allAssignments = classAssignmentService.getClassAssignmentListConflict(classAssignment1);
        //检查冲突
        return CheckConflicts.checkConflicts(allAssignments, classAssignment1);
    }
    private void exchangeOtherDetails(ClassAssignmentDTO classAssignment1, @NotNull AdjustmentDetailsVO details) {
        // 交换其他细节
        if (details.getConsecutiveSessions() != null ) {
            classAssignment1.setConsecutiveSessions(details.getConsecutiveSessions());
        }
        if (details.getSchedulingPriority() != null ) {
            classAssignment1.setSchedulingPriority(details.getSchedulingPriority());
        }
    }

    private void exchangeTimeSlot(ClassAssignmentDTO classAssignment1, @NotNull AdjustmentDetailsVO details) {
        // 创建一个列表来存储所有解析出来的目标时间槽 DTO
        List<TimeSlotDTO> targetTimeSlots = new ArrayList<>();
        if ( details.getClassTime() != null && !details.getClassTime().isEmpty()) {
            // 遍历 AdjustmentDetailsVO 中的每个 ClassTimeVO 对象
            for (ClassTimeVO classTime : details.getClassTime()) {
                Integer day = classTime.getDayOfWeek();
                Integer startPeriod = classTime.getPeriodStart();
                Integer endPeriod = classTime.getPeriodEnd();
                List<Integer> weeks = classTime.getWeekNumbers();
                // 校验当前 ClassTimeVO 条目是否包含所有必需的信息，并且节次是否有效
                if (day != null
                        && startPeriod != null
                        && endPeriod != null
                        && weeks != null
                        && !weeks.isEmpty()
                        && startPeriod <= endPeriod) {
                    // 外层循环：遍历所有指定的周
                    for (Integer week : weeks) {
                        // 内层循环：遍历从开始节次到结束节次的所有节次（包含）
                        for (int period = startPeriod; period <= endPeriod; period++) {
                            // 创建一个新的 TimeSlotDTO 实例
                            TimeSlotDTO timeSlot = new TimeSlotDTO()
                                    .setWeek(week)
                                    .setDay(day)
                                    .setPeriod(period);
                            // 将创建的 TimeSlotDTO 添加到结果列表中
                            targetTimeSlots.add(timeSlot);
                        }
                    }
                } else {
                    throw new BusinessException("调整时间信息不完整或无效", ErrorCode.BODY_ERROR);
                }
            }
            classAssignment1.setClassTime(JSONUtil.toJsonStr(targetTimeSlots));
        }
    }

    private void exchangeTeacher(ClassAssignmentDTO classAssignment, @NotNull AdjustmentDetailsVO details) {
        if (details.getTeacherId() != null
                && !details.getTeacherId().isEmpty()) {
            //获取教师
            TeacherDTO teacher = teacherService.getTeacher(details.getTeacherId());
            classAssignment.setTeacherUuid(teacher.getTeacherUuid());
        }
    }

    private void exchangeClassroom(ClassAssignmentDTO classAssignment,
                                   @NotNull AdjustmentDetailsVO details) {
        //检测是否为更换新教室
        if (details.getClassroomId() != null
                && !details.getClassroomId().isEmpty()) {
            //获取教室
            ClassroomInfoDTO classroomInfoDTO = classroomService.getClassroomByUuid(details.getClassroomId());
            if (classroomInfoDTO == null) {
                throw new BusinessException("教室不存在", ErrorCode.BODY_ERROR);
            }
            classAssignment
                    .setClassroomType(classroomInfoDTO.getType().getClassTypeUuid())
                    .setBuildingUuid(classAssignment.getBuildingUuid())
                    .setClassroomUuid(classAssignment.getClassroomUuid())
                    .setTeachingCampus(classroomInfoDTO.getCampus().getCampusUuid())
                    .setTeachingCampus(classroomInfoDTO.getCampus().getCampusUuid());
        }
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
