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
    private String studentUuid;
    private Boolean status;
}
