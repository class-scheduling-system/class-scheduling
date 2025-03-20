package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 批量导入学生信息的dto
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class StudentImportDTO {
    // Excel表头对应的字段
    private String id;
    private String name;
    private String gender;
    private String gradeName;
    private String departmentName;
    private String majorName;
    private String className;
}
