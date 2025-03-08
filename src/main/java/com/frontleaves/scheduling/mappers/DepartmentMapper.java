package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 部门Mapper
 * @author FLASHLACK
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<DepartmentDO> {

    /**
     * 根据部门名称获取部门uuid
     *
     * @param departmentName 部门名称
     */
    default List<String> getDepartmentUuidByName(String departmentName) {
        QueryWrapper<DepartmentDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("department_uuid")
                .like("department_name", departmentName);
        return this.selectObjs(queryWrapper);
    }
}
