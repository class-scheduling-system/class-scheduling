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
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.GradeMapper;
import com.frontleaves.scheduling.models.entity.base.GradeDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 年级数据访问对象
 * <p>
 * 此类继承自ServiceImpl，实现IService接口，主要负责对年级（Grade）数据的操作，
 * 包括但不限于增、删、改、查等数据库操作。通过与GradeMapper的交互，
 * 提供了面向业务的数据库访问方法。
 * </p>
 *
 * @author xiao_lfneg
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class GradeDAO extends ServiceImpl<GradeMapper, GradeDO> {
    private final RedissonClient redisson;
    
    /**
     * 保存年级信息并更新缓存
     *
     * @param gradeDO 年级信息实体
     * @return 保存结果，true表示成功，false表示失败
     */
    @Override
    @Transactional
    public boolean save(GradeDO gradeDO) {
        log.debug("保存年级信息: {}", gradeDO.getName());
        boolean result = super.save(gradeDO);
        if (result) {
            // 保存成功后，更新缓存
            updateGradeCache(gradeDO);
            // 清除列表缓存，强制重新加载
            clearGradeListCache();
        }
        return result;
    }
    
    /**
     * 更新年级信息并更新缓存
     *
     * @param gradeDO 年级信息实体
     * @return 更新结果，true表示成功，false表示失败
     */
    @Override
    @Transactional
    public boolean updateById(GradeDO gradeDO) {
        log.debug("更新年级信息: {} (uuid: {})", gradeDO.getName(), gradeDO.getGradeUuid());
        boolean result = super.updateById(gradeDO);
        if (result) {
            // 更新成功后，更新缓存
            updateGradeCache(gradeDO);
            // 清除列表缓存，强制重新加载
            clearGradeListCache();
        }
        return result;
    }
    
    /**
     * 删除年级信息并清除缓存
     *
     * @param id 年级ID（即年级UUID）
     * @return 删除结果，true表示成功，false表示失败
     */
    @Override
    @Transactional
    public boolean removeById(Serializable id) {
        log.debug("删除年级信息: {}", id);
        boolean result = super.removeById(id);
        if (result) {
            // 删除成功后，清除缓存
            redisson.getMap(StringConstant.Redis.GRADE_UUID + id).delete();
            // 清除列表缓存，强制重新加载
            clearGradeListCache();
        }
        return result;
    }

    /**
     * 根据UUID获取年级信息
     * <p>
     * 首先尝试从Redis中获取年级信息，如果不存在，则从数据库中获取，并将其存入Redis中以供下次快速访问
     * </p>
     *
     * @param uuid 年级的唯一标识符
     * @return 返回年级对象，如果找不到则返回null
     */
    @Nullable
    public GradeDO getGradeByUuid(String uuid) {
        log.debug("根据UUID获取年级信息: {}", uuid);
        // 构造Redis中年级信息的键
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.GRADE_UUID + uuid);
        // 检查Redis中是否存在该年级信息
        if (!rMap.isExists()) {
            log.debug("Redis中不存在年级信息，从数据库获取: {}", uuid);
            // 如果Redis中不存在，从数据库中获取年级信息
            GradeDO gradeDO = this.getById(uuid);

            // 如果从数据库中获取到了年级信息
            if (gradeDO != null) {
                log.debug("数据库中找到年级信息，更新缓存: {} (uuid: {})", gradeDO.getName(), uuid);
                // 将年级信息转换为Map并存入Redis
                rMap.putAll(ConvertUtil.convertObjectToMapString(gradeDO));
                // 设置Redis中年级信息的过期时间
                rMap.expire(Duration.ofDays(1));
                // 返回从数据库中获取到的年级信息
                return gradeDO;
            } else {
                log.debug("数据库中未找到年级信息: {}", uuid);
            }
        } else {
            log.debug("Redis中存在年级信息，直接返回: {}", uuid);
            // 如果Redis中存在该年级信息，将其转换为GradeDO对象并返回
            return BeanUtil.toBean(rMap, GradeDO.class);
        }
        // 如果既没有从Redis中获取到年级信息，也没有从数据库中获取到，返回null
        return null;
    }

    /**
     * 获取年级列表
     * <p>
     * 本方法首先尝试从Redis中获取年级列表，如果Redis中不存在该列表，
     * 则从数据库中查询，并将结果存入Redis中，以提高下次查询的效率
     * </p>
     *
     * @return 年级列表，如果列表为空，则返回空列表
     */
    public List<GradeDO> getGradeListForUpdate() {
        log.debug("获取年级列表");
        // 从Redis中获取年级列表
        RList<String> rList = redisson.getList(StringConstant.Redis.GRADE_LIST);
        // 检查Redis列表是否存在
        if (!rList.isExists()) {
            log.debug("Redis中不存在年级列表，从数据库获取");
            // 从数据库中查询年级列表
            List<GradeDO> gradeDOList = this.list(
                    new QueryWrapper<GradeDO>()
                            .orderByDesc("year")
            );
            // 检查查询结果是否为空
            if (gradeDOList != null && !gradeDOList.isEmpty()) {
                log.debug("数据库中找到{}条年级记录，更新缓存", gradeDOList.size());
                // 将查询结果添加到Redis列表中，需要转换为JSON字符串
                for (GradeDO gradeDO : gradeDOList) {
                    rList.add(JSONUtil.toJsonStr(gradeDO));
                }
                // 设置过期时间
                rList.expire(Duration.ofDays(1));
                // 返回查询结果
                return gradeDOList;
            } else {
                log.debug("数据库中未找到年级记录");
            }
        } else {
            log.debug("Redis中存在年级列表，直接返回");
            // 如果Redis列表存在，直接读取并返回
            List<String> jsonList = rList.readAll();
            if (!jsonList.isEmpty()) {
                log.debug("从Redis中读取到{}条年级记录", jsonList.size());
                // 将JSON字符串转换为GradeDO对象列表
                return jsonList.stream()
                        .map(json -> JSONUtil.toBean(json, GradeDO.class))
                        .toList();
            }
        }
        // 如果查询结果为空，返回空列表
        return Collections.emptyList();
    }

    /**
     * 根据年级名称查询年级
     * <p>
     * 首先尝试从Redis中获取年级信息，如果不存在，则从数据库中查询，
     * 并将查询结果缓存到Redis中，以提高后续查询效率
     * </p>
     *
     * @param name 年级名称
     * @return 年级对象，如果找不到则返回null
     */
    @Nullable
    public GradeDO getGradeByName(String name) {
        log.debug("根据名称查询年级: {}", name);
        // 构造Redis中年级信息的键
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.GRADE_NAME + name);
        // 检查Redis中是否存在该年级信息
        if (!rMap.isExists()) {
            log.debug("Redis中不存在名称为{}的年级信息，从数据库获取", name);
            // 构建查询条件
            QueryWrapper<GradeDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", name);
            // 执行查询
            GradeDO gradeDO = this.getOne(queryWrapper);
            
            // 如果从数据库中获取到了年级信息
            if (gradeDO != null) {
                log.debug("数据库中找到名称为{}的年级信息，更新缓存", name);
                // 将年级UUID存入Redis
                rMap.put("gradeUuid", gradeDO.getGradeUuid());
                // 设置Redis中年级信息的过期时间
                rMap.expire(Duration.ofDays(1));
                
                // 同时更新年级UUID对应的缓存
                updateGradeCache(gradeDO);
                
                return gradeDO;
            } else {
                log.debug("数据库中未找到名称为{}的年级信息", name);
            }
        } else {
            log.debug("Redis中存在名称为{}的年级信息，获取对应的年级UUID", name);
            // 如果Redis中存在该年级名称对应的UUID，获取年级UUID
            String gradeUuid = rMap.get("gradeUuid");
            if (gradeUuid != null) {
                // 根据UUID获取年级详细信息
                return getGradeByUuid(gradeUuid);
            }
        }
        // 如果未找到年级信息，返回null
        return null;
    }

    /**
     * 根据入学年份查询年级
     * <p>
     * 首先尝试从Redis中获取年级信息，如果不存在，则从数据库中查询，
     * 并将查询结果缓存到Redis中，以提高后续查询效率
     * </p>
     *
     * @param year 入学年份
     * @return 年级对象，如果找不到则返回null
     */
    @Nullable
    public GradeDO getGradeByYear(Short year) {
        if (year == null) {
            return null;
        }
        
        log.debug("根据入学年份查询年级: {}", year);
        // 构造Redis中年级信息的键
        String cacheKey = StringConstant.Redis.GRADE_UUID + "year:" + year;
        RMap<String, String> rMap = redisson.getMap(cacheKey);
        // 检查Redis中是否存在该年级信息
        if (!rMap.isExists()) {
            log.debug("Redis中不存在入学年份为{}的年级信息，从数据库获取", year);
            // 构建查询条件
            QueryWrapper<GradeDO> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("year", year);
            // 执行查询
            GradeDO gradeDO = this.getOne(queryWrapper);
            
            // 如果从数据库中获取到了年级信息
            if (gradeDO != null) {
                log.debug("数据库中找到入学年份为{}的年级信息，更新缓存", year);
                // 将年级UUID存入Redis
                rMap.put("gradeUuid", gradeDO.getGradeUuid());
                // 设置Redis中年级信息的过期时间
                rMap.expire(Duration.ofDays(1));
                
                // 同时更新年级UUID对应的缓存
                updateGradeCache(gradeDO);
                
                return gradeDO;
            } else {
                log.debug("数据库中未找到入学年份为{}的年级信息", year);
            }
        } else {
            log.debug("Redis中存在入学年份为{}的年级信息，获取对应的年级UUID", year);
            // 如果Redis中存在该年份对应的UUID，获取年级UUID
            String gradeUuid = rMap.get("gradeUuid");
            if (gradeUuid != null) {
                // 根据UUID获取年级详细信息
                return getGradeByUuid(gradeUuid);
            }
        }
        // 如果未找到年级信息，返回null
        return null;
    }

    /**
     * 根据开始日期查询年级
     * <p>
     * 从数据库中查询指定开始日期的年级信息
     * </p>
     *
     * @param startDate 开始日期
     * @return 年级对象，如果找不到则返回null
     */
    @Nullable
    public GradeDO getGradeByStartDate(java.util.Date startDate) {
        log.debug("根据开始日期查询年级: {}", startDate);
        if (startDate == null) {
            return null;
        }
        
        // 构建查询条件
        QueryWrapper<GradeDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("start_date", startDate);
        
        // 执行查询
        return this.getOne(queryWrapper);
    }

    /**
     * 获取所有年级列表（按入学年份降序排序）
     * <p>
     * 从数据库中获取所有年级记录，并按入学年份降序排序
     * </p>
     *
     * @return 年级列表
     */
    public List<GradeDO> getAllGradesOrderByYearDesc() {
        log.debug("获取按入学年份降序排序的所有年级列表");
        // 构建查询条件，按入学年份降序排序
        QueryWrapper<GradeDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("year");
        // 执行查询
        List<GradeDO> gradeDOList = this.list(queryWrapper);
        
        // 更新缓存
        if (gradeDOList != null && !gradeDOList.isEmpty()) {
            log.debug("查询到{}条年级记录，更新列表缓存", gradeDOList.size());
            // 清除之前的列表缓存
            clearGradeListCache();
            
            // 更新列表缓存
            RList<String> rList = redisson.getList(StringConstant.Redis.GRADE_LIST);
            for (GradeDO gradeDO : gradeDOList) {
                // 更新单个年级缓存
                updateGradeCache(gradeDO);
                // 将年级信息添加到列表缓存
                rList.add(JSONUtil.toJsonStr(gradeDO));
            }
            // 设置列表缓存过期时间
            rList.expire(Duration.ofDays(1));
        } else {
            log.debug("未查询到年级记录");
        }
        
        return gradeDOList;
    }
    
    /**
     * 更新年级缓存
     *
     * @param gradeDO 年级信息实体
     */
    private void updateGradeCache(GradeDO gradeDO) {
        if (gradeDO == null || gradeDO.getGradeUuid() == null) {
            return;
        }
        
        log.debug("更新年级缓存: {} (uuid: {})", gradeDO.getName(), gradeDO.getGradeUuid());
        
        // 更新UUID对应的缓存
        RMap<String, String> uuidMap = redisson.getMap(StringConstant.Redis.GRADE_UUID + gradeDO.getGradeUuid());
        uuidMap.putAll(ConvertUtil.convertObjectToMapString(gradeDO));
        uuidMap.expire(Duration.ofDays(1));
        
        // 更新名称对应的缓存
        if (gradeDO.getName() != null) {
            RMap<String, String> nameMap = redisson.getMap(StringConstant.Redis.GRADE_NAME + gradeDO.getName());
            nameMap.put("gradeUuid", gradeDO.getGradeUuid());
            nameMap.expire(Duration.ofDays(1));
        }
        
        // 更新年份对应的缓存
        if (gradeDO.getYear() != null) {
            RMap<String, String> yearMap = redisson.getMap(StringConstant.Redis.GRADE_UUID + "year:" + gradeDO.getYear());
            yearMap.put("gradeUuid", gradeDO.getGradeUuid());
            yearMap.expire(Duration.ofDays(1));
        }
    }
    
    /**
     * 清除年级列表缓存
     */
    private void clearGradeListCache() {
        log.debug("清除年级列表缓存");
        redisson.getList(StringConstant.Redis.GRADE_LIST).delete();
    }
}
