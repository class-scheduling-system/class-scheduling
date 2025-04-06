package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.CourseTypeDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程类型 Mapper 接口
 * 对应数据库表：cs_course_type
 *
 * @author FLASHLACK
 */
@Mapper
public interface CourseTypeMapper extends BaseMapper<CourseTypeDO> {
}