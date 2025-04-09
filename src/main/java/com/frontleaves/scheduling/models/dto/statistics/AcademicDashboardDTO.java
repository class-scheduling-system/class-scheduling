package com.frontleaves.scheduling.models.dto.statistics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 教务仪表盘数据DTO
 * 用于返回教务查看的学院统计信息
 */
@Data
@Accessors(chain = true)
public class AcademicDashboardDTO {
    
    /**
     * 当前学院教师人数
     */
    private Long teacherCount;
    
    /**
     * 当前学院在校生人数
     */
    private Long studentCount;
    
    /**
     * 当前学院各专业学生人数
     * Key: 专业名称
     * Value: 该专业的学生人数
     */
    private List<MajorStudentCount> majorStudentCounts;
    
    /**
     * 当前学院行政班数
     */
    private Long administrativeClassCount;
    
    /**
     * 当前学院教学班总数
     */
    private Long teachingClassCount;
    
    /**
     * 当前学院课程库总数
     */
    private Long courseLibraryCount;
    
    @Data
    @Accessors(chain = true)
    public static class MajorStudentCount {
        /**
         * 专业UUID
         */
        private String majorUuid;
        /**
         * 专业名称
         */
        private String majorName;
        /**
         * 该专业的学生人数
         */
        private Long count;
    }
} 