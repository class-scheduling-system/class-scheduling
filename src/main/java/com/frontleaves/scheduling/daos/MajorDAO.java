package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.MajorMapper;
import com.frontleaves.scheduling.models.entity.MajorDO;

/**
 * 专业major
 *
 * @author FLASHLACK
 */
public class MajorDAO extends ServiceImpl<MajorMapper, MajorDO> implements IService<MajorDO> {
}
