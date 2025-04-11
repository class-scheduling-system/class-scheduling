package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * 课程导入数据传输对象
 * <p>
 * 该类用于从Excel导入的课程数据的封装，包含了课程的基本信息
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class CourseImportDTO {

    /**
     * 课程ID
     */
    private String id;

    /**
     * 课程名称
     */
    private String name;


    /**
     * 课程类别
     */
    private String category;

    /**
     * 课程属性
     */
    private String property;

    /**
     * 课程类型
     */
    private String type;

    /**
     * 课程性质
     */
    private String nature;

    /**
     * 所属院系
     */
    private String department;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 总课时
     */
    private BigDecimal totalHours;

    /**
     * 周课时
     */
    private BigDecimal weekHours;

    /**
     * 理论课时
     */
    private BigDecimal theoryHours;

    /**
     * 实验课时
     */
    private BigDecimal experimentHours;

    /**
     * 实践课时
     */
    private BigDecimal practiceHours;

    /**
     * 上机课时
     */
    private BigDecimal computerHours;

    /**
     * 其他课时
     */
    private BigDecimal otherHours;

    /**
     * 学分
     */
    private BigDecimal credit;

    /**
     * 理论教室类型
     */
    private String theoryClassroomType;

    /**
     * 实验教室类型
     */
    private String experimentClassroomType;

    /**
     * 实践教室类型
     */
    private String practiceClassroomType;

    /**
     * 上机教室类型
     */
    private String computerClassroomType;


}
