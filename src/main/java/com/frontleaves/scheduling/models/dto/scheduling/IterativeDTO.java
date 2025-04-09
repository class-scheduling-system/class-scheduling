package com.frontleaves.scheduling.models.dto.scheduling;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 迭代算法排课DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class IterativeDTO {
    /**
     * 排课任务ID
     */
    private String taskId;
    /**
     * 排课任务最大迭代次数
     */
    private Integer maximumNumberOfIterations;
    /**
     * 排课任务迭代次数
     */
    private Integer number;
}
