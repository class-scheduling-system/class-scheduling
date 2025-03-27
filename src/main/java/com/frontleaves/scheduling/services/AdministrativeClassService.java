package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.ClassMappingDTO;

/**
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface AdministrativeClassService {

    /**
     * 根据行政班级UUID获取班级映射信息
     *
     * @param clazz 行政班级UUID
     * @return 班级映射 DTO 对象
     */
    ClassMappingDTO getClassMappingByClazz(String clazz);
}
