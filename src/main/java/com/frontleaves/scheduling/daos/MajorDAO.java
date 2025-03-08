/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.DepartmentMapper;
import com.frontleaves.scheduling.mappers.MajorMapper;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * 专业数据访问对象
 * <p>
 * 此类继承自ServiceImpl，实现IService接口，主要负责对专业（Major）数据的操作，
 * 包括但不限于增、删、改、查等数据库操作。通过与MajorMapper的交互，
 * 提供了面向业务的数据库访问方法。
 * </p>
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author FLASHLACK
 */
@Repository
public class MajorDAO extends ServiceImpl<MajorMapper, MajorDO> implements IService<MajorDO> {

    private final RedissonClient redisson;
    private final DepartmentMapper departmentMapper;

    @Autowired
    public MajorDAO(RedissonClient redisson, DepartmentMapper departmentMapper) {
        this.redisson = redisson;
        this.departmentMapper = departmentMapper;
    }

    /**
     * 根据专业 UUID 获取专业信息
     */
    public MajorDO getMajorByUuid(String majorUuid) {
        // 先从 Redis 查询
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.MAJOR_UUID + majorUuid);
        if (!map.isExists()) {
            // Redis无数据,查询数据库
            MajorDO majorDO = this.getById(majorUuid);
            if (majorDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(majorDO));
                map.expire(Duration.ofSeconds(86400));
                return majorDO;
            }
        } else {
            return BeanUtil.toBean(map, MajorDO.class);
        }
        return null;
    }

    /**
     * 判断专业是否被系统中其他数据引用
     *
     * @param majorUuid 专业UUID
     * @return 如果有引用则返回 true,否则返回 false
     */
    public boolean isReferenced(String majorUuid) {
        // 查询引用了指定专业UUID的部分的数量
        Long count = departmentMapper.selectCount(
                new LambdaQueryWrapper<DepartmentDO>()
                        .eq(DepartmentDO::getMajorUuid, majorUuid)
        );
        // 若查询结果不为空且大于零,说明该专业被引用
        return count != null && count > 0;
    }

    public Page<MajorDO> listMajors(int page, int size, Boolean isDesc, String department, String name) {
        Page<MajorDO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MajorDO> queryWrapper = new LambdaQueryWrapper<>();

        if (CharSequenceUtil.isNotBlank(department)) {
            List<String> departmentUuids = departmentMapper.getDepartmentUuidByName(department);
            if (departmentUuids.isEmpty()) {
                throw new BusinessException("学院不存在", ErrorCode.BODY_ERROR);
            } else {
                queryWrapper.in(MajorDO::getDepartmentUuid, departmentUuids);
            }
        }
        if (CharSequenceUtil.isNotBlank(name)) {
            queryWrapper.like(MajorDO::getMajorName, name);
        }
        queryWrapper.orderBy(isDesc == null || isDesc, false, MajorDO::getCreatedAt);

        return this.page(pageParam, queryWrapper);
    }

}

