package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.CourseTypeMapper;
import com.frontleaves.scheduling.models.entity.CourseTypeDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 课程类型 DAO 类
 * 对应数据库表：cs_course_type
 *
 * @author FLASHLACK
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class CourseTypeDAO extends ServiceImpl<CourseTypeMapper, CourseTypeDO> {


}