package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.xlf.utility.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 学生管理控制器
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
        return null;
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
    public ResponseEntity<BaseResponse<StudentDTO>> addStudent(
            @RequestBody StudentVO studentVO
    ) {
        return null;
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
