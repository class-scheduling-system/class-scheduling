package com.frontleaves.scheduling.models.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 行政班级视图对象
 * <p>
 * 该类用于前端传入行政班级信息
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class AdministrativeClassVO {
    
    /**
     * 所属部门/院系UUID
     */
    private String departmentUuid;
    
    /**
     * 所属专业UUID
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
     * 是否启用
     */
    private Boolean isEnabled;
    
    /**
     * 班级描述
     */
    private String description;
}