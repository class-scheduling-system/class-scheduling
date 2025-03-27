package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 教师课程时间偏好表
 * @author FLASHLACK
 */
@Data
@TableName("cs_teacher_preferences")
public class TeacherPreferencesDO {
    /**
     * 教师喜好主键
     */
    @TableId(value = "preference_uuid", type = IdType.ASSIGN_UUID)
    private String preferenceUuid;
    /**
     * 教师主键
     */
    private String teacherUuid;
    /**
     * 学期主键
     */
    private String semesterUuid;
    /**
     * 星期几（1-7）
     */
    private Integer dayOfWeek;
    /**
     * 第几节课（1-12）
     */
    private Integer timeSlot;
    /**
     * 偏好程度（1：最不期望，2：尽量避免，3：可接受，4：较期望，5：非常期望）
     */
    private Integer preferenceLevel;
    /**
     * 偏好原因
     */
    private String reason;
    /**
     * 创建时间
     */

    private  Timestamp createdAt;
    /**
     * 更新时间
     */

    private Timestamp updatedAt;
}

