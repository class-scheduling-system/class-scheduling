package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CourseLibraryMapper;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RTransaction;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

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
        if (!map.isExists()) {
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

    /**
     * 更新课程库信息
     * 此方法首先在数据库中更新课程库记录，然后删除Redis缓存中的相应信息
     * 通过传入的CourseLibraryDO对象来更新数据库中的记录，并确保缓存中的数据与数据库保持一致
     *
     * @param courseLibraryDO 包含更新后的课程库信息的对象
     */
    public void updateCourseLibrary(CourseLibraryDO courseLibraryDO) {
        // 创建Redis事务，用于处理缓存数据的一致性
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            // 更新数据库中的课程库信息
            this.updateById(courseLibraryDO);

            // 删除Redis中的课程库信息，确保缓存数据与数据库数据保持一致
            transaction.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryDO.getCourseLibraryUuid()).delete();

        } catch (Exception e) {
            // 如果操作失败，抛出服务器内部错误异常
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 删除课程库信息
     * 此方法协调数据库和Redis缓存中课程库数据的一致性
     * 首先从数据库中删除课程库信息，然后从Redis缓存中删除相应的缓存数据
     *
     * @param courseLibraryDO 课程库数据对象，包含要删除的课程库的信息
     * @throws ServerInternalErrorException 如果数据库操作失败，抛出此异常
     */
    public void deleteCourseLibrary(CourseLibraryDO courseLibraryDO) {
        // 创建Redis事务，用于处理Redis缓存数据的删除
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            // 删除数据库中的课程库信息
            this.removeById(courseLibraryDO);

            // 删除Redis中的课程库信息，确保缓存数据与数据库数据保持一致
            transaction.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryDO.getCourseLibraryUuid()).delete();

        } catch (Exception e) {
            // 如果操作失败，抛出服务器内部错误异常
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 根据条件获取课程库的分页信息
     *
     * @param page 页码，表示请求的是第几页数据
     * @param size 每页大小，即每页包含的课程库数量
     * @param name 课程库名称的模糊查询条件，如果为空或null则不进行模糊查询
     * @return 返回一个分页对象，包含符合条件的课程库列表和分页信息
     */
    public Page<CourseLibraryDO> getCourseLibraryPage(Integer page, Integer size, String name) {
        // 创建一个Lambda查询链式包装器，用于构造查询条件
        LambdaQueryChainWrapper<CourseLibraryDO> query = this.lambdaQuery();

        // 如果名称参数不为空也不为null，则添加模糊查询条件
        if (name != null && !name.isEmpty()) {
            query.like(CourseLibraryDO::getName, name);
        }

        // 执行分页查询，并返回结果
        return query.page(new Page<>(page, size));
    }

    public List<CourseLibraryDO> getCourseLibraryList(String courseCategoryUuid, String coursePropertyUuid, String courseTypeUuid, String courseNatureUuid, String courseDepartmentUuid) {
        return null;
    }
}



