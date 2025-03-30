package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.CourseLiteDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CourseLibraryService {
    void addCourseLibrary(CourseLibraryVO courseLibraryVO);

    void updateCourseLibrary(String getUuid, CourseLibraryVO courseLibraryVO);

    void deleteCourseLibrary(String getUuid);

    PageDTO<CourseLibraryDTO> getCourseLibrary(Integer page, Integer size, String name);

    List<CourseLiteDTO> getCourseLibraryList(String courseCategoryUuid, String coursePropertyUuid, String courseTypeUuid, String courseNatureUuid, String courseDepartmentUuid);
}
