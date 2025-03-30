package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CourseLibraryMapper;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CourseLibraryDAO extends ServiceImpl<CourseLibraryMapper, CourseLibraryDO> {

    private final RedissonClient redisson;

    /**
     * 根据UUID获取课程库信息
     * 首先尝试从Redis中获取课程库信息，如果不存在，则从数据库中查询，并将结果缓存到Redis中
     *
     * @param courseLibraryUuid 课程库的UUID
     * @return CourseLibraryDO类型的对象，如果找不到则返回null
     */
    public CourseLibraryDO getCourseLibraryByUuid(String courseLibraryUuid) {
        // 获取Redis中的课程库信息映射
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryUuid);
        // 检查Redis中是否不存在该课程库信息
        if(!map.isExists()){
            // 从数据库中获取课程库信息
            CourseLibraryDO courseLibraryDO = this.getById(courseLibraryUuid);
            // 如果数据库中存在该课程库信息
            if (courseLibraryDO != null) {
                // 将课程库信息转换为字符串映射，并存入Redis中
                map.putAll(ConvertUtil.convertObjectToMapString(courseLibraryDO));
                // 设置Redis缓存的过期时间为86400秒
                map.expire(Duration.ofSeconds(86400));
                // 返回从数据库中获取的课程库信息
                return courseLibraryDO;
            }
        } else {
            // 如果Redis中存在该课程信息，则直接转换并返回课程对象
            return BeanUtil.toBean(map, CourseLibraryDO.class);
        }
        // 如果Redis和数据库中均未找到课程库信息，则返回null
        return null;
    }
}
