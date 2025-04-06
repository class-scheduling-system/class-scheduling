package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CreditHourTypeMapper;
import com.frontleaves.scheduling.models.entity.base.CreditHourTypeDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * 学时类型 DAO 类
 * @author FLASHLACK
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class CreditHourTypeDAO extends ServiceImpl<CreditHourTypeMapper, CreditHourTypeDO> {
    private final RedissonClient redisson;
    /**
     * 根据UUID获取学时类型的详细信息
     * 首先尝试从Redis中获取数据，如果不存在，则从数据库中获取，并将其缓存到Redis中
     * 使用缓存的目的是为了减少数据库的访问压力，提高系统的响应速度
     * @param uuid 学时类型的唯一标识符，用于在Redis和数据库中查询学时类型信息
     * @return 返回学时类型的详细信息对象，如果找不到则返回null
     */
    public CreditHourTypeDO getCreditHourTypeByUuid(String uuid) {
        // 构建Redis中存储学时类型信息的键名
        RMap<String,String> rMap = redisson.getMap(StringConstant.Redis.CREDIT_HOUR_TYPE_UUID + uuid);

        // 检查Redis中是否存在该学时类型的信息
        if (!rMap.isExists()){
            // 如果Redis中不存在，从数据库中查询学时类型信息
            CreditHourTypeDO creditHourTypeDO = this.getById(uuid);

            // 如果查询到了学时类型信息，将其缓存到Redis中，并设置过期时间
            if (creditHourTypeDO != null) {
                rMap.putAll(ConvertUtil.convertObjectToMapString(creditHourTypeDO));
                rMap.expire(Duration.ofSeconds(3600));
                return creditHourTypeDO;
            }
        } else {
            // 如果Redis中存在该学时类型的信息，将其转换为CreditHourTypeDO对象并返回
            return BeanUtil.toBean(rMap, CreditHourTypeDO.class);
        }
        // 如果数据库中也找不到该学时类型的信息，返回null
        return null;
    }

    /**
     * 获取学时类型列表
     * 首先尝试从Redis中获取列表，如果不存在，则从数据库中获取，并存入Redis中
     * 此方法解释了为什么首先从Redis获取数据，以及如果Redis中没有数据时的处理方式
     *
     * @return 学时类型列表，如果列表为空或不存在，则返回空列表
     */
    public List<CreditHourTypeDO> getList() {
        // 从Redis中获取学时类型列表
        RList<CreditHourTypeDO> rList = redisson.getList(StringConstant.Redis.CREDIT_HOUR_TYPE_LIST);
        // 检查列表在Redis中是否存在
        if (!rList.isExists()){
            // 从数据库中获取学时类型列表
            List<CreditHourTypeDO> creditHourTypeDOList = this.list();
            // 检查列表是否非空
            if (creditHourTypeDOList != null && !creditHourTypeDOList.isEmpty()){
                // 将列表添加到Redis中，并设置过期时间
                rList.addAll(creditHourTypeDOList);
                rList.expire(Duration.ofSeconds(3600));
                // 返回从数据库中获取的列表
                return creditHourTypeDOList;
            }
            // 如果列表为空，返回空列表
            return List.of();
        }
        // 如果Redis中存在列表，读取并返回
        return rList.readAll();
    }
}
