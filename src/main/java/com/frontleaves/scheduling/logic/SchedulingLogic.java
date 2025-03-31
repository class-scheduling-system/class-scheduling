package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.models.dto.base.SemesterDTO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.services.SchedulingService;
import com.frontleaves.scheduling.thread.ScheduleLessonsDataPreparationThread;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * 调度逻辑
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingLogic implements SchedulingService {
    private final ScheduleLessonsDataPreparationThread scheduleLessonsDataPreparationThread;

    /**
     * 检查结束周是否超过学期周
     *
     * @param endWeek     结束周
     * @param semesterDTO 学期信息
     * @throws BusinessException 当结束周超过学期周时抛出异常
     */
    public static void checkEndWeekExceedSemesterWeeks(Integer endWeek, @NotNull SemesterDTO semesterDTO) {
        // 计算学期总周数
        long totalWeeks = (semesterDTO.getEndDate().getTime() - semesterDTO.getStartDate().getTime())
                / (7 * 24 * 60 * 60 * 1000) + 1;
        if (endWeek > totalWeeks) {
            throw new BusinessException("结束周超过学期总周数", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 获取自动排课基础DTO
     *
     * @param automaticClassSchedulingVO 自动排课请求对象，包含排课所需的各种设置和参数
     * @param request                    HTTP请求对象，用于获取当前用户信息
     */
    @Override
    public void getAutoClassSchedulingBaseDTO(
            @NotNull AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request
    ) {
        scheduleLessonsDataPreparationThread.startUp(automaticClassSchedulingVO, request);
    }
}
