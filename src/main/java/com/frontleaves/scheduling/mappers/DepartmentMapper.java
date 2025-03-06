package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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
    @Select("""
        SELECT department_uuid
        FROM cs_department
        WHERE department_name LIKE CONCAT('%', #{departmentName}, '%')
""")
    List<String> getDepartmentUuidByName(String departmentName);
}
