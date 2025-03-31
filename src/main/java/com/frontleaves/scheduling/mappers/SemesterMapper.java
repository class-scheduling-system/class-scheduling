package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.SemesterDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学期数据访问映射器
 * <p>
 * 该接口继承自 MyBatis-Plus 的 {@code BaseMapper} 接口，用于提供对学期表的基本数据库操作。
 * 通过 MyBatis-Plus 的功能，自动实现了基础的 CRUD 方法。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Mapper
public interface SemesterMapper extends BaseMapper<SemesterDO> {
}