package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.statistics.AdminDashboardDTO;
import com.frontleaves.scheduling.models.dto.statistics.AcademicDashboardDTO;
import com.frontleaves.scheduling.models.dto.statistics.TeacherDashboardDTO;
import com.frontleaves.scheduling.services.StatisticsService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 仪表盘控制器
 * <p>
 * 提供各个角色查看统计数据的接口
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    /**
     * 获取管理员仪表盘统计数据
     * 包括用户总数、建筑总数、教师总数、学生总数、校区总数
     * 仅管理员可访问
     *
     * @return 包含统计数据的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/admin-dashboard")
    public ResponseEntity<BaseResponse<AdminDashboardDTO>> getAdminDashboardStatistics() {
        AdminDashboardDTO statistics = statisticsService.getAdminDashboardStatistics();
        return ResultUtil.success("获取管理员仪表盘数据成功", statistics);
    }

    /**
     * 获取教务仪表盘统计数据
     * 包括当前学院的教师人数、在校生人数（含专业分布）、行政班数、教学班总数、课程库总数
     * 仅教务可访问
     *
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return 包含统计数据的响应实体
     */
    @RequestRole({"教务"})
    @GetMapping("/academic-dashboard")
    public ResponseEntity<BaseResponse<AcademicDashboardDTO>> getAcademicDashboardStatistics(HttpServletRequest request) {
        AcademicDashboardDTO statistics = statisticsService.getAcademicDashboardStatistics(request);
        return ResultUtil.success("获取教务仪表盘数据成功", statistics);
    }

    /**
     * 获取教师仪表盘统计数据
     * 包括教授课程总数、带学生总人数、上课班级总数、总课时数
     * 仅教师可访问
     *
     * @param request HTTP请求对象，用于获取当前教师信息
     * @return 包含统计数据的响应实体
     */
    @RequestRole({"教师"})
    @GetMapping("/teacher-dashboard")
    public ResponseEntity<BaseResponse<TeacherDashboardDTO>> getTeacherDashboardStatistics(HttpServletRequest request) {
        TeacherDashboardDTO statistics = statisticsService.getTeacherDashboardStatistics(request);
        return ResultUtil.success("获取教师仪表盘数据成功", statistics);
    }
}
