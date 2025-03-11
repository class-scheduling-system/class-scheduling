package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

/**
 * 学生控制器
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/student")
public class StudentController {

    private final StudentService studentService;
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
        if (!Pattern.matches(StringConstant.Regular.UUID_REGULAR_EXPRESSION, studentUuid)){
            return ResultUtil.error(ErrorCode.PARAMETER_INVALID, "UUID格式错误", null);
        }
        // 查询学生信息
        StudentDTO studentDTO = studentService.getStudentByUuid(studentUuid);
        if (studentDTO == null) {
            return ResultUtil.error(ErrorCode.NOT_EXIST, "学生不存在", null);
        }
        return ResultUtil.success("获取学生信息成功", studentDTO);
    }


    /**
     * 查看学生列表
     */
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<StudentDTO>> getStudentList(
            @RequestParam(value = "page") int page,
            @RequestParam(value = "size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "class") String clazz,
            @RequestParam(value = "status", required = false) Boolean status,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "id") String id
    ) {
        return null;
    }

    /**
     * 添加学生
     */
    @PostMapping("")
    public @NotNull ResponseEntity<BaseResponse<Void>> addStudent(
            @RequestBody StudentVO studentVO
    ) {
        // 校检学生数据是否合法
        if (!Pattern.matches(StringConstant.Regular.UUID_REGULAR_EXPRESSION, studentVO.getId())) {
            return ResultUtil.error(ErrorCode.PARAMETER_INVALID, "学生ID格式错误", null);
        }
        if (!Pattern.matches(StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION, studentVO.getName())) {
            return ResultUtil.error(ErrorCode.PARAMETER_INVALID, "学生姓名格式错误", null);
        }
        // 调用逻辑层添加学生
        studentService.addStudent(studentVO);

        return ResultUtil.success("添加学生成功");
    }

    /**
     * 停用学生
     */
    @PutMapping("/disable/{student_uuid}")
    public ResponseEntity<BaseResponse<Void>> disableStudent(
            @PathVariable("student_uuid") String studentUuid,
            @RequestParam("disable") Boolean disable
    ) {
        return null;
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/{student_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteStudent(
            @PathVariable("student_uuid") String studentUuid
    ) {
        return null;
    }

    /**
     * 编辑学生
     */
    @PutMapping("/{student_uuid}")
    public ResponseEntity<BaseResponse<StudentDTO>> editStudent(
            @PathVariable("student_uuid") String studentUuid,
            @RequestBody StudentVO studentVO
    ) {
        return null;
    }
}
