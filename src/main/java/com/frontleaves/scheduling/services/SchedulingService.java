package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.SchedulingTaskDTO;
import com.frontleaves.scheduling.models.dto.scheduling.SchedulingTaskStatusDTO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.jetbrains.annotations.NotNull;

/**
 * 调度服务
 * @author FLASHLACK
 */
public interface SchedulingService {
    /**
     * 获取自动排课基础DTO
     * @param automaticClassSchedulingVO  自动排课视图对象
     * @param request 请求
     * @return 自动排课基础DTO
     */
    SchedulingTaskDTO getAutoClassSchedulingBaseDTO(
            @Valid AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request);


    /**
     * 获取调度任务状态
     * @return 调度任务状态
     */
    @NotNull
    SchedulingTaskStatusDTO getSchedulingTaskStatus(String taskId);
}
