package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.TeachingClassDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学班数据访问对象
 * @author FLASHLACK
 */
@Mapper
public interface TeachingClassMapper extends BaseMapper<TeachingClassDO> {
}
