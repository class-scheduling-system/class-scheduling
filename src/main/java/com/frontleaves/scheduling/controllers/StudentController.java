package com.frontleaves.scheduling.controllers;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.vo.BatchAddStudentVO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.services.BuildingService;
import com.frontleaves.scheduling.services.StudentService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 学生控制器
 *
 * @author FLASHLACK | fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;
    private final BuildingService buildingService;

    /**
     * 查看学生
     *
     * @param studentUuid 学生的唯一标识符
     * @return 返回一个包含学生信息的ResponseEntity对象
     */
    @GetMapping("/{student_uuid}")
    public ResponseEntity<BaseResponse<StudentDTO>> getStudentInfo(
            @PathVariable("student_uuid") String studentUuid
    ) {
        // 校检UUID是否符合格式
        if (!Pattern.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, studentUuid)){
            throw new BusinessException("学生UUID格式错误", ErrorCode.PARAMETER_ERROR);
        }
        // 查询学生信息
        StudentDTO studentDTO = studentService.getStudentByUuid(studentUuid);
        if (studentDTO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }
        return ResultUtil.success("获取学生信息成功", studentDTO);
    }

    /**
     * 查看学生列表
     * <p>
     * 该接口用于分页查询学生信息,可以根据班级、姓名、学号等条件进行筛选
     * </p>
     *
     * @param page        页码,从1开始
     * @param size        每页大小
     * @param isDesc      是否降序排列,默认为true
     * @param clazz       班级名称
     * @param isGraduated 是否毕业
     * @param name        学生姓名
     * @param id          学生学号
     * @return 返回包含学生信息列表的响应实体
     */
    @GetMapping("/list")
    public @NotNull ResponseEntity<BaseResponse<PageDTO<StudentDTO>>> getStudentList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "class", required = false) String clazz,
            @RequestParam(value = "is_graduated", required = false, defaultValue = "false") Boolean isGraduated,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "id", required = false) String id
    ) {
        PageDTO<StudentDTO> result = studentService.getStudentList(page, size, isDesc, clazz, isGraduated, name, id);
        return ResultUtil.success("查询成功", result);
    }

    /**
     * 添加学生
     * <p>
     * 此方法通过POST请求接收学生信息,将其转换为领域对象(DO),然后调用Service层方法进行保存
     * 保存成功后,将领域对象转换为DTO并返回,以告知前端操作成功
     * </p>
     *
     * @param studentVO 学生视图对象,包含从前端传入的学生信息
     * @return 返回包含学生信息的响应实体,包括操作结果和学生DTO
     */
    @PostMapping("")
    public @NotNull ResponseEntity<BaseResponse<StudentDTO>> addStudent(
            @Valid @RequestBody StudentVO studentVO
    ) {
        // VO -> DTO
        StudentDTO studentDTO = BeanUtil.toBean(studentVO, StudentDTO.class);

        // 调用 Service 层处理
        StudentDTO createdStudent = studentService.addStudent(studentDTO);

        return ResultUtil.success("添加学生成功", createdStudent);
    }

    /**
     * 停用学生
     *
     * @param studentUuid 学生的唯一标识符
     * @param disable 表示是否禁用学生账户,true为禁用,false为启用
     * @return 返回一个包含禁用或启用结果的ResponseEntity对象
     */
    @PutMapping("/disable/{student_uuid}")
    public ResponseEntity<BaseResponse<StudentDisableDTO>> disableStudent(
            @PathVariable("student_uuid") String studentUuid,
            @Valid @RequestParam(defaultValue = "true") Boolean disable
    ) {
        // 校检UUID是否为空
        if (studentUuid == null || studentUuid.isBlank()) {
            throw new BusinessException("学生UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        StudentDisableDTO studentDisableDTO = studentService.disableStudent(studentUuid, disable);

        // 根据disable值动态返回不同的信息
        String message = disable ? "停用学生成功" : "启用学生成功";
        return ResultUtil.success(message, studentDisableDTO);
    }

    /**
     * 删除学生
     * 该接口接收一个学生UUID作为路径变量,通过DELETE请求调用,用于删除系统中的学生信息
     *
     * @param studentUuid 学生的唯一标识符（UUID）,通过URL路径传递
     * @return 返回一个包含成功消息的响应实体,表示学生删除成功
     */
    @DeleteMapping("/{student_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteStudent(
            @PathVariable("student_uuid") String studentUuid
    ) {
        studentService.deleteStudent(studentUuid);
        return ResultUtil.success("删除学生成功");
    }

    /**
     * 编辑学生
     * <p>
     * 该方法通过PUT请求接收一个学生UUID和新的学生信息,然后更新对应学生的信息
     * 如果学生不存在,返回错误信息；否则,返回更新后的学生信息
     * </p>
     *
     * @param studentUuid 学生的唯一标识符UUID,用于定位需要编辑的学生
     * @param studentVO 包含新的学生信息的实体对象,用于更新学生数据
     * @return 返回一个包含执行结果和学生数据的响应实体
     */
    @PutMapping("/{student_uuid}")
    public @NotNull ResponseEntity<BaseResponse<StudentDTO>> editStudent(
            @PathVariable("student_uuid") String studentUuid,
            @Valid @RequestBody StudentVO studentVO
    ) {
        StudentDTO studentDTO = studentService.editStudent(studentUuid, studentVO);

        // 检查编辑结果,如果返回null,则表示学生不存在
        if (studentDTO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }
        return ResultUtil.success("编辑学生成功", studentDTO);
    }

    /**
     * 获取学生导入模板
     * 该方法返回一个包含示例数据的响应实体，客户端可以通过下载链接下载该文件
     *
     * @return 返回一个包含示例数据的响应实体
     */
    @RequestRole("教务")
    @GetMapping("/get-example")
    public ResponseEntity<byte[]> getExample(
            @NotNull HttpServletRequest request
    ) {
        PrepareStudentExampleDTO prepareStudentExampleDTO = studentService.prepareDepartmentData(request);
        byte[] bytes = studentService.getExample(prepareStudentExampleDTO);

        HttpHeaders headers = Optional.of(new HttpHeaders())
                .map(header -> {
                    header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    header.setContentLength(bytes.length);
                    String fileName = URLEncoder.encode("学生导入模板.xlsx", StandardCharsets.UTF_8)
                            .replace("+", "%20");
                    header.add(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
                    return header;
                })
                .orElseThrow(() -> new BusinessException("获取响应头失败", ErrorCode.SERVER_INTERNAL_ERROR));

        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * 处理批量导入学生的请求
     * 该方法首先验证和检查批量添加学生的数据，然后调用服务层方法进行导入
     *
     * @param batchAddStudentVO 包含批量添加学生信息的请求体
     * @return 返回一个包含添加结果的响应实体
     */
    @PostMapping("/batch-import")
    public ResponseEntity<BaseResponse<BackAddStudentDTO>> batchImport(
            @RequestBody @Validated BatchAddStudentVO batchAddStudentVO,
            @NotNull HttpServletRequest request
    ) {
        byte[] file = studentService.verifyStudentBatchAndBackFile(batchAddStudentVO);
        String departmentUuid = studentService.getDepartmentUuid(request);
        // 执行批量导入学生的操作
        BackAddStudentDTO backAddStudentDTO = Optional.ofNullable(batchAddStudentVO.getIgnoreError())
                .filter(Boolean.TRUE::equals)
                .map(ignoreError -> studentService.batchImportIgnoreError(file, departmentUuid))
                .orElseGet(() -> studentService.batchImportNoIgnoreError(file, departmentUuid));

        if (backAddStudentDTO.getFailedCount() > 0) {
            // 如果有学生导入失败，返回带有错误信息的响应
            return ResultUtil.success("存在添加失败的学生情况", backAddStudentDTO);
        }
        // 返回批量添加学生成功的响应
        return ResultUtil.success("批量添加学生成功", backAddStudentDTO);
    }
}
