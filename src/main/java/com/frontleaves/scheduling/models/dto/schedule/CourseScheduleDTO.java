package com.frontleaves.scheduling.models.dto.schedule;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * 课程表数据传输对象
 * <p>
 * 该对象用于前端展示课程表信息，包含了课程安排的详细信息。
 * 数据结构设计考虑了教师和学生共同使用的场景。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class CourseScheduleDTO {
    
    /**
     * 学期信息
     */
    private SemesterInfo semester;
    
    /**
     * 课程安排列表，按星期和节次组织
     */
    private List<ScheduleItem> scheduleItems;
    
    /**
     * 学期信息内部类
     */
    @Data
    @Accessors(chain = true)
    public static class SemesterInfo {
        /**
         * 学期UUID
         */
        private String semesterUuid;
        
        /**
         * 学期名称
         */
        private String semesterName;
        
        /**
         * 学期开始日期
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Timestamp startDate;
        
        /**
         * 学期结束日期
         */
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Timestamp endDate;
    }
    
    /**
     * 课程表项内部类
     */
    @Data
    @Accessors(chain = true)
    public static class ScheduleItem {
        /**
         * 课程安排UUID
         */
        private String classAssignmentUuid;
        
        /**
         * 课程UUID
         */
        private String courseUuid;
        
        /**
         * 课程名称
         */
        private String courseName;
        
        /**
         * 教师UUID
         */
        private String teacherUuid;
        
        /**
         * 教师姓名
         */
        private String teacherName;
        
        /**
         * 教学班UUID
         */
        private String teachingClassUuid;
        
        /**
         * 教学班名称
         */
        private String teachingClassName;
        
        /**
         * 校区UUID
         */
        private String campusUuid;
        
        /**
         * 校区名称
         */
        private String campusName;
        
        /**
         * 教学楼UUID
         */
        private String buildingUuid;
        
        /**
         * 教学楼名称
         */
        private String buildingName;
        
        /**
         * 教室UUID
         */
        private String classroomUuid;
        
        /**
         * 教室名称
         */
        private String classroomName;
        
        /**
         * 学时类型UUID
         */
        private String creditHourTypeUuid;
        
        /**
         * 学时类型名称
         */
        private String creditHourTypeName;
        
        /**
         * 总学时
         */
        private BigDecimal totalHours;
        
        /**
         * 课程星期几(1-7)
         */
        private Integer dayOfWeek;
        
        /**
         * 开始节次(1-12)
         */
        private Integer startSlot;
        
        /**
         * 结束节次(1-12)
         */
        private Integer endSlot;
        
        /**
         * 连堂节数
         */
        private Integer consecutiveSessions;
        
        /**
         * 课程所在周次(1-N)
         */
        private Integer week;
    }
} 