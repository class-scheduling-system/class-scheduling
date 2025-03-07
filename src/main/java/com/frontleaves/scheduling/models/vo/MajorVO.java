package com.frontleaves.scheduling.models.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;

/**
 * 专业视图对象
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class MajorVO {

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
     * 专业状态(0:禁用; 1:启用)
     */
    private Integer majorStatus;

    /**
     * 所属学院UUID(必填)
     */
    @NotNull
    private String departmentUuid;

    /**
     * 学制(年)
     */
    private Integer educationYears;

    /**
     * 培养层析
     */
    private String trainingLevel;
}
