package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程库逻辑层
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLibraryLogic implements CourseLibraryService {
    private final CourseLibraryDAO courseLibraryDAO;


    /**
     * 根据部门UUID、特定课程ID列表和排除课程ID列表获取课程库列表
     * 如果查询结果为空，则抛出业务异常
     * @param departmentUuid 部门UUID，用于查询课程库
     * @param specificCourseIds 特定课程ID列表，用于过滤课程库
     * @param excludeCourseIds 排除课程ID列表，用于过滤课程库
     * @return 课程库列表，如果列表为空则抛出异常
     * @throws BusinessException 当课程库列表为空时抛出的业务异常
     */
    @Override
    public List<CourseLibraryDO> listCourseLibraryByDepartmentAndSpecifyWithThrow(
            @NotBlank String departmentUuid, List<String> specificCourseIds, List<String> excludeCourseIds) {
        // 调用DAO层方法获取课程库列表
        List<CourseLibraryDO> libraryDOS = courseLibraryDAO.getListCourseLibraryByDepartmentAndSpecify(
                departmentUuid, specificCourseIds, excludeCourseIds);
        // 检查获取的课程库列表是否为空，如果为空则抛出业务异常
        if (libraryDOS != null && libraryDOS.isEmpty()) {
            throw new BusinessException("课程库列表为空", ErrorCode.BODY_ERROR);
        }
        // 返回获取的课程库列表
        return libraryDOS;
    }
}
