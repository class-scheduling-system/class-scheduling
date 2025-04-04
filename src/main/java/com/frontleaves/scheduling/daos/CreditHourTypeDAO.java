package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.CreditHourTypeMapper;
import com.frontleaves.scheduling.models.entity.CreditHourTypeDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 学时类型 DAO 类
 * @author FLASHLACK
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class CreditHourTypeDAO extends ServiceImpl<CreditHourTypeMapper, CreditHourTypeDO> {
}
