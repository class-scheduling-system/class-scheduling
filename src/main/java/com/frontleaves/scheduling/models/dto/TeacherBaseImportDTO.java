package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.TeacherTypeDO;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 导入基础教师 DTO
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class TeacherBaseImportDTO {
    /**
     * 单位信息
     */
    private DepartmentDO department;

    /**
     * 教师类型列表，用于校验教师类型
     */
    private List<TeacherTypeDO> teacherTypes;
}
