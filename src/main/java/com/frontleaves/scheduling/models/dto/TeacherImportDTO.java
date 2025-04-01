package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 批量导入教师信息的 DTO
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class TeacherImportDTO {
    // Excel 表头对应的字段
    private String id;
    private String name;
    private String englishName;
    private String ethnic;
    private String sex;
    private String type;
    private String phone;
    private String email;
    private String jobTitle;
    private String departmentName;
    private String description;
}
