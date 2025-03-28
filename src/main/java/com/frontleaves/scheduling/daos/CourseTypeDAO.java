package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CourseTypeMapper;
import com.frontleaves.scheduling.models.entity.CourseTypeDO;
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
 * 课程类型 DAO 类
 * 对应数据库表：cs_course_type
 *
 * @author FLASHLACK
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class CourseTypeDAO extends ServiceImpl<CourseTypeMapper, CourseTypeDO> {
    private final RedissonClient redisson;

    /**
     * 根据UUID获取课程类型信息
     * 首先尝试从Redis中获取课程类型信息，如果不存在，则从数据库中获取，并存入Redis中以缓存
     *
     * @param uuid 课程类型的唯一标识符
     * @return CourseTypeDO对象，包含课程类型信息，如果找不到则返回null
     */
    public CourseTypeDO getCourseTypeByUuid(String uuid) {
        // 构造Redis中课程类型信息的键
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.COURSE_TYPE_UUID + uuid);

        // 检查Redis中是否存在该课程类型信息
        if (!rMap.isExists()) {
            // 如果Redis中不存在，从数据库中获取课程类型信息
            CourseTypeDO courseTypeDO = this.getById(uuid);

            // 如果数据库中存在该课程类型信息
            if (courseTypeDO != null) {
                // 将课程类型信息转换为Map并存入Redis中以缓存
                rMap.putAll(ConvertUtil.convertObjectToMapString(courseTypeDO));
                // 设置Redis缓存的过期时间为1小时
                rMap.expire(Duration.ofSeconds(3600));
                // 返回从数据库中获取的课程类型信息
                return courseTypeDO;
            }
            // 如果数据库中也不存在，返回null
            return null;
        }
        // 如果Redis中存在该课程类型信息，将其转换为CourseTypeDO对象并返回
        return BeanUtil.toBean(rMap, CourseTypeDO.class);
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