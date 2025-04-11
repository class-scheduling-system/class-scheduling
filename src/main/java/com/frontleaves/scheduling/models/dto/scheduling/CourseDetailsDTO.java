package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;



/**
 * 课程详情数据传输对象（DTO）
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseDetailsDTO {
    /**
     * 教室ID
     */
    private String classroomId;
    /**
     * 教室名称
     */
    private String classroomName;

    /**
     * 教师ID
     */
    private String teacherId;

    /**
     * 教师姓名
     */
    private String teacherName;

    /**
     * 上课时间安排
     */
    private List<ClassTimeDTO> classTime;

    /**
     * 连堂节数
     */
    private Integer consecutiveSessions;

    /**
     * 排课优先级
     */
    private Integer schedulingPriority;
}
