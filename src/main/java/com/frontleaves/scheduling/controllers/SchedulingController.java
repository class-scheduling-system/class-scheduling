package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.BackAutomaticClassSchedulingDTO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.services.SchedulingService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
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

    private final SchedulingService schedulingService;


    @RequestRole("教务")
    @PostMapping("/auto")
    public ResponseEntity<BaseResponse<BackAutomaticClassSchedulingDTO>> automaticClassScheduling(
            @RequestBody @Valid AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request) {
        //检查排课策略是否符合预期
        if (!automaticClassSchedulingVO.getStrategy().matches("^(optimal|balanced|quick)$")) {
            throw new BusinessException("排课策略必须是 optimal、balanced 或 quick 之一", ErrorCode.BODY_ERROR);
        }
        //检查结束周是否大于等于1
        if (automaticClassSchedulingVO.getEndWeek() < 1) {
            throw new BusinessException("排课结束周必须大于等于1", ErrorCode.BODY_ERROR);
        }
        //检查晚间排课约束是否为空
        Optional.ofNullable(automaticClassSchedulingVO.getTimePreferences().getPreferredTimeSlots())
                .orElseThrow(() -> new BusinessException("晚间排课约束为空数据", ErrorCode.BODY_ERROR));
        if (Boolean.TRUE.equals(automaticClassSchedulingVO.getScopeSettings().getIncludeAllSemesterCourses())) {
            // 检测列表是否为空
            Optional.ofNullable(automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds())
                    .filter(data -> !data.isEmpty())
                    .orElseThrow(() -> new BusinessException("课程ID列表为空数据", ErrorCode.BODY_ERROR));
        }
        //检测排除课程列表是否为空
        Optional.ofNullable(automaticClassSchedulingVO.getScopeSettings().getExcludeCourseIds())
                .filter(data -> !data.isEmpty())
                .orElseThrow(() -> new BusinessException("排除课程ID列表为空数据", ErrorCode.BODY_ERROR));
        //准备用户数据
        schedulingService.getAutoClassSchedulingBaseDTO(automaticClassSchedulingVO, request);
        return null;
    }
}
