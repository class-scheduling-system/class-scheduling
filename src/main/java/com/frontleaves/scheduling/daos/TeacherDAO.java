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
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TeacherMapper;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RTransaction;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 教师数据访问对象
 * <p>
 * 该类提供了对教师数据的操作方法，包括从 Redis 或数据库中获取教师信息、更新教师的用户 UUID 等。
 * 通过继承 {@code ServiceImpl} 类并实现 {@code IService} 接口，提供了基础的 CRUD 操作，并扩展了特定的业务逻辑。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherDAO extends ServiceImpl<TeacherMapper, TeacherDO> implements IService<TeacherDO> {
    private final RedissonClient redisson;

    /**
     *
     */
    @Nullable
    public TeacherDO getTeacherById(String id) throws ServerInternalErrorException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TEACHER_ID + id);
        if (!map.isExists()) {
            TeacherDO teacherDO = this.lambdaQuery().eq(TeacherDO::getId, id).one();
            if (teacherDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(teacherDO));
                map.expire(Duration.ofSeconds(86400));
                return teacherDO;
            }
        } else {
            return BeanUtil.toBean(map, TeacherDO.class);
        }
        return null;
    }

    /**
     * 通过教师 UUID 获取教师信息
     * <p>
     * 该方法首先尝试从 Redis 中获取教师信息，如果 Redis 中不存在，则从数据库中查询教师信息并将其存入 Redis。
     * 如果在 Redis 和数据库中都未找到教师信息，则返回 null。
     * </p>
     *
     * @param teacherUuid 教师的 UUID
     * @return 返回教师信息，如果未找到则返回 null
     * @throws ServerInternalErrorException 如果数据库操作失败
     */
    @Nullable
    public TeacherDO getTeacherByUuid(String teacherUuid) throws ServerInternalErrorException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TEACHER_UUID + teacherUuid);
        if (map.isEmpty()) {
            TeacherDO teacherDO = this.lambdaQuery().eq(TeacherDO::getTeacherUuid, teacherUuid).one();
            if (teacherDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(teacherDO));
                map.expire(Duration.ofSeconds(86400));
                return teacherDO;
            }
        } else {
            return BeanUtil.toBean(map, TeacherDO.class);
        }
        return null;
    }

    /**
     * 更新教师的用户 UUID
     * <p>
     * 该方法用于更新指定教师的用户 UUID。首先，通过 {@code teacherId} 获取教师信息。
     * 如果教师信息存在，则删除 Redis 中与该教师相关的缓存数据，并更新数据库中的用户 UUID。
     * 最后，提交事务以确保数据一致性。如果在操作过程中发生异常，将回滚事务并抛出相应的异常。
     * </p>
     *
     * @param userUuid  新的用户 UUID
     * @param teacherId 教师的 ID
     * @throws BusinessException            如果未找到对应的教师信息
     * @throws ServerInternalErrorException 如果更新教师信息失败
     */
    public void updateUserUuid(String userUuid, String teacherId) throws BusinessException, ServerInternalErrorException {
        TeacherDO teacherDO = this.getTeacherById(teacherId);
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            if (teacherDO != null) {
                transaction.getSet(StringConstant.Redis.TEACHER_ID + teacherDO.getId()).delete();
                transaction.getSet(StringConstant.Redis.TEACHER_UUID + teacherDO.getTeacherUuid()).delete();
                this.lambdaUpdate()
                        .eq(TeacherDO::getTeacherUuid, teacherDO.getTeacherUuid())
                        .set(TeacherDO::getUserUuid, userUuid)
                        .update();
                transaction.commit();
            } else {
                throw new BusinessException("未找到对应的教师信息", ErrorCode.NOT_EXIST);
            }
        } catch (Exception e) {
            log.error("更新教师信息失败", e);
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 通过用户 UUID 获取教师信息
     * <p>
     * 该方法首先尝试从 Redis 中获取教师信息，如果 Redis 中不存在，则从数据库中查询教师信息并将其存入 Redis。
     * 如果在 Redis 和数据库中都未找到教师信息，则返回 null。
     * </p>
     *
     * @param userUuid 用户的 UUID
     * @return 返回教师信息，如果未找到则返回 null
     */
    public TeacherDO getTeacherByUserUuid(String userUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TEACHER_USER_UUID + userUuid);
        if (!map.isExists()) {
            TeacherDO teacherDO = this.lambdaQuery().eq(TeacherDO::getUserUuid, userUuid).one();
            if (teacherDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(teacherDO));
                map.expire(Duration.ofSeconds(86400));
                return teacherDO;
            }
        } else {
            return BeanUtil.toBean(map, TeacherDO.class);
        }
        return null;
    }
}
