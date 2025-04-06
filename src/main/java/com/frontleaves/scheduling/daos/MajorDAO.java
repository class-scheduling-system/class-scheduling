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
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
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
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.MajorMapper;
import com.frontleaves.scheduling.models.entity.base.MajorDO;
import com.frontleaves.scheduling.models.entity.base.StudentDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
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
 * 专业数据访问对象
 * <p>
 * 此类继承自ServiceImpl，实现IService接口，主要负责对专业（Major）数据的操作，
 * 包括但不限于增、删、改、查等数据库操作。通过与MajorMapper的交互，
 * 提供了面向业务的数据库访问方法。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class MajorDAO extends ServiceImpl<MajorMapper, MajorDO> {

    private final RedissonClient redisson;
    private final DepartmentDAO departmentDAO;
    private final StudentDAO studentDAO;


    /**
     * 根据专业 UUID 获取专业信息
     */
    public MajorDO getMajorByUuid(String majorUuid) {
        // 先从 Redis 查询
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.MAJOR_UUID + majorUuid);
        if (!map.isExists()) {
            // Redis无数据,查询数据库
            MajorDO majorDO = this.getById(majorUuid);
            if (majorDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(majorDO));
                map.expire(Duration.ofSeconds(86400));
                return majorDO;
            }
        } else {
            return BeanUtil.toBean(map, MajorDO.class);
        }
        return null;
    }

    /**
     * 判断专业是否被系统中其他数据引用
     *
     * @param majorUuid 专业UUID
     */
    public void isReferenced(String majorUuid) {
        // 1.检查该专业是否存在
        MajorDO getMajor = this.getById(majorUuid);
        if (getMajor == null) {
            throw new BusinessException("该专业不存在", ErrorCode.BODY_ERROR);
        }

        // 2. 检查学生是否绑定了该专业
        List<StudentDO> getStudentList = studentDAO.getStudentByMajorUuid(majorUuid);
        if (!getStudentList.isEmpty()) {
            throw new BusinessException("该专业已被学生绑定，无法删除", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 删除专业(仅在未被引用的情况下)
     *
     * @param majorUuid 专业 UUID
     * @return 是否删除成功
     */
    public boolean deleteMajor(String majorUuid) {
        // 检查该专业是否已被其他实体引用
        this.isReferenced(majorUuid);

        redisson.getKeys().delete(StringConstant.Redis.MAJOR_UUID + majorUuid);
        redisson.getKeys().delete(StringConstant.Redis.MAJOR_LIST);
        redisson.getKeys().deleteByPattern(StringConstant.Redis.MAJOR_LIST_BY_DEPARTMENT_UUID);
        int deletedRows = this.getBaseMapper().delete(
                new LambdaQueryWrapper<MajorDO>().eq(MajorDO::getMajorUuid, majorUuid)
        );
        // 根据删除结果返回
        if (deletedRows > 0) {
            return true;
        } else {
            throw new BusinessException("专业删除失败", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 查询专业列表
     *
     * @param page       页码
     * @param size       每页记录数
     * @param isDesc     是否降序排列（null或true为降序，false为升序）
     * @param department 学院名称，用于模糊查询
     * @param name       专业名称，用于模糊查询
     * @return 返回包含专业列表的Page对象
     */
    public Page<MajorDO> listMajors(int page, int size, Boolean isDesc, String department, String name) {
        Page<MajorDO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MajorDO> queryWrapper = new LambdaQueryWrapper<>();

        // 查看学院名称是否为空
        if (CharSequenceUtil.isNotBlank(department)) {
            List<String> departmentUuids = departmentDAO.getDepartmentUuidListByName(department);
            if (departmentUuids.isEmpty()) {
                throw new BusinessException("学院不存在", ErrorCode.BODY_ERROR);
            } else {
                queryWrapper.in(MajorDO::getDepartmentUuid, departmentUuids);
            }
        }

        // 查看专业名称是否为空
        if (CharSequenceUtil.isNotBlank(name)) {
            queryWrapper.like(MajorDO::getMajorName, name);
        }
        // 根据isDesc参数决定排序方式
        queryWrapper.orderBy(isDesc == null || isDesc, false, MajorDO::getCreatedAt);

        return this.page(pageParam, queryWrapper);
    }

    /**
     * 获取专业列表
     * <p>
     * 本方法首先尝试从Redis中获取专业列表，如果Redis中不存在该列表，
     * 则从数据库中查询，并将结果存入Redis以备下次快速访问
     * 这种做法减少了数据库的访问次数，提高了数据获取效率
     *
     * @return 专业列表，如果列表为空则返回空列表
     */
    public List<MajorDO> getMajorList() {
        // 尝试从Redis中获取专业列表
        RList<MajorDO> redissonList = redisson.getList(StringConstant.Redis.MAJOR_LIST);
        // 检查Redis中是否存在该列表
        if (!redissonList.isExists()) {
            // 从数据库中查询专业列表
            List<MajorDO> majorList = this.list();
            // 如果查询结果不为空，则将其添加到Redis中，并设置过期时间
            if (!majorList.isEmpty()) {
                redissonList.addAll(majorList);
                redissonList.expire(Duration.ofSeconds(86400));
                return majorList;
            }
        } else {
            // 如果Redis中存在该列表，则直接读取并返回
            return redissonList.readAll();
        }
        // 如果数据库中也没有专业列表，则返回空列表
        return Collections.emptyList();
    }

    /**
     * 根据部门UUID获取专业列表
     * 该方法首先尝试从Redis中获取专业列表，如果不存在，则从数据库中查询，并将结果缓存到Redis中
     * 使用缓存旨在提高查询效率，减少数据库访问次数
     *
     * @param departmentUuid 部门UUID，用于标识部门
     * @return 专业列表，如果找不到则返回空列表
     */
    @Transactional
    public List<MajorDO> getMajorListByDepartmentUuidForUpdate(String departmentUuid) {
        // 尝试从Redis中获取缓存的专业列表
        RList<MajorDO> rList = redisson.getList(
                StringConstant.Redis.MAJOR_LIST_BY_DEPARTMENT_UUID + departmentUuid);
        // 检查缓存是否存在
        if (!rList.isExists()) {
            // 如果缓存不存在，从数据库中查询专业列表（添加悲观锁）
            // 方式1：使用LambdaQueryWrapper添加FOR UPDATE子句
            List<MajorDO> majorDOList = this.lambdaQuery()
                    .eq(MajorDO::getDepartmentUuid, departmentUuid)
                    .last("FOR UPDATE")
                    .list();
            // 如果查询结果不为空，将其添加到Redis缓存中，并设置过期时间
            if (!majorDOList.isEmpty()) {
                rList.addAll(majorDOList);
                rList.expire(Duration.ofSeconds(86400));
                return majorDOList;
            }
        } else {
            // 如果缓存存在，直接读取并返回缓存中的专业列表
            return rList.readAll();
        }
        // 如果没有查询到任何数据，返回空列表
        return Collections.emptyList();
    }


}

