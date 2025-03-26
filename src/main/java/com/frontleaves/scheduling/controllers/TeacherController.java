package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherDTO;
import com.frontleaves.scheduling.models.dto.TeacherDisableDTO;
import com.frontleaves.scheduling.models.dto.TeacherLiteDTO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import com.frontleaves.scheduling.services.TeacherService;
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
 * 教师控制器
 * <p>
 * 该类提供了处理教师相关请求的 RESTful API，包括获取教师列表等功能。
 *
 * @author qiyu
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher")

public class TeacherController {

    private final TeacherService teacherService;

    /**
     * 添加新教师的接口
     * 该方法通过POST请求接收教师信息，并进行验证和处理，返回添加成功的响应
     *
     * @param teacherVO 教师添加请求对象，包含需要验证的教师信息
     * @return 返回包含成功消息的响应实体
     */
    @RequestRole({"教务"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<Void>> addTeacher(
            @RequestBody @Validated TeacherVO teacherVO
    ) {
        teacherService.addTeacher(teacherVO);
        return ResultUtil.success("教师添加成功");

    }

    /**
     * 根据教师UUID获取教师信息
     *
     * @param teacherUuid 教师的唯一标识符（UUID）
     * @return 返回包含教师信息的响应实体
     * <p>
     * 此方法首先会检查传入的教师UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 表示参数错误接着，调用教师服务的getTeacher方法获取教师信息最后，使用ResultUtil
     * 工具类生成包含“查询成功”消息和教师信息的响应实体返回
     */
    @RequestRole({"教务"})
    @GetMapping("/{teacher_uuid}")
    public ResponseEntity<BaseResponse<TeacherDTO>> getTeacher(
            @PathVariable("teacher_uuid") String teacherUuid
    ) {
        String getUuid = Optional.ofNullable(teacherUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        TeacherDTO teacherDTO = teacherService.getTeacher(getUuid);
        return ResultUtil.success("查询成功", teacherDTO);
    }

    /**
     * 获取教师列表接口
     * 该接口允许用户根据分页参数和筛选条件获取教师信息列表
     *
     * @param page       页码，默认为1，表示获取第一页数据
     * @param size       每页记录数，默认为20，表示每页获取20条记录
     * @param isDesc     是否降序，默认为true，表示结果按照降序排列
     * @param department 部门名称，可选参数，如果提供，则按部门筛选教师
     * @param status     状态，可选参数，如果提供，则按状态筛选教师
     * @param name       教师姓名，可选参数，如果提供，则按姓名筛选教师
     * @return 返回包含教师列表的PageDTO对象，封装在BaseResponse中
     */
    @RequestRole({"管理员", "教务"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<TeacherDTO>>> getTeacherList(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "name", required = false) String name
    ) {
        // 调用服务层方法获取教师列表
        PageDTO<TeacherDTO> teacherList = teacherService.getTeacherList(page, size, isDesc, department, status, name);
        // 使用ResultUtil工具类封装成功响应并返回
        return ResultUtil.success("查询教师列表成功", teacherList);
    }

    /**
     * 禁用教师接口
     * 该接口允许用户禁用或启用指定的教师
     *
     * @param teacherUuid 教师的唯一标识符（UUID）
     * @param disable     是否禁用教师，true表示禁用，false表示启用
     * @return 返回包含禁用操作结果的响应实体
     */
    @RequestRole({"教务"})
    @PutMapping("/disable/{teacher_uuid}")
    public ResponseEntity<BaseResponse<TeacherDisableDTO>> disableTeacher(
            @PathVariable("teacher_uuid") String teacherUuid,
            @RequestParam(defaultValue = "true") Boolean disable
    ) {
        String getUuid = Optional.ofNullable(teacherUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        TeacherDisableDTO teacherDisableDTO = teacherService.disableTeacher(getUuid, disable);
        return ResultUtil.success("禁用教师成功", teacherDisableDTO);
    }

    /**
     * 删除教师接口
     * 该接口允许用户删除指定的教师记录
     *
     * @param teacherUuid 教师的唯一标识符（UUID）
     * @return 返回包含删除操作结果的响应实体
     */
    @RequestRole({"教务"})
    @DeleteMapping("/{teacher_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteTeacher(
            @PathVariable("teacher_uuid") String teacherUuid
    ) {
        String getUuid = Optional.ofNullable(teacherUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        teacherService.deleteTeacher(getUuid);
        return ResultUtil.success("教师记录已删除");
    }

    /**
     * 更新教师信息接口
     * 该接口允许用户更新指定教师的详细信息
     *
     * @param teacherUuid 教师的唯一标识符（UUID）
     * @param teacherVO   教师更新请求对象，包含需要验证的教师信息
     * @return 返回包含更新操作结果的响应实体
     */
    @RequestRole({"教务"})
    @PutMapping("/{teacher_uuid}")
    public ResponseEntity<BaseResponse<Void>> updateTeacher(
            @PathVariable("teacher_uuid") String teacherUuid,
            @RequestBody @Validated TeacherVO teacherVO
    ) {
        String getUuid = Optional.ofNullable(teacherUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        teacherService.updateTeacher(getUuid, teacherVO);
        return ResultUtil.success("教师信息已更新");
    }

    /**
     * 获取教师简单列表接口
     * 该接口返回教师的基本信息列表，包括UUID、姓名、部门和类型。
     * 支持按部门和教师类型进行筛选。
     *
     * @param departmentUuid  部门UUID，可选参数
     * @param teacherTypeUuid 教师类型UUID，可选参数
     * @return 返回包含教师简单信息列表的响应实体
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<TeacherLiteDTO>>> getTeacherLiteList(
            @RequestParam(value = "department_uuid", required = false) String departmentUuid,
            @RequestParam(value = "teacher_type_uuid", required = false) String teacherTypeUuid
    ) {
        // 验证部门UUID格式（如果提供）
        if (departmentUuid != null && !departmentUuid.isBlank() && !departmentUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.DEPARTMENT_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }

        // 验证教师类型UUID格式（如果提供）
        if (teacherTypeUuid != null && !teacherTypeUuid.isBlank() && !teacherTypeUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException(StringConstant.TEACHER_TYPE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
        }

        List<TeacherLiteDTO> teacherList = teacherService.getTeacherLiteList(departmentUuid, teacherTypeUuid);
        return ResultUtil.success("查询教师列表成功", teacherList);
    }
}
