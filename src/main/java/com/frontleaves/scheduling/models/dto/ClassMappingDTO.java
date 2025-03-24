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
public class ClassMappingDTO {
    /**
     * 年级UUID
     */
    private String gradeUuid;

    /**
     * 院系UUID(所属部门)
     */
    private  String departmentUuid;

    /**
     * 专业UUID
     */
    private String majorUuid;
}
