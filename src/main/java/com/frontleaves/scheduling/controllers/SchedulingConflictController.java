/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.logic.SchedulingConflictLogic;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 排课冲突控制器
 * <p>
 * 提供排课冲突相关的 API 接口，包括查询冲突详情、分页查询冲突列表和查询简单冲突列表。
 * 所有接口均为只读操作，不提供修改、删除等功能。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conflict")
public class SchedulingConflictController {

    private final SchedulingConflictLogic schedulingConflictLogic;

    /**
     * 获取排课冲突详情
     *
     * @param conflictUuid 冲突UUID
     * @return 冲突详情
     */
    @RequestLogin
    @GetMapping("/{conflict_uuid}")
    public ResponseEntity<BaseResponse<SchedulingConflictDTO>> getConflictDetail(
            @PathVariable("conflict_uuid") String conflictUuid
    ) {
        if (conflictUuid == null || conflictUuid.isBlank()) {
            throw new BusinessException("冲突UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        
        if (!conflictUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("冲突UUID格式错误", ErrorCode.PARAMETER_ERROR);
        }
        
        SchedulingConflictDTO conflictDTO = schedulingConflictLogic.getConflictDetail(conflictUuid);
        return ResultUtil.success("获取冲突详情成功", conflictDTO);
    }

    /**
     * 分页查询排课冲突列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param semesterUuid 学期UUID
     * @param conflictType 冲突类型
     * @param resolutionStatus 解决状态
     * @return 分页数据
     */
    @RequestLogin
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<SchedulingConflictDTO>>> page(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "semester_uuid", required = false) String semesterUuid,
            @RequestParam(value = "conflict_type", required = false) Integer conflictType,
            @RequestParam(value = "resolution_status", required = false) Integer resolutionStatus
    ) {
        // 验证分页参数
        if (page <= 0) {
            throw new BusinessException("页码必须大于0", ErrorCode.PARAMETER_ERROR);
        }
        
        if (size <= 0 || size > 200) {
            throw new BusinessException("每页大小必须大于0且不超过200", ErrorCode.PARAMETER_ERROR);
        }
        
        // 验证学期UUID格式
        if (semesterUuid != null && !semesterUuid.isBlank() && 
                !semesterUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("学期UUID格式错误", ErrorCode.PARAMETER_ERROR);
        }
        
        // 验证冲突类型
        if (conflictType != null && (conflictType < 1 || conflictType > 4)) {
            throw new BusinessException("冲突类型必须在1-4之间", ErrorCode.PARAMETER_ERROR);
        }
        
        // 验证解决状态
        if (resolutionStatus != null && (resolutionStatus < 0 || resolutionStatus > 2)) {
            throw new BusinessException("解决状态必须在0-2之间", ErrorCode.PARAMETER_ERROR);
        }
        
        PageDTO<SchedulingConflictDTO> pageDTO = schedulingConflictLogic.page(
                page, size, semesterUuid, conflictType, resolutionStatus);
        return ResultUtil.success("获取冲突列表成功", pageDTO);
    }

    /**
     * 获取简单冲突列表
     *
     * @param semesterUuid 学期UUID
     * @param resolutionStatus 解决状态
     * @return 冲突列表
     */
    @RequestLogin
    @GetMapping("/list/simple")
    public ResponseEntity<BaseResponse<List<SchedulingConflictDTO>>> listSimple(
            @RequestParam("semester_uuid") String semesterUuid,
            @RequestParam(value = "resolution_status", required = false) Integer resolutionStatus
    ) {
        // 验证学期UUID
        if (semesterUuid == null || semesterUuid.isBlank()) {
            throw new BusinessException("学期UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        
        if (!semesterUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("学期UUID格式错误", ErrorCode.PARAMETER_ERROR);
        }
        
        // 验证解决状态
        if (resolutionStatus != null && (resolutionStatus < 0 || resolutionStatus > 2)) {
            throw new BusinessException("解决状态必须在0-2之间", ErrorCode.PARAMETER_ERROR);
        }
        
        List<SchedulingConflictDTO> conflictList = schedulingConflictLogic.listSimple(
                semesterUuid, resolutionStatus);
        return ResultUtil.success("获取简单冲突列表成功", conflictList);
    }
} 