package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 课程库数据传输对象
 * <p>
 * 用于传输课程库相关信息，包含课程的基本信息和详细信息；
 * 包含课程ID、名称、英文名称、类别、属性、类型、性质、所属院系、
 * 课时信息（总课时、周课时、理论课时、实验课时等）、学分等信息。
 *
 * @author qiyu
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CourseLibraryDTO {

    /**
     * 课程库UUID
     */
    private String courseLibraryUuid;

    /**
     * 课程ID
     */
    private String id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程英文名称
     */
    @Nullable
    private String englishName;

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
     * 课程描述
     */
    @Nullable
    private String description;

    /**
     * 编辑用户
     */
    private String editUser;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}