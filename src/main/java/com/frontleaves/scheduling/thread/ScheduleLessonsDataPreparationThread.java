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

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.logic.SchedulingLogic;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.services.*;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

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
    private final boolean running = true;
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
    private RedissonClient redisson;
    private AutomaticClassSchedulingVO automaticClassSchedulingVO;
    private HttpServletRequest request;


    @Override
    public void run() {
        log.info("线程启动，等待任务...");

        while (running) {
            lock.lock();
            try {

                // 根据请求获取用户信息
                UserDO userDO = userService.getUserByRequest(request);
                assert userDO != null;
                // 检查用户所属部门与所填写部门是否一致
                AcademicAffairsPermissionDTO academicAffairsPermissionDTO =
                        academicAffairsPermissionService.getAcademicAffairsPermission(userDO.getUserUuid());
                assert academicAffairsPermissionDTO != null;
                if (!academicAffairsPermissionDTO.getDepartment().equals(automaticClassSchedulingVO.getDepartmentId())) {
                    throw new BusinessException("用户所属部门与所填写部门不一致", ErrorCode.BODY_ERROR);
                }
                //检查学期是否存在并且是否启用
                SemesterDTO semesterDTO =
                        semesterService.getSemesterByUuidCheckEnabled(automaticClassSchedulingVO.getSemesterId());
                assert semesterDTO != null;
                //检查结束周是否超过学期周
                SchedulingLogic.checkEndWeekExceedSemesterWeeks(automaticClassSchedulingVO.getEndWeek(), semesterDTO);
                // 使用 Map 存储课程类型优先级，以 courseTypeUuid 为键
                Map<String, CourseTypePriorityDTO> courseTypePriorityMap = new HashMap<>();
                // 获取优先级并填充到 Map 中
                //检查优先级是否存在
                if (automaticClassSchedulingVO.getPrioritySettings().getCourseTypes().isEmpty()) {
                    //全部设为5
                    for (CourseTypeDTO courseTypeDTO : courseTypeService.listCourseType()) {
                        CourseTypePriorityDTO courseTypePriorityDTO = new CourseTypePriorityDTO();
                        courseTypePriorityDTO.setCourseTypeDTO(courseTypeDTO).setPriority((short) 5);
                        courseTypePriorityMap.put(courseTypeDTO.getCourseTypeUuid(), courseTypePriorityDTO);
                    }
                } else {
                    for (AutomaticClassSchedulingVO.PrioritySettings.CourseTypePriority courseTypePriority
                            : automaticClassSchedulingVO.getPrioritySettings().getCourseTypes()) {
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
                //获取课程库和学生班级
                List<CourseLibraryAndTeacherCourseQualificationListDTO> libraryAndClassDTOList = courseLibraryService.getCourseListAndClassDTO(
                        automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds()
                );
                //获取老师所有数据
                List<CourseLibraryAndTeacherCourseQualificationListDTO> courseQualificationList = teacherCourseQualificationService
                        .getCourseLibraryAndTeacherCourseQualificationList(
                                libraryAndClassDTOList, automaticClassSchedulingVO.getConstraints().getTeacherPreference()
                        );
                assert courseQualificationList != null;
                for (CourseLibraryAndTeacherCourseQualificationListDTO dto : courseQualificationList) {
                    //设置优先级
                    CourseLibraryDTO courseLibraryDTO = dto.getCourse();
                    assert courseLibraryDTO != null;
                    // 获取课程类型 UUID
                    String courseTypeUuid = courseLibraryDTO.getType();
                    assert courseTypeUuid != null;
                    // 在 Map 中查找对应的优先级信息
                    CourseTypePriorityDTO courseTypePriorityDTO = courseTypePriorityMap.get(courseTypeUuid);
                    if (courseTypePriorityDTO == null) {
                        dto.setCourseTypes((short) 5);
                    } else {
                        // 匹配成功，设置优先级
                        dto.setCourseTypes(courseTypePriorityDTO.getPriority());
                    }
                }
                //获取教室数据
                List<ClassroomAndTypeDTO> classroomAndTypeDTOList = new ArrayList<>();
                for (String buildingUuid : automaticClassSchedulingVO.getScopeSettings().getAllowedBuildingIds()) {
                    classroomAndTypeDTOList.addAll(classroomService.getClassroomAndTypeByUuidWihError(buildingUuid));
                }
                //获取部门DTO
                DepartmentDTO departmentDTO = departmentService.
                        getDepartmentByUuid(automaticClassSchedulingVO.getDepartmentId());
                if (departmentDTO == null) {
                    throw new BusinessException("部门不存在", ErrorCode.BODY_ERROR);
                }
                //创建返回结果
                AutomaticClassSchedulingBaseDTO automaticClassSchedulingBaseDTO = new AutomaticClassSchedulingBaseDTO();
                //设置学期、部门、策略、结束周、课程和教师列表、教室和类型
                automaticClassSchedulingBaseDTO.setSemester(semesterDTO)
                        .setDepartment(departmentDTO)
                        .setStrategy(automaticClassSchedulingVO.getStrategy())
                        .setEndWeek(automaticClassSchedulingVO.getEndWeek())
                        .setCourseAndTeacherList(courseQualificationList)
                        .setClassroomAndType(classroomAndTypeDTOList);
                //设置约束
                AutomaticClassSchedulingBaseDTO.Constraints constraints =
                        new AutomaticClassSchedulingBaseDTO.Constraints();
                constraints.setTeacherPreference(automaticClassSchedulingVO.getConstraints().getTeacherPreference())
                        .setRoomOptimization(automaticClassSchedulingVO.getConstraints().getRoomOptimization())
                        .setStudentConflictAvoidance(automaticClassSchedulingVO.getConstraints().getStudentConflictAvoidance())
                        .setConsecutiveCoursesPreferred(automaticClassSchedulingVO.getConstraints().getConsecutiveCoursesPreferred())
                        .setSpecializationRoomMatching(automaticClassSchedulingVO.getConstraints().getSpecializationRoomMatching());
                automaticClassSchedulingBaseDTO.setConstraints(constraints);
                //设置算法参数
                AutomaticClassSchedulingBaseDTO.AlgorithmParams algorithmParams =
                        new AutomaticClassSchedulingBaseDTO.AlgorithmParams();
                algorithmParams.setPopulationSize(automaticClassSchedulingVO.getAlgorithmParams().getPopulationSize())
                        .setMaxIterations(automaticClassSchedulingVO.getAlgorithmParams().getMaxIterations())
                        .setCrossoverRate(automaticClassSchedulingVO.getAlgorithmParams().getCrossoverRate())
                        .setMutationRate(automaticClassSchedulingVO.getAlgorithmParams().getMutationRate());
                automaticClassSchedulingBaseDTO.setAlgorithmParams(algorithmParams);
                //设置时间偏好
                AutomaticClassSchedulingBaseDTO.TimePreferences timePreferences =
                        new AutomaticClassSchedulingBaseDTO.TimePreferences();
                for (AutomaticClassSchedulingVO.TimePreferences.PreferredTimeSlot preferredTimeSlot
                        : automaticClassSchedulingVO.getTimePreferences().getPreferredTimeSlots()) {
                    AutomaticClassSchedulingBaseDTO.TimePreferences.PreferredTimeSlot preferredTimeSlotDTO =
                            new AutomaticClassSchedulingBaseDTO.TimePreferences.PreferredTimeSlot();
                    preferredTimeSlotDTO.setDay(preferredTimeSlot.getDay())
                            .setPeriodStart(preferredTimeSlot.getPeriodStart())
                            .setPeriodEnd(preferredTimeSlot.getPeriodEnd());
                    timePreferences.setPreferredTimeSlots(new ArrayList<>());
                    timePreferences.getPreferredTimeSlots().add(preferredTimeSlotDTO);
                }
                timePreferences.setAvoidEveningCourses(automaticClassSchedulingVO.getTimePreferences().getAvoidEveningCourses())
                        .setBalanceWeekdayCourses(automaticClassSchedulingVO.getTimePreferences().getBalanceWeekdayCourses());
                automaticClassSchedulingBaseDTO.setTimePreferences(timePreferences);

                RBucket<AutomaticClassSchedulingBaseDTO> cacheBaseData = redisson.getBucket(StringConstant.Redis.SCHEDULE_LESSONS + userDO.getUserUuid());
                cacheBaseData.set(automaticClassSchedulingBaseDTO);

                AutomaticClassSchedulingThread automaticThread = new AutomaticClassSchedulingThread();
                automaticThread.startUp(userDO);

            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
        }

        log.info("线程结束运行");
    }


    /**
     * 执行具体的任务
     */
    public void startUp(
            @NotNull AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request
    ) {
        this.automaticClassSchedulingVO = automaticClassSchedulingVO;
        this.request = request;
        try {
            condition.signal();
            log.info("已通知线程执行任务");
        } finally {
            lock.unlock();
        }
    }
}
