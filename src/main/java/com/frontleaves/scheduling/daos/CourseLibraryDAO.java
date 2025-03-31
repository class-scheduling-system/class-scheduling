package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CourseLibraryMapper;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * @author qiyu
 */
@Repository
@RequiredArgsConstructor
public class CourseLibraryDAO extends ServiceImpl<CourseLibraryMapper, CourseLibraryDO> {

    private final RedissonClient redisson;
    /**
     * 根据部门UUID、指定课程ID列表
     *
     * @param departmentUuid    部门UUID，用于筛选属于该部门的课程库
     * @param specificCourseIds 指定的课程ID列表，如果非空，则只返回这些ID对应的课程库
     * @return 返回根据条件筛选出的课程库列表
     */
    public List<CourseLibraryDO> getListCourseLibraryByDepartmentAndSpecify(
            @NotBlank String departmentUuid, List<String> specificCourseIds) {
        // 如果指定了具体课程ID列表且不为空，则查询属于该部门且在指定课程ID列表中的课程库
        if (specificCourseIds != null && !specificCourseIds.isEmpty()) {
            return this.lambdaQuery().eq(CourseLibraryDO::getDepartment, departmentUuid)
                    .eq(CourseLibraryDO::getIsEnabled, true)
                    .in(CourseLibraryDO::getCourseLibraryUuid, specificCourseIds)
                    .list();
        }
        // 如果没有指定课程ID列表，返回空链表
        return List.of();
    }

    public List<CourseLibraryDO> getCourseListByDepart(String departmentUuid) {
        RList<CourseLibraryDO> rList = redisson.getList(StringConstant.Redis.COURSE_LIBRARY_LIST);
        if (!rList.isExists()) {
            List<CourseLibraryDO> courseLibraryList = this
                    .lambdaQuery()
                    .eq(CourseLibraryDO::getDepartment,departmentUuid)
                    .list();
            if (!courseLibraryList.isEmpty()) {
                rList.addAll(courseLibraryList);
                rList.expire(Duration.ofHours(24));
                return courseLibraryList;
            }
            return List.of();
        }
        return rList.readAll();
    }
}
