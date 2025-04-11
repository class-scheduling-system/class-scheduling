package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.schedule.CourseScheduleDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 课程表服务接口
 * <p>
 * 该接口定义了课程表相关的业务操作，包括获取教师和学生的课程表信息。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface CourseScheduleService {

    /**
     * 获取当前教师课程表
     * <p>
     * 该方法用于获取当前登录教师在当前学期的课程表信息。
     * </p>
     *
     * @param request HttpServletRequest对象，用于获取当前登录用户信息
     * @return 返回教师课程表信息
     */
    CourseScheduleDTO getTeacherCourseSchedule(HttpServletRequest request);

    /**
     * 获取指定学期的教师课程表
     * <p>
     * 该方法用于获取当前登录教师在指定学期的课程表信息。
     * </p>
     *
     * @param request      HttpServletRequest对象，用于获取当前登录用户信息
     * @param semesterUuid 学期UUID
     * @return 返回教师课程表信息
     */
    CourseScheduleDTO getTeacherCourseSchedule(HttpServletRequest request, String semesterUuid);

    /**
     * 获取当前学生课程表
     * <p>
     * 该方法用于获取当前登录学生在当前学期的课程表信息。
     * </p>
     *
     * @param request HttpServletRequest对象，用于获取当前登录用户信息
     * @return 返回学生课程表信息
     */
    CourseScheduleDTO getStudentCourseSchedule(HttpServletRequest request);

    /**
     * 获取指定学期的学生课程表
     * <p>
     * 该方法用于获取当前登录学生在指定学期的课程表信息。
     * </p>
     *
     * @param request      HttpServletRequest对象，用于获取当前登录用户信息
     * @param semesterUuid 学期UUID
     * @return 返回学生课程表信息
     */
    CourseScheduleDTO getStudentCourseSchedule(HttpServletRequest request, String semesterUuid);
} 