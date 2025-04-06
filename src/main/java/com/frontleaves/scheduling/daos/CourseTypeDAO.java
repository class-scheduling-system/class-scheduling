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
import com.frontleaves.scheduling.mappers.CourseTypeMapper;
import com.frontleaves.scheduling.models.entity.base.CourseTypeDO;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * 课程类型数据访问对象
 * <p>
 * 该类继承自 MyBatis-Plus 的 ServiceImpl 类，提供了对课程类型表的基本 CRUD 操作。
 * 通过该类可以方便地操作课程类型数据。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CourseTypeDAO extends ServiceImpl<CourseTypeMapper, CourseTypeDO> {

    /**
     * Redis 客户端
     */
    private final RedissonClient redisson;

    /**
     * 根据UUID获取课程类型
     * <p>
     * 该方法首先尝试从Redis缓存中获取课程类型信息，如果不存在，则从数据库中获取，
     * 并将查询结果缓存到Redis中以提高后续查询效率。缓存过期时间为24小时。
     * </p>
     *
     * @param courseTypeUuid 课程类型UUID
     * @return 课程类型数据对象，如果不存在则返回null
     */
    @Nullable
    public CourseTypeDO getCourseTypeByUuid(String courseTypeUuid) {
        // 获取Redis中的课程类型信息映射
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.COURSE_TYPE_UUID + courseTypeUuid);
        // 检查Redis中是否不存在该课程类型信息
        if (!map.isExists()) {
            // 从数据库中获取课程类型信息
            CourseTypeDO courseTypeDO = this.getById(courseTypeUuid);
            // 如果数据库中存在该课程类型信息
            if (courseTypeDO != null) {
                // 将课程类型信息转换为字符串映射，并存入Redis中
                map.putAll(ConvertUtil.convertObjectToMapString(courseTypeDO));
                // 设置Redis缓存的过期时间为86400秒（24小时）
                map.expire(Duration.ofSeconds(86400));
                // 返回从数据库中获取的课程类型信息
                return courseTypeDO;
            }
        } else {
            // 如果Redis中存在该课程类型信息，则直接转换并返回课程类型对象
            return BeanUtil.toBean(map, CourseTypeDO.class);
        }
        // 如果Redis和数据库中均未找到课程类型信息，则返回null
        return null;
    }

    /**
     * 获取课程类型列表
     * <p>
     * 该方法首先尝试从Redis缓存中获取课程类型列表，如果不存在，则从数据库中获取，
     * 并将查询结果缓存到Redis中以提高后续查询效率。缓存过期时间为24小时。
     * </p>
     *
     * @return 课程类型列表
     */
    public List<CourseTypeDO> getCourseTypeList() {
        // 构建缓存键
        String cacheKey = StringConstant.Redis.COURSE_TYPE_LIST;
        // 尝试从缓存获取数据
        RList<CourseTypeDO> cacheList = redisson.getList(cacheKey);
        if (!cacheList.isExists()) {
            List<CourseTypeDO> courseTypeList = this.list();
            cacheList.addAll(courseTypeList);
            cacheList.expire(Duration.ofSeconds(86400));
            return courseTypeList;
        }
        return cacheList.readAll();
    }

    /**
     * 获取课程类型列表
     * 首先尝试从Redis中获取课程类型列表如果Redis中不存在，则从数据库中获取，并存入Redis中
     * 此方法解释了为什么要在Redis中缓存课程类型列表：提高访问速度，减轻数据库压力
     *
     * @return 课程类型列表
     */
    public List<CourseTypeDO> listCourseType() {
        // 从Redis中获取课程类型列表
        RList<CourseTypeDO> rList = redisson.getList(StringConstant.Redis.COURSE_TYPE_LIST);
        // 检查Redis中是否存在课程类型列表
        if (!rList.isExists()) {
            // 从数据库中获取课程类型列表
            List<CourseTypeDO> courseTypeDOList = this.list();
            // 检查获取的课程类型列表是否为空
            if (courseTypeDOList != null) {
                // 将课程类型列表添加到Redis中，并设置过期时间
                rList.addAll(courseTypeDOList);
                rList.expire(Duration.ofSeconds(3600));
                return courseTypeDOList;
            }
            // 如果数据库中获取的数据为空，则直接返回数据库查询结果
            return list();
        }
        // 如果Redis中存在课程类型列表，则直接读取并返回
        return rList.readAll();
    }
}
