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
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCourseQualificationDTO;
import com.frontleaves.scheduling.models.entity.base.TeacherDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.models.vo.TeacherCourseQualificationQueryVO;
import com.frontleaves.scheduling.models.vo.TeacherCourseQualificationVO;
import com.frontleaves.scheduling.services.TeacherCourseQualificationService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 教师课程资格控制器
 * <p>
 * 该类提供了处理教师课程资格相关请求的 RESTful API，包括获取、添加、修改和删除教师课程资格等功能。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher-course-qualification")
public class TeacherCourseQualificationController {

    private final TeacherCourseQualificationService teacherCourseQualificationService;
    private final UserService userService;
    private final TeacherDAO teacherDAO;

    /**
     * 分页获取教师课程资格列表
     *
     * @param page 页码，默认为1
     * @param size 每页大小，默认为20
     * @param isDesc 是否降序排序，默认为true
     * @param teacherUuid 教师UUID，可选参数
     * @param courseUuid 课程UUID，可选参数
     * @param qualificationLevel 资格等级，可选参数
     * @param isPrimary 是否主讲教师，可选参数
     * @param status 状态，可选参数
     * @return 包含教师课程资格列表的分页数据
     */
    @RequestRole({"教务", "管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<TeacherCourseQualificationDTO>>> getTeacherCourseQualificationPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "teacher_uuid", required = false) String teacherUuid,
            @RequestParam(value = "course_uuid", required = false) String courseUuid,
            @RequestParam(value = "qualification_level", required = false) Integer qualificationLevel,
            @RequestParam(value = "is_primary", required = false) Boolean isPrimary,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        // 构建查询条件
        TeacherCourseQualificationQueryVO queryVO = new TeacherCourseQualificationQueryVO()
                .setTeacherUuid(teacherUuid)
                .setCourseUuid(courseUuid)
                .setQualificationLevel(qualificationLevel)
                .setIsPrimary(isPrimary)
                .setStatus(status);

        // 查询分页数据
        PageDTO<TeacherCourseQualificationDTO> pageDTO = teacherCourseQualificationService
                .getTeacherCourseQualificationList(page, size, isDesc, queryVO);

        return ResultUtil.success("查询成功", pageDTO);
    }

    /**
     * 获取教师课程资格列表（不分页）
     *
     * @param teacherUuid 教师UUID，可选参数
     * @param courseUuid 课程UUID，可选参数
     * @param qualificationLevel 资格等级，可选参数
     * @param isPrimary 是否主讲教师，可选参数
     * @param status 状态，可选参数
     * @return 教师课程资格列表
     */
    @RequestRole({"教务", "管理员", "教师"})
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<TeacherCourseQualificationDTO>>> getTeacherCourseQualificationList(
            @RequestParam(value = "teacher_uuid", required = false) String teacherUuid,
            @RequestParam(value = "course_uuid", required = false) String courseUuid,
            @RequestParam(value = "qualification_level", required = false) Integer qualificationLevel,
            @RequestParam(value = "is_primary", required = false) Boolean isPrimary,
            @RequestParam(value = "status", required = false) Integer status
    ) {
        // 构建查询条件
        TeacherCourseQualificationQueryVO queryVO = new TeacherCourseQualificationQueryVO()
                .setTeacherUuid(teacherUuid)
                .setCourseUuid(courseUuid)
                .setQualificationLevel(qualificationLevel)
                .setIsPrimary(isPrimary)
                .setStatus(status);

        // 查询列表数据
        List<TeacherCourseQualificationDTO> list = teacherCourseQualificationService
                .getTeacherCourseQualificationSimpleList(queryVO);

        return ResultUtil.success("查询成功", list);
    }

    /**
     * 获取教师课程资格详情
     *
     * @param qualificationUuid 资格UUID
     * @return 教师课程资格详情
     */
    @RequestRole({"教务", "管理员", "教师"})
    @GetMapping("/{qualification_uuid}")
    public ResponseEntity<BaseResponse<TeacherCourseQualificationDTO>> getTeacherCourseQualification(
            @PathVariable("qualification_uuid") String qualificationUuid
    ) {
        // 校验UUID格式
        String validUuid = Optional.ofNullable(qualificationUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException("资格UUID格式错误", ErrorCode.PARAMETER_ERROR));

        // 查询资格详情
        TeacherCourseQualificationDTO dto = teacherCourseQualificationService
                .getTeacherCourseQualification(validUuid);

        return ResultUtil.success("查询成功", dto);
    }

    /**
     * 添加教师课程资格
     *
     * @param qualificationVO 教师课程资格信息
     * @return 添加成功的资格UUID
     */
    @RequestRole({"教务", "管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<String>> addTeacherCourseQualification(
            @RequestBody @Validated TeacherCourseQualificationVO qualificationVO
    ) {
        // 添加资格
        String qualificationUuid = teacherCourseQualificationService
                .addTeacherCourseQualification(qualificationVO);

        return ResultUtil.success("添加成功", qualificationUuid);
    }

    /**
     * 更新教师课程资格
     *
     * @param qualificationUuid 资格UUID
     * @param qualificationVO 教师课程资格信息
     * @return 操作结果
     */
    @RequestRole({"教务", "管理员"})
    @PutMapping("/{qualification_uuid}")
    public ResponseEntity<BaseResponse<Void>> updateTeacherCourseQualification(
            @PathVariable("qualification_uuid") String qualificationUuid,
            @RequestBody @Validated TeacherCourseQualificationVO qualificationVO
    ) {
        // 校验UUID格式
        String validUuid = Optional.ofNullable(qualificationUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException("资格UUID格式错误", ErrorCode.PARAMETER_ERROR));

        // 更新资格
        teacherCourseQualificationService.updateTeacherCourseQualification(validUuid, qualificationVO);

        return ResultUtil.success("更新成功");
    }

    /**
     * 删除教师课程资格
     *
     * @param qualificationUuid 资格UUID
     * @return 操作结果
     */
    @RequestRole({"教务", "管理员"})
    @DeleteMapping("/{qualification_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteTeacherCourseQualification(
            @PathVariable("qualification_uuid") String qualificationUuid
    ) {
        // 校验UUID格式
        String validUuid = Optional.ofNullable(qualificationUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException("资格UUID格式错误", ErrorCode.PARAMETER_ERROR));

        // 删除资格
        teacherCourseQualificationService.deleteTeacherCourseQualification(validUuid);

        return ResultUtil.success("删除成功");
    }

    /**
     * 审核教师课程资格
     *
     * @param qualificationUuid 资格UUID
     * @param status 审核状态（1:通过 2:驳回）
     * @param remarks 审核备注
     * @param request HTTP请求
     * @return 操作结果
     */
    @RequestRole({"教务", "管理员"})
    @PutMapping("/{qualification_uuid}/approve")
    public ResponseEntity<BaseResponse<Void>> approveTeacherCourseQualification(
            @PathVariable("qualification_uuid") String qualificationUuid,
            @RequestParam("status") Integer status,
            @RequestParam(value = "remarks", required = false) String remarks,
            HttpServletRequest request
    ) {
        // 校验UUID格式
        String validUuid = Optional.ofNullable(qualificationUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException("资格UUID格式错误", ErrorCode.PARAMETER_ERROR));

        // 获取当前用户作为审核人
        UserDO userDO = userService.getUserByRequest(request);
        if (userDO == null) {
            throw new BusinessException("获取当前用户信息失败", ErrorCode.PARAMETER_ERROR);
        }

        // 审核资格
        teacherCourseQualificationService.approveTeacherCourseQualification(
                validUuid, status, remarks, userDO.getUserUuid());

        return ResultUtil.success("审核成功");
    }

    /**
     * 申请教师课程资格
     * <p>
     * 与添加资格不同，申请的资格状态初始为待审核，需要管理员或教务审核
     * </p>
     *
     * @param qualificationVO 教师课程资格信息
     * @return 申请成功的资格UUID
     */
    @RequestRole({"教师"})
    @PostMapping("/apply")
    public ResponseEntity<BaseResponse<String>> applyTeacherCourseQualification(
            @RequestBody @Validated TeacherCourseQualificationVO qualificationVO,
            HttpServletRequest request
    ) {
        // 如果是教师角色申请，则自动设置为当前登录教师
        UserDO userDO = userService.getUserByRequest(request);
        if (userDO != null) {
            TeacherDO teacherDO = teacherDAO.getTeacherByUserUuid(userDO.getUserUuid());
            if (teacherDO != null) {
                qualificationVO.setTeacherUuid(teacherDO.getTeacherUuid());
            }
        }

        // 申请资格
        String qualificationUuid = teacherCourseQualificationService
                .applyTeacherCourseQualification(qualificationVO);

        return ResultUtil.success("申请成功，等待审核", qualificationUuid);
    }
}
