package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherTypeDTO;
import com.frontleaves.scheduling.services.TeacherTypeService;
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
 * 教师类型控制器
 * <p>
 * 该类提供了处理教师类型相关请求的 RESTful API，包括获取单个教师类型、
 * 获取教师类型分页列表、获取教师类型简洁列表等功能。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/teacher-type")
public class TeacherTypeController {

    private final TeacherTypeService teacherTypeService;

    /**
     * 根据UUID获取教师类型信息
     *
     * @param teacherTypeUuid 教师类型的唯一标识符（UUID）
     * @return 返回包含教师类型信息的响应实体
     *
     * 此方法首先会检查传入的教师类型UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 表示参数错误。接着，调用教师类型服务的getTeacherType方法获取教师类型信息。
     * 最后，使用ResultUtil工具类生成包含"查询成功"消息和教师类型信息的响应实体返回。
     */
    @RequestRole({"教务", "管理员"})
    @GetMapping("/{teacher_type_uuid}")
    public ResponseEntity<BaseResponse<TeacherTypeDTO>> getTeacherType(
            @PathVariable("teacher_type_uuid") String teacherTypeUuid
    ) {
        if (teacherTypeUuid == null || teacherTypeUuid.isBlank()) {
            throw new BusinessException(StringConstant.TEACHER_TYPE_UUID_NOT_EMPTY, ErrorCode.PARAMETER_ERROR);
        }
        TeacherTypeDTO teacherTypeDTO = teacherTypeService.getTeacherType(teacherTypeUuid);
        return ResultUtil.success("查询成功", teacherTypeDTO);
    }

    /**
     * 获取教师类型分页列表接口
     * 该接口允许用户根据分页参数和筛选条件获取教师类型信息分页列表
     *
     * @param page        页码，默认为1，表示获取第一页数据
     * @param size        每页记录数，默认为20，表示每页获取20条记录
     * @param isDesc      是否降序，默认为true，表示结果按照降序排列
     * @param name        教师类型名称，可选参数，如果提供，则按名称筛选教师类型
     * @return            返回包含教师类型列表的PageDTO对象，封装在BaseResponse中
     */
    @RequestRole({"管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<TeacherTypeDTO>>> getTeacherTypePage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "name", required = false) String name
    ) {
        // 调用服务层方法获取教师类型分页列表
        PageDTO<TeacherTypeDTO> teacherTypePage = teacherTypeService.getTeacherTypePage(page, size, isDesc, name);
        // 使用ResultUtil工具类封装成功响应并返回
        return ResultUtil.success("查询教师类型列表成功", teacherTypePage);
    }

    /**
     * 获取所有教师类型简洁列表接口
     * 该接口提供所有教师类型的简洁列表，常用于下拉选择等场景
     *
     * @return 返回包含所有教师类型的列表，封装在BaseResponse中
     */
    @RequestRole({"教务", "管理员"})
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<TeacherTypeDTO>>> getTeacherTypeList() {
        // 调用服务层方法获取所有教师类型列表
        List<TeacherTypeDTO> teacherTypeList = teacherTypeService.getTeacherTypeList();
        // 使用ResultUtil工具类封装成功响应并返回
        return ResultUtil.success("查询教师类型列表成功", teacherTypeList);
    }

    /**
     * 添加教师类型接口
     * 该接口允许用户添加新的教师类型
     *
     * @param typeName 教师类型名称，必填
     * @param typeEnglishName 教师类型英文名称，必填
     * @param typeDesc 教师类型描述，可选
     * @return 返回添加成功的教师类型信息，封装在BaseResponse中
     */
    @RequestRole({"管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<TeacherTypeDTO>> addTeacherType(
            @RequestParam("type_name") String typeName,
            @RequestParam("type_english_name") String typeEnglishName,
            @RequestParam(value = "type_desc", required = false) String typeDesc
    ) {
        TeacherTypeDTO teacherTypeDTO = teacherTypeService.addTeacherType(typeName, typeEnglishName, typeDesc);
        return ResultUtil.success("教师类型添加成功", teacherTypeDTO);
    }

    /**
     * 更新教师类型接口
     * 该接口允许用户更新现有教师类型的信息
     *
     * @param teacherTypeUuid 教师类型的唯一标识符（UUID）
     * @param typeName 教师类型名称，可选
     * @param typeEnglishName 教师类型英文名称，可选
     * @param typeDesc 教师类型描述，可选
     * @return 返回更新后的教师类型信息，封装在BaseResponse中
     */
    @RequestRole({"管理员"})
    @PutMapping("/{teacher_type_uuid}")
    public ResponseEntity<BaseResponse<TeacherTypeDTO>> updateTeacherType(
            @PathVariable("teacher_type_uuid") String teacherTypeUuid,
            @RequestParam(value = "type_name", required = false) String typeName,
            @RequestParam(value = "type_english_name", required = false) String typeEnglishName,
            @RequestParam(value = "type_desc", required = false) String typeDesc
    ) {
        if (teacherTypeUuid == null || teacherTypeUuid.isBlank()) {
            throw new BusinessException("教师类型UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }

        TeacherTypeDTO teacherTypeDTO = teacherTypeService.updateTeacherType(teacherTypeUuid, typeName, typeEnglishName, typeDesc);
        return ResultUtil.success("教师类型更新成功", teacherTypeDTO);
    }

    /**
     * 删除教师类型接口
     * 该接口允许用户删除指定的教师类型
     *
     * @param teacherTypeUuid 教师类型的唯一标识符（UUID）
     * @return 返回删除结果，封装在BaseResponse中
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{teacher_type_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteTeacherType(
            @PathVariable("teacher_type_uuid") String teacherTypeUuid
    ) {
        if (teacherTypeUuid == null || teacherTypeUuid.isBlank()) {
            throw new BusinessException("教师类型UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }

        teacherTypeService.deleteTeacherType(teacherTypeUuid);
        return ResultUtil.success("教师类型删除成功");
    }
}
