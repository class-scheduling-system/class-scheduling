package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.DepartmentMapper;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 部门 数据访问对象
 *
 * @author FLASHLACK
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class DepartmentDAO extends ServiceImpl<DepartmentMapper, DepartmentDO> implements
        IService<DepartmentDO> {
    private final RedissonClient redisson;

    /**
     * 根据部门 UUID 获取部门信息
     * <p>
     * 该方法用于根据部门 UUID 获取部门信息。
     * 该方法首先会从 Redis 缓存中获取部门信息，如果缓存中不存在，则会从数据库中获取部门信息。
     * 如果数据库中存在部门信息，则将部门信息存入 Redis 缓存中，并设置过期时间为 24 小时。
     * 如果数据库中不存在部门信息，则返回 null。
     * </p>
     *
     * @param departmentUuid 部门 UUID
     * @return 部门信息
     */
    public DepartmentDO getDepartmentByUuid(String departmentUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.DEPARTMENT_UUID + departmentUuid);
        if (map.isEmpty()) {
            DepartmentDO departmentDO = this.getById(departmentUuid);
            if (departmentDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(departmentDO));
                map.expire(Duration.ofSeconds(86400));
                return departmentDO;
            }
        } else {
            return BeanUtil.toBean(map, DepartmentDO.class);
        }
        return null;
    }
}
