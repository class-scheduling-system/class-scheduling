package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.schedule.CourseScheduleDTO;
import com.frontleaves.scheduling.services.CourseScheduleService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

/**
 * 课程表控制器
 * <p>
 * 该控制器提供了获取教师和学生课程表的接口。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/course-schedule")
@RequiredArgsConstructor
public class CourseScheduleController {

    private final CourseScheduleService courseScheduleService;

    /**
     * 获取教师课程表
     * <p>
     * 该接口获取当前登录教师的课程表信息。
     * 如果不指定学期，则默认获取当前学期的课程表。
     * </p>
     *
     * @param request      HTTP请求对象
     * @param semesterUuid 学期UUID（可选）
     * @return 返回教师课程表信息
     */
    @GetMapping("/teacher")
    @RequestRole({"教师", "管理员"})
    public ResponseEntity<BaseResponse<CourseScheduleDTO>> getTeacherCourseSchedule(
            HttpServletRequest request,
            @RequestParam(value = "semester_uuid", required = false) String semesterUuid
    ) {
        CourseScheduleDTO scheduleDTO;
        if (semesterUuid == null || semesterUuid.isEmpty()) {
            scheduleDTO = courseScheduleService.getTeacherCourseSchedule(request);
        } else {
            scheduleDTO = courseScheduleService.getTeacherCourseSchedule(request, semesterUuid);
        }
        return ResultUtil.success("获取教师课程表成功", scheduleDTO);
    }

    /**
     * 获取学生课程表
     * <p>
     * 该接口获取当前登录学生的课程表信息。
     * 如果不指定学期，则默认获取当前学期的课程表。
     * </p>
     *
     * @param request      HTTP请求对象
     * @param semesterUuid 学期UUID（可选）
     * @return 返回学生课程表信息
     */
    @GetMapping("/student")
    @RequestRole({"学生", "管理员"})
    public ResponseEntity<BaseResponse<CourseScheduleDTO>> getStudentCourseSchedule(
            HttpServletRequest request,
            @RequestParam(value = "semester_uuid", required = false) String semesterUuid
    ) {
        CourseScheduleDTO scheduleDTO;
        if (semesterUuid == null || semesterUuid.isEmpty()) {
            scheduleDTO = courseScheduleService.getStudentCourseSchedule(request);
        } else {
            scheduleDTO = courseScheduleService.getStudentCourseSchedule(request, semesterUuid);
        }
        return ResultUtil.success("获取学生课程表成功", scheduleDTO);
    }
}
