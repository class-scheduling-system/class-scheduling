package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherTypeDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 教师类型服务接口
 * <p>
 * 该接口定义了处理教师类型相关业务的方法，包括获取单个教师类型、
 * 获取教师类型列表、获取教师类型分页数据等功能。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
public interface TeacherTypeService {
    
    /**
     * 根据UUID获取教师类型信息
     *
     * @param teacherTypeUuid 教师类型UUID
     * @return 教师类型DTO对象
     */
    TeacherTypeDTO getTeacherType(String teacherTypeUuid);

    /**
     * 获取教师类型分页列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序
     * @param name 教师类型名称（可选，用于筛选）
     * @return 教师类型分页数据
     */
    PageDTO<TeacherTypeDTO> getTeacherTypePage(Integer page, Integer size, Boolean isDesc, String name);

    /**
     * 获取所有教师类型简洁列表
     *
     * @return 教师类型DTO列表
     */
    List<TeacherTypeDTO> getTeacherTypeList();
    
    /**
     * 添加教师类型
     *
     * @param typeName 类型名称
     * @param typeEnglishName 类型英文名称
     * @param typeDesc 类型描述
     * @return 添加成功的教师类型DTO
     */
    TeacherTypeDTO addTeacherType(String typeName, String typeEnglishName, String typeDesc);
    
    /**
     * 更新教师类型
     *
     * @param teacherTypeUuid 教师类型UUID
     * @param typeName 类型名称
     * @param typeEnglishName 类型英文名称
     * @param typeDesc 类型描述
     * @return 更新后的教师类型DTO
     */
    TeacherTypeDTO updateTeacherType(String teacherTypeUuid, String typeName, String typeEnglishName, String typeDesc);
    
    /**
     * 删除教师类型
     *
     * @param teacherTypeUuid 教师类型UUID
     * @return 是否删除成功
     */
    boolean deleteTeacherType(String teacherTypeUuid);
}
