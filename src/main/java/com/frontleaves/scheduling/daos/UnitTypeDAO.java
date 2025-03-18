package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.UnitTypeMapper;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UnitTypeDAO extends ServiceImpl<UnitTypeMapper, UnitTypeDO> {

    private final RedissonClient redisson;

    /**
     * 根据UUID获取单位类型信息
     * 首先尝试从Redis中获取单位类型信息，如果不存在，则从数据库中获取，并存入Redis中以缓存
     *
     * @param unitTypeUuid 单位类型UUID，用于唯一标识一个单位类型
     * @return 返回单位类型对象，如果未找到则返回null
     */
    public UnitTypeDO getUnitTypeByUuid(String unitTypeUuid) {
        // 构造Redis中单位类型信息的键名
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.UNIT_TYPE_UUID + unitTypeUuid);

        // 检查Redis中是否存在该单位类型信息
        if (!map.isExists()) {
            // 如果Redis中不存在，从数据库中获取单位类型信息
            UnitTypeDO unitTypeDO = this.getById(unitTypeUuid);

            // 如果从数据库中获取到了单位类型信息
            if (unitTypeDO != null) {
                // 将单位类型信息转换为Map并存入Redis中以缓存
                map.putAll(ConvertUtil.convertObjectToMapString(unitTypeDO));
                // 设置缓存过期时间为86400秒，即24小时
                map.expire(java.time.Duration.ofSeconds(86400));
                // 返回获取到的单位类型信息
                return unitTypeDO;
            }
        } else {
            // 如果Redis中存在该单位类型信息，直接从Redis中读取并返回
            return BeanUtil.toBean(map, UnitTypeDO.class);
        }
        // 如果数据库和Redis中均未找到该单位类型信息，返回null
        return null;
    }
}
