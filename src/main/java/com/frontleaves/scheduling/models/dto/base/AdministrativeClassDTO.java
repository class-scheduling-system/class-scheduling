package com.frontleaves.scheduling.models.dto.base;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 行政班级DTO
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class AdministrativeClassDTO {
    /**
     * 行政班主键
     */
    private String administrativeClassUuid;
    /**
     * 所属部门/院系
     */
    private String departmentUuid;
    /**
     * 所属专业
     */
    private String majorUuid;
    /**
     * 班级编号
     */
    private String classCode;
    /**
     * 班级名称
     */
    private String className;
    /**
     * 年级UUID
     */
    private String gradeUuid;
    /**
     * 学生人数
     */
    private Integer studentCount;
    /**
     * 辅导员UUID
     */
    private String counselorUuid;
    /**
     * 班长UUID
     */
    private String monitorUuid;
    /**
     * 是否启用(0:禁用,1:启用)
     */
    private Boolean isEnabled;
    /**
     * 班级描述
     */
    private String description;
    /**
     * 创建时间
     */
    private Timestamp createdAt;
    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
