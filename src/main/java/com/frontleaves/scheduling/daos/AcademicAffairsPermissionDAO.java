package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.AcademicAffairsPermissionMapper;
import com.frontleaves.scheduling.models.entity.AcademicAffairsPermissionDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 教务权限 数据访问对象
 *
 * @author FLASHLACK
 */
@Repository
@RequiredArgsConstructor
public class AcademicAffairsPermissionDAO extends ServiceImpl<AcademicAffairsPermissionMapper, AcademicAffairsPermissionDO>
        implements IService<AcademicAffairsPermissionDO> {

}
