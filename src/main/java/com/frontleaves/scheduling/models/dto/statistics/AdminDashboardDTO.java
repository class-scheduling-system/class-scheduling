package com.frontleaves.scheduling.models.dto.statistics;

import java.util.List;

import com.frontleaves.scheduling.models.dto.base.RequestLogDTO;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 管理员仪表盘数据传输对象
 * <p>
 * 该类包含管理员仪表盘所需的所有统计数据，包括：
 * - 用户总数
 * - 建筑总数
 * - 教师总数
 * - 学生总数
 * - 校区总数
 * - 最近100条系统请求日志
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class AdminDashboardDTO {
    
    /**
     * 用户总数
     */
    private Long userCount;
    
    /**
     * 建筑总数
     */
    private Long buildingCount;
    
    /**
     * 教师总数
     */
    private Long teacherCount;
    
    /**
     * 学生总数
     */
    private Long studentCount;
    
    /**
     * 校区总数
     */
    private Long campusCount;

    /**
     * 最近100条系统请求日志
     */
    private List<RequestLogDTO> requestLogs;
} 