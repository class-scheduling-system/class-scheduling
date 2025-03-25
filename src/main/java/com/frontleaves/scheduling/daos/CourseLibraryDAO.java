package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.CourseLibraryMapper;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import org.springframework.stereotype.Repository;

/**
 * @author qiyu
 */
@Repository
public class CourseLibraryDAO extends ServiceImpl<CourseLibraryMapper, CourseLibraryDO> {
}
