package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.SemesterDTO;
import com.frontleaves.scheduling.models.vo.SemesterVO;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 学期服务接口
 * <p>
 * 该接口定义了学期相关的业务操作，包括添加、删除、更新和查询学期信息等功能。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface SemesterService {
    /**
     * 添加学期
     *
     * @param vo 学期信息
     */
    void add(SemesterVO vo);

    /**
     * 删除学期
     *
     * @param semesterUuid 学期UUID
     */
    void delete(String semesterUuid);

    /**
     * 更新学期信息
     *
     * @param semesterUuid 学期UUID
     * @param vo          更新的学期信息
     */
    void update(String semesterUuid, SemesterVO vo);

    /**
     * 根据UUID获取学期信息
     *
     * @param semesterUuid 学期UUID
     * @return 学期信息
     */
    SemesterDTO getById(String semesterUuid);

    /**
     * 获取学期分页列表
     *
     * @param page    页码
     * @param size    每页大小
     * @param isDesc  是否降序
     * @param keyword 搜索关键字
     * @return 学期分页列表
     */
    PageDTO<SemesterDTO> page(Integer page, Integer size, Boolean isDesc, String keyword);

    /**
     * 获取启用的学期列表
     *
     * @return 启用的学期列表
     */
    List<SemesterDTO> list();

    /**
     * 根据学期UUID获取学期，并且检查是否启用
     *
     * @param semesterUuid 学期的唯一标识符
     * @return 返回学期信息对象，如果找不到则返回null，若不启用则报错
     */
    @NotNull
    SemesterDTO getSemesterByUuidCheckEnabled(
            String semesterUuid
    );
}
