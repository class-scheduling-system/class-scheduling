package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.SemesterDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学期表映射器
 * @author FLASHLACK
 */
@Mapper
public interface SemesterMapper extends BaseMapper<SemesterDO> {
}
