package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.ClassAssignmentDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.vo.ClassAssignmentVO;
import com.frontleaves.scheduling.services.ClassAssignmentService;
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
 * 排课分配控制器
 * <p>
 * 该类提供了处理排课分配相关请求的 RESTful API，包括新增、修改、删除、查询等功能。
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/class-assignments")
public class ClassAssignmentController {

    private final ClassAssignmentService classAssignmentService;

    /**
     * 添加新排课分配的接口
     * 该方法通过POST请求接收排课分配信息，并进行验证和处理
     *
     * @param vo 排课分配请求对象，包含需要验证的信息
     * @return 返回包含成功消息的响应实体
     */
    @RequestRole({"教务"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<Void>> add(
            @RequestBody @Validated ClassAssignmentVO vo
    ) {
        classAssignmentService.add(vo);
        return ResultUtil.success("排课分配添加成功");
    }

    /**
     * 根据UUID获取排课分配信息
     *
     * @param classAssignmentUuid 排课分配的唯一标识符（UUID）
     * @return 返回包含排课分配信息的响应实体
     */
    @RequestRole({"教务"})
    @GetMapping("/{class_assignment_uuid}")
    public ResponseEntity<BaseResponse<ClassAssignmentDTO>> getById(
            @PathVariable("class_assignment_uuid") String classAssignmentUuid
    ) {
        String getUuid = Optional.ofNullable(classAssignmentUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        ClassAssignmentDTO dto = classAssignmentService.getById(getUuid);
        return ResultUtil.success("查询成功", dto);
    }

    /**
     * 获取排课分配分页列表接口
     * 该接口允许用户根据分页参数和筛选条件获取排课分配信息列表
     *
     * @param page         页码，默认为1
     * @param size         每页记录数，默认为20
     * @param semesterUuid 学期UUID，可选参数
     * @param courseUuid   课程UUID，可选参数
     * @param teacherUuid  教师UUID，可选参数
     * @return 返回包含排课分配列表的分页数据
     */
    @RequestRole({"教务"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<ClassAssignmentDTO>>> page(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "semester_uuid", required = false) String semesterUuid,
            @RequestParam(value = "course_uuid", required = false) String courseUuid,
            @RequestParam(value = "teacher_uuid", required = false) String teacherUuid
    ) {
        if (size > 200) {
            throw new BusinessException(StringConstant.ErrorMessage.PAGE_SIZE_TOO_LARGE, ErrorCode.PARAMETER_INVALID);
        }

        // 验证UUID格式（如果提供）
        String getSemesterUuid = Optional.ofNullable(semesterUuid)
                .filter(uuid -> !uuid.isBlank())
                .map(uuid -> {
                    if (!uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
                        throw new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
                    }
                    return uuid;
                })
                .orElse(null);

        String getCourseUuid = Optional.ofNullable(courseUuid)
                .filter(uuid -> !uuid.isBlank())
                .map(uuid -> {
                    if (!uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
                        throw new BusinessException(StringConstant.ErrorMessage.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
                    }
                    return uuid;
                })
                .orElse(null);

        String getTeacherUuid = Optional.ofNullable(teacherUuid)
                .filter(uuid -> !uuid.isBlank())
                .map(uuid -> {
                    if (!uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
                        throw new BusinessException(StringConstant.ErrorMessage.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
                    }
                    return uuid;
                })
                .orElse(null);

        PageDTO<ClassAssignmentDTO> pageResult = classAssignmentService.page(page, size, getSemesterUuid, getCourseUuid, getTeacherUuid);
        return ResultUtil.success("查询排课分配列表成功", pageResult);
    }

    /**
     * 删除排课分配接口
     *
     * @param classAssignmentUuid 排课分配的唯一标识符（UUID）
     * @return 返回包含删除操作结果的响应实体
     */
    @RequestRole({"教务"})
    @DeleteMapping("/{class_assignment_uuid}")
    public ResponseEntity<BaseResponse<Void>> delete(
            @PathVariable("class_assignment_uuid") String classAssignmentUuid
    ) {
        String getUuid = Optional.ofNullable(classAssignmentUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        classAssignmentService.delete(getUuid);
        return ResultUtil.success("排课分配记录已删除");
    }

    /**
     * 更新排课分配信息接口
     *
     * @param classAssignmentUuid 排课分配的唯一标识符（UUID）
     * @param vo                  排课分配更新请求对象
     * @return 返回包含更新操作结果的响应实体
     */
    @RequestRole({"教务"})
    @PutMapping("/{class_assignment_uuid}")
    public ResponseEntity<BaseResponse<Void>> update(
            @PathVariable("class_assignment_uuid") String classAssignmentUuid,
            @RequestBody @Validated ClassAssignmentVO vo
    ) {
        String getUuid = Optional.ofNullable(classAssignmentUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.ErrorMessage.CLASS_ASSIGNMENT_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        classAssignmentService.update(getUuid, vo);
        return ResultUtil.success("排课分配信息已更新");
    }

    /**
     * 获取排课分配列表接口
     * 该接口返回排课分配的列表信息，支持按学期、课程和教师进行筛选
     *
     * @param semesterUuid 学期UUID，可选参数
     * @param courseUuid   课程UUID，可选参数
     * @param teacherUuid  教师UUID，可选参数
     * @return 返回包含排课分配列表的响应实体
     */
    @RequestRole({"教务"})
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<ClassAssignmentDTO>>> list(
            @RequestParam(value = "semester_uuid", required = false) String semesterUuid,
            @RequestParam(value = "course_uuid", required = false) String courseUuid,
            @RequestParam(value = "teacher_uuid", required = false) String teacherUuid
    ) {
        // 验证UUID格式（如果提供）
        String getSemesterUuid = Optional.ofNullable(semesterUuid)
                .filter(uuid -> !uuid.isBlank())
                .map(uuid -> {
                    if (!uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
                        throw new BusinessException(StringConstant.ErrorMessage.SEMESTER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
                    }
                    return uuid;
                })
                .orElse(null);

        String getCourseUuid = Optional.ofNullable(courseUuid)
                .filter(uuid -> !uuid.isBlank())
                .map(uuid -> {
                    if (!uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
                        throw new BusinessException(StringConstant.ErrorMessage.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
                    }
                    return uuid;
                })
                .orElse(null);

        String getTeacherUuid = Optional.ofNullable(teacherUuid)
                .filter(uuid -> !uuid.isBlank())
                .map(uuid -> {
                    if (!uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
                        throw new BusinessException(StringConstant.ErrorMessage.TEACHER_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR);
                    }
                    return uuid;
                })
                .orElse(null);

        List<ClassAssignmentDTO> list = classAssignmentService.list(getSemesterUuid, getCourseUuid, getTeacherUuid);
        return ResultUtil.success("查询排课分配列表成功", list);
    }
}
