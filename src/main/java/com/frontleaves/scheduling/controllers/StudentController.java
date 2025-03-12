package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.dto.StudentDisableDTO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
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
        if (!Pattern.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, studentUuid)){
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
     * <p>
     * 该接口用于分页查询学生信息，可以根据班级、姓名、学号等条件进行筛选
     * </p>
     *
     * @param page        页码，从1开始
     * @param size        每页大小
     * @param isDesc      是否降序排列，默认为true
     * @param clazz       班级名称
     * @param isGraduated 是否毕业
     * @param name        学生姓名
     * @param id          学生学号
     * @return 返回包含学生信息列表的响应实体
     */
    @GetMapping("/list")
    public @NotNull ResponseEntity<BaseResponse<PageDTO<StudentDTO>>> getStudentList(
            @RequestParam(value = "page") int page,
            @RequestParam(value = "size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "class") String clazz,
            @RequestParam(value = "is_graduated", required = false) Boolean isGraduated,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "id") String id
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
     * @param studentVO 学生视图对象，包含从前端传入的学生信息
     * @return 返回包含学生信息的响应实体，包括操作结果和学生DTO
     */
    @PostMapping("")
    public @NotNull ResponseEntity<BaseResponse<StudentDTO>> addStudent(
            @RequestBody StudentVO studentVO
    ) {
        if (studentVO == null) {
            return ResultUtil.error(ErrorCode.PARAMETER_INVALID, "请求体不能为空", null);
        }

        // VO -> DO
        StudentDO studentDO = new StudentDO();
        BeanUtils.copyProperties(studentVO, studentDO);
        // 调用 Service 层方法添加学生
        StudentDO createdStudent = studentService.addStudent(studentDO);

        // DO -> DTO
        StudentDTO studentDTO = new StudentDTO();
        BeanUtils.copyProperties(createdStudent, studentDTO);

        return ResultUtil.success("添加学生成功", studentDTO);
    }

    /**
     * 停用学生
     *
     * @param studentUuid 学生的唯一标识符
     * @param disable 表示是否禁用学生账户，true为禁用，false为启用
     * @return 返回一个包含禁用或启用结果的ResponseEntity对象
     */
    @PutMapping("/disable/{student_uuid}")
    public ResponseEntity<BaseResponse<StudentDisableDTO>> disableStudent(
            @PathVariable("student_uuid") String studentUuid,
            @RequestParam("disable") Boolean disable
    ) {
        StudentDisableDTO dto = studentService.disableStudent(studentUuid, disable);
        return ResultUtil.success("停用学生成功", dto);

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
     * <p>
     * 该方法通过PUT请求接收一个学生UUID和新的学生信息，然后更新对应学生的信息
     * 如果学生不存在，返回错误信息；否则，返回更新后的学生信息
     * </p>
     *
     * @param studentUuid 学生的唯一标识符UUID，用于定位需要编辑的学生
     * @param studentVO 包含新的学生信息的实体对象，用于更新学生数据
     * @return 返回一个包含执行结果和学生数据的响应实体
     */
    @PutMapping("/{student_uuid}")
    public @NotNull ResponseEntity<BaseResponse<StudentDTO>> editStudent(
            @PathVariable("student_uuid") String studentUuid,
            @RequestBody StudentVO studentVO
    ) {
        StudentDTO studentDTO = studentService.editStudent(studentUuid, studentVO);

        // 检查编辑结果，如果返回null，则表示学生不存在
        if (studentDTO == null) {
            return ResultUtil.error(ErrorCode.NOT_EXIST, "学生不存在", null);
        }
        return ResultUtil.success("编辑学生成功", studentDTO);
    }
}
