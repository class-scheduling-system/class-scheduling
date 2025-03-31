package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.models.vo.SpecificCourseIdVO;
import com.frontleaves.scheduling.services.SchedulingService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
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
 * 排课控制器
 *
 * @author FLASHLACK
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/scheduling")
public class SchedulingController {

    private final SchedulingService schedulingService;

    /**
     * 自动排课
     * @param automaticClassSchedulingVO 自动排课请求对象，包含排课所需的各种设置和参数
     * @param request HTTP请求对象，用于获取当前用户信息
     * @return ResponseEntity<BaseResponse<Void>> 返回排课结果
     */

    @RequestRole("教务")
    @PostMapping("/auto")
    public ResponseEntity<BaseResponse<Void>> automaticClassScheduling(
            @RequestBody @Valid AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request
    ) {
        //检查结束周是否大于等于1
        if (automaticClassSchedulingVO.getEndWeek() < 1) {
            throw new BusinessException("排课结束周必须大于等于1", ErrorCode.BODY_ERROR);
        }
        //检查晚间排课约束是否为空
        Optional.ofNullable(automaticClassSchedulingVO.getTimePreferences().getPreferredTimeSlots())
                .orElseThrow(() -> new BusinessException("晚间排课约束为空数据", ErrorCode.BODY_ERROR));
        //检查课程ID列表是否为空
        Optional.ofNullable(automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds())
                .filter(data -> !data.isEmpty())
                .orElseThrow(() -> new BusinessException("课程ID列表不能为空", ErrorCode.BODY_ERROR));
        // 检查 classID 和 number 是否同时为空
        for (SpecificCourseIdVO course :
                automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds()
        ) {
            if ((course.getClassId() != null && course.getClassId().isEmpty())  && course.getNumber() == null) {
                throw new BusinessException("班级或者人数选择为空", ErrorCode.BODY_ERROR);
            }
        }
        // 准备数据并排课
        schedulingService.getAutoClassSchedulingBaseDTO(automaticClassSchedulingVO, request);

        return ResultUtil.success("开始排课");
    }
}
