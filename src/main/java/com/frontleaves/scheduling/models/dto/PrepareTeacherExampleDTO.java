package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 教师示例传输数据
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class PrepareTeacherExampleDTO {

    /**
     * 单位数据
     */
    private List<UnitDTO> unitList;

    /**
     * 教师类型数据
     */
    private List<TeacherTypeDTO> teacherTypeList;

    /**
     * 单位DTO
     */
    @Data
    @Accessors(chain = true)
    public static class UnitDTO {
        private String unitUuid;
        private String unitName;
    }
}