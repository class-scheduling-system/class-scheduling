package com.frontleaves.scheduling.models.vo;

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
    private String assignmentId;
    /**
     * 具体的调整内容
     */
    private AdjustmentDetailsVO adjustments;

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
