package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教师映射器
 * @author FLASHLACK
 */
@Mapper
public interface TeacherMapper extends BaseMapper<TeacherDO> {
}
