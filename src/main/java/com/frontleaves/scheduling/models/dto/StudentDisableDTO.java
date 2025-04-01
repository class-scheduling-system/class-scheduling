package com.frontleaves.scheduling.models.dto;

import lombok.*;
import lombok.experimental.Accessors;

/**
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StudentDisableDTO {
    /**
     * 学生主键
     */
    private String studentUuid;

    /**
     * 学生状态 0: 禁用 1: 启用
     */
    private Boolean status;
}
