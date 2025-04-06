package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.entity.base.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 导入课程基础数据传输对象
 * <p>
 * 该类用于封装批量导入课程库时所需的基础数据，包括课程类别、属性、类型、性质、部门和教室类型
 * </p>
 *
 * @author Claude AI
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportBaseCourseDTO {
    /**
     * 课程类别列表
     */
    private List<CourseCategoryDO> categoryList;

    /**
     * 课程属性列表
     */
    private List<CoursePropertyDO> propertyList;

    /**
     * 课程类型列表
     */
    private List<CourseTypeDO> typeList;

    /**
     * 课程性质列表
     */
    private List<CourseNatureDO> natureList;

    /**
     * 部门列表
     */
    private List<DepartmentDO> departmentList;

    /**
     * 教室类型列表
     */
    private List<ClassroomTypeDO> classroomTypeList;
}
