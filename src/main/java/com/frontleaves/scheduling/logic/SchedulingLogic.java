package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.models.dto.AcademicAffairsPermissionDTO;
import com.frontleaves.scheduling.models.dto.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.SemesterDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.services.*;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 调度逻辑
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulingLogic implements SchedulingService {
    private final UserService userService;
    private final AcademicAffairsPermissionService academicAffairsPermissionService;
    private final SemesterService semesterService;
    private final CourseLibraryService courseLibraryService;

    /**
     * 检查结束周是否超过学期周
     *
     * @param endWeek     结束周
     * @param semesterDTO 学期信息
     * @throws BusinessException 当结束周超过学期周时抛出异常
     */
    private static void checkEndWeekExceedSemesterWeeks(Integer endWeek, SemesterDTO semesterDTO) {
        // 计算学期总周数
        long totalWeeks = (semesterDTO.getEndDate().getTime() - semesterDTO.getStartDate().getTime())
                / (7 * 24 * 60 * 60 * 1000) + 1;
        if (endWeek > totalWeeks) {
            throw new BusinessException("结束周超过学期总周数", ErrorCode.BODY_ERROR);
        }
    }

    @Override
    public AutomaticClassSchedulingBaseDTO getAutoClassSchedulingBaseDTO(
            AutomaticClassSchedulingVO automaticClassSchedulingVO, HttpServletRequest request) {
        UserDO userDO = userService.getUserByRequest(request);
        assert userDO != null;
        // 检查用户所属部门与所填写部门是否一致
        AcademicAffairsPermissionDTO academicAffairsPermissionDTO =
                academicAffairsPermissionService.getAcademicAffairsPermission(userDO.getUserUuid());
        assert academicAffairsPermissionDTO != null;
        if (!academicAffairsPermissionDTO.getDepartment().equals(automaticClassSchedulingVO.getDepartmentId())) {
            throw new BusinessException("用户所属部门与所填写部门不一致", ErrorCode.BODY_ERROR);
        }
        //检查学期是否存在并且是否启用
        SemesterDTO semesterDTO =
                semesterService.getSemesterByUuidCheckEnabled(automaticClassSchedulingVO.getSemesterId());
        assert semesterDTO != null;
        //检查结束周是否超过学期周
        checkEndWeekExceedSemesterWeeks(automaticClassSchedulingVO.getEndWeek(), semesterDTO);
        List<CourseLibraryDTO> courseLibraryDTOList =
                courseLibraryService.listCourseLibraryByDepartmentAndSpecifyWithThrow(
                        automaticClassSchedulingVO.getDepartmentId(),
                        automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds(),
                        automaticClassSchedulingVO.getScopeSettings().getExcludeCourseIds());
        assert courseLibraryDTOList != null;
        //获取课程库中教课的老师表
        return null;
    }
}
