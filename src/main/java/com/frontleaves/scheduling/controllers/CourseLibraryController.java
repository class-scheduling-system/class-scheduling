package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.PrepareCourseDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.excel.BackAddCourseDTO;
import com.frontleaves.scheduling.models.dto.lite.CourseLiteDTO;
import com.frontleaves.scheduling.models.vo.BatchAddCourseVO;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.List;
import java.util.Optional;

/**
 * 课程库控制器
 * <p>
 * 该控制器处理与课程库相关的请求，包括添加、更新、删除课程库信息，
 * 以及获取课程库列表和简洁列表等功能。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */

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
    @RequestRole({"教务", "管理员"})
    @RequestMapping("")
    public ResponseEntity<BaseResponse<Void>> addCourseLibrary(
            @RequestBody @Validated CourseLibraryVO courseLibraryVO,
            @NotNull HttpServletRequest request
    ) {
        courseLibraryService.addCourseLibrary(courseLibraryVO, request);
        return ResultUtil.success("课程添加成功");
    }

    /**
     * 更新课程库
     *
     * @param courseUuid    课程的唯一标识符（UUID）
     * @param courseLibraryVO 课程库更新请求对象，包含需要验证的课程库信息
     * @return 返回包含成功消息的响应实体
     * <p>
     * 此方法首先会检查传入的课程UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 如果格式不正确，则抛出参数错误异常
     */
    @RequestRole({"教务", "管理员"})
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
    @RequestRole({"教务", "管理员"})
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
    @RequestLogin
    @GetMapping("")
    public ResponseEntity<BaseResponse<PageDTO<CourseLibraryDTO>>> getCourseLibrary(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "10") Integer size,
            @RequestParam(value = "name", required = false) String name
    ) {
        PageDTO<CourseLibraryDTO> courseLibraryPage = courseLibraryService.getCourseLibrary(page, size, name);
        return ResultUtil.success("课程库获取成功", courseLibraryPage);
    }

    /**
     * 获取课程库简洁列表
     *
     * @param courseCategoryUuid    课程类别UUID
     * @param coursePropertyUuid    课程属性UUID
     * @param courseTypeUuid        课程类型UUID
     * @param courseNatureUuid      课程性质UUID
     * @param courseDepartmentUuid  课程部门UUID
     * @return 返回包含课程库简洁列表的响应实体
     */
    @RequestLogin
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

    /**
     * 获取课程导入模板
     *
     * @return 返回包含课程导入模板的响应实体
     * <p>
     * 此方法用于获取课程导入模板，返回一个包含模板文件的字节数组和HTTP响应头的响应实体。
     * </p>
     */
    @GetMapping("/get-template")
    public ResponseEntity<byte[]> getCourseImportTemplate() {
        // 准备课程库示例数据，用于生成导入模板
        PrepareCourseDTO prepareCourseExampleDTO = courseLibraryService.prepareCourseData();
        // 获取课程库导入模板的字节数组
        byte[] bytes = courseLibraryService.getCourseImportTemplate(prepareCourseExampleDTO);
        // 初始化HTTP响应头，用于设置模板文件的下载信息
        HttpHeaders headers = Optional.of(new HttpHeaders())
                .map(header -> {
                    // 设置内容类型为二进制流
                    header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    // 设置内容长度
                    header.setContentLength(bytes.length);
                    // 对文件名进行URL编码，确保文件名在不同浏览器中正确显示
                    String fileName = URLEncoder.encode("课程库导入模板.xlsx", StandardCharsets.UTF_8)
                            .replace("+", "%20");
                    // 设置内容处置，以附件形式下载文件，并处理文件名的UTF-8编码
                    header.add(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
                    return header;
                })
                // 如果生成响应头失败，则抛出业务异常
                .orElseThrow(() -> new BusinessException("获取响应头失败", ErrorCode.SERVER_INTERNAL_ERROR));
        // 返回包含模板文件的响应实体，状态码为HTTP 200 OK
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * 处理批量导入课程的请求
     *
     * @param batchAddCourseVO 包含批量导入课程信息的请求体
     * @return 返回包含导入结果的响应实体
     */
    @PostMapping("/batch-import")
    public ResponseEntity<BaseResponse<BackAddCourseDTO>> batchImportCourses(
            @RequestBody @Validated BatchAddCourseVO batchAddCourseVO
    ) {
        // 验证课程批处理文件并获取处理后的文件
        byte[] file = courseLibraryService.verifyCourseBatchAndBackFile(batchAddCourseVO);
        // 根据是否忽略错误来选择不同的处理策略
        BackAddCourseDTO backAddCourseDTO = Optional.ofNullable(batchAddCourseVO.getIgnoreError())
                .filter(Boolean.TRUE::equals)
                .map(ignoreError -> courseLibraryService.batchImportIgnoreError(file))
                .orElseGet(() -> courseLibraryService.batchImportNoIgnoreError(file));
        // 如果有课程添加失败，则返回成功但提示存在添加失败的课程库
        if (backAddCourseDTO.getFailedCount() > 0) {
            return ResultUtil.success("存在添加失败的课程库", backAddCourseDTO);
        }
        // 所有课程添加成功，返回成功响应
        return ResultUtil.success("批量添加课程库成功", backAddCourseDTO);
    }
}
