package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import org.springframework.stereotype.Service;

@Service
public interface CourseLibraryService {
    void addCourseLibrary(CourseLibraryVO courseLibraryVO);
}
