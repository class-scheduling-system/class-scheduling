/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.models.dto.base.CoursePropertyDTO;
import com.frontleaves.scheduling.services.CoursePropertyService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 课程属性控制器
 * <p>
 * 该类用于处理与课程属性相关的 HTTP 请求，提供查询课程属性列表和单个课程属性信息的接口。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/course-property")
public class CoursePropertyController {

    /**
     * 课程属性服务接口
     */
    private final CoursePropertyService coursePropertyService;

    /**
     * 获取课程属性列表
     * <p>
     * 获取系统中所有课程属性的列表，不需要分页。
     * </p>
     *
     * @return 课程属性列表
     */
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<CoursePropertyDTO>>> getCoursePropertyList() {
        List<CoursePropertyDTO> coursePropertyDTOList = coursePropertyService.getCoursePropertyList();
        return ResultUtil.success("获取课程属性列表成功", coursePropertyDTOList);
    }

    /**
     * 根据UUID获取课程属性详情
     * <p>
     * 根据提供的课程属性UUID获取其详细信息。
     * </p>
     *
     * @param coursePropertyUuid 课程属性UUID
     * @return 课程属性详情
     */
    @GetMapping("/{course_property_uuid}")
    public ResponseEntity<BaseResponse<CoursePropertyDTO>> getCoursePropertyByUuid(
            @PathVariable("course_property_uuid") String coursePropertyUuid
    ) {
        CoursePropertyDTO coursePropertyDTO = coursePropertyService.getCoursePropertyByUuidWithError(coursePropertyUuid);
        return ResultUtil.success("获取课程属性详情成功", coursePropertyDTO);
    }
}
