package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.CourseLiteDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/course")
public class CourseLibraryController {

    private final CourseLibraryService courseLibraryService;
    /**
     * 添加课程库
     * <p>
     * 该方法通过POST请求接收课程库信息，并进行验证和处理，返回添加成功的响应
     *
     * @param courseLibraryVO 课程库添加请求对象，包含需要验证的课程库信息
     * @return 返回包含成功消息的响应实体
     */
    @RequestMapping("")
    public ResponseEntity<BaseResponse<Void>> addCourseLibrary(
            @RequestBody @Validated CourseLibraryVO courseLibraryVO
    ) {
        courseLibraryService.addCourseLibrary(courseLibraryVO);
        return ResultUtil.success("课程添加成功");
    }

    /**
     * 根据课程UUID获取课程库信息
     *
     * @param courseUuid 课程的唯一标识符（UUID）
     * @return 返回包含课程库信息的响应实体
     * <p>
     * 此方法首先会检查传入的课程UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 如果格式不正确，则抛出参数错误异常
     */
    @PutMapping("/{course_uuid}")
    public ResponseEntity<BaseResponse<Void>> updateCourseLibrary(
            @PathVariable("course_uuid") String courseUuid,
            @RequestBody @Validated CourseLibraryVO courseLibraryVO
    ) {
        String getUuid = Optional.ofNullable(courseUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        courseLibraryService.updateCourseLibrary(getUuid, courseLibraryVO);
        return ResultUtil.success("课程更新成功");
    }

    /**
     * 删除课程库
     *
     * @param courseUuid 课程的唯一标识符（UUID）
     * @return 返回包含成功消息的响应实体
     * <p>
     * 此方法首先会检查传入的课程UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 如果格式不正确，则抛出参数错误异常
     */
    @DeleteMapping("/{course_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteCourseLibrary(
            @PathVariable("course_uuid") String courseUuid
    ) {
        String getUuid = Optional.ofNullable(courseUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException(StringConstant.COURSE_UUID_FORMAT_ERROR, ErrorCode.PARAMETER_ERROR));
        courseLibraryService.deleteCourseLibrary(getUuid);
        return ResultUtil.success("课程删除成功");
    }

    /**
     * 获取课程库列表
     *
     * @param page 当前页数，默认为1
     * @param size 每页显示数量，默认为10
     * @param name 搜索关键字（课程名称），可选参数
     * @return 返回包含课程库分页信息的响应实体
     */
    @GetMapping("")
    public ResponseEntity<BaseResponse<PageDTO<CourseLibraryDTO>>> getCourseLibrary(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name
    ) {
        PageDTO<CourseLibraryDTO> courseLibraryPage = courseLibraryService.getCourseLibrary(page, size, name);
        return ResultUtil.success("课程库获取成功", courseLibraryPage);
    }

    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<CourseLiteDTO>>> getCourseLibraryList(
            @RequestParam(value = "course_category_uuid", required = false) String courseCategoryUuid,
            @RequestParam(value = "course_property_uuid", required = false) String coursePropertyUuid,
            @RequestParam(value = "course_type_uuid", required = false) String courseTypeUuid,
            @RequestParam(value = "course_nature_uuid", required = false) String courseNatureUuid,
            @RequestParam(value = "course_department_uuid", required = false) String courseDepartmentUuid
    ) {

        if (courseCategoryUuid != null && !courseCategoryUuid.isBlank() && !courseCategoryUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("课程类别UUID格式不正确", ErrorCode.PARAMETER_ERROR);
        }
        if (coursePropertyUuid != null && !coursePropertyUuid.isBlank() && !coursePropertyUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("课程属性UUID格式不正确", ErrorCode.PARAMETER_ERROR);
        }
        if (courseTypeUuid != null && !courseTypeUuid.isBlank() && !courseTypeUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("课程类型UUID格式不正确", ErrorCode.PARAMETER_ERROR);
        }
        if (courseNatureUuid != null && !courseNatureUuid.isBlank() && !courseNatureUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("课程性质UUID格式不正确", ErrorCode.PARAMETER_ERROR);
        }
        if (courseDepartmentUuid != null && !courseDepartmentUuid.isBlank() && !courseDepartmentUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("课程部门UUID格式不正确", ErrorCode.PARAMETER_ERROR);
        }
        List<CourseLiteDTO> courseLibraryList = courseLibraryService.getCourseLibraryList(
                courseCategoryUuid,
                coursePropertyUuid,
                courseTypeUuid,
                courseNatureUuid,
                courseDepartmentUuid
        );
        return ResultUtil.success("课程库列表获取成功", courseLibraryList);
    }
}
