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
import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.base.GradeDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.vo.GradeVO;
import com.frontleaves.scheduling.services.GradeService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 年级管理控制器
 * <p>
 * 提供年级相关的接口，包括创建、更新、删除、查询等功能。
 * </p>
 *
 * @author AI Assistant
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/grade")
@RequiredArgsConstructor
public class GradeController {

    private final GradeService gradeService;

    /**
     * 创建年级
     * <p>
     * 该方法通过POST请求接收一个GradeVO对象，用于创建新的年级信息。
     * 主要功能包括验证传入的年级信息是否有效，以及将新年级信息保存到数据库中。
     * </p>
     *
     * @param gradeVO 年级信息视图对象
     * @return 创建结果
     */
    @RequestRole({"管理员"})
    @PostMapping
    public ResponseEntity<BaseResponse<GradeDTO>> createGrade(@RequestBody @Valid GradeVO gradeVO) {
        log.debug("创建年级: {}", gradeVO.getName());
        
        // 直接调用service层创建年级，传入VO对象
        GradeDTO result = gradeService.createGrade(gradeVO);
        
        return ResultUtil.success("年级创建成功", result);
    }

    /**
     * 更新年级
     * <p>
     * 该方法通过PUT请求编辑已有年级信息。
     * 主要功能包括验证传入的年级信息是否有效，以及将修改后的年级信息保存到数据库中。
     * </p>
     *
     * @param gradeUuid 年级UUID
     * @param gradeVO  年级信息视图对象
     * @return 更新结果
     */
    @RequestRole({"管理员"})
    @PutMapping("/{gradeUuid}")
    public ResponseEntity<BaseResponse<GradeDTO>> updateGrade(
            @PathVariable String gradeUuid,
            @RequestBody @Valid GradeVO gradeVO) {
        log.debug("更新年级: {}", gradeUuid);
        
        // 直接调用service层更新年级，传入UUID和VO对象
        GradeDTO result = gradeService.updateGrade(gradeUuid, gradeVO);
        
        return ResultUtil.success("年级修改成功", result);
    }

    /**
     * 删除年级
     * <p>
     * 该方法通过DELETE请求删除指定的年级信息，
     * 接收路径参数gradeUuid作为年级的唯一标识符。
     * </p>
     *
     * @param gradeUuid 年级UUID
     * @return 删除结果
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{gradeUuid}")
    public ResponseEntity<BaseResponse<Boolean>> deleteGrade(
            @PathVariable String gradeUuid) {
        log.debug("删除年级: {}", gradeUuid);
        
        boolean result = gradeService.deleteGrade(gradeUuid);
        
        return ResultUtil.success("年级删除成功", result);
    }

    /**
     * 获取年级详情
     * <p>
     * 根据年级的UUID查询年级信息，
     * 返回年级名称、入学年份、年级开始日期、年级结束日期、年级描述等详细信息。
     * </p>
     *
     * @param gradeUuid 年级UUID
     * @return 年级详情
     */
    @RequestLogin
    @GetMapping("/{gradeUuid}")
    public ResponseEntity<BaseResponse<GradeDTO>> getGradeDetail(
            @PathVariable String gradeUuid) {
        log.debug("查询年级详情: {}", gradeUuid);
        
        GradeDTO result = gradeService.getGradeDetail(gradeUuid);
        
        return ResultUtil.success("查询成功", result);
    }

    /**
     * 分页查询年级列表
     * <p>
     * 该接口用于分页查询年级信息，
     * 支持按年级名称和入学年份进行筛选。
     * </p>
     *
     * @param page 页码
     * @param size 每页大小
     * @param name 年级名称，可选，用于模糊查询
     * @param year 入学年份，可选
     * @return 分页数据
     */
    @RequestRole({"管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<GradeDTO>>> page(
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Short year) {
        log.debug("分页查询年级列表: page={}, size={}, name={}, year={}", page, size, name, year);
        
        if (size != null && size > 100) {
            throw new BusinessException("每页大小不能超过100", ErrorCode.PARAMETER_INVALID);
        }
        
        PageDTO<GradeDTO> result = gradeService.page(page, size, name, year);
        
        return ResultUtil.success("查询成功", result);
    }

    /**
     * 获取简单年级列表
     * <p>
     * 该接口用于获取所有年级的简单列表，不分页。
     * </p>
     *
     * @return 年级列表
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<GradeDTO>>> listSimple() {
        log.debug("查询年级简单列表");
        
        List<GradeDTO> result = gradeService.listSimple();
        
        return ResultUtil.success("查询成功", result);
    }
} 
