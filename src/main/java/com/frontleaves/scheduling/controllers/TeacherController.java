package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.models.vo.TeacherVO;
import com.frontleaves.scheduling.services.TeacherService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("")
    public ResponseEntity<BaseResponse<Void>>addTeacher(
            @RequestBody @Validated TeacherVO teacherVO
    ) {
        teacherService.addTeacher(teacherVO);
        return ResultUtil.success("教师添加成功");

    }
}
