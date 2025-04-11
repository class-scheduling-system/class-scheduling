package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.lite.ClassLiteDTO;
import com.frontleaves.scheduling.models.entity.base.AdministrativeClassDO;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 行政班级服务接口
 * <p>
 * 该接口定义了行政班级管理的基本操作，
 * 如创建、修改、删除和查询行政班级信息。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface AdministrativeClassService {

    /**
     * 创建行政班级
     *
     * @param administrativeClassDTO 行政班级信息
     * @return 创建成功的行政班级信息
     */
    AdministrativeClassDTO createAdministrativeClass(AdministrativeClassDTO administrativeClassDTO);

    /**
     * 修改行政班级
     *
     * @param administrativeClassUuid 行政班级主键
     * @param administrativeClassDTO  行政班级信息
     * @return 修改后的行政班级信息
     */
    AdministrativeClassDTO updateAdministrativeClass(String administrativeClassUuid, AdministrativeClassDTO administrativeClassDTO) throws BusinessException;

    /**
     * 删除行政班级
     *
     * @param administrativeClassUuid 行政班级主键
     */
    void deleteAdministrativeClass(String administrativeClassUuid) throws BusinessException;

    /**
     * 查询行政班级详情
     *
     * @param administrativeClassUuid 行政班级主键
     * @return 行政班级详情
     * @throws BusinessException 当行政班级不存在时抛出异常
     */
    AdministrativeClassDTO getAdministrativeClass(String administrativeClassUuid) throws BusinessException;

    /**
     * 查询行政班级列表(管理员)
     *
     * @param page 页码(必填)
     * @param size 每页记录数(必填)
     * @param isDesc 是否倒序(选填,默认true)
     * @param departmentUuid 所属学院UUID(选填)
     * @param majorUuid 所属专业UUID(选填)
     * @param name 行政班级名称模糊查询(选填)
     * @return 分页后的行政班级列表
     */
    PageDTO<AdministrativeClassDTO> listAdministrativeClassForAdmin(int page, int size, Boolean isDesc, 
                                                 String departmentUuid, String majorUuid, String name);

    /**
     * 查询行政班级列表(教务)
     *
     * @param page 页码(必填)
     * @param size 每页记录数(必填)
     * @param isDesc 是否倒序(选填,默认true)
     * @param departmentUuid 所属学院UUID(选填)
     * @param majorUuid 所属专业UUID(选填)
     * @param name 行政班级名称模糊查询(选填)
     * @return 分页后的行政班级列表
     */
    PageDTO<AdministrativeClassDTO> listAdministrativeClassForAcademic(int page, int size, Boolean isDesc, 
                                                   String departmentUuid, String majorUuid, String name);

    /**
     * 查询行政班级列表(学生)
     *
     * @param page 页码（必填）
     * @param size 每页记录数（必填）
     * @param isDesc 是否倒序（选填，默认 true）
     * @param departmentUuid 所属学院UUID筛选（选填）
     * @param majorUuid 所属专业UUID筛选（选填）
     * @param name 行政班级名称模糊查询（选填）
     * @return 分页后的行政班级列表
     */
    PageDTO<AdministrativeClassDTO> listAdministrativeClassForStudent(int page, int size, Boolean isDesc, 
                                                   String departmentUuid, String majorUuid, String name);
    
    /**
     * 获取所有行政班级列表(不分页)
     * 
     * @return 所有行政班级列表
     */
    List<AdministrativeClassDTO> listAllAdministrativeClass();

    /**
     * 根据行政班级UUID获取班级映射信息
     *
     * @param clazz 行政班级UUID
     * @return 班级映射 DTO 对象
     */
    ClassLiteDTO getClassMappingByClazz(String clazz);

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

    /**
     * 根据UUID获取行政班级信息
     * @param uuid 行政班级UUID
     * @return 行政班级信息
     */
    @NotNull
    AdministrativeClassDTO getClassByUuid(String uuid);
}