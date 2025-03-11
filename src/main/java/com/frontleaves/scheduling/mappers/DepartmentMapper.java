package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门Mapper
 * @author FLASHLACK
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentDO> {
}
