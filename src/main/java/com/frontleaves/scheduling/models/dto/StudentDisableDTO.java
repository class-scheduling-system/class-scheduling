package com.frontleaves.scheduling.models.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StudentDisableDTO {
    private String studentUuid;
    private Boolean status;
    private Boolean disabled;
}
