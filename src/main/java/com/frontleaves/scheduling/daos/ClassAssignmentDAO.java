package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.ClassAssignmentMapper;
import com.frontleaves.scheduling.models.entity.base.ClassAssignmentDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 排课分配数据访问对象
 * <p>
 * 该类提供了对排课分配数据的操作方法，包括从 Redis 或数据库中获取排课分配信息、更新排课分配信息等。
 * 通过继承 {@code ServiceImpl} 类并实现 {@code IService} 接口，提供了基础的 CRUD 操作，并扩展了特定的业务逻辑。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ClassAssignmentDAO extends ServiceImpl<ClassAssignmentMapper, ClassAssignmentDO> {
    private final RedissonClient redisson;

    /**
     * 根据排课分配的唯一标识获取排课分配信息
     * <p>
     * 该方法首先尝试从 Redis 缓存中获取排课分配数据。如果缓存中没有找到对应的排课分配信息，则会从数据库中查询。
     * 查询到的数据会被存入 Redis 缓存中，并设置过期时间为一天（86400秒）。如果在缓存和数据库中都没有找到对应的排课分配信息，则返回 {@code null}。
     *
     * @param classAssignmentUuid 排课分配的唯一标识符
     * @return 返回与给定 UUID 对应的排课分配对象，如果没有找到则返回 {@code null}
     */
    @Nullable
    public ClassAssignmentDO getClassAssignmentByUuid(String classAssignmentUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.CLASS_ASSIGNMENT_UUID + classAssignmentUuid);
        if (map.isEmpty()) {
            ClassAssignmentDO entity = this.getById(classAssignmentUuid);
            if (entity != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(entity));
                map.expire(Duration.ofSeconds(86400));
                return entity;
            }
        } else {
            return BeanUtil.toBean(map, ClassAssignmentDO.class);
        }
        return null;
    }

    /**
     * 获取分页排课分配列表
     * <p>
     * 该方法用于获取排课分配的分页列表，支持按学期、课程和教师进行筛选。
     * 结果会被缓存在 Redis 中，缓存时间为一小时。
     * </p>
     *
     * @param page         页码
     * @param size         每页大小
     * @param semesterUuid 学期UUID（可选）
     * @param courseUuid   课程UUID（可选）
     * @param teacherUuid  教师UUID（可选）
     * @return 返回分页的排课分配列表
     */
    public Page<ClassAssignmentDO> page(Integer page, Integer size, String semesterUuid, String courseUuid, String teacherUuid) {
        // 构建缓存键
        String cacheKey = StringConstant.Redis.CLASS_ASSIGNMENT_PAGE +
                page + ":" + size + ":" +
                (semesterUuid != null ? semesterUuid : "all") + ":" +
                (courseUuid != null ? courseUuid : "all") + ":" +
                (teacherUuid != null ? teacherUuid : "all");

        RMap<String, String> cacheMap = redisson.getMap(cacheKey);
        if (cacheMap.isEmpty()) {
            // 构建查询条件
            LambdaQueryChainWrapper<ClassAssignmentDO> wrapper = this.lambdaQuery();
            if (semesterUuid != null) {
                wrapper.eq(ClassAssignmentDO::getSemesterUuid, semesterUuid);
            }
            if (courseUuid != null) {
                wrapper.eq(ClassAssignmentDO::getCourseUuid, courseUuid);
            }
            if (teacherUuid != null) {
                wrapper.eq(ClassAssignmentDO::getTeacherUuid, teacherUuid);
            }

            return ProjectUtil.queryAndCache(wrapper, page, size, cacheMap);
        } else {
            return ProjectUtil.convertMapToPage(cacheMap, ClassAssignmentDO.class);
        }
    }

    /**
     * 获取排课分配列表
     * <p>
     * 该方法用于获取排课分配的列表，支持按学期、课程和教师进行筛选。
     * 结果会被缓存在 Redis 中，缓存时间为一小时。
     * </p>
     *
     * @param semesterUuid          学期UUID（可选）
     * @param courseUuid            课程UUID（可选）
     * @param teacherUuid           教师UUID（可选）
     * @param teachingClassUuidList 教学班UUID列表（可选）
     * @return 返回排课分配列表
     */
    public List<ClassAssignmentDO> getList(
            String semesterUuid,
            String courseUuid,
            String teacherUuid,
            List<String> teachingClassUuidList) {
        // 构建缓存键，包含教学班列表的哈希
        String cacheKey = StringConstant.Redis.CLASS_ASSIGNMENT_LIST +
                (semesterUuid != null ? semesterUuid : "all") + ":" +
                (courseUuid != null ? courseUuid : "all") + ":" +
                (teacherUuid != null ? teacherUuid : "all") + ":" +
                teachingClassUuidList.hashCode();
        RList<ClassAssignmentDO> cacheList = redisson.getList(cacheKey);
        if (!cacheList.isExists()) {
            // 构建查询条件
            LambdaQueryWrapper<ClassAssignmentDO> wrapper = new LambdaQueryWrapper<>();
            if (semesterUuid != null) {
                wrapper.eq(ClassAssignmentDO::getSemesterUuid, semesterUuid);
            }
            if (courseUuid != null) {
                wrapper.eq(ClassAssignmentDO::getCourseUuid, courseUuid);
            }
            if (teacherUuid != null) {
                wrapper.eq(ClassAssignmentDO::getTeacherUuid, teacherUuid);
            }
            // 添加教学班UUID列表过滤条件
            if (teachingClassUuidList != null && !teachingClassUuidList.isEmpty()) {
                teachingClassUuidList.forEach(teachingClassUuid ->
                        wrapper.or().like(ClassAssignmentDO::getTeachingClassUuid, teachingClassUuid)
                );
            }
            // 执行查询
            List<ClassAssignmentDO> entityList = this.list(wrapper);
            // 如果查询结果为空，返回空列表
            if (entityList.isEmpty()) {
                return new ArrayList<>();
            }
            // 缓存结果
            cacheList.addAll(entityList);
            cacheList.expire(Duration.ofHours(1));

            return entityList;
        }
        return cacheList.readAll();
    }

    /**
     * 更新排课分配信息
     * <p>
     * 该方法用于更新排课分配信息，同时会清除相关的 Redis 缓存。
     * </p>
     *
     * @param entity 要更新的排课分配信息
     * @throws ServerInternalErrorException 如果更新操作失败
     */
    public void updateClassAssignment(ClassAssignmentDO entity) {
        RKeys keys = redisson.getKeys();
        this.updateById(entity);
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_UUID + entity.getClassAssignmentUuid());
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_LIST + "*");
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_PAGE + "*");
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_UUID + "*");
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_LIST_SEMESTER + "*");
        // 删除冲突缓存
        keys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST + "*");
        keys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST_CLASS_ASSIGNMENT + "*");
    }

    /**
     * 删除排课分配
     * <p>
     * 该方法用于删除指定的排课分配信息，同时会清除相关的 Redis 缓存。
     * </p>
     *
     * @param classAssignmentUuid 要删除的排课分配的UUID
     * @throws ServerInternalErrorException 如果删除操作失败
     */
    public void removeClassAssignment(String classAssignmentUuid) {
        RKeys keys = redisson.getKeys();
        this.removeById(classAssignmentUuid);
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_UUID + classAssignmentUuid);
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_LIST + "*");
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_PAGE + "*");
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_LIST_SEMESTER + "*");
        // 删除冲突缓存
        keys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST + "*");
        keys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST_CLASS_ASSIGNMENT + "*");
    }

    /**
     * 获取排课分配分页数据
     * <p>
     * 该方法用于获取排课分配的分页数据，支持按学期、课程和教师进行筛选。
     * 结果会被缓存在 Redis 中，缓存时间为一小时。
     * </p>
     *
     * @param page              当前页码
     * @param size              每页大小
     * @param semesterUuid      学期UUID（可选）
     * @param courseUuid        课程UUID（可选）
     * @param teacherUuid       教师UUID（可选）
     * @param teachingClassUuidList 教学班UUID列表（可选）
     * @return 返回排课分配分页数据
     */
    @Nullable
    public Page<ClassAssignmentDO> getClassAssignmentPage(
            Integer page,
            Integer size,
            String semesterUuid,
            String courseUuid,
            String teacherUuid, List<String> teachingClassUuidList) {

        String cacheKey = StringConstant.Redis.CLASS_ASSIGNMENT_PAGE +
                page + ":" + size + ":" +
                (semesterUuid != null ? semesterUuid : "all") + ":" +
                (courseUuid != null ? courseUuid : "all") + ":" +
                (teacherUuid != null ? teacherUuid : "all") + ":" +
                teachingClassUuidList.hashCode();
        RMap<String, String> cacheMap = redisson.getMap(cacheKey);
        if (cacheMap.isEmpty()) {
            // 构建查询条件
            LambdaQueryChainWrapper<ClassAssignmentDO> wrapper = this.lambdaQuery();
            if (semesterUuid != null) {
                wrapper.eq(ClassAssignmentDO::getSemesterUuid, semesterUuid);
            }
            if (courseUuid != null) {
                wrapper.eq(ClassAssignmentDO::getCourseUuid, courseUuid);
            }
            if (teacherUuid != null) {
                wrapper.eq(ClassAssignmentDO::getTeacherUuid, teacherUuid);
            }
            // 添加教学班UUID列表的过滤条件
            if (teachingClassUuidList != null && !teachingClassUuidList.isEmpty()) {
                teachingClassUuidList.forEach(
                        teachingClassUuid -> wrapper.or().like(ClassAssignmentDO::getTeachingClassUuid, teachingClassUuid)
                );
            }
            // 执行分页查询并缓存结果
            return ProjectUtil.queryAndCache(wrapper, page, size, cacheMap);
        } else {
            return ProjectUtil.convertMapToPage(cacheMap, ClassAssignmentDO.class);
        }
    }

    /**
     * 根据学期UUID获取排课分配列表
     *
     * @param semesterUuid 学期UUID
     * @return 排课分配列表
     */
    public List<ClassAssignmentDO> getClassAssignmentListBySemester(String semesterUuid) {
        RList<ClassAssignmentDO> rList = redisson.getList(
                StringConstant.Redis.CLASS_ASSIGNMENT_LIST_SEMESTER + semesterUuid);
        if (!rList.isExists()) {
            List<ClassAssignmentDO> list = this.lambdaQuery()
                    .eq(ClassAssignmentDO::getSemesterUuid, semesterUuid)
                    .list();
            if (!list.isEmpty()) {
                rList.addAll(list);
                // 设置缓存过期时间为1小时，确保缓存不会无限期存在
                rList.expire(Duration.ofHours(1));
                return list;
            }
            return List.of();
        }
        return rList.readAll();
    }

    /**
     * 保存排课分配
     *
     * @param classAssignmentDO 排课分配数据对象
     */
    public void saveClassAssignment(ClassAssignmentDO classAssignmentDO) {
        RKeys keys = redisson.getKeys();
        this.save(classAssignmentDO);
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_LIST + "*");
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_PAGE + "*");
        keys.delete(StringConstant.Redis.CLASS_ASSIGNMENT_LIST_SEMESTER + "*");
        // 删除冲突缓存
        keys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST + "*");
        keys.deleteByPattern(StringConstant.Redis.SCHEDULING_CONFLICT_LIST_CLASS_ASSIGNMENT + "*");
    }


}
