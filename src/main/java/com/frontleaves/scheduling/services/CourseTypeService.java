package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.CourseTypeDTO;

import java.util.List;

/**
 * 课程类型服务接口
 *
 * @author FLASHLACK
 */
public interface CourseTypeService {
    /**
     * 根据uuid获取课程类型
     * @param uuid  课程类型uuid
     * @return 课程类型DTO
     */
    CourseTypeDTO getCourseTypeByUuidWithError (
        String uuid
    );

    List<CourseTypeDTO> listCourseType(

    );
}
