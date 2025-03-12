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
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
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
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.GradeMapper;
import com.frontleaves.scheduling.models.entity.GradeDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 年级数据访问对象
 * <p>
 * 此类继承自ServiceImpl，实现IService接口，主要负责对年级（Grade）数据的操作，
 * 包括但不限于增、删、改、查等数据库操作。通过与GradeMapper的交互，
 * 提供了面向业务的数据库访问方法。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class GradeDAO extends ServiceImpl<GradeMapper, GradeDO> implements IService<GradeDO> {
    private final RedissonClient redisson;


    /**
     * 根据UUID获取年级信息
     * 首先尝试从Redis中获取年级信息，如果不存在，则从数据库中获取，并将其存入Redis中以供下次快速访问
     *
     * @param uuid 年级的唯一标识符
     * @return 返回年级对象，如果找不到则返回null
     */
    public GradeDO getGradeByUuid(String uuid) {
        // 构造Redis中年级信息的键
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.GRADE_UUID + uuid);
        // 检查Redis中是否存在该年级信息
        if (!rMap.isExists()) {
            // 如果Redis中不存在，从数据库中获取年级信息
            GradeDO gradeDO = this.getById(uuid);

            // 如果从数据库中获取到了年级信息
            if (gradeDO != null) {
                // 将年级信息转换为Map并存入Redis
                rMap.putAll(ConvertUtil.convertObjectToMapString(gradeDO));
                // 设置Redis中年级信息的过期时间
                rMap.expire(Duration.ofSeconds(86400));
                // 返回从数据库中获取到的年级信息
                return gradeDO;
            }
        } else {
            // 如果Redis中存在该年级信息，将其转换为GradeDO对象并返回
            return BeanUtil.toBean(rMap, GradeDO.class);
        }
        // 如果既没有从Redis中获取到年级信息，也没有从数据库中获取到，返回null
        return null;
    }
}
