package com.frontleaves.scheduling.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Student 数据传输对象
 * <p>
 * 用于在不同层之间传输学生基本信息。
 * 该 DTO 包含学生主键、学号、学生姓名、性别、年级、学院、专业、班级、对应用户主键、
 * 创建时间及更新时间等字段。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StudentDTO {

    /**
     * 学生主键
     */
    private String studentUuid;

    /**
     * 学号
     */
    private String id;

    /**
     * 学生姓名
     */
    private String name;

    /**
     * 性别（0：女，1：男）
     */
    private Boolean gender;

    /**
     * 年级主键
     */
    private String gradeUuid;

    /**
     * 学院
     */
    private String department;

    /**
     * 专业
     */
    private String major;

    /**
     * 班级
     */
    private String clazz;

    /**
     * 是否毕业
     */
    private Boolean graduated;

    /**
     * 对应用户主键
     */
    private String userUuid;

    /**
     * 创建时间（单位：毫秒时间戳）
     */
    private Long createdAt;

    /**
     * 更新时间（单位：毫秒时间戳）
     */
    private Long updatedAt;

    /**
     * 学生状态（0：未注册，1：已注册，2：已停用）
     */
    private Byte status;
}
