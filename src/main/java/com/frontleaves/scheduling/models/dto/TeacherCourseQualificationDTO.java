package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 教师课程资格DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class TeacherCourseQualificationDTO {
    /**
     * 资格主键
     */
    private String qualificationUuid;

    /**
     * 教师主键
     */
    private String teacherUuid;

    /**
     * 课程主键
     */
    private String courseUuid;

    /**
     * 资格等级 1:初级 2:中级 3:高级
     */
    private Integer qualificationLevel;

    /**
     * 是否主讲教师
     */
    private Boolean isPrimary;

    /**
     * 教授年限
     */
    private Integer teachYears;

    /**
     * 状态 0:待审核 1:已审核 2:已驳回
     */
    private Integer status;

    /**
     * 备注说明
     */
    private String remarks;

    /**
     * 审核人
     */
    private String approvedBy;

    /**
     * 审核时间
     */
    private Timestamp approvedAt;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */

    private Timestamp updatedAt;
}
