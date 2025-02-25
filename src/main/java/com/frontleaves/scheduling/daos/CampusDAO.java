package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CampusMapper;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 校区数据访问对象
 * <p>
 * 该类实现了对校区数据的增删改查操作，并提供了通过校区UUID、名称和编码获取校区信息的方法。
 * 同时，利用Redis进行数据缓存，以提高查询效率。
 * </p>
 *
 * @version v1.0.0
 * @since v1.0.0
 * @author xiao_lfeng
 */
@Repository
@RequiredArgsConstructor
public class CampusDAO extends ServiceImpl<CampusMapper, CampusDO> implements IService<CampusDO> {
    private final RedissonClient redisson;

    /**
     * 通过校区UUID获取校区信息
     * <p>
     * 该方法根据给定的校区唯一标识 {@code campusUuid} 从Redis缓存或数据库中查询对应的校区信息。如果在Redis缓存中没有找到相应的数据，则从数据库中查询，并将结果缓存到Redis中以提高后续查询效率。
     * 如果缓存中存在，则直接从缓存中读取校区信息。
     * </p>
     *
     * @param campusUuid 校区唯一标识
     * @return 返回与给定校区唯一标识匹配的校区信息，如果未找到则返回null
     * @throws ServerInternalErrorException 如果服务器内部发生错误
     */
    public CampusDO getCampusByUuid(String campusUuid) throws ServerInternalErrorException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.CAMPUS_UUID + campusUuid);
        if (!map.isExists()) {
            CampusDO campusDO = getById(campusUuid);
            if (campusDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(campusDO));
                map.expire(Duration.ofSeconds(86400));
                return campusDO;
            }
        } else {
            return BeanUtil.toBean(map, CampusDO.class);
        }
        return null;
    }

    /**
     * 通过校区名称获取校区信息
     * <p>
     * 该方法根据给定的校区名称从Redis缓存或数据库中查询对应的校区信息。如果在Redis缓存中没有找到相应的数据，则会从数据库中进行查询，并将查询结果存入Redis缓存中以便后续快速访问。
     * </p>
     *
     * @param campusName 校区名称
     * @return 返回与给定校区名称匹配的校区实体对象，如果未找到则返回 {@code null}
     * @throws ServerInternalErrorException 如果在执行过程中发生服务器内部错误
     */
    public CampusDO getCampusByName(String campusName) throws ServerInternalErrorException {
        RBucket<String> rBucket = redisson.getBucket(StringConstant.Redis.CAMPUS_NAME + campusName);
        return getCampusAndCache(
                rBucket,
                this.lambdaQuery().eq(CampusDO::getCampusName, campusName)
        );
    }

    /**
     * 通过校区编码获取校区信息
     * <p>
     * 该方法通过传入的校区编码 {@code campusCode}，从Redis缓存或数据库中查询并返回对应的校区信息。
     * 如果在Redis缓存中未找到，则从数据库中查询，并将结果缓存到Redis中以提高后续查询效率。
     * </p>
     *
     * @param campusCode 校区编码
     * @return 返回与给定校区编码匹配的校区信息，如果未找到则返回null
     * @throws ServerInternalErrorException 如果在处理过程中发生服务器内部错误
     */
    public CampusDO getCampusByCode(String campusCode) throws ServerInternalErrorException {
        RBucket<String> rBucket = redisson.getBucket(StringConstant.Redis.CAMPUS_CODE + campusCode);
        return getCampusAndCache(
                rBucket,
                this.lambdaQuery().eq(CampusDO::getCampusCode, campusCode)
        );
    }

    /**
     * 从缓存或数据库中获取校区信息
     * <p>
     * 该方法首先检查Redis缓存中是否存在指定的校区信息。如果不存在，则从数据库中查询校区信息，并将查询结果存入Redis缓存中。
     * 如果缓存中存在，则直接从缓存中读取校区信息。
     * </p>
     *
     * @param rBucket Redis缓存桶，用于存储和获取校区UUID
     * @param eq 查询条件链，用于构建查询条件
     * @return 返回查询到的校区信息，如果未找到则返回null
     */
    @Nullable
    public CampusDO getCampusAndCache(@NotNull RBucket<String> rBucket, LambdaQueryChainWrapper<CampusDO> eq) {
        if (!rBucket.isExists()) {
            CampusDO campusDO = eq.one();
            if (campusDO != null) {
                // 插入 Redis 数据并设置过期时间
                rBucket.set(campusDO.getCampusUuid());
                rBucket.expire(Duration.ofSeconds(86400));
                RMap<String, String> campusMap = redisson.getMap(StringConstant.Redis.CAMPUS_UUID + campusDO.getCampusUuid());
                campusMap.putAll(ConvertUtil.convertObjectToMapString(campusDO));
                campusMap.expire(Duration.ofSeconds(86400));
                return campusDO;
            }
        } else {
            return this.getCampusByUuid(rBucket.get());
        }
        return null;
    }
}
