package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.DepartmentMapper;
import com.frontleaves.scheduling.models.entity.DepartmentDO;

/**
 * 部门 数据访问对象
 * @author FLASHLACK
 */
public class DepartmentDAO extends ServiceImpl<DepartmentMapper, DepartmentDO> implements
        IService<DepartmentDO> {
}
