package com.frontleaves.scheduling.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * 专业数据传输对象
 * <p>
 * 该类用于在不同层之间传输专业(Major)基本信息,对应数据库实体 MajorDO 中的字段.
 * 包含专业主键、专业名称、专业描述、专业代码、专业状态、所属学院主键、专业学制、专业级别、创建时间、更新时间等字段.
 * 创建时间和更新时间在 DTO 中使用 Long 类型,在 Service 中使用 Timestamp 类型.
 * </p>
 *
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MajorDTO {
    /**
     * 专业主键
     */
    private String majorUuid;

    /**
     * 专业名称
     */
    private String majorName;

    /**
     * 专业描述
     */
    private String majorDescription;

    /**
     * 专业代码
     */
    private String majorCode;

    /**
     * 专业状态 (0:禁用; 1:启用)
     */
    private Integer majorStatus;

    /**
     * 所属学院UUID
     */
    @NotNull
    private String departmentUuid;

    /**
     * 学制(年)
     */
    private Integer educationYears;

    /**
     * 培养层次
     */
    private String trainingLevel;

    /**
     * 创建时间(单位:毫秒时间戳)
     */
    private Long createdAt;

    /**
     * 更新时间(单位:毫秒时间戳)
     */
    private Long updatedAt;
}
