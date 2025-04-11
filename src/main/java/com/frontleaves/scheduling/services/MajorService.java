package com.frontleaves.scheduling.services;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.frontleaves.scheduling.models.dto.base.MajorDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.lite.MajorLiteDTO;
import com.frontleaves.scheduling.models.entity.base.MajorDO;
import com.xlf.utility.exception.BusinessException;

import io.micrometer.common.lang.Nullable;

/**
 * 专业服务接口
 * <p>
 * 该接口定义了专业管理的基本操作，
 * 如创建、修改、删除和查询专业信息。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface MajorService {
    /**
     * 创建专业
     *
     * @param majorDTO 专业信息
     * @return 创建成功的专业信息
     */
    MajorDTO createMajor(MajorDTO majorDTO);

    /**
     * 修改专业
     *
     * @param majorUuid 专业主键
     * @param majorDTO  专业信息
     * @return 修改后的专业信息
     */
    MajorDO updateMajor(String majorUuid, MajorDO majorDTO) throws BusinessException;

    /**
     * 删除专业
     *
     * @param majorUuid 专业主键
     */
    void deleteMajor(String majorUuid) throws BusinessException;

    /**
     * 查询专业详情
     *
     * @param majorUuid 专业主键
     * @return 专业详情
     * @throws BusinessException 当专业不存在时抛出异常
     */
    MajorDTO getMajor(String majorUuid) throws BusinessException;

    /**
     * 查询专业列表(管理员)
     *
     * @param page 页码(必填)
     * @param size 每页记录数(必填)
     * @param isDesc 是否倒序(选填,默认true)
     * @param department 所属学院(选填)
     * @param name 专业名称模糊查询(选填)
     * @return 分页后的专业列表
     */
    PageDTO<MajorDTO> listMajorsForAdmin(int page, int size, Boolean isDesc, String department, String name);

    /**
     * 查询专业列表(教务)
     *
     * @param page       页码(必填)
     * @param size       每页记录数(必填)
     * @param isDesc     是否倒序(选填,默认true)
     * @param department 所属学院(选填)
     * @param name       专业名称模糊查询(选填)
     */
    PageDTO<MajorDTO> listMajorsForAcademic(int page, int size, Boolean isDesc, String department, String name);

    /**
     * 查询专业列表(学生)
     * <p>
     * 该接口用于获取学生的专业列表,支持分页查询和排序,
     * 可以根据部门和专业名称进行筛选.
     * </p>
     *
     * @param page       页码（必填）
     * @param size       每页记录数（必填）
     * @param isDesc     是否倒序（选填，默认 true）
     * @param department 所属学院筛选（选填）
     * @param name       专业名称模糊查询（选填）
     * @return 分页后的 MajorStudentDTO 列表
     */
    PageDTO<MajorDTO> listMajorsForStudent(int page, int size, Boolean isDesc, String department, String name);

    /**
     * 获取专业简单列表
     * <p>
     * 该接口用于获取专业的简单列表,支持分页查询和排序,
     * 可以根据部门和专业名称进行筛选.
     * </p>
     *
     * @param department 部门名称（可选）
     * @param name 专业名称（可选）
     * @return 返回一个PageDTO对象，包含分页查询结果和成功消息
     */
    List<MajorLiteDTO> majorLiteList(@Nullable String department, @Nullable String name);
}
