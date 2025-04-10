package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.base.SchedulingTaskDTO;
import com.frontleaves.scheduling.models.dto.scheduling.SchedulingTaskStatusDTO;
import com.frontleaves.scheduling.models.entity.base.*;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.models.vo.SpecificCourseIdVO;
import com.frontleaves.scheduling.services.SchedulingService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 排课控制器
 *
 * @author FLASHLACK
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scheduling")
public class SchedulingController {
    @Resource
    private SchedulingService schedulingService;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private UserDAO userDAO;
    @Resource
    private CourseLibraryDAO courseLibraryDAO;
    @Resource
    private AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    @Resource
    private TeacherCourseQualificationDAO teacherCourseQualificationDAO;


    @GetMapping("/base-data")
    @RequestRole("教务")
    public ResponseEntity<BaseResponse<List<CourseLibraryDO>>> getBaseDate(){
        log.debug("SchedulingLogic单元测试初始化");
        // 创建测试用户
        UserDO setUpUser = userDAO.lambdaQuery().eq(UserDO::getRoleUuid, SystemConstant.getRoleAcademic()).one();
        // 创建教务权限
        AcademicAffairsPermissionDO setUpPermission = academicAffairsPermissionDAO.lambdaQuery()
                .eq(AcademicAffairsPermissionDO::getAuthorizedUser, setUpUser.getUserUuid()).one();
        //创建测试部门
        DepartmentDO setUpDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, setUpPermission.getDepartment()).one();
        //获取有老师分配的课程库
        List<TeacherCourseQualificationDO> teacherCourseQualificationDOList = teacherCourseQualificationDAO
                .lambdaQuery().list();
        List<CourseLibraryDO> setUpCourseLibraries = new ArrayList<>();
        for (TeacherCourseQualificationDO teacherCourseQualificationDO : teacherCourseQualificationDOList) {
            CourseLibraryDO courseLibraryDO = courseLibraryDAO.lambdaQuery()
                    .eq(CourseLibraryDO::getDepartment, setUpDepartment.getDepartmentUuid())
                    .eq(CourseLibraryDO::getCourseLibraryUuid, teacherCourseQualificationDO.getCourseUuid())
                    .one();
            if (courseLibraryDO != null) {
                setUpCourseLibraries.add(courseLibraryDO);
            }
        }
        return ResultUtil.success("基础数据",setUpCourseLibraries);
    }

    /**
     * 自动排课
     * @param automaticClassSchedulingVO 自动排课请求对象，包含排课所需的各种设置和参数
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return ResponseEntity<BaseResponse<Void>> 返回排课结果
     */

    @RequestRole("教务")
    @PostMapping("/auto")
    public ResponseEntity<BaseResponse<SchedulingTaskDTO>> automaticClassScheduling(
            @RequestBody @Valid AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request
    ) {
        if (automaticClassSchedulingVO == null){
            throw new BusinessException("请求体不能为空", ErrorCode.BODY_ERROR);
        }
        Optional.ofNullable(automaticClassSchedulingVO)
                .map(AutomaticClassSchedulingVO::getTimePreferences)
                .map(AutomaticClassSchedulingVO.TimePreferences::getPreferredTimeSlots)
                // 确保 slots 不为 null
                .filter(slots -> slots != null && !slots.isEmpty())
                // 如果 slots 为 null 或空，返回空列表
                .orElse(Collections.emptyList())
                .stream()
                .filter(slot -> slot.getPeriodStart() > slot.getPeriodEnd())
                .findFirst()
                .ifPresent(badSlot -> {
                    throw new BusinessException(
                            String.format("时段校验失败：第%d天 %d-%d节 结束节次应大于开始节次",
                                    badSlot.getDay(),
                                    badSlot.getPeriodStart(),
                                    badSlot.getPeriodEnd()),
                            ErrorCode.BODY_ERROR
                    );
                });
        //检查课程ID列表是否为空
        Optional.of(automaticClassSchedulingVO)
                .map(AutomaticClassSchedulingVO::getScopeSettings)
                .map(AutomaticClassSchedulingVO.ScopeSettings::getSpecificCourseIds)
                .filter(data -> !data.isEmpty())
                .orElseThrow(() -> new BusinessException("课程ID列表为空", ErrorCode.BODY_ERROR));
        // 检查 classID 和 number 是否同时为空
        for (SpecificCourseIdVO course :
                automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds()
        ) {
            if ((course.getClassId() == null && course.getNumber() == null) || (
                    course.getClassId() != null && course.getClassId().isEmpty() && course.getNumber() == null)) {
                throw new BusinessException("班级或者人数选择为空", ErrorCode.BODY_ERROR);
            }
            // 检查课程周数是否在范围内
            Optional.ofNullable(course.getWeeklyHours())
                    .filter(h -> h % 2 > 0  && course.getIsOddWeek() == null)
                    .ifPresent(h -> {
                        throw new BusinessException("每周双周节课未指定是否为单双周", ErrorCode.BODY_ERROR);
                    });
        }
        // 准备数据并排课
        SchedulingTaskDTO schedulingTaskDTO = schedulingService.getAutoClassSchedulingBaseDTO(automaticClassSchedulingVO, request);
        return ResultUtil.success("开始排课", schedulingTaskDTO);
    }

    /**
     * 获取排课任务状态
     * @param taskId 排课任务ID
     * @return ResponseEntity<BaseResponse<SchedulingTaskStatusDTO>> 返回排课任务状态
     */
    @GetMapping("/tasks/{task_id}")
    @RequestRole("教务")
    public ResponseEntity<BaseResponse<SchedulingTaskStatusDTO>> getSchedulingTaskStatus(
            @PathVariable("task_id") String taskId
    ) {
        // 数据检查
        if (taskId == null || taskId.isEmpty()){
            throw new BusinessException("任务ID不能为空", ErrorCode.BODY_ERROR);
        }
        // 获取排课任务状态
        SchedulingTaskStatusDTO schedulingTaskDTO = schedulingService.getSchedulingTaskStatus(taskId);
        return ResultUtil.success("获取排课任务状态成功", schedulingTaskDTO);
    }



    /**
     * 获取排课任务列表
     * @param request HTTP请求对象
     * @return ResponseEntity<BaseResponse<List<String>>> 返回排课任务列表
     */
    @GetMapping("/tasks")
    @RequestRole("教务")
    public ResponseEntity<BaseResponse<List<String>>> getSchedulingTasks(
            HttpServletRequest request
    ) {
        // 获取排课任务列表
        List<String> list = schedulingService.getSchedulingTasks(request);
        return ResultUtil.success("获取排课任务列表成功", list);
    }
}
