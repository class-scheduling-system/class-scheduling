package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.statistics.AdminDashboardDTO;
import com.frontleaves.scheduling.models.dto.statistics.AcademicDashboardDTO;
import com.frontleaves.scheduling.models.dto.statistics.TeacherDashboardDTO;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 统计服务接口
 * 提供系统各项数据的统计功能
 */
public interface StatisticsService {
    
    /**
     * 获取管理员仪表盘统计数据
     * 包括用户总数、建筑总数、教师总数、学生总数、校区总数
     *
     * @return 统计数据DTO对象
     */
    AdminDashboardDTO getAdminDashboardStatistics();
    
    /**
     * 获取教务仪表盘统计数据
     * 包括当前学院的教师人数、在校生人数、行政班数、教学班总数、课程库总数
     *
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return 教务统计数据DTO对象
     */
    AcademicDashboardDTO getAcademicDashboardStatistics(HttpServletRequest request);
    
    /**
     * 获取教师仪表盘统计数据
     * 包括教授课程总数、带学生总人数、上课班级总数、总课时数
     *
     * @param request HTTP请求对象，用于获取当前教师信息
     * @return 教师统计数据DTO对象
     */
    TeacherDashboardDTO getTeacherDashboardStatistics(HttpServletRequest request);
} 