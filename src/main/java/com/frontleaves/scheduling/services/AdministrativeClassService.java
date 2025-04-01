package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.ClassMappingDTO;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;

import java.util.List;

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

    /**
     * 获取行政班级列表，用于下拉框显示
     *
     * @return 行政班级列表
     */
    List<AdministrativeClassDO> getAdministrativeClassList();

    /**
     * 根据行政班级UUID获取行政班级信息
     * @param classGroup 行政班级UUID列表
     * @return 行政班级信息列表
     */
    List<String> getClassNameByGroup(List<AdministrativeClassDTO> classGroup);
}
