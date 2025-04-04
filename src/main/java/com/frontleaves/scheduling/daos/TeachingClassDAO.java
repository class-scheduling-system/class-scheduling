package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TeachingClassMapper;
import com.frontleaves.scheduling.models.entity.TeachingClassDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
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

}
