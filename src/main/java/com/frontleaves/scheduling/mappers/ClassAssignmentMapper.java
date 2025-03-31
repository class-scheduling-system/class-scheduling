package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.ClassAssignmentDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 排课分配 Mapper 接口
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface ClassAssignmentMapper extends BaseMapper<ClassAssignmentDO> {
}