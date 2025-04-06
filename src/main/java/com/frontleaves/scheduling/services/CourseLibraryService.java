package com.frontleaves.scheduling.services;


import com.frontleaves.scheduling.models.dto.PrepareCourseDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.excel.BackAddCourseDTO;
import com.frontleaves.scheduling.models.dto.lite.CourseLiteDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.vo.BatchAddCourseVO;
import com.frontleaves.scheduling.models.vo.CourseLibraryVO;
import com.frontleaves.scheduling.models.vo.SpecificCourseIdVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 课程库服务接口，定义了课程库相关的操作。
 *
 * @author FLASHLACK | qiyu
 */
public interface CourseLibraryService {
    void addCourseLibrary(CourseLibraryVO courseLibraryVO, HttpServletRequest request);

    void updateCourseLibrary(String courseUuid, CourseLibraryVO courseLibraryVO);

    void deleteCourseLibrary(String courseUuid);

    PageDTO<CourseLibraryDTO> getCourseLibrary(Integer page, Integer size, String name);

    List<CourseLiteDTO> getCourseLibraryList(String courseCategoryUuid, String coursePropertyUuid, String courseTypeUuid, String courseNatureUuid, String courseDepartmentUuid);

    PrepareCourseDTO prepareCourseData();

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

    byte[] getCourseImportTemplate(PrepareCourseDTO prepareCourseExampleDTO);

    /**
     * 获取课程库和班级DTO列表
     *
     * @param specificCourseIds 指定课程ID列表
     * @return 课程库和班级DTO列表
     */
    List<CourseLibraryAndTeacherCourseQualificationListDTO> getCourseListAndClassDTO(
            List<SpecificCourseIdVO> specificCourseIds,
            String departmentUuid
    );

    /**
     * 根据课程UUID获取课程库信息
     *
     * @param courseUuid 课程UUID
     * @return 课程库信息
     */
    @NotNull
    CourseLibraryDTO getCourseByUuid(@NotBlank String courseUuid);

    byte[] verifyCourseBatchAndBackFile(BatchAddCourseVO batchAddCourseVO);

    BackAddCourseDTO batchImportIgnoreError(byte[] file);

    BackAddCourseDTO batchImportNoIgnoreError(byte[] file);
}
