package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.CourseLibraryMapper;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author qiyu
 */
@Repository
public class CourseLibraryDAO extends ServiceImpl<CourseLibraryMapper, CourseLibraryDO> {
    /**
     * 根据部门UUID、指定课程ID列表和排除课程ID列表获取课程库列表
     * @param departmentUuid 部门UUID，用于筛选属于该部门的课程库
     * @param specificCourseIds 指定的课程ID列表，如果非空，则只返回这些ID对应的课程库
     * @param excludeCourseIds 排除的课程ID列表，如果非空，则返回除这些ID之外的课程库
     * @return 返回根据条件筛选出的课程库列表
     */
    public List<CourseLibraryDO> getListCourseLibraryByDepartmentAndSpecify(
            @NotBlank String departmentUuid, List<String> specificCourseIds, List<String> excludeCourseIds) {
        // 如果指定了具体课程ID列表且不为空，则查询属于该部门且在指定课程ID列表中的课程库
        if (specificCourseIds != null && !specificCourseIds.isEmpty()){
            return this.lambdaQuery().eq(CourseLibraryDO::getDepartment, departmentUuid)
                    .in(CourseLibraryDO::getCourseLibraryUuid, specificCourseIds)
                    .list();
        }
        // 如果指定了排除课程ID列表且不为空，则查询属于该部门且不在排除课程ID列表中的课程库
        else if (excludeCourseIds != null && !excludeCourseIds.isEmpty()){
            return this.lambdaQuery().eq(CourseLibraryDO::getDepartment, departmentUuid)
                    .notIn(CourseLibraryDO::getCourseLibraryUuid, excludeCourseIds)
                    .list();
        }
        // 如果没有指定具体课程ID列表和排除课程ID列表，则查询属于该部门的所有课程库
        return this.lambdaQuery().eq(CourseLibraryDO::getDepartment, departmentUuid).list();
    }
}
