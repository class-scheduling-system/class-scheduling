package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
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
     * @param excludeCourseIds  排除的课程id
     * @return 课程库列表
     */
    List<CourseLibraryDO> listCourseLibraryByDepartmentAndSpecifyWithThrow(
            @NotBlank String departmentUuid,
            List<String> specificCourseIds,
            List<String> excludeCourseIds
    );
}
