package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import enums.CourseEnuType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

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
    void getAutoClassSchedulingBaseDTO(
            @Valid AutomaticClassSchedulingVO automaticClassSchedulingVO,
            HttpServletRequest request);
    /**
     * 复制原 DTO 并修改课程类型和学时
     */
    @NotNull
    CourseLibraryAndTeacherCourseQualificationListDTO copyAndSet(
            CourseLibraryAndTeacherCourseQualificationListDTO originalDto,
            CourseEnuType newType,
            BigDecimal hours);

}
