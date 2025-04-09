package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;
import com.frontleaves.scheduling.models.dto.lite.TeachingClassLiteDTO;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 教学班服务接口
 * @author FLASHLACK
 */
public interface TeachingClassService {
    /**
     * 根据学期主键和查询教学班
     * @param semesterUuid 学期UUID
     * @return 教学班数据传输对象
     */
    List<TeachingClassDTO> getTeachingClassListBySemester(String semesterUuid);

    /**
     * 根据UUID查询教学班
     * @param teachingClassUuid 教学班UUID
     * @return 教学班数据传输对象
     */
    @NotNull
    TeachingClassDTO getTeachingClassByUuid(String teachingClassUuid);

    /**
     * 保存教学班
     * @param teachingClassDO 教学班数据对象
     */
    void save(TeachingClassDO teachingClassDO);

    /**
     * 根据UUID查询教学班 - 为空
     * @param teachingClassUuid 教学班UUID
     * @return 教学班数据传输对象
     */
    @Nullable
    TeachingClassDTO getTeachingClassByUuidNoError(String teachingClassUuid);
    
    /**
     * 获取教学班分页列表
     * 
     * @param page 页码
     * @param size 每页大小
     * @param keyword 关键字（课程名称或教学班名称）
     * @param departmentUuid 部门UUID（可选）
     * @param semesterUuid 学期UUID（可选）
     * @param isDesc 是否降序排序
     * @return 教学班简化DTO分页列表
     */
    PageDTO<TeachingClassLiteDTO> getTeachingClassList(int page, int size, String keyword, 
                                                    String departmentUuid, String semesterUuid, boolean isDesc);
    
    /**
     * 根据部门UUID获取教学班列表
     * 
     * @param departmentUuid 部门UUID
     * @return 教学班简化DTO列表
     */
    List<TeachingClassLiteDTO> getTeachingClassListByDepartment(String departmentUuid);
    
    /**
     * 创建新教学班
     * 
     * @param teachingClassDTO 教学班数据传输对象
     * @return 创建成功的教学班数据传输对象
     */
    TeachingClassDTO createTeachingClass(TeachingClassDTO teachingClassDTO);
    
    /**
     * 更新教学班信息
     * 
     * @param teachingClassUuid 教学班UUID
     * @param teachingClassDTO 教学班数据传输对象
     * @return 更新后的教学班数据传输对象
     */
    TeachingClassDTO updateTeachingClass(String teachingClassUuid, TeachingClassDTO teachingClassDTO);
    
    /**
     * 删除教学班
     * 
     * @param teachingClassUuid 教学班UUID
     */
    void deleteTeachingClass(String teachingClassUuid);
}
