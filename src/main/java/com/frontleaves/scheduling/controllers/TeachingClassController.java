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

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;
import com.frontleaves.scheduling.models.dto.lite.TeachingClassLiteDTO;
import com.frontleaves.scheduling.services.TeachingClassService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教学班控制器
 * <p>
 * 处理与教学班相关的HTTP请求，包括查询、创建、更新和删除教学班
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teaching-class")
public class TeachingClassController {

    private final TeachingClassService teachingClassService;

    /**
     * 分页获取教学班列表
     *
     * @param page          页码
     * @param size          每页大小
     * @param keyword       关键字（可选）
     * @param departmentUuid 部门UUID（可选）
     * @param semesterUuid  学期UUID（可选）
     * @param isDesc        是否降序排序（可选，默认true）
     * @return 教学班分页列表
     */
    @GetMapping("")
    @RequestRole({"管理员", "教务"})
    public ResponseEntity<BaseResponse<PageDTO<TeachingClassLiteDTO>>> getTeachingClassList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentUuid,
            @RequestParam(required = false) String semesterUuid,
            @RequestParam(defaultValue = "true") boolean isDesc) {

        PageDTO<TeachingClassLiteDTO> result = teachingClassService.getTeachingClassList(
                page, size, keyword, departmentUuid, semesterUuid, isDesc);
        return ResultUtil.success("获取教学班列表成功", result);
    }

    /**
     * 获取教学班列表（不分页）
     *
     * @param keyword       关键字（可选）
     * @param departmentUuid 部门UUID（可选）
     * @param semesterUuid  学期UUID（可选）
     * @param teacherUuid   教师UUID（可选）
     * @param isEnabled     是否启用（可选，默认true）
     * @param isDesc        是否降序排序（可选，默认true）
     * @return 教学班列表
     */
    @GetMapping("/list")
    @RequestRole({"管理员", "教务", "教师"})
    public ResponseEntity<BaseResponse<List<TeachingClassLiteDTO>>> getTeachingClassList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String departmentUuid,
            @RequestParam(required = false) String semesterUuid,
            @RequestParam(required = false) String teacherUuid,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(defaultValue = "true") boolean isDesc) {

        List<TeachingClassLiteDTO> result = teachingClassService.getTeachingClassesList(
                keyword, departmentUuid, semesterUuid, teacherUuid, isEnabled, isDesc);
        return ResultUtil.success("获取教学班列表成功", result);
    }
    
    /**
     * 根据学期获取教学班列表（不分页）
     *
     * @param semesterUuid 学期UUID
     * @return 教学班列表
     */
    @GetMapping("/semester/{semesterUuid}")
    @RequestRole({"管理员", "教务", "教师"})
    public ResponseEntity<BaseResponse<List<TeachingClassDTO>>> getTeachingClassListBySemester(
            @PathVariable String semesterUuid) {

        List<TeachingClassDTO> result = teachingClassService.getTeachingClassListBySemester(semesterUuid);
        return ResultUtil.success("获取学期教学班列表成功", result);
    }

    /**
     * 根据部门获取教学班列表（不分页）
     *
     * @param departmentUuid 部门UUID
     * @return 教学班列表
     */
    @GetMapping("/department/{departmentUuid}")
    @RequestRole({"管理员", "教务"})
    public ResponseEntity<BaseResponse<List<TeachingClassLiteDTO>>> getTeachingClassListByDepartment(
            @PathVariable String departmentUuid) {

        List<TeachingClassLiteDTO> result = teachingClassService.getTeachingClassesList(
                null, departmentUuid, null, null, true, true);
        return ResultUtil.success("获取部门教学班列表成功", result);
    }

    /**
     * 根据UUID获取教学班详情
     *
     * @param teachingClassUuid 教学班UUID
     * @return 教学班详情
     */
    @GetMapping("/{teachingClassUuid}")
    @RequestRole({"管理员", "教务", "教师"})
    public ResponseEntity<BaseResponse<TeachingClassDTO>> getTeachingClassByUuid(
            @PathVariable String teachingClassUuid) {

        TeachingClassDTO result = teachingClassService.getTeachingClassByUuid(teachingClassUuid);
        return ResultUtil.success("获取教学班详情成功", result);
    }

    /**
     * 创建新教学班
     *
     * @param teachingClassDTO 教学班数据
     * @return 创建的教学班
     */
    @PostMapping("")
    @RequestRole({"管理员", "教务"})
    public ResponseEntity<BaseResponse<TeachingClassDTO>> createTeachingClass(
            @RequestBody @Valid TeachingClassDTO teachingClassDTO) {

        TeachingClassDTO result = teachingClassService.createTeachingClass(teachingClassDTO);
        return ResultUtil.success("创建教学班成功", result);
    }

    /**
     * 更新教学班
     *
     * @param teachingClassUuid 教学班UUID
     * @param teachingClassDTO  教学班数据
     * @return 更新后的教学班
     */
    @PutMapping("/{teachingClassUuid}")
    @RequestRole({"管理员", "教务"})
    public ResponseEntity<BaseResponse<TeachingClassDTO>> updateTeachingClass(
            @PathVariable String teachingClassUuid,
            @RequestBody @Valid TeachingClassDTO teachingClassDTO) {

        TeachingClassDTO result = teachingClassService.updateTeachingClass(teachingClassUuid, teachingClassDTO);
        return ResultUtil.success("更新教学班成功", result);
    }

    /**
     * 删除教学班
     *
     * @param teachingClassUuid 教学班UUID
     * @return 操作结果
     */
    @DeleteMapping("/{teachingClassUuid}")
    @RequestRole({"管理员", "教务"})
    public ResponseEntity<BaseResponse<Void>> deleteTeachingClass(
            @PathVariable String teachingClassUuid) {

        teachingClassService.deleteTeachingClass(teachingClassUuid);
        return ResultUtil.success("删除教学班成功");
    }
} 