package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CourseScheduleDAO;
import com.frontleaves.scheduling.daos.StudentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.models.dto.schedule.CourseScheduleDTO;
import com.frontleaves.scheduling.models.entity.base.StudentDO;
import com.frontleaves.scheduling.models.entity.base.TeacherDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.services.CourseScheduleService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 课程表服务实现类
 * <p>
 * 该类实现了 {@link CourseScheduleService} 接口，提供了获取教师和学生课程表的具体实现。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseScheduleLogic implements CourseScheduleService {

    private final CourseScheduleDAO courseScheduleDAO;
    private final UserService userService;
    private final TeacherDAO teacherDAO;
    private final StudentDAO studentDAO;

    @Override
    public CourseScheduleDTO getTeacherCourseSchedule(HttpServletRequest request) {
        // 获取当前登录用户
        UserDO user = userService.getUserByRequest(request);
        
        // 查询教师信息
        TeacherDO teacher = teacherDAO.getTeacherByUserUuid(user.getUserUuid());
        if (teacher == null) {
            throw new BusinessException("当前用户不是教师", ErrorCode.OPERATION_DENIED);
        }
        
        // 获取课程表（默认获取当前学期）
        return courseScheduleDAO.getTeacherCourseSchedule(teacher.getTeacherUuid(), null);
    }

    @Override
    public CourseScheduleDTO getTeacherCourseSchedule(HttpServletRequest request, String semesterUuid) {
        // 获取当前登录用户
        UserDO user = userService.getUserByRequest(request);
        
        // 查询教师信息
        TeacherDO teacher = teacherDAO.getTeacherByUserUuid(user.getUserUuid());
        if (teacher == null) {
            throw new BusinessException("当前用户不是教师", ErrorCode.OPERATION_DENIED);
        }
        
        // 获取指定学期的课程表
        return courseScheduleDAO.getTeacherCourseSchedule(teacher.getTeacherUuid(), semesterUuid);
    }

    @Override
    public CourseScheduleDTO getStudentCourseSchedule(HttpServletRequest request) {
        // 获取当前登录用户
        UserDO user = userService.getUserByRequest(request);
        
        // 查询学生信息
        StudentDO student = studentDAO.getStudentByUserUuid(user.getUserUuid());
        if (student == null) {
            throw new BusinessException("当前用户不是学生", ErrorCode.OPERATION_DENIED);
        }
        
        // 获取课程表（默认获取当前学期）
        return courseScheduleDAO.getStudentCourseSchedule(student.getStudentUuid(), null);
    }

    @Override
    public CourseScheduleDTO getStudentCourseSchedule(HttpServletRequest request, String semesterUuid) {
        // 获取当前登录用户
        UserDO user = userService.getUserByRequest(request);
        
        // 查询学生信息
        StudentDO student = studentDAO.getStudentByUserUuid(user.getUserUuid());
        if (student == null) {
            throw new BusinessException("当前用户不是学生", ErrorCode.OPERATION_DENIED);
        }
        
        // 获取指定学期的课程表
        return courseScheduleDAO.getStudentCourseSchedule(student.getStudentUuid(), semesterUuid);
    }
} 