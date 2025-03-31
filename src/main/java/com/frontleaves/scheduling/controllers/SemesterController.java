package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.SemesterDTO;
import com.frontleaves.scheduling.models.vo.SemesterVO;
import com.frontleaves.scheduling.services.SemesterService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 学期管理控制器
 * <p>
 * 该控制器提供了学期管理相关的 RESTful API 接口，包括添加、删除、更新、查询学期信息等功能。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/semester")
public class SemesterController {
    private final SemesterService semesterService;

    /**
     * 添加学期
     *
     * @param vo 学期信息
     * @return 添加结果
     */
    @RequestRole({"管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<Void>> add(
            @RequestBody @Validated SemesterVO vo
    ) {
        semesterService.add(vo);
        return ResultUtil.success("学期添加成功");
    }

    /**
     * 删除学期
     *
     * @param semesterUuid 学期UUID
     * @return 删除结果
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{semester_uuid}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable("semester_uuid") String semesterUuid
    ) {
        String getUuid = Optional.ofNullable(semesterUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        semesterService.delete(getUuid);
        return ResultUtil.success("学期删除成功");
    }

    /**
     * 更新学期
     *
     * @param semesterUuid 学期UUID
     * @param vo          学期信息
     * @return 更新结果
     */
    @RequestRole({"管理员"})
    @PutMapping("/{semester_uuid}")
    public ResponseEntity<BaseResponse<Void>> update(
            @PathVariable("semester_uuid") String semesterUuid,
            @RequestBody @Validated SemesterVO vo
    ) {
        String getUuid = Optional.ofNullable(semesterUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        semesterService.update(getUuid, vo);
        return ResultUtil.success("学期更新成功");
    }

    /**
     * 获取学期信息
     *
     * @param semesterUuid 学期UUID
     * @return 学期信息
     */
    @RequestLogin
    @GetMapping("/{semester_uuid}")
    public ResponseEntity<BaseResponse<SemesterDTO>> getById(
            @PathVariable("semester_uuid") String semesterUuid
    ) {
        String getUuid = Optional.ofNullable(semesterUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        return ResultUtil.success("查询成功", semesterService.getById(getUuid));
    }

    /**
     * 获取学期分页列表
     *
     * @param page    页码
     * @param size    每页大小
     * @param isDesc  是否降序排序
     * @param keyword 搜索关键字
     * @return 学期分页列表
     */
    @RequestLogin
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<SemesterDTO>>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "true") Boolean isDesc,
            @RequestParam(required = false) String keyword
    ) {
        if (size > 200) {
            throw new BusinessException(StringConstant.ErrorMessage.PAGE_SIZE_TOO_LARGE, ErrorCode.PARAMETER_INVALID);
        }
        return ResultUtil.success("查询学期列表成功", semesterService.page(page, size, isDesc, keyword));
    }

    /**
     * 获取启用的学期列表
     *
     * @return 启用的学期列表
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<SemesterDTO>>> list() {
        return ResultUtil.success("查询启用的学期列表成功", semesterService.list());
    }
}
