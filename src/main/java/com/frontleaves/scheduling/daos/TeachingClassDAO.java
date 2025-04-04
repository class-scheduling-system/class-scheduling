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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TeachingClassMapper;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
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
 * 教学班数据访问对象
 * @author FLASHLACK
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeachingClassDAO extends ServiceImpl<TeachingClassMapper, TeachingClassDO> {
    private final RedissonClient redisson;

/**
 * 根据学期UUID获取教学班级列表
 * 首先尝试从Redis中获取缓存的班级列表，如果不存在，则从数据库中查询，并将结果缓存到Redis中
 *
 * @param semesterUuid 学期的唯一标识符
 * @return 教学班级列表，如果找不到则返回空列表
 */
public List<TeachingClassDO> getTeachingClassBySemester(String semesterUuid) {
    // 从Redis中获取缓存的 teachingClass 列表
    RList<TeachingClassDO> list = redisson.getList(
            StringConstant.Redis.TEACHING_CLASS_LIST_SEMESTER + semesterUuid);

    // 检查缓存是否存在，如果不存在，则从数据库中查询
    if (!list.isExists()) {
        List<TeachingClassDO> teachingClassList = this.lambdaQuery()
                .eq(TeachingClassDO::getSemesterUuid, semesterUuid)
                .eq(TeachingClassDO::getIsAdministrative,true)
                .eq(TeachingClassDO::getIsEnabled,1)
                .list();

        // 如果查询结果不为空，则将其添加到缓存列表中，并设置过期时间
        if (teachingClassList != null && !teachingClassList.isEmpty()) {
            list.addAll(teachingClassList);
            list.expire(Duration.ofSeconds(3600));
        }
        // 返回查询结果，如果为空则返回空列表
        return List.of();
    }
    // 缓存命中，返回所有缓存的 teachingClass
    return list.readAll();
}

    public TeachingClassDO getTeachingClassByUuid(String teachingClassUuid) {
        RMap<String,String> rMap = redisson.getMap(
                StringConstant.Redis.TEACHING_CLASS_UUID + teachingClassUuid);
        if (!rMap.isExists()){
            TeachingClassDO teachingClassDO = this.lambdaQuery()
                    .eq(TeachingClassDO::getTeachingClassUuid,teachingClassUuid)
                    .one();
            if (teachingClassDO != null) {
                rMap.putAll(ConvertUtil.convertObjectToMapString(teachingClassDO));
                rMap.expire(Duration.ofSeconds(3600));
            }
            return null;
        }
        return BeanUtil.toBean(rMap,TeachingClassDO.class);
    }
}
