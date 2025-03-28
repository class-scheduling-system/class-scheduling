package com.frontleaves.scheduling.logic;


import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLibraryLogic implements CourseLibraryService {
    private final UnitCategoryDAO unitCategoryDAO;

    @Override
    public void addCourseLibrary(CourseLibraryVO courseLibraryVO) {
        CourseCategoryDO courseCategoryDO = new CourseCategoryDO();


    }
}
