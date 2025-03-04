package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.DepartmentMapper;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RTransaction;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

/**
 * 部门 数据访问对象
 * @author FLASHLACK
 */
@Repository
@RequiredArgsConstructor
public class DepartmentDAO extends ServiceImpl<DepartmentMapper, DepartmentDO> implements
        IService<DepartmentDO> {

    // Redis 缓存客户端
    private final RedissonClient redisson;

    /**
     * 根据部门的UUID获取部门对象
     * 首先尝试从Redis中获取部门信息，如果不存在，则从数据库中获取，并将结果缓存到Redis中
     *
     * @param departmentUuid 部门的唯一标识符
     * @return DepartmentDO类型的对象，表示部门信息，如果找不到则返回null
     */
    public DepartmentDO getDepartmentByUuid(String departmentUuid) {
        // 从Redis中获取部门信息
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.DEPARTMENT_UUID + departmentUuid);
        if (!map.isExists()) {
            // 如果Redis中不存在该部门信息，则从数据库中获取
            DepartmentDO departmentDO = this.getById(departmentUuid);
            if (departmentDO != null) {
                // 将获取到的部门信息存入Redis，并设置过期时间
                map.putAll(ConvertUtil.convertObjectToMapString(departmentDO));
                map.expire(Duration.ofSeconds(86400));
                return departmentDO;
            }
        } else {
            // 如果Redis中存在该部门信息，则直接转换并返回部门对象
            return BeanUtil.toBean(map, DepartmentDO.class);
        }
        return null;
    }

    @Transactional
    public void deleteDepartment(DepartmentDO departmentDO) throws ServerInternalErrorException {



    }

    @Transactional
    public void updateDepartment(DepartmentDO departmentDO) {
        redisson.getKeys().deleteByPattern(StringConstant.Redis.DEPARTMENT_LIST);
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            //this.deleteDepartmentRedis(departmentDO.getDepartmentUuid());
            this.updateById(departmentDO);
            transaction.commit();
        } catch (Exception e) {
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }

    }

    private void deleteDepartmentRedis(@NotNull RTransaction rTransaction, @NotNull DepartmentDO departmentDO) {

    }

    public Page<DepartmentDO> getDepartmentList(@NotNull Integer page, @NotNull Integer size, Boolean isDesc, String name) {
        LambdaQueryChainWrapper<DepartmentDO> query = this.lambdaQuery();
        if (name != null && !name.isEmpty()) {
            query.like(DepartmentDO::getDepartmentName, name);
        }
        if(Boolean.TRUE.equals(isDesc)) {
            query.orderByDesc(DepartmentDO::getDepartmentCode);
        } else {
            query.orderByAsc(DepartmentDO::getDepartmentCode);
        }
        return query.page(new Page<>(page, size));
    }
}
