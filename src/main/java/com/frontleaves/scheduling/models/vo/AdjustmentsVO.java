package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 调整课程表的VO类
 *
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdjustmentsVO {
    /**
     * 排课分配ID
     */
    @NotBlank
    private String assignmentId;
    /**
     * 具体的调整内容
     */
    @NotNull
    private AdjustmentDetailsVO adjustments;
    /**
     * 调整的教学班信息
     */
    @NotNull
    private AdjustTeachingClassVO adjustTeachingClass;
    /**
     * 是否忽略可能产生的冲突，默认false
     */
    private Boolean ignoreConflicts = false;

    /**
     * 可选，调整原因
     */
    private String reason;

}
