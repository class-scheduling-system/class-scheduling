package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @RequestMapping("/library")
    public ResponseEntity<BaseResponse<Void>> addCourseLibrary(
            @RequestBody @Validated CourseLibraryVO courseLibraryVO
    ) {
        courseLibraryService.addCourseLibrary(courseLibraryVO);
        return ResultUtil.success("课程添加成功");
    }



}
