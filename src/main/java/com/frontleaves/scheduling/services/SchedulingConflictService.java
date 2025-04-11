package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.ClassAssignmentDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;

import java.util.List;

/**
 * 排课冲突服务接口
 * <p>
 * 定义了排课冲突相关的服务方法，包括获取冲突详情、分页查询冲突列表和获取简单冲突列表。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface SchedulingConflictService {

    /**
     * 获取排课冲突详情
     *
     * @param conflictUuid 冲突UUID
     * @return 排课冲突DTO
     */
    SchedulingConflictDTO getConflictDetail(String conflictUuid);

    /**
     * 分页查询排课冲突列表
     *
     * @param page             页码
     * @param size             每页大小
     * @param semesterUuid     学期UUID
     * @param conflictType     冲突类型
     * @param resolutionStatus 解决状态
     * @return 分页数据
     */
    PageDTO<SchedulingConflictDTO> page(
            Integer page, Integer size,
            String semesterUuid,
            Integer conflictType,
            Integer resolutionStatus);

    /**
     * 获取简单冲突列表
     *
     * @param semesterUuid     学期UUID
     * @param resolutionStatus 解决状态
     * @return 冲突列表
     */
    List<SchedulingConflictDTO> listSimple(String semesterUuid, Integer resolutionStatus);

    /**
     * 检查冲突解决
     * @param classAssignment1 第一个排课安排
     */
    void checkForConflictResolution(ClassAssignmentDTO classAssignment1);
}