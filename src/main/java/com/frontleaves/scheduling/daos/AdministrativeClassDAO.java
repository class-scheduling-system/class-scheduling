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
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.AdministrativeClassMapper;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 行政班数据访问对象
 * <p>
 * 此类继承自ServiceImpl，实现IService接口，主要负责对行政班（AdministrativeClass）数据的操作，
 * 包括但不限于增、删、改、查等数据库操作。通过与AdministrativeClassMapper的交互，
 * 提供了面向业务的数据库访问方法。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class AdministrativeClassDAO extends ServiceImpl<AdministrativeClassMapper, AdministrativeClassDO> {

    private final RedissonClient redisson;


    /**
     * 根据UUID获取行政班级信息
     * 首先尝试从Redis中获取行政班级信息，如果不存在，则从数据库中获取，并将其存入Redis中以供下次快速访问
     *
     * @param uuid 行政班级的唯一标识符
     * @return 返回行政班级对象，如果找不到则返回null
     */
    public AdministrativeClassDO getAdministrativeClassByUuid(String uuid) {
        // 构造Redis中行政班级信息的键
        RMap<String,String> rMap = redisson.getMap(StringConstant.Redis.ADMINISTRATIVE_CLASS_UUID + uuid);
        // 检查Redis中是否存在该行政班级信息
        if (!rMap.isExists()){
            // 如果Redis中不存在，从数据库中获取行政班级信息
            AdministrativeClassDO administrativeClassDO = this.getById(uuid);
            // 如果从数据库中获取到行政班级信息
            if (administrativeClassDO != null){
                // 将行政班级信息转换为Map并存入Redis中
                rMap.putAll(ConvertUtil.convertObjectToMapString(administrativeClassDO));
                // 设置Redis中行政班级信息的过期时间为86400秒（1天）
                rMap.expire(Duration.ofSeconds(86400));
                // 返回从数据库中获取到的行政班级信息
                return administrativeClassDO;
            }
        }else {
            // 如果Redis中存在行政班级信息，将其转换为AdministrativeClassDO对象并返回
            return BeanUtil.toBean(rMap,AdministrativeClassDO.class);
        }
        // 如果既没有从数据库中获取到信息，Redis中也没有信息，则返回null
        return  null;
    }
    /**
     * 获取管理班级列表
     * 该方法首先尝试从Redis中获取管理班级列表如果列表在Redis中不存在，
     * 则从数据库中获取列表，并将其存入Redis中，以提高下次访问的速度
     *
     * @return 返回管理班级列表如果列表为空，则返回空列表
     */
    public List<AdministrativeClassDO> getAdministrativeClassList() {
        // 从Redis中获取管理班级列表
        RList<AdministrativeClassDO> rList = redisson.getList(StringConstant.Redis.ADMINISTRATIVE_CLASS_LIST);

        // 检查Redis列表是否存在
        if (!rList.isExists()){
            // 从数据库中获取管理班级列表
            List<AdministrativeClassDO> administrativeClassDOList = this.list();

            // 检查获取的列表是否非空
            if (administrativeClassDOList != null){
                // 将列表添加到Redis中，并设置过期时间
                rList.addAll(administrativeClassDOList);
                rList.expire(Duration.ofSeconds(86400));
                // 返回从数据库中获取的列表
                return administrativeClassDOList;
            }
        } else {
            // 如果Redis列表存在，则直接读取并返回
            return rList.readAll();
        }
        // 如果列表为空，则返回空列表
        return Collections.emptyList();
    }


    /**
     * 根据部门UUID获取行政班级列表
     * 该方法首先尝试从Redis中获取班级列表，如果不存在，则从数据库中查询，并将结果缓存到Redis中
     * 使用缓存旨在提高相同查询的响应速度，减少数据库的访问压力
     *
     * @param departmentUuid 部门的唯一标识符
     * @return 行政班级列表，如果找不到则返回空列表
     */
    @Transactional
    public List<AdministrativeClassDO> getAdministrativeClassListByDepartmentForUpdate(String departmentUuid) {
        // 尝试从Redis中获取缓存的行政班级列表
        RList<AdministrativeClassDO> rList = redisson.getList(
                StringConstant.Redis.ADMINISTRATIVE_CLASS_LIST_BY_DEPARTMENT + departmentUuid);
        // 检查缓存是否存在
        if (!rList.isExists()){
            // 从数据库中查询行政班级列表（添加悲观锁）
            List<AdministrativeClassDO> administrativeClassDOList =
                    this.lambdaQuery()
                            .eq(AdministrativeClassDO::getDepartmentUuid, departmentUuid)
                            .last("FOR UPDATE")
                            .list();
            // 如果查询结果不为空，则将其添加到Redis缓存中，并设置过期时间
            if (!administrativeClassDOList.isEmpty()){
                rList.addAll(administrativeClassDOList);
                rList.expire(Duration.ofSeconds(86400));
                return administrativeClassDOList;
            }
        }else {
            // 如果缓存存在，则直接读取并返回缓存中的列表
            return rList.readAll();
        }
        // 如果没有找到任何行政班级，则返回空列表
        return Collections.emptyList();
    }

    /**
     * 根据班级标识获取行政班级映射信息
     * 首先尝试从Redis缓存中获取数据，如果缓存未命中，则从数据库中查询，并将结果缓存到Redis中
     *
     * @param clazz 班级标识
     * @return 行政班级映射信息对象，如果找不到则返回null
     */
    public AdministrativeClassDO getAdministrativeClassMappingByClazz(String clazz) {
        RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.ADMINISTRATIVE_CLASS_MAPPING_BY_CALZZ + clazz);
        // 检查缓存是否存在;若缓存不存在,则从数据库中查询
        if (!rMap.isExists()) {
            AdministrativeClassDO administrativeClassDO = this.lambdaQuery()
                    .eq(AdministrativeClassDO::getAdministrativeClassUuid, clazz)
                    .one();
            if (administrativeClassDO != null) {
                rMap.putAll(ConvertUtil.convertObjectToMapString(administrativeClassDO));
                rMap.expire(Duration.ofSeconds(86400));
            }
            return administrativeClassDO;
        } else {
            return BeanUtil.toBean(rMap, AdministrativeClassDO.class);
        }
    }
}