package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.entity.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 验证课程返回DTO
 * <p>
 * 此类用于在验证课程信息后返回验证结果和对应的实体对象
 * </p>
 *
 * @author Claude AI
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class ValidateCourseReturnDTO {
    /**
     * 课程类别实体
     */
    private CourseCategoryDO categoryDO;
    
    /**
     * 课程属性实体
     */
    private CoursePropertyDO propertyDO;
    
    /**
     * 课程类型实体
     */
    private CourseTypeDO typeDO;
    
    /**
     * 课程性质实体
     */
    private CourseNatureDO natureDO;
    
    /**
     * 部门实体
     */
    private DepartmentDO departmentDO;
    
    /**
     * 理论教室类型实体
     */
    private ClassroomTypeDO theoryClassroomTypeDO;
    
    /**
     * 实验教室类型实体
     */
    private ClassroomTypeDO experimentClassroomTypeDO;
    
    /**
     * 实践教室类型实体
     */
    private ClassroomTypeDO practiceClassroomTypeDO;
    
    /**
     * 上机教室类型实体
     */
    private ClassroomTypeDO computerClassroomTypeDO;
} 