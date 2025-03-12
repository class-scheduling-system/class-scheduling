package com.frontleaves.scheduling.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class MajorAcademicDTO {
    private String majorUuid;
    private String majorName;
    private String majorDescription;
    private String majorCode;
    private Integer majorStatus;
    private String departmentUuid;
    private Integer educationYears;
    private String trainingLevel;
    private Long createdAt;
    private Long updatedAt;
}
