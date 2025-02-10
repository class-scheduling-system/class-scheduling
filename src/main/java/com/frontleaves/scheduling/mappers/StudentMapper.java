package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.StudentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生Mapper
 * @author FLASHLACK
 */
@Mapper
public interface StudentMapper extends BaseMapper<StudentDO> {
}
