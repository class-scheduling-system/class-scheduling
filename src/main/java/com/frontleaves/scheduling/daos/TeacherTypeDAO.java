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
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TeacherTypeMapper;
import com.frontleaves.scheduling.models.entity.TeacherTypeDO;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * 教师类型数据访问对象
 * <p>
 * 此类继承自ServiceImpl，实现IService接口，主要负责对教师类型（TeacherType）数据的操作，
 * 包括但不限于增、删、改、查等数据库操作。通过与TeacherTypeMapper的交互，
 * 提供了面向业务的数据库访问方法。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherTypeDAO extends ServiceImpl<TeacherTypeMapper, TeacherTypeDO> {

    private final RedissonClient redisson;

    /**
     * 根据UUID获取教师类型信息，优先从缓存获取，如果缓存不存在则从数据库获取并更新缓存
     *
     * @param teacherTypeUuid 教师类型UUID
     * @return 教师类型DO对象，如果不存在则返回null
     */
    @Nullable
    public TeacherTypeDO getTeacherTypeByUuid(@NotNull String teacherTypeUuid) {
        RMap<String,String> map = redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + teacherTypeUuid);
        if (map.isEmpty()){
            log.debug("Cache miss for teacher type UUID: {}", teacherTypeUuid);
            TeacherTypeDO teacherType = this.getById(teacherTypeUuid);
            if (teacherType != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(teacherType));
                map.expire(Duration.ofSeconds(86400));
                return teacherType;
            }
        } else {
            log.debug("Cache hit for teacher type UUID: {}", teacherTypeUuid);
            return BeanUtil.toBean(map, TeacherTypeDO.class);
        }
        return null;
    }

    /**
     * 获取所有教师类型列表，优先从缓存获取
     *
     * @return 所有教师类型的列表
     */
    public List<TeacherTypeDO> getAllTeacherTypes() {
        RList<TeacherTypeDO> teacherTypeList = redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST);
        if (!teacherTypeList.isExists()) {
            log.debug("Cache miss for teacher types list");
            List<TeacherTypeDO> teacherTypes = this.list();
            if (teacherTypes != null && !teacherTypes.isEmpty()) {
                teacherTypeList.addAll(teacherTypes);
                teacherTypeList.expire(Duration.ofSeconds(3600));
                return teacherTypes;
            }
            return List.of();
        } else {
            log.debug("Cache hit for teacher types list");
            return teacherTypeList.readAll();
        }
    }

    /**
     * 获取教师类型分页数据
     *
     * @param page   页码
     * @param size   每页大小
     * @param isDesc 是否降序排序
     * @param name   教师类型名称（可选，用于筛选）
     * @return 分页结果
     */
    public Page<TeacherTypeDO> getTeacherTypePage(int page, int size, boolean isDesc, String name) {
        // 创建缓存Key
        String cacheKey = StringConstant.Redis.TEACHER_TYPE_PAGE + page + ":" + size + ":" + isDesc + ":" + (name == null ? "" : name);
        RMap<String, String> map = redisson.getMap(cacheKey);

        if (!map.isExists()) {
            log.debug("Cache miss for teacher type page, params: page={}, size={}, isDesc={}, name={}", page, size, isDesc, name);

            // 创建查询条件
            LambdaQueryChainWrapper<TeacherTypeDO> queryWrapper = this.lambdaQuery();

            // 添加名称筛选条件（如果提供）
            if (name != null && !name.isBlank()) {
                queryWrapper.like(TeacherTypeDO::getTypeName, name);
            }

            // 设置排序
            if (isDesc) {
                queryWrapper.orderByDesc(TeacherTypeDO::getCreatedAt);
            } else {
                queryWrapper.orderByAsc(TeacherTypeDO::getCreatedAt);
            }

            // 执行查询并缓存结果
            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            log.debug("Cache hit for teacher type page");
            return ProjectUtil.convertMapToPage(map, TeacherTypeDO.class);
        }
    }

    /**
    * 根据名称模糊查询教师类型
    *
    * @param name 教师类型名称（部分匹配）
    * @return 匹配的教师类型列表
    */
    public List<TeacherTypeDO> getTeacherTypeByNameLike(String name) {
    if (Objects.isNull(name) || name.isBlank()) {
    return List.of();
    }

    LambdaQueryWrapper<TeacherTypeDO> queryWrapper = new LambdaQueryWrapper<>();
    queryWrapper.like(TeacherTypeDO::getTypeName, name);
    return this.list(queryWrapper);
    }

    /**
     * 根据名称获取教师类型
     *
     * @param typeName 教师类型名称（精确匹配）
     * @return 教师类型对象，如果不存在返回null
     */
    public TeacherTypeDO getTeacherTypeByName(String typeName) {
        if (Objects.isNull(typeName) || typeName.isBlank()) {
            return null;
        }

        return this.lambdaQuery().eq(TeacherTypeDO::getTypeName, typeName).one();
    }

    /**
     * 根据英文名称获取教师类型
     *
     * @param typeEnglishName 教师类型英文名称（精确匹配）
     * @return 教师类型对象，如果不存在返回null
     */
    public TeacherTypeDO getTeacherTypeByEnglishName(String typeEnglishName) {
        if (Objects.isNull(typeEnglishName) || typeEnglishName.isBlank()) {
            return null;
        }

        return this.lambdaQuery().eq(TeacherTypeDO::getTypeEnglishName, typeEnglishName).one();
    }

    /**
     * 添加教师类型并清除相关缓存
     *
     * @param teacherType 要添加的教师类型对象
     * @return 添加后的教师类型对象
     */
    public TeacherTypeDO addTeacherType(TeacherTypeDO teacherType) {
        this.save(teacherType);

        // 清除列表缓存
        RList<TeacherTypeDO> teacherTypeList = redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST);
        teacherTypeList.delete();

        // 清除分页缓存
        redisson.getKeys().deleteByPattern(StringConstant.Redis.TEACHER_TYPE_PAGE + "*");

        return teacherType;
    }

    /**
     * 更新教师类型并清除相关缓存
     *
     * @param teacherType 要更新的教师类型对象
     * @return 是否更新成功
     */
    public boolean updateTeacherType(TeacherTypeDO teacherType) {
        boolean result = this.updateById(teacherType);

        if (result) {
            // 清除特定教师类型的缓存
            redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + teacherType.getTeacherTypeUuid()).delete();

            // 清除列表缓存
            RList<TeacherTypeDO> teacherTypeList = redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST);
            teacherTypeList.delete();

            // 清除分页缓存
            redisson.getKeys().deleteByPattern(StringConstant.Redis.TEACHER_TYPE_PAGE + "*");
        }

        return result;
    }

    /**
     * 删除教师类型并清除相关缓存
     *
     * @param teacherTypeUuid 教师类型UUID
     * @return 是否删除成功
     */
    public boolean deleteTeacherType(String teacherTypeUuid) {
        TeacherTypeDO teacherType = this.getTeacherTypeByUuid(teacherTypeUuid);
        if (teacherType == null) {
            return false;
        }

        boolean result = this.removeById(teacherTypeUuid);

        if (result) {
            // 清除特定教师类型的缓存
            redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + teacherTypeUuid).delete();

            // 清除列表缓存
            RList<TeacherTypeDO> teacherTypeList = redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST);
            teacherTypeList.delete();

            // 清除分页缓存
            redisson.getKeys().deleteByPattern(StringConstant.Redis.TEACHER_TYPE_PAGE + "*");
        }

        return result;
    }
}
