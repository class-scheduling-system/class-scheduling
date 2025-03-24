package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.BackAutomaticClassSchedulingDTO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

/**
 * 调度控制器
 *
 * @author FLASHLACK
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scheduling")
public class SchedulingController {


    @RequestRole("教务")
    @PostMapping("/auto")
    public ResponseEntity<BaseResponse<BackAutomaticClassSchedulingDTO>> automaticClassScheduling(
            @RequestBody @Valid AutomaticClassSchedulingVO automaticClassSchedulingVO) {
        //检测参数
        Optional.ofNullable(automaticClassSchedulingVO.getDepartmentId())
                // 过滤出空字符串
                .filter(String::isEmpty)
                .ifPresent(departmentId -> {
                    throw new BusinessException("院系ID不能为空", ErrorCode.BODY_ERROR);
                });
        if (automaticClassSchedulingVO.getScopeSettings().getIncludeAllSemesterCourses()) {
            // 检测列表是否为空
            Optional.ofNullable(automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds())
                    .filter(data -> !data.isEmpty())
                    .orElseThrow(() -> new BusinessException("课程ID列表不能为空", ErrorCode.BODY_ERROR));
        }
        //检测排除课程列表是否为空
        Optional.ofNullable(automaticClassSchedulingVO.getScopeSettings().getExcludeCourseIds())
                .filter(data -> !data.isEmpty())
                .orElseThrow(() -> new BusinessException("排除课程ID列表不能为空", ErrorCode.BODY_ERROR));
        return null;
    }
}
