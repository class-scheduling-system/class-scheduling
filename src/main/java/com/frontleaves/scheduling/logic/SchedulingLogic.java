package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.AutomaticClassSchedulingVO;
import com.frontleaves.scheduling.services.*;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final TeacherCourseQualificationService teacherCourseQualificationService;
    private final CourseTypeService courseTypeService;

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
            @NotNull AutomaticClassSchedulingVO automaticClassSchedulingVO, HttpServletRequest request) {
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
        // 使用 Map 存储课程类型优先级，以 courseTypeUuid 为键
        Map<String, CourseTypePriorityDTO> courseTypePriorityMap = new HashMap<>();
        // 获取优先级并填充到 Map 中
        for (AutomaticClassSchedulingVO.PrioritySettings.CourseTypePriority courseTypePriority
                : automaticClassSchedulingVO.getPrioritySettings().getCourseTypes()) {
            // 根据 typeId 获取 CourseTypeDTO
            CourseTypeDTO courseTypeDTO =
                    courseTypeService.getCourseTypeByUuidWithError(courseTypePriority.getTypeId());
            assert courseTypeDTO != null;
            // 创建 CourseTypePriorityDTO 并设置优先级
            CourseTypePriorityDTO courseTypePriorityDTO = new CourseTypePriorityDTO();
            courseTypePriorityDTO.setCourseTypeDTO(courseTypeDTO)
                    .setPriority(Short.parseShort(courseTypePriority.getTypeId()));
            // 将其添加到 Map 中，以 courseTypeUuid 为键
            courseTypePriorityMap.put(courseTypeDTO.getCourseTypeUuid(), courseTypePriorityDTO);
        }
        //获取课程库
        List<CourseLibraryDTO> courseLibraryDTOList =
                courseLibraryService.listCourseLibraryByDepartmentAndSpecifyWithThrow(
                        automaticClassSchedulingVO.getDepartmentId(),
                        automaticClassSchedulingVO.getScopeSettings().getSpecificCourseIds(),
                        automaticClassSchedulingVO.getScopeSettings().getExcludeCourseIds());
        assert courseLibraryDTOList != null;
        //获取老师所有数据
        List<CourseLibraryAndTeacherCourseQualificationListDTO> courseQualificationList =
                teacherCourseQualificationService.getCourseLibraryAndTeacherCourseQualificationList(
                        courseLibraryDTOList, automaticClassSchedulingVO.getConstraints().getTeacherPreference());
        assert courseQualificationList != null;
        for (CourseLibraryAndTeacherCourseQualificationListDTO dto : courseQualificationList) {
            //设置优先级
            CourseLibraryDTO courseLibraryDTO = dto.getCourseLibraryDTO();
            assert courseLibraryDTO != null;
            // 获取课程类型 UUID
            String courseTypeUuid = courseLibraryDTO.getType();
            assert courseTypeUuid != null;
            // 在 Map 中查找对应的优先级信息
            CourseTypePriorityDTO courseTypePriorityDTO = courseTypePriorityMap.get(courseTypeUuid);
            // 匹配成功，设置优先级
            dto.setCourseTypes(courseTypePriorityDTO.getPriority());
        }
        //获取教室数据

        //创建返回结果
        return null;
    }
}
