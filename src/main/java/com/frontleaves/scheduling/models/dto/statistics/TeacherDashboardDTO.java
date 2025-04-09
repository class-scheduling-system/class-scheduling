package com.frontleaves.scheduling.models.dto.statistics;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.List;

/**
 * 教师仪表盘数据传输对象
 * <p>
 * 该类包含教师仪表盘所需的所有统计数据，包括：
 * - 教授课程总数
 * - 带学生总人数
 * - 上课班级总数
 * - 总课时数
 * - 班级详情列表（包含每个班级的学生总数、学时总数和学时类型）
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class TeacherDashboardDTO {
    /**
     * 教授课程总数
     */
    private Long courseCount;

    /**
     * 带学生总人数
     */
    private Long studentCount;

    /**
     * 上课班级总数
     */
    private Long classCount;

    /**
     * 总课时数
     */
    private Long totalHours;

    /**
     * 班级详情列表
     */
    private List<ClassDetail> classDetails;

    /**
     * 班级详情内部类
     * 用于存储每个班级的详细信息
     */
    @Data
    @Accessors(chain = true)
    public static class ClassDetail {
        /**
         * 教学班UUID
         */
        private String teachingClassUuid;

        /**
         * 教学班名称
         */
        private String teachingClassName;

        /**
         * 课程名称
         */
        private String courseName;

        /**
         * 学生总数
         */
        private Integer studentCount;

        /**
         * 学时总数
         */
        private BigDecimal totalHours;

        /**
         * 学时类型
         */
        private String creditHourType;
    }
} 