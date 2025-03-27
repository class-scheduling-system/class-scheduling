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
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TeacherPreferencesMapper;
import com.frontleaves.scheduling.models.entity.TeacherPreferencesDO;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 *
 */
@Repository
@RequiredArgsConstructor
public class TeacherPreferencesDAO extends ServiceImpl<TeacherPreferencesMapper, TeacherPreferencesDO> {
    private final RedissonClient redisson;

    /**
     * 获取教师课程偏好分页数据
     * <p>
     * 该方法用于从数据库中获取符合条件的教师课程偏好信息，并返回分页结果。支持根据教师UUID和学期UUID进行过滤，同时可以指定排序方式。
     * 如果 Redis 中存在缓存，则直接从缓存中读取数据并返回；否则，从数据库查询并将结果缓存到 Redis 中。
     * </p>
     *
     * @param page         分页页码，从1开始
     * @param size         每页显示的记录数
     * @param isDesc       是否降序排列，true表示降序，false表示升序
     * @param teacherUuid  教师UUID，用于精确匹配，可为空
     * @param semesterUuid 学期UUID，用于精确匹配，可为空
     * @return 返回包含教师课程偏好信息的分页对象 {@code Page<TeacherPreferencesDO>}
     */
    public Page<TeacherPreferencesDO> getTeacherPreferencesPage(
            int page,
            int size,
            boolean isDesc,
            @Nullable String teacherUuid,
            @Nullable String semesterUuid
    ) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TEACHER_PREFERENCES_PAGE + page + ":" + size + ":" + isDesc + ":" + teacherUuid + ":" + semesterUuid);
        if (!map.isExists()) {
            LambdaQueryChainWrapper<TeacherPreferencesDO> queryWrapper = this.lambdaQuery();
            if (teacherUuid != null && !teacherUuid.isBlank()) {
                queryWrapper.eq(TeacherPreferencesDO::getTeacherUuid, teacherUuid);
            }
            if (semesterUuid != null && !semesterUuid.isBlank()) {
                queryWrapper.eq(TeacherPreferencesDO::getSemesterUuid, semesterUuid);
            }
            queryWrapper.orderBy(true, isDesc, TeacherPreferencesDO::getCreatedAt);

            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            return ProjectUtil.convertMapToPage(map, TeacherPreferencesDO.class);
        }
    }

    /**
     * 根据教师课程偏好UUID获取偏好信息
     * <p>
     * 该方法通过传入的教师课程偏好UUID从Redis缓存中查找偏好信息。如果缓存中不存在，则从数据库中查询，并将结果缓存到Redis中。
     * 如果数据库中也不存在对应的偏好信息，则返回null。
     * </p>
     *
     * @param preferenceUuid 教师课程偏好的唯一标识符 {@code String}
     * @return 返回教师课程偏好信息，如果找不到则返回null {@code TeacherPreferencesDO}
     */
    @Nullable
    public TeacherPreferencesDO getTeacherPreferencesByUuid(String preferenceUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TEACHER_PREFERENCES_UUID + preferenceUuid);
        if (!map.isExists()) {
            TeacherPreferencesDO teacherPreferencesDO = this.getById(preferenceUuid);
            if (teacherPreferencesDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(teacherPreferencesDO));
                map.expire(Duration.ofSeconds(3600));
                return teacherPreferencesDO;
            }
        } else {
            return BeanUtil.toBean(map, TeacherPreferencesDO.class, ProjectOption.stringBlankToNull());
        }
        return null;
    }

    /**
     * 获取教师在指定学期的所有课程偏好
     * <p>
     * 该方法用于获取特定教师在指定学期的所有课程偏好设置。结果会按照星期和时间段排序。
     * 如果缓存中存在数据，则直接返回缓存数据；否则从数据库查询并缓存结果。
     * </p>
     *
     * @param teacherUuid  教师UUID
     * @param semesterUuid 学期UUID
     * @return 返回教师课程偏好列表
     */
    @Nullable
    public List<TeacherPreferencesDO> getTeacherPreferencesByTeacherAndSemester(String teacherUuid, String semesterUuid) {
        String cacheKey = StringConstant.Redis.TEACHER_PREFERENCES_LIST + teacherUuid + ":" + semesterUuid;
        RList<TeacherPreferencesDO> cacheList = redisson.getList(cacheKey);
        if (!cacheList.isExists()) {
            List<TeacherPreferencesDO> preferences = this.lambdaQuery()
                    .eq(TeacherPreferencesDO::getTeacherUuid, teacherUuid)
                    .eq(TeacherPreferencesDO::getSemesterUuid, semesterUuid)
                    .orderByAsc(TeacherPreferencesDO::getDayOfWeek)
                    .orderByAsc(TeacherPreferencesDO::getTimeSlot)
                    .list();
            if (!preferences.isEmpty()) {
                cacheList.addAll(preferences);
                cacheList.expire(Duration.ofSeconds(3600));
                return preferences;
            }
        } else {
            return cacheList;
        }
        return null;
    }

    /**
     * 更新教师课程偏好信息
     * <p>
     * 该方法用于更新教师课程偏好信息。首先会删除Redis中与该偏好相关的缓存，然后调用MyBatis-Plus的updateById方法将更新后的偏好信息保存到数据库中。
     * </p>
     *
     * @param teacherPreferencesDO 要更新的教师课程偏好实体对象 {@code TeacherPreferencesDO}
     */
    public void updateTeacherPreferences(TeacherPreferencesDO teacherPreferencesDO) {
        Optional.of(redisson.getKeys())
                .ifPresent(rKeys -> {
                    rKeys.delete(StringConstant.Redis.TEACHER_PREFERENCES_UUID + teacherPreferencesDO.getPreferenceUuid());
                    rKeys.delete(StringConstant.Redis.TEACHER_PREFERENCES_LIST + teacherPreferencesDO.getTeacherUuid() + ":" + teacherPreferencesDO.getSemesterUuid());
                });
        this.updateById(teacherPreferencesDO);
    }

    /**
     * 删除教师课程偏好信息
     * <p>
     * 该方法用于根据传入的教师课程偏好UUID删除对应的偏好信息。首先会从数据库中查找该偏好信息，如果找到，则删除Redis中与该偏好相关的缓存，
     * 然后从数据库中删除该偏好记录。如果删除成功，返回true；否则，返回false。
     * </p>
     *
     * @param preferenceUuid 教师课程偏好的唯一标识符 {@code String}
     * @return 如果删除成功返回true，否则返回false {@code boolean}
     */
    public boolean deleteTeacherPreferences(String preferenceUuid) {
        TeacherPreferencesDO teacherPreferencesDO = this.getById(preferenceUuid);
        if (teacherPreferencesDO != null) {
            Optional.of(redisson.getKeys())
                    .ifPresent(rKeys -> {
                        rKeys.delete(StringConstant.Redis.TEACHER_PREFERENCES_UUID + preferenceUuid);
                        rKeys.delete(StringConstant.Redis.TEACHER_PREFERENCES_LIST + teacherPreferencesDO.getTeacherUuid() + ":" + teacherPreferencesDO.getSemesterUuid());
                    });
            return this.removeById(preferenceUuid);
        }
        return false;
    }
}
