package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.GradeDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导入基础学生DTO
 * @author FLASHLAKC
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportBaseStudentDTO {
    private DepartmentDO department;
    private List<MajorDO> majors;
    private List<GradeDO> grades;
    private List<AdministrativeClassDO> clazz;
}
