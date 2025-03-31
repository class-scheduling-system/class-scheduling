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
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.logic.UserLogic;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherPreferencesDTO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.TeacherPreferencesVO;
import com.frontleaves.scheduling.services.TeacherPreferencesService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 教师课程偏好控制器
 * <p>
 * 该控制器提供了与教师课程偏好相关的 RESTful API 接口。通过这些接口，用户可以进行教师课程偏好的管理操作，
 * 包括但不限于创建、查询、更新和删除教师课程偏好信息。所有请求都应通过 {@code /api/v1/teacher-preferences} 路径进行。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher/preferences")
public class TeacherPreferencesController {
    private final TeacherPreferencesService teacherPreferencesService;
    private final UserLogic userLogic;
    private final TeacherDAO teacherDAO;

    /**
     * 获取教师课程偏好列表分页数据（管理员和教务使用）
     * <p>
     * 该方法用于根据给定的参数获取教师课程偏好信息的分页数据。支持通过教师UUID和学期UUID进行过滤，并允许指定分页参数。
     * </p>
     *
     * @param page         分页的页码，从1开始，默认值为1
     * @param size         每页显示的数据条数，默认值为20，最大值为200
     * @param isDesc       是否按降序排序，默认值为true
     * @param teacherUuid  教师UUID，可选参数
     * @param semesterUuid 学期UUID，可选参数
     * @return 包含教师课程偏好列表分页数据的响应实体
     */
    @RequestRole({"管理员", "教务"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<TeacherPreferencesDTO>>> getTeacherPreferencesPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "teacher_uuid", required = false) String teacherUuid,
            @RequestParam(value = "semester_uuid", required = false) String semesterUuid
    ) {
        if (size > 200) {
            throw new BusinessException("单页查询不允许超过 200", ErrorCode.PARAMETER_INVALID);
        }
        PageDTO<TeacherPreferencesDTO> preferencesPage = teacherPreferencesService.getTeacherPreferencesPage(
                page, size, isDesc, teacherUuid, semesterUuid
        );
        return ResultUtil.success("获取教师课程偏好列表成功", preferencesPage);
    }

    /**
     * 获取当前登录教师的课程偏好列表分页数据
     * <p>
     * 该方法用于获取当前登录教师的课程偏好信息的分页数据。支持通过学期UUID进行过滤，并允许指定分页参数。
     * 该方法会自动获取当前登录用户的教师信息，并只返回该教师的课程偏好数据。
     * </p>
     *
     * @param page         分页的页码，从1开始，默认值为1
     * @param size         每页显示的数据条数，默认值为20，最大值为200
     * @param isDesc       是否按降序排序，默认值为true
     * @param semesterUuid 学期UUID，可选参数
     * @param request      HTTP请求对象，用于获取当前登录用户信息
     * @return 包含教师课程偏好列表分页数据的响应实体
     */
    @RequestRole({"教师"})
    @GetMapping("/page/me")
    public ResponseEntity<BaseResponse<PageDTO<TeacherPreferencesDTO>>> getMyTeacherPreferencesPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "semester_uuid", required = false) String semesterUuid,
            HttpServletRequest request
    ) {
        if (size > 200) {
            throw new BusinessException("单页查询不允许超过 200", ErrorCode.PARAMETER_INVALID);
        }

        // 获取当前登录用户信息
        UserDO userDO = userLogic.getUserByRequest(request);
        // 获取教师信息
        TeacherDO teacherDO = teacherDAO.getTeacherByUserUuid(userDO.getUserUuid());
        if (teacherDO == null) {
            throw new BusinessException(StringConstant.TEACHER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }

        PageDTO<TeacherPreferencesDTO> preferencesPage = teacherPreferencesService.getTeacherPreferencesPage(
                page, size, isDesc, teacherDO.getTeacherUuid(), semesterUuid
        );
        return ResultUtil.success("获取个人课程偏好列表成功", preferencesPage);
    }

    /**
     * 获取教师在指定学期的课程偏好列表
     * <p>
     * 该方法用于获取特定教师在指定学期的所有课程偏好设置。结果会按照星期和时间段排序。
     * </p>
     *
     * @param teacherUuid  教师UUID
     * @param semesterUuid 学期UUID
     * @return 包含教师课程偏好列表的响应实体
     */
    @RequestRole({"管理员", "教务"})
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<TeacherPreferencesDTO>>> getTeacherPreferencesList(
            @RequestParam("teacher_uuid") String teacherUuid,
            @RequestParam("semester_uuid") String semesterUuid
    ) {
        if (teacherUuid == null || teacherUuid.isBlank() || semesterUuid == null || semesterUuid.isBlank()) {
            throw new BusinessException("教师UUID和学期UUID不能为空", ErrorCode.PARAMETER_INVALID);
        }
        List<TeacherPreferencesDTO> preferencesList = teacherPreferencesService.getTeacherPreferencesList(teacherUuid, semesterUuid);
        return ResultUtil.success(StringConstant.OPERATE_SUCCESS, preferencesList);
    }

    /**
     * 获取单个教师课程偏好信息
     * <p>
     * 该方法用于根据教师课程偏好的 UUID 获取偏好的详细信息。如果提供的偏好 UUID 不符合 UUID 格式，
     * 将抛出 {@code BusinessException} 异常，并返回相应的错误信息。如果偏好不存在，
     * 也将抛出 {@code BusinessException} 异常。成功获取后，将返回包含偏好详细信息的响应。
     * </p>
     *
     * @param preferenceUuid 教师课程偏好的唯一标识符，必须符合 UUID 格式（不含中划线）
     * @return 包含教师课程偏好详细信息的 ResponseEntity 对象
     */
    @RequestLogin
    @GetMapping("/{preference_uuid}")
    public ResponseEntity<BaseResponse<TeacherPreferencesDTO>> getTeacherPreferences(
            @PathVariable("preference_uuid") String preferenceUuid
    ) {
        if (!preferenceUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.TEACHER_PREFERENCES_UUID_ILLEGAL, ErrorCode.PARAMETER_INVALID);
        }
        TeacherPreferencesDTO preferencesDTO = Optional.ofNullable(teacherPreferencesService.getTeacherPreferencesByUuid(preferenceUuid))
                .orElseThrow(() -> new BusinessException(StringConstant.TEACHER_PREFERENCES_NOT_EXIST, ErrorCode.NOT_EXIST));
        return ResultUtil.success(StringConstant.OPERATE_SUCCESS, preferencesDTO);
    }

    /**
     * 添加教师课程偏好
     * <p>
     * 该方法用于根据传入的 {@code TeacherPreferencesVO} 对象添加一个新的教师课程偏好。在添加过程中，会进行一系列数据可用性检查，
     * 确保教师和学期存在，以及时间段合法。如果所有数据验证通过，则调用服务层的方法将新的偏好信息保存到数据库中，
     * 并返回包含成功信息及新偏好详情的响应。
     * </p>
     *
     * @param teacherPreferencesVO 包含待添加教师课程偏好详细信息的视图对象
     * @return 响应实体，包含操作结果和新创建的教师课程偏好信息
     */
    @RequestRole({"教师"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<TeacherPreferencesDTO>> addTeacherPreferences(
            @RequestBody @Validated TeacherPreferencesVO teacherPreferencesVO
    ) {
        TeacherPreferencesDTO preferencesDTO = teacherPreferencesService.addTeacherPreferences(teacherPreferencesVO);
        return ResultUtil.success(StringConstant.OPERATE_SUCCESS, preferencesDTO);
    }

    /**
     * 编辑教师课程偏好
     * <p>
     * 该方法用于更新现有的教师课程偏好记录。在更新之前，会验证教师和学期是否存在，以及时间段是否合法。
     * 如果验证通过，则更新偏好记录并返回更新后的详细信息。
     * </p>
     *
     * @param preferenceUuid       教师课程偏好的唯一标识符
     * @param teacherPreferencesVO 包含要更新的教师课程偏好信息的视图对象
     * @return 响应实体，包含操作结果和更新后的教师课程偏好信息
     */
    @RequestRole({"教师"})
    @PutMapping("/{preference_uuid}")
    public ResponseEntity<BaseResponse<TeacherPreferencesDTO>> editTeacherPreferences(
            @NotNull @PathVariable("preference_uuid") String preferenceUuid,
            @NotNull @RequestBody @Validated TeacherPreferencesVO teacherPreferencesVO,
            @NotNull HttpServletRequest request
    ) {
        if (!preferenceUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.TEACHER_PREFERENCES_UUID_ILLEGAL, ErrorCode.PARAMETER_INVALID);
        }
        TeacherPreferencesDTO preferencesDTO = teacherPreferencesService.editTeacherPreferences(preferenceUuid, teacherPreferencesVO, request);
        return ResultUtil.success(StringConstant.OPERATE_SUCCESS, preferencesDTO);
    }

    /**
     * 删除教师课程偏好
     * <p>
     * 该方法用于删除指定的教师课程偏好记录。在删除之前，会验证偏好记录是否存在。
     * 如果验证通过，则删除该偏好记录。
     * </p>
     *
     * @param preferenceUuid 教师课程偏好的唯一标识符
     * @return 响应实体，包含操作结果
     */
    @RequestRole({"教师"})
    @DeleteMapping("/{preference_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteTeacherPreferences(
            @NotNull @PathVariable("preference_uuid") String preferenceUuid,
            @NotNull HttpServletRequest request
    ) {
        if (!preferenceUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.TEACHER_PREFERENCES_UUID_ILLEGAL, ErrorCode.PARAMETER_INVALID);
        }
        teacherPreferencesService.deleteTeacherPreferences(preferenceUuid, request);
        return ResultUtil.success(StringConstant.OPERATE_SUCCESS);
    }
}
