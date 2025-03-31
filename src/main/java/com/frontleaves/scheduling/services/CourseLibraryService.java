package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.CourseLiteDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CourseLibraryService {
    void addCourseLibrary(CourseLibraryVO courseLibraryVO, HttpServletRequest request);

    void updateCourseLibrary(String courseUuid, CourseLibraryVO courseLibraryVO);

    void deleteCourseLibrary(String courseUuid);

    PageDTO<CourseLibraryDTO> getCourseLibrary(Integer page, Integer size, String name);

    List<CourseLiteDTO> getCourseLibraryList(String courseCategoryUuid, String coursePropertyUuid, String courseTypeUuid, String courseNatureUuid, String courseDepartmentUuid);
}
