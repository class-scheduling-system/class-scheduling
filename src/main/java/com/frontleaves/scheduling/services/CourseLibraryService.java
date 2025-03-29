package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.vo.SpecificCourseIdVO;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * 课程库服务接口，定义了课程库相关的操作。
 *
 * @author FLASHLACK
 */
public interface CourseLibraryService {

    /**
     * 根据部门uuid获取课程库,若查询出来为空则报错
     *
     * @param departmentUuid    部门uuid
     * @param specificCourseIds 指定的课程id
     * @return 课程库列表
     */
    List<CourseLibraryDTO> listCourseLibraryByDepartmentAndSpecifyWithThrow(
            @NotBlank String departmentUuid,
            List<String> specificCourseIds
    );

    /**
     * 获取课程库和班级DTO列表
     * @param specificCourseIds 指定课程ID列表
     * @return 课程库和班级DTO列表
     */
    List<CourseLibraryAndClassDTO> getCourseListAndClassDTO(
            List<SpecificCourseIdVO> specificCourseIds);
}
