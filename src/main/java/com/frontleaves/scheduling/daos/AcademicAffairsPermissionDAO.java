package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.AcademicAffairsPermissionMapper;
import com.frontleaves.scheduling.models.entity.AcademicAffairsPermissionDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 教务权限 数据访问对象
 *
 * @author FLASHLACK
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class AcademicAffairsPermissionDAO extends ServiceImpl<AcademicAffairsPermissionMapper, AcademicAffairsPermissionDO> {
    private final RedissonClient redisson;


    /**
     * 根据UUID获取学术事务权限信息
     * 首先尝试从Redis中获取权限信息，如果不存在，则从数据库中获取，并将其存入Redis中以供下次快速访问
     *
     * @param uuid 权限记录的唯一标识符
     * @return 如果找到权限信息，则返回AcademicAffairsPermissionDO对象；否则返回null
     */
    public AcademicAffairsPermissionDO getAcademicAffairsPermissionByUuid(String uuid) {
        // 构造Redis中存储权限信息的键名
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.ACADEMIC_AFFAIRS_PERMISSION_UUID + uuid);
        // 检查Redis中是否存在该权限信息
        if (!rMap.isExists()) {
            // 如果Redis中不存在，尝试从数据库中获取权限信息
            AcademicAffairsPermissionDO academicAffairsPermissionDO = this.getById(uuid);
            // 如果数据库中存在该权限信息，将其存入Redis，并设置过期时间
            if (academicAffairsPermissionDO != null) {
                rMap.putAll(ConvertUtil.convertObjectToMapString(academicAffairsPermissionDO));
                rMap.expire(Duration.ofSeconds(86400));
                return academicAffairsPermissionDO;
            }
        } else {
            // 如果Redis中存在该权限信息，将其转换为AcademicAffairsPermissionDO对象并返回
            return BeanUtil.toBean(rMap, AcademicAffairsPermissionDO.class);
        }
        return null;
    }

    /**
     * 根据用户UUID获取学术事务权限信息
     * 首先尝试从Redis中获取权限信息，如果不存在，则从数据库中查询，并将结果缓存到Redis中
     *
     * @param userUuid 用户UUID
     * @return 学术事务权限信息对象，如果未找到则返回null
     */
    public AcademicAffairsPermissionDO getAcademicAffairsPermissionByUserUuid(String userUuid) {
        // 构造Redis桶的键名，用于存储学术事务权限信息
        RBucket<String> rBucket = redisson.getBucket(
                StringConstant.Redis.ACADEMIC_AFFAIRS_PERMISSION_USER_UUID + userUuid);
        // 检查Redis中是否已存在该用户的权限信息
        if (!rBucket.isExists()) {
            // 如果Redis中不存在权限信息，则从数据库中查询
            AcademicAffairsPermissionDO academicAffairsPermissionDO =
                    this.lambdaQuery().eq(AcademicAffairsPermissionDO::getAuthorizedUser, userUuid).one();
            // 如果查询到权限信息，则将其添加到Redis中，并设置过期时间
            if (academicAffairsPermissionDO != null) {
                rBucket.set(academicAffairsPermissionDO.getAcademicAffairsPermissionUuid());
                rBucket.expire(Duration.ofSeconds(86400));
                // 同时将权限信息的详细字段存储到Redis的Map中，并设置过期时间
                RMap<String, String> rMap = redisson.getMap(
                        StringConstant.Redis.ACADEMIC_AFFAIRS_PERMISSION_UUID
                                + academicAffairsPermissionDO.getAcademicAffairsPermissionUuid());
                rMap.putAll(ConvertUtil.convertObjectToMapString(academicAffairsPermissionDO));
                rMap.expire(Duration.ofSeconds(86400));
                // 返回查询到的权限信息对象
                return academicAffairsPermissionDO;
            }
        } else {
            // 如果Redis中已存在权限信息，则根据缓存的UUID从Redis中获取并返回权限信息对象
            return this.getAcademicAffairsPermissionByUuid(rBucket.get());
        }
        // 如果未找到权限信息，则返回null
        return null;
    }

    public RedissonClient getRedisson() {
        return redisson;
    }
}
