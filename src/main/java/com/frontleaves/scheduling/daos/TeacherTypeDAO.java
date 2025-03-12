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
import com.frontleaves.scheduling.mappers.TeacherTypeMapper;
import com.frontleaves.scheduling.models.entity.TeacherTypeDO;
import com.xlf.utility.util.ConvertUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 教师类型数据访问对象
 * <p>
 * 此类继承自ServiceImpl，实现IService接口，主要负责对教师类型（TeacherType）数据的操作，
 * 包括但不限于增、删、改、查等数据库操作。通过与TeacherTypeMapper的交互，
 * 提供了面向业务的数据库访问方法。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherTypeDAO extends ServiceImpl<TeacherTypeMapper, TeacherTypeDO> implements IService<TeacherTypeDO> {

    private final RedissonClient redisson;

    public TeacherTypeDO getTeacherTypeByUuid(@NotNull String teacherTypeUuid) {
        RMap<String,String> map =redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + teacherTypeUuid);
        if (map.isEmpty()){
            log.debug("{}", teacherTypeUuid);
            TeacherTypeDO teacherType = this.getById(teacherTypeUuid);
            if (teacherType != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(teacherType));
                map.expire(Duration.ofSeconds(86400));
                return teacherType;
            }
        } else {
            return BeanUtil.toBean(map, TeacherTypeDO.class);
        }
        return null;
    }
}
