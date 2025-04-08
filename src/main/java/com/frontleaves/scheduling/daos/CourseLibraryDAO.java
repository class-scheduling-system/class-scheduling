package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.CourseLibraryMapper;
import com.frontleaves.scheduling.models.dto.excel.BackAddCourseDTO;
import com.frontleaves.scheduling.models.entity.base.CourseLibraryDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.redisson.api.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
            if (!this.updateById(courseLibraryDO)) {
                throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
            }

            // 删除Redis中的课程库信息，确保缓存数据与数据库数据保持一致
            transaction.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryDO.getCourseLibraryUuid())
                    .delete();
            transaction.getBucket(StringConstant.Redis.COURSE_LIBRARY_ID + courseLibraryDO.getId()).delete();

            // 提交Redis事务
            transaction.commit();
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
            transaction.getMap(StringConstant.Redis.COURSE_LIBRARY_UUID + courseLibraryDO.getCourseLibraryUuid())
                    .delete();
            transaction.getBucket(StringConstant.Redis.COURSE_LIBRARY_ID + courseLibraryDO.getId()).delete();
            transaction.commit();

        } catch (Exception e) {
            // 如果操作失败，抛出服务器内部错误异常
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 根据条件获取课程库的分页信息
     *
     * @param page           页码，表示请求的是第几页数据
     * @param size           每页大小，即每页包含的课程库数量
     * @param name           课程库名称的模糊查询条件，如果为空或null则不进行模糊查询
     * @param departmentUuid 部门UUID，用于按部门筛选，如果为空或null则不筛选部门
     * @return 返回一个分页对象，包含符合条件的课程库列表和分页信息
     */
    public Page<CourseLibraryDO> getCourseLibraryPage(Integer page, Integer size, String name, String departmentUuid, Boolean isDesc) {
        RMap<String, String> map = redisson
                .getMap(StringConstant.Redis.COURSE_LIBRARY_PAGE_DEPARTMENT + departmentUuid);
        if (!map.isExists()) {
            // 创建一个Lambda查询链式包装器，用于构造查询条件
            LambdaQueryChainWrapper<CourseLibraryDO> query = this.lambdaQuery();

            // 如果部门UUID参数不为空也不为null，则添加部门筛选条件
            if (departmentUuid != null && !departmentUuid.isEmpty()) {
                query.eq(CourseLibraryDO::getDepartment, departmentUuid);
            }

            // 如果名称参数不为空也不为null，则添加模糊查询条件
            if (name != null && !name.isEmpty()) {
                query
                        .or(i -> i.like(CourseLibraryDO::getName, name))
                        .or(i -> i.like(CourseLibraryDO::getId, name));
            }

            // 如果isDesc参数不为空也不为null，则添加排序条件
            query.orderBy(isDesc != null && isDesc, false, CourseLibraryDO::getId);

            // 执行分页查询，并返回结果
            return ProjectUtil.queryAndCache(query, page, size, map);
        } else {
            return ProjectUtil.convertMapToPage(map, CourseLibraryDO.class);
        }
    }

    public List<CourseLibraryDO> getCourseLibraryList(String courseCategoryUuid, String coursePropertyUuid,
            String courseTypeUuid, String courseNatureUuid, String courseDepartmentUuid) {
        // 构建缓存键
        String cacheKey = StringConstant.Redis.COURSE_LIBRARY_LITE_LIST +
                (courseCategoryUuid != null && !courseCategoryUuid.isEmpty() ? courseCategoryUuid : "all") + ":" +
                (coursePropertyUuid != null && !coursePropertyUuid.isEmpty() ? coursePropertyUuid : "all") + ":" +
                (courseTypeUuid != null && !courseTypeUuid.isEmpty() ? courseTypeUuid : "all") + ":" +
                (courseNatureUuid != null && !courseNatureUuid.isEmpty() ? courseNatureUuid : "all") + ":" +
                (courseDepartmentUuid != null && !courseDepartmentUuid.isEmpty() ? courseDepartmentUuid : "all");

        // 尝试从缓存获取数据
        RList<CourseLibraryDO> cacheList = redisson.getList(cacheKey);
        if (!cacheList.isExists()) {
            LambdaQueryWrapper<CourseLibraryDO> queryWrapper = new LambdaQueryWrapper<>();
            if (courseCategoryUuid != null && !courseCategoryUuid.isEmpty()) {
                queryWrapper.eq(CourseLibraryDO::getCategory, courseCategoryUuid);
            }
            if (coursePropertyUuid != null && !coursePropertyUuid.isEmpty()) {
                queryWrapper.eq(CourseLibraryDO::getProperty, coursePropertyUuid);
            }
            if (courseTypeUuid != null && !courseTypeUuid.isEmpty()) {
                queryWrapper.eq(CourseLibraryDO::getType, courseTypeUuid);
            }
            if (courseNatureUuid != null && !courseNatureUuid.isEmpty()) {
                queryWrapper.eq(CourseLibraryDO::getNature, courseNatureUuid);
            }
            if (courseDepartmentUuid != null && !courseDepartmentUuid.isEmpty()) {
                queryWrapper.eq(CourseLibraryDO::getDepartment, courseDepartmentUuid);
            }
            queryWrapper.orderByAsc(CourseLibraryDO::getName);

            List<CourseLibraryDO> courseLibraryList = this.list(queryWrapper);
            if (!courseLibraryList.isEmpty()) {
                // 将查询结果存入缓存
                cacheList.addAll(courseLibraryList);
                // 设置缓存过期时间
                cacheList.expire(Duration.ofSeconds(86400));
                return courseLibraryList;
            }
            return new ArrayList<>();
        } else {
            // 如果缓存存在，则直接返回缓存中的数据
            return cacheList.readAll();
        }
    }

    /**
     * 保存课程库信息，忽略错误并返回失败详情
     * <p>
     * 该方法尝试保存课程库信息，发生异常时不抛出，而是收集错误信息并返回。
     * </p>
     *
     * @param courseLibraryDO 课程库实体对象
     * @param i               当前处理的行索引
     * @return 失败详情列表，如果成功则返回空列表
     */
    public List<BackAddCourseDTO.FailedDetail> saveCourseLibraryIgnoreError(CourseLibraryDO courseLibraryDO, int i) {
        try {
            this.save(courseLibraryDO);
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.singletonList(createCourseLibraryFailedDetail(e, i));
        }
    }

    /**
     * 根据异常创建课程库失败详情
     *
     * @param e 异常对象
     * @param i 行索引
     * @return 失败详情对象
     */
    private BackAddCourseDTO.FailedDetail createCourseLibraryFailedDetail(Exception e, int i) {
        BackAddCourseDTO.FailedDetail failedDetail = new BackAddCourseDTO.FailedDetail();
        failedDetail.setRow(i + 3); // +3 是因为Excel文件中有表头和示例行

        if (e instanceof DuplicateKeyException) {
            failedDetail.setReason("课程ID或名称重复");
        } else if (e instanceof DataIntegrityViolationException) {
            String errorMessage = e.getMessage();
            failedDetail.setReason(analyzeCourseLibraryDataIntegrityError(errorMessage));
        } else {
            failedDetail.setReason("保存失败：" + e.getMessage());
        }

        return failedDetail;
    }

    /**
     * 分析课程库数据完整性错误
     *
     * @param errorMessage 错误信息
     * @return 格式化的错误原因
     */
    private String analyzeCourseLibraryDataIntegrityError(String errorMessage) {
        // 外键错误映射
        Map<String, String> foreignKeyErrors = Map.of(
                "fk_cs_course_library_cs_department", "部门信息错误",
                "fk_cs_course_library_cs_course_category", "课程类别信息错误",
                "fk_cs_course_library_cs_course_property", "课程属性信息错误",
                "fk_cs_course_library_cs_course_type", "课程类型信息错误",
                "fk_cs_course_library_cs_course_nature", "课程性质信息错误");

        // 检查外键错误
        for (Map.Entry<String, String> entry : foreignKeyErrors.entrySet()) {
            if (errorMessage.contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        // 长度错误检查
        if (errorMessage.contains("Data too long")) {
            if (errorMessage.contains("id")) {
                return "课程ID长度超出限制，最大32个字符";
            } else if (errorMessage.contains("name")) {
                return "课程名称长度超出限制，最大64个字符";
            }
            return "数据长度超出限制";
        }

        // 默认错误信息
        return "数据错误：可能包含错误的值";
    }

    /**
     * 保存课程库信息，遇到错误时抛出异常
     * <p>
     * 该方法尝试保存课程库信息，并处理可能出现的各种异常，将其转换为业务异常抛出。
     * </p>
     *
     * @param courseLibraryDO 课程库实体对象
     * @param i               行号索引
     * @throws BusinessException 当保存过程中发生异常时抛出，并包含详细的错误信息
     */
    public void saveCourseLibraryBackError(CourseLibraryDO courseLibraryDO, int i) {
        try {
            this.save(courseLibraryDO);
        } catch (DuplicateKeyException e) {
            // 课程ID或名称重复异常
            log.error("课程ID或名称重复", e);
            throw new BusinessException("第" + (i + 3) + "行课程ID或名称重复，请检查", ErrorCode.BODY_ERROR);
        } catch (DataIntegrityViolationException e) {
            // 分析数据完整性异常的具体原因
            String errorMessage = e.getMessage();
            String detailedReason = analyzeCourseLibraryDataIntegrityError(errorMessage);
            log.error("数据完整性错误", e);
            throw new BusinessException("第" + (i + 3) + "行" + detailedReason, ErrorCode.BODY_ERROR);
        } catch (Exception e) {
            // 其他未预期的异常
            throw new BusinessException("第" + (i + 3) + "行保存失败：" + e.getMessage(), ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 根据ID获取课程库信息
     * 首先尝试从Redis中获取课程库信息，如果不存在，则从数据库中查询，并将结果缓存到Redis中
     *
     * @param id 课程库的ID
     * @return CourseLibraryDO类型的对象，如果找不到则返回null
     */
    public CourseLibraryDO getCourseLibraryById(String id) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.COURSE_LIBRARY_ID + id);
        if (!map.isExists()) {
            CourseLibraryDO courseLibraryDO = this.lambdaQuery()
                    .eq(CourseLibraryDO::getId, id)
                    .one();
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

    public void saveCourseLibrary(CourseLibraryDO newCourseLibraryDO) {
        this.save(newCourseLibraryDO);
        RKeys rKeys = redisson.getKeys();
        // rKeys.delete(StringConstant.Redis.
    }

    /**
     * 根据部门UUID、指定课程ID列表
     *
     * @param departmentUuid    部门UUID，用于筛选属于该部门的课程库
     * @param specificCourseIds 指定的课程ID列表，如果非空，则只返回这些ID对应的课程库
     * @return 返回根据条件筛选出的课程库列表
     */
    public List<CourseLibraryDO> getListCourseLibraryByDepartmentAndSpecify(
            @NotBlank String departmentUuid, List<String> specificCourseIds) {
        // 如果指定了具体课程ID列表且不为空，则查询属于该部门且在指定课程ID列表中的课程库
        if (specificCourseIds != null && !specificCourseIds.isEmpty()) {
            return this.lambdaQuery().eq(CourseLibraryDO::getDepartment, departmentUuid)
                    .eq(CourseLibraryDO::getIsEnabled, true)
                    .in(CourseLibraryDO::getCourseLibraryUuid, specificCourseIds)
                    .list();
        }
        // 如果没有指定课程ID列表，返回空链表
        return List.of();
    }

    public List<CourseLibraryDO> getCourseListByDepart(String departmentUuid) {
        RList<CourseLibraryDO> rList = redisson.getList(StringConstant.Redis.COURSE_LIBRARY_LIST);
        if (!rList.isExists()) {
            List<CourseLibraryDO> courseLibraryList = this
                    .lambdaQuery()
                    .eq(CourseLibraryDO::getDepartment, departmentUuid)
                    .list();
            if (!courseLibraryList.isEmpty()) {
                rList.addAll(courseLibraryList);
                rList.expire(Duration.ofHours(24));
                return courseLibraryList;
            }
            return List.of();
        }
        return rList.readAll();
    }
}
