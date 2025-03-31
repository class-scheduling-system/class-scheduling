package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CourseLibraryMapper;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 课程库数据访问对象
 * <p>
 * 该类实现了对课程库数据的增删改查操作，并提供了通过课程UUID获取课程信息的方法。
 * 同时，利用Redis进行数据缓存，以提高查询效率。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CourseLibraryDAO extends ServiceImpl<CourseLibraryMapper, CourseLibraryDO> {
    private final RedissonClient redisson;

    /**
     * 根据课程的唯一标识获取课程信息
     * <p>
     * 该方法首先尝试从 Redis 缓存中获取课程数据。如果缓存中没有找到对应的课程信息，则会从数据库中查询。
     * 查询到的数据会被存入 Redis 缓存中，并设置过期时间为一天（86400秒）。如果在缓存和数据库中都没有找到对应的课程信息，则返回 {@code null}。
     * </p>
     *
     * @param courseUuid 课程的唯一标识符
     * @return 返回与给定 UUID 对应的课程对象，如果没有找到则返回 {@code null}
     */
    @Nullable
    public CourseLibraryDO getCourseByUuid(String courseUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseUuid);
        if (map.isEmpty()) {
            CourseLibraryDO courseLibraryDO = this.getById(courseUuid);
            if (courseLibraryDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(courseLibraryDO));
                map.expire(Duration.ofSeconds(86400));
                return courseLibraryDO;
            }
        } else {
            return BeanUtil.toBean(map, CourseLibraryDO.class);
        }
        return null;
    }
}

