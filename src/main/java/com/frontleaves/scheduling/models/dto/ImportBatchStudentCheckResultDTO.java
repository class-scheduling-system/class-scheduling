package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;


/**
 * 导入检查结果DTO
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class ImportBatchStudentCheckResultDTO {
    private String departmentUuid;
    private String majorUuid;
    private String administrativeClassUuid;
    private String gradeUuid;
}
