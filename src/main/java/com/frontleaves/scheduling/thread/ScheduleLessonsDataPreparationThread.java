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
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

package com.frontleaves.scheduling.thread;

import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.logic.SchedulingLogic;
import com.frontleaves.scheduling.models.dto.base.*;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseTypePriorityDTO;
import com.frontleaves.scheduling.models.dto.scheduling.*;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.models.vo.SpecificCourseIdVO;
import com.frontleaves.scheduling.services.*;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import enums.CourseEnuType;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 线程类，用于准备自动排课的数据
 * <p>
 * 该类继承自 {@link Thread} 类，并实现了数据准备的逻辑。
 * </p>
 *
 * @author FLASHLACK
 */
@Slf4j
public class ScheduleLessonsDataPreparationThread extends Thread {

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    @Resource
    AutomaticClassSchedulingThread automaticThread;
    @Resource
    private UserService userService;
    @Resource
    private AcademicAffairsPermissionService academicAffairsPermissionService;
    @Resource
    private SemesterService semesterService;
    @Resource
    private CourseLibraryService courseLibraryService;
    @Resource
    private TeacherCourseQualificationService teacherCourseQualificationService;
    @Resource
    private CourseTypeService courseTypeService;
    @Resource
    private ClassroomService classroomService;
    @Resource
    private DepartmentService departmentService;
    @Resource
    private ClassAssignmentService classAssignmentService;
    @Resource
    private TeacherService teacherService;
    @Resource
    private AdministrativeClassService administrativeClassService;
    @Resource
    private CreditHourTypeService creditHourTypeService;
    @Resource
    private TeachingClassService teachingClassService;
    @Resource
    private RedissonClient redisson;
    private HttpServletRequest request;
    private AutomaticClassSchedulingVO classSchedulingVO;
    private boolean hasTask = false;
    private String taskId;
    public ScheduleLessonsDataPreparationThread(String name) {
        super(name);
    }

    @Override
    public void run() {
        log.info(LogConstant.THREAD + "线程启动，等待任务...");

        while (true) {
            lock.lock();
            try {
                while (!hasTask) {
                    log.info(LogConstant.THREAD + "线程进入等待状态");
                    condition.await();
                }
                log.debug(LogConstant.THREAD + "线程已获取锁，准备执行任务");
                log.debug(LogConstant.THREAD + "获取到的自动排课请求数据: {}", classSchedulingVO);
                log.debug(LogConstant.THREAD + "获取用户信息: {}", request);
                // 根据请求获取用户信息
                UserDO userDO = userService.getUserByRequest(request);
                log.debug(LogConstant.THREAD + "检查用户所属部门与所填写部门是否一致");
                this.validateUserDepartmentPermission(userDO.getUserUuid(), classSchedulingVO.getDepartmentUuid());
                log.debug(LogConstant.THREAD + "检查学期是否存在并且是否启用");
                SemesterDTO semesterDTO =
                        semesterService.getSemesterByUuidCheckEnabled(classSchedulingVO.getSemesterUuid());
                assert semesterDTO != null;
                log.debug(LogConstant.THREAD + "检查结束周是否超过学期周");
                this.validateCourseWeeks(classSchedulingVO.getScopeSettings().getSpecificCourseIds(), semesterDTO);
                log.debug(LogConstant.THREAD + "获取课程库和学生班级");
                List<CourseLibraryAndTeacherCourseQualificationListDTO> libraryAndClassDTOList =
                        courseLibraryService.getCourseListAndClassDTO(
                                classSchedulingVO.getScopeSettings().getSpecificCourseIds(),
                                classSchedulingVO.getDepartmentUuid()
                        );
                log.debug("检查课程学分是否能够修满");
                this.fillSpecificCourseInfo(classSchedulingVO.getScopeSettings().getSpecificCourseIds(), libraryAndClassDTOList);
                log.debug(LogConstant.THREAD + "获取老师所有数据");
                List<CourseLibraryAndTeacherCourseQualificationListDTO> courseQualificationList = teacherCourseQualificationService
                        .getCourseLibraryAndTeacherCourseQualificationList(
                                libraryAndClassDTOList, classSchedulingVO.getConstraints().getTeacherPreference()
                        );
                assert courseQualificationList != null;
                log.debug(LogConstant.THREAD + "把混排课程的分为数据库内定义课程的类");
                List<CourseLibraryAndTeacherCourseQualificationListDTO> updatedList =
                        this.expandMixedCourses(courseQualificationList);
                log.debug("更新课程表");
                courseQualificationList.clear();
                courseQualificationList.addAll(updatedList);
                log.debug(LogConstant.THREAD + "使用 Map 存储课程类型优先级，以 courseTypeUuid 为键");
                Map<String, CourseTypePriorityDTO> courseTypePriorityMap = this.setupCourseTypePriority(classSchedulingVO);
                log.debug("设置课程优先级");
                this.applyCourseTypePriority(courseQualificationList, courseTypePriorityMap);
                log.debug(LogConstant.THREAD + "获取教室数据");
                List<ClassroomInfoDTO> allClassroomInfo =
                        this.getAllClassroomInfo(classSchedulingVO.getScopeSettings().getAllowedBuildingIds());
                log.debug(LogConstant.THREAD + "获取部门DTO");
                DepartmentDTO departmentDTO = departmentService.
                        getDepartmentByUuidWithThrows(classSchedulingVO.getDepartmentUuid());
                assert departmentDTO != null;
                //创建返回结果
                log.debug(LogConstant.THREAD + "创建返回结果");
                AutomaticClassSchedulingBaseDTO automaticClassSchedulingBaseDTO = new AutomaticClassSchedulingBaseDTO();
                //设置学期、部门、策略、结束周、课程和教师列表、教室和类型
                automaticClassSchedulingBaseDTO.setSemester(semesterDTO)
                        .setDepartment(departmentDTO)
                        .setStrategy(classSchedulingVO.getStrategy())
                        .setCourseList(courseQualificationList)
                        .setClassroomList(allClassroomInfo);
                //设置约束
                AutomaticClassSchedulingBaseDTO.Constraints constraints =
                        new AutomaticClassSchedulingBaseDTO.Constraints();
                constraints.setTeacherPreference(classSchedulingVO.getConstraints().getTeacherPreference())
                        .setRoomOptimization(classSchedulingVO.getConstraints().getRoomOptimization())
                        .setStudentConflictAvoidance(classSchedulingVO.getConstraints().getStudentConflictAvoidance())
                        .setConsecutiveCoursesPreferred(classSchedulingVO.getConstraints().getConsecutiveCoursesPreferred())
                        .setSpecializationRoomMatching(classSchedulingVO.getConstraints().getSpecializationRoomMatching());
                automaticClassSchedulingBaseDTO.setConstraints(constraints);
                //根据策略生成算法参数
                AutomaticClassSchedulingBaseDTO.AlgorithmParams algorithmParams = switch (classSchedulingVO.getStrategy()) {
                    case OPTIMAL -> new AutomaticClassSchedulingBaseDTO.AlgorithmParams()
                            .setPopulationSize(200)
                            .setMaxIterations(1000)
                            .setCrossoverRate(0.8)
                            .setMutationRate(0.1);
                    case BALANCED -> new AutomaticClassSchedulingBaseDTO.AlgorithmParams()
                            .setPopulationSize(100)
                            .setMaxIterations(500)
                            .setCrossoverRate(0.7)
                            .setMutationRate(0.2);
                    case QUICK -> new AutomaticClassSchedulingBaseDTO.AlgorithmParams()
                            .setPopulationSize(50)
                            .setMaxIterations(200)
                            .setCrossoverRate(0.6)
                            .setMutationRate(0.3);
                };
                automaticClassSchedulingBaseDTO.setAlgorithmParams(algorithmParams);
                //设置时间偏好
                AutomaticClassSchedulingBaseDTO.TimePreferences timePreferences =
                        new AutomaticClassSchedulingBaseDTO.TimePreferences();
                for (AutomaticClassSchedulingVO.TimePreferences.PreferredTimeSlot preferredTimeSlot
                        : classSchedulingVO.getTimePreferences().getPreferredTimeSlots()) {
                    AutomaticClassSchedulingBaseDTO.TimePreferences.PreferredTimeSlot preferredTimeSlotDTO =
                            new AutomaticClassSchedulingBaseDTO.TimePreferences.PreferredTimeSlot();
                    preferredTimeSlotDTO.setDay(preferredTimeSlot.getDay())
                            .setPeriodStart(preferredTimeSlot.getPeriodStart())
                            .setPeriodEnd(preferredTimeSlot.getPeriodEnd());
                    timePreferences.setPreferredTimeSlots(new ArrayList<>());
                    timePreferences.getPreferredTimeSlots().add(preferredTimeSlotDTO);
                }
                timePreferences.setEveningCourses(classSchedulingVO.getTimePreferences().getAvoidEveningCourses())
                        .setBalanceWeekdayCourses(classSchedulingVO.getTimePreferences().getBalanceWeekdayCourses());
                automaticClassSchedulingBaseDTO.setTimePreferences(timePreferences);
                log.debug("获取排课表内存在可能冲突的课程排课数据");
                List<ClassAssignmentDTO> classAssignmentDTOList =
                        classAssignmentService.getClassAssignmentListByLimit(automaticClassSchedulingBaseDTO);
                //将数据库内排课数据转换成baseDTO
                log.debug("准备数据库内排课数据");
                List<CourseScheduleDTO> courseScheduleDTOList = this.buildCourseScheduleList(classAssignmentDTOList);
                automaticClassSchedulingBaseDTO.setDataCourseScheduleList(courseScheduleDTOList);
                log.debug("准备的基础上数据为: {}", JSONUtil.toJsonStr(automaticClassSchedulingBaseDTO));
                RBucket<AutomaticClassSchedulingBaseDTO> cacheBaseData = redisson.getBucket(StringConstant.Redis.SCHEDULE_LESSONS + userDO.getUserUuid());
                cacheBaseData.set(automaticClassSchedulingBaseDTO);
                automaticThread.startUp(userDO, taskId);
                hasTask = false;
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error("{}获取报错信息：{}", LogConstant.THREAD, e.getMessage(), e);
                hasTask = false;
                lock.unlock();
            } finally {
                lock.unlock();
            }
        }
    }




    /**
     * 执行具体的任务
     */
    public void startUp(
            @NotNull AutomaticClassSchedulingVO automaticClassSchedulingVO,
            String taskId,
            HttpServletRequest request
    ) {
        lock.lock();
        try {
            this.classSchedulingVO = automaticClassSchedulingVO;
            this.request = request;
            this.taskId = taskId;
            hasTask = true;
            condition.signal();
            log.info(LogConstant.THREAD + "已通知线程执行任务");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 验证用户部门权限
     *
     * @param userUuid       用户UUID
     * @param departmentUuid 部门UUID
     * @throws BusinessException 如果用户部门权限不匹配
     */
    private void validateUserDepartmentPermission(String userUuid, String departmentUuid) {
        AcademicAffairsPermissionDTO academicAffairsPermissionDTO =
                academicAffairsPermissionService.getAcademicPermissionByUserUuid(userUuid);
        assert academicAffairsPermissionDTO != null;
        if (!academicAffairsPermissionDTO.getDepartment().equals(departmentUuid)) {
            throw new BusinessException("用户所属部门与所填写部门不一致", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 检查课程的开始周和结束周是否在学期范围内
     *
     * @param specificCourseIds 特定课程ID列表
     * @param semesterDTO       学期信息
     */
    private void validateCourseWeeks(@NotNull List<SpecificCourseIdVO> specificCourseIds, SemesterDTO semesterDTO) {
        for (SpecificCourseIdVO specificCourseIdVO : specificCourseIds) {
            SchedulingLogic.checkEndWeekExceedSemesterWeeks(specificCourseIdVO.getStartWeek(), semesterDTO);
            SchedulingLogic.checkEndWeekExceedSemesterWeeks(specificCourseIdVO.getEndWeek(), semesterDTO);
        }
    }

    /**
     * 设置课程类型优先级
     *
     * @param classSchedulingVO 自动排课VO
     * @return 课程类型优先级映射
     */
    private @NotNull Map<String, CourseTypePriorityDTO> setupCourseTypePriority(@NotNull AutomaticClassSchedulingVO classSchedulingVO) {
        Map<String, CourseTypePriorityDTO> courseTypePriorityMap = new HashMap<>();
        //检查优先级是否存在
        if (classSchedulingVO.getPrioritySettings().getCourseTypes().isEmpty()) {
            //全部设为5
            for (CourseTypeDTO courseTypeDTO : courseTypeService.listCourseType()) {
                CourseTypePriorityDTO courseTypePriorityDTO = new CourseTypePriorityDTO();
                courseTypePriorityDTO.setCourseTypeDTO(courseTypeDTO).setPriority(5);
                courseTypePriorityMap.put(courseTypeDTO.getCourseTypeUuid(), courseTypePriorityDTO);
            }
        } else {
            for (AutomaticClassSchedulingVO.PrioritySettings.CourseTypePriority courseTypePriority
                    : classSchedulingVO.getPrioritySettings().getCourseTypes()) {
                // 根据 typeId 获取 CourseTypeDTO
                CourseTypeDTO courseTypeDTO = courseTypeService
                        .getCourseTypeByUuidWithError(courseTypePriority.getTypeId());
                assert courseTypeDTO != null;
                // 创建 CourseTypePriorityDTO 并设置优先级
                CourseTypePriorityDTO courseTypePriorityDTO = new CourseTypePriorityDTO();
                courseTypePriorityDTO.setCourseTypeDTO(courseTypeDTO)
                        .setPriority(courseTypePriority.getPriority());
                // 将其添加到 Map 中，以 courseTypeUuid 为键
                courseTypePriorityMap.put(courseTypeDTO.getCourseTypeUuid(), courseTypePriorityDTO);
            }
        }
        return courseTypePriorityMap;
    }

    /**
     * 处理特定课程的信息
     *
     * @param specificCourseIdVO     包含特定课程ID的信息
     * @param libraryAndClassDTOList 课程库和教师课程资格列表，不能为空
     *                               此方法首先从课程库中查找与特定课程ID匹配的课程如果找不到匹配的课程，
     *                               则抛出一个表示业务异常的错误如果找到了匹配的课程，则更新该课程的信息，
     *                               包括课程类型、每周课时、是否单周、开始周、结束周和预期总学时
     */
    private void processSpecificCourse(SpecificCourseIdVO specificCourseIdVO,
                                       @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> libraryAndClassDTOList) {
        // 从列表中查找与特定课程ID匹配的课程，如果找不到则抛出异常
        CourseLibraryAndTeacherCourseQualificationListDTO qualificationList =
                libraryAndClassDTOList.stream()
                        .filter(dto -> dto.getCourse().getCourseLibraryUuid()
                                .equals(specificCourseIdVO.getCourseId()))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("课程不存在", ErrorCode.BODY_ERROR));
        // 获取特定课程的学时
        BigDecimal selectedCredit = this.getBigDecimal(specificCourseIdVO, qualificationList);
        qualificationList.setCourseEnuType(specificCourseIdVO.getCourseEnuType())
                .setWeeklyHours(specificCourseIdVO.getWeeklyHours())
                .setIsOddWeek(specificCourseIdVO.getIsOddWeek())
                .setStartWeek(specificCourseIdVO.getStartWeek())
                .setEndWeek(specificCourseIdVO.getEndWeek())
                .setExpectedTotalHours(selectedCredit);
    }

    /**
     * 根据特定课程信息和课程资格列表获取课程学时
     * 此方法用于计算和验证课程的计划学时是否满足规定的学时要求
     *
     * @param specificCourseIdVO 特定课程ID信息，包含课程类型、起始周、结束周和每周学时等数据
     * @param qualificationList  课程资格列表，包含课程库信息和教师课程资格信息
     * @return 返回根据课程类型选择的规定学时
     * @throws BusinessException 如果课程的计划学时小于规定学时，则抛出业务异常
     */
    private BigDecimal getBigDecimal(@NotNull SpecificCourseIdVO specificCourseIdVO, @NotNull CourseLibraryAndTeacherCourseQualificationListDTO qualificationList) {
        // 获取课程库信息
        CourseLibraryDTO courseLibraryDTO = qualificationList.getCourse();
        // 根据课程类型选择对应的学时
        BigDecimal selectedCredit = switch (specificCourseIdVO.getCourseEnuType()) {
            case THEORY -> courseLibraryDTO.getTheoryHours();
            case EXPERIMENT -> courseLibraryDTO.getExperimentHours();
            case PRACTICE -> courseLibraryDTO.getPracticeHours();
            case COMPUTER -> courseLibraryDTO.getComputerHours();
            case MIXED -> courseLibraryDTO.getTotalHours();
            case OTHER -> courseLibraryDTO.getOtherHours();
        };
        // 计算课程的持续周数
        int durationWeeks = specificCourseIdVO.getEndWeek() - specificCourseIdVO.getStartWeek() + 1;
        // 计算课程的总课时（课程周数 * 每周课时）
        BigDecimal expectedTotalHours = BigDecimal.valueOf(durationWeeks)
                .multiply(BigDecimal.valueOf(specificCourseIdVO.getWeeklyHours()));
        // 如果课程的计划学时小于规定学时，抛出异常
        if (expectedTotalHours.compareTo(selectedCredit) < 0) {
            throw new BusinessException("课程 [" + courseLibraryDTO.getName() + "] 的课程类型 [" + specificCourseIdVO.getCourseEnuType().getChineseName() +
                    "] 课时无法完成: 计划课时 " + expectedTotalHours + " < 规定课时 " + selectedCredit,
                    ErrorCode.BODY_ERROR);
        }
        // 返回根据课程类型选择的规定学时
        return selectedCredit;
    }

    /**
     * 填充特定课程的信息
     * 该方法遍历特定课程ID列表，并为每个课程ID调用processSpecificCourse方法来处理课程信息
     *
     * @param specificCourseIds      包含特定课程ID的列表，用于标识需要处理的课程这些课程ID不能为空
     * @param libraryAndClassDTOList 包含课程库和教师课程资格列表的DTO列表，用于与特定课程ID进行匹配和处理
     */
    private void fillSpecificCourseInfo(@NotNull List<SpecificCourseIdVO> specificCourseIds,
                                        List<CourseLibraryAndTeacherCourseQualificationListDTO> libraryAndClassDTOList) {
        for (SpecificCourseIdVO specificCourseIdVO : specificCourseIds) {
            this.processSpecificCourse(specificCourseIdVO, libraryAndClassDTOList);
        }
    }

    /**
     * 扩展混合课程以生成一个新的列表
     * 此方法遍历给定的课程资格列表，对于每个被认为是混合课程的项目，将其拆分并添加到新的列表中
     * 非混合课程直接添加到列表中该方法确保返回的列表中没有混合课程
     *
     * @param courseQualificationList 一个包含课程资格的列表，不能为空
     * @return 一个扩展后的列表，其中包含拆分后的混合课程和其他未更改的课程资格
     */
    private @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> expandMixedCourses(
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courseQualificationList) {
        // 创建一个新的列表来存储更新后的课程资格
        List<CourseLibraryAndTeacherCourseQualificationListDTO> updatedList = new ArrayList<>();
        for (CourseLibraryAndTeacherCourseQualificationListDTO dto : courseQualificationList) {
            if (this.isMixedCourseValid(dto)) {
                updatedList.addAll(this.splitMixedCourse(dto));
            } else {
                updatedList.add(dto);
            }
        }
        return updatedList;
    }

    /**
     * 检查混合课程是否有效
     * 有效性条件包括：
     * 1. 课程类型为混合课程（CourseEnuType.MIXED）
     * 2. 课程对象不为空
     * 3. 课程的总学时字段存在且大于零
     *
     * @param dto 包含课程库和教师课程资格的DTO对象，不能为空
     * @return 如果课程满足上述条件，则返回true，否则返回false
     */
    private boolean isMixedCourseValid(@NotNull CourseLibraryAndTeacherCourseQualificationListDTO dto) {
        return dto.getCourseEnuType() == CourseEnuType.MIXED
                && dto.getCourse() != null
                && dto.getCourse().getTotalHours() != null
                && dto.getCourse().getTotalHours().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 将混合类型的课程拆分为多个单一类型的课程
     * 此方法接收一个包含课程信息及其对应课时的DTO对象，根据课程类型（如理论、实验等）
     * 拆分成多个仅包含一种课程类型信息的DTO对象，便于后续处理和展示
     *
     * @param dto 课程库和教师课程资格列表DTO对象，包含课程信息及其课时
     * @return 返回一个List，其中包含按课程类型拆分后的课程信息DTO对象
     */
    private List<CourseLibraryAndTeacherCourseQualificationListDTO> splitMixedCourse(
            @NotNull CourseLibraryAndTeacherCourseQualificationListDTO dto) {
        CourseLibraryDTO course = dto.getCourse();
        Map<CourseEnuType, BigDecimal> hourMap = Map.of(
                CourseEnuType.THEORY, course.getTheoryHours(),
                CourseEnuType.EXPERIMENT, course.getExperimentHours(),
                CourseEnuType.PRACTICE, course.getPracticeHours(),
                CourseEnuType.COMPUTER, course.getComputerHours(),
                CourseEnuType.OTHER, course.getOtherHours()
        );
        return hourMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().compareTo(BigDecimal.ZERO) > 0)
                .map(entry -> SchedulingLogic.copyAndSet(dto, entry.getKey(), entry.getValue()))
                .toList();
    }

    /**
     * 根据课程类型优先级配置更新课程列表中的每个课程优先级
     * 如果课程类型没有对应的优先级配置，则将优先级设置为默认值
     *
     * @param courseList            课程列表，包含课程信息和教师课程资格信息
     * @param courseTypePriorityMap 课程类型优先级配置，键为课程类型UUID，值为课程类型优先级DTO
     */
    private void applyCourseTypePriority(
            @NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courseList,
            @NotNull Map<String, CourseTypePriorityDTO> courseTypePriorityMap
    ) {
        for (CourseLibraryAndTeacherCourseQualificationListDTO dto : courseList) {
            String typeUuid = dto.getCourse().getType();
            CourseTypePriorityDTO priorityDTO = courseTypePriorityMap.get(typeUuid);
            if (priorityDTO != null) {
                // 如果找到了对应的优先级配置，设置课程优先级为配置的值
                dto.setPriority(priorityDTO.getPriority());
            } else {
                // 没配置的课程类型统一设置为默认优先级 5（可根据业务调整）
                dto.setPriority(5);
            }
        }
    }

    /**
     * 准备课程时间表项
     * 此方法根据班级分配信息来准备课程时间表项，包括获取课程信息、教师信息、教室信息和行政班信息等
     *
     * @param classAssignment 班级分配信息，包含课程UUID、教师UUID、教室UUID等
     * @return 返回一个课程时间表项DTO，包含所有准备好的信息
     */
    private @NotNull CourseScheduleItemDTO prepareTheCourseSchedule(@NotNull ClassAssignmentDTO classAssignment) {
        CourseLibraryDTO courseLibraryDTO = courseLibraryService.getCourseLibraryByUuid(classAssignment.getCourseUuid());
        TeacherDTO teacherDTO = teacherService.getTeacher(classAssignment.getTeacherUuid());
        TeacherCoursePreferencesDTO teacherCoursePreferencesDTO = new TeacherCoursePreferencesDTO();
        teacherCoursePreferencesDTO.setTeacher(teacherDTO);
        ClassroomInfoDTO classroomDTO = classroomService.getClassroomByUuid(classAssignment.getClassroomUuid());
        //获取行政班级信息
        TeachingClassDTO teachingClassDTO =
                teachingClassService.getTeachingClassByUuid(classAssignment.getTeachingClassUuid());
        List<String> classUuid = JSONUtil.toList(teachingClassDTO.getAdministrativeClasses(), String.class);
        List<AdministrativeClassDTO> administrativeClassDTOList = new ArrayList<>();
        for (String uuid : classUuid) {
            administrativeClassDTOList.add(administrativeClassService.getClassByUuid(uuid));
        }
        CreditHourTypeEnuDTO creditHourTypeEnuDTO =
                creditHourTypeService.getCreditHourTypeByUuid(classAssignment.getCreditHourType());
        return new CourseScheduleItemDTO(UuidUtil.generateStringUuid(),
                courseLibraryDTO, teacherCoursePreferencesDTO, classroomDTO, administrativeClassDTOList, creditHourTypeEnuDTO,
                classAssignment.getSchedulingPriority(),0,teachingClassDTO);
    }

    /**
     * 获取所有允许访问的教室信息
     * 此方法通过遍历允许的建筑ID列表，获取每个建筑ID对应的教室信息，并将这些信息收集到一个列表中
     * 它确保了只有指定建筑ID的教室信息会被获取，从而保证了数据的权限控制
     *
     * @param allowedBuildingIds 允许访问的建筑ID列表，不能为空
     * @return 包含教室信息的列表，不能为空
     */
    private @NotNull List<ClassroomInfoDTO> getAllClassroomInfo(@NotNull List<String> allowedBuildingIds) {
        // 初始化一个列表，用于存储所有允许访问的教室信息
        List<ClassroomInfoDTO> classroomAndTypeDTOList = new ArrayList<>();
        // 遍历每个允许访问的建筑ID
        for (String buildingId : allowedBuildingIds) {
            //获取教学楼内教室的uuid链表
            List<ClassroomDTO> classroomUuids = classroomService.getClassroomUuidsByBuildingId(buildingId);
            for (ClassroomDTO classroomDTO : classroomUuids) {
                // 通过建筑ID获取对应的教室信息
                ClassroomInfoDTO classroomInfoDTO = classroomService.getClassroomByUuid(classroomDTO.getClassroomUuid());
                // 确保获取到的教室信息不为空
                assert classroomInfoDTO != null;
                // 将获取到的教室信息添加到列表中
                classroomAndTypeDTOList.add(classroomInfoDTO);
            }
        }
        // 返回收集到的教室信息列表
        return classroomAndTypeDTOList;
    }

    /**
     * 构建课程时间表列表
     * 该方法负责将一组班级分配信息转换为课程时间表对象列表每个班级分配信息
     * 将被转换为一个课程时间表对象，其中包含根据班级分配信息中包含的时间
     * 信息构建的时间槽和课程安排项的映射
     * @param classAssignmentDTOList 不为空的班级分配DTO列表，包含班级分配信息
     * @return 返回一个填充了课程安排信息的课程时间表DTO列表
     */
    private @NotNull List<CourseScheduleDTO> buildCourseScheduleList(@NotNull List<ClassAssignmentDTO> classAssignmentDTOList) {
        List<CourseScheduleDTO> courseScheduleDTOList = new ArrayList<>();
        for (ClassAssignmentDTO classAssignment : classAssignmentDTOList) {
            CourseScheduleDTO courseScheduleDTO = new CourseScheduleDTO();
            CourseScheduleItemDTO courseScheduleItemDTO = this.prepareTheCourseSchedule(classAssignment);
            Map<List<TimeSlotDTO>, CourseScheduleItemDTO> assignments = new HashMap<>();
            String classTime = classAssignment.getClassTime();
            if (classTime != null && !classTime.isEmpty()) {
                try {
                    // 使用JSONUtil解析JSON字符串
                    List<TimeSlotDTO> timeSlots = JSONUtil.toList(classTime, TimeSlotDTO.class);
                    // 设置到map的key
                    assignments.put(timeSlots, courseScheduleItemDTO);
                } catch (Exception e) {
                    log.error("解析classTime失败: {}", classTime, e);
                }
            }
            courseScheduleDTO.setAssignments(assignments);
            courseScheduleDTOList.add(courseScheduleDTO);
        }
        return courseScheduleDTOList;
    }
}
