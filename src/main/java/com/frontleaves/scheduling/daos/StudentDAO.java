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
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.StudentMapper;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.RMap;
import org.redisson.api.RTransaction;
import org.redisson.api.RedissonClient;
import org.redisson.api.TransactionOptions;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 学生数据访问对象类
 * <p>
 * 该类继承自ServiceImpl，专门用于实现对学生表(StudentDO)的数据访问操作。
 * 利用MyBatis-Plus框架简化了数据操作，实现了IService接口以提供更便捷的数据处理方法。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @see StudentMapper
 * @see StudentDO
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StudentDAO extends ServiceImpl<StudentMapper, StudentDO> implements IService<StudentDO> {
    private final RedissonClient redisson;

    /**
     * 根据学生ID获取学生信息
     * <p>
     * 该方法首先尝试从Redis缓存中获取学生信息。如果缓存中不存在，则从数据库查询，并将查询结果存储到Redis缓存中，设置过期时间为24小时。
     * 如果在数据库中也未找到对应的学生信息，则返回null。
     *
     * @param id 学生的唯一标识符 {@code String}
     * @return 返回与给定ID匹配的学生信息 {@code StudentDO}，如果没有找到则返回null
     */
    @Nullable
    public StudentDO getStudentById(String id) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_ID + id);
        if (!map.isExists()) {
            StudentDO studentDO = this.lambdaQuery().eq(StudentDO::getId, id).one();
            if (studentDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(studentDO));
                map.expire(Duration.ofSeconds(86400));
                return studentDO;
            }
        } else {
            return BeanUtil.toBean(map, StudentDO.class);
        }
        return null;
    }

    /**
     * 通过UUID获取学生信息
     * <p>该方法首先尝试从Redis缓存中根据给定的UUID查找学生信息。如果在Redis中找不到，则会尝试从数据库中查询。
     * 如果从数据库中成功查找到学生信息，会将该信息存入Redis，并设置过期时间为一天（86400秒），然后返回该学生信息。
     * 如果既在Redis中也未在数据库中找到学生信息，则返回null。
     * @param studentUuid 学生的唯一标识符 {@code String}
     * @return 返回与给定UUID对应的学生信息对象 {@code StudentDO}，若未找到则返回null
     */
    @Nullable
    public StudentDO getStudentByUuid(String studentUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentUuid);
        if (!map.isExists()) {
            StudentDO studentDO = this.getById(studentUuid);
            if (studentDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(studentDO));
                map.expire(Duration.ofSeconds(86400));
                return studentDO;
            }
        } else {
            return BeanUtil.toBean(map, StudentDO.class);
        }
        return null;
    }

    /**
     * 更新学生信息中的用户 UUID
     * <p>
     * 该方法用于根据给定的学生 ID 更新其对应的用户 UUID。更新操作包括在 Redis 中删除与旧 UUID 相关的键，并在数据库中更新新的 UUID。
     * 如果找不到对应的学生信息，将抛出业务异常。如果在执行过程中发生任何其他错误，将抛出服务器内部错误异常。
     *
     * @param userUuid  新的用户 UUID
     * @param studentId 学生 ID
     * @throws ServerInternalErrorException 如果在更新过程中发生服务器内部错误
     * @throws BusinessException            如果未找到对应的学生信息
     */
    public void updateUserUuid(String userUuid, String studentId) throws BusinessException, ServerInternalErrorException {
        StudentDO studentDO = this.getStudentById(studentId);
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            if (studentDO != null) {
                transaction.getBucket(StringConstant.Redis.STUDENT_ID + studentDO.getId()).delete();
                transaction.getBucket(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid()).delete();
                this.lambdaUpdate()
                        .eq(StudentDO::getStudentUuid, studentDO.getStudentUuid())
                        .set(StudentDO::getUserUuid, userUuid)
                        .update();
                transaction.commit();
            } else {
                transaction.rollback();
                throw new BusinessException("未找到对应的教师信息", ErrorCode.NOT_EXIST);
            }
        } catch (Exception e) {
            transaction.rollback();
            log.error(LogConstant.DAO + "更新学生信息中的用户 UUID 失败", e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 删除学生并删除token
     * <p>
     * 该方法用于删除学生信息，首先通过学生 UUID 获取学生信息，然后删除学生信息。
     * 如果学生信息存在，则删除 Redis 中与学生相关的所有数据。
     * 如果学生信息不存在或者删除失败，则抛出 {@code ServerInternalErrorException} 异常。
     * </p>
     *
     * @param studentDO 学生实体
     * @throws ServerInternalErrorException 如果删除过程中发生服务器内部错误
     */
    public void deleteStudent(StudentDO studentDO) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try  {
            this.lambdaUpdate().eq(StudentDO::getId, studentDO.getId()).remove();
            transaction.getBucket(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid()).delete();
            transaction.getBucket(StringConstant.Redis.STUDENT_ID + studentDO.getId()).delete();
            transaction.commit();
        } catch (Exception e) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 通过用户 UUID 获取学生信息
     * <p>
     * 该方法根据提供的用户 UUID 从系统中检索对应的学生信息。首先尝试从缓存（如 Redis）中获取数据，如果缓存中没有找到，则从数据库中查询，并将结果存入缓存以提高后续访问速度。
     * 如果在缓存和数据库中均未找到与给定 UUID 对应的学生信息，则返回 null。
     * </p>
     *
     * @param userUuid 用户的唯一标识符
     * @return 返回与指定 UUID 关联的学生信息，若无匹配项则返回 {@code null}
     * @throws ServerInternalErrorException 当执行数据库查询或缓存操作时遇到错误
     */
    public StudentDO getStudentByUserUuid(String userUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_USER_UUID + userUuid);
        if (!map.isExists()) {
            StudentDO studentDO = this.lambdaQuery().eq(StudentDO::getUserUuid, userUuid).one();
            if (studentDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(studentDO));
                map.expire(Duration.ofSeconds(86400));
                return studentDO;
            }
        } else {
            return BeanUtil.toBean(map, StudentDO.class);
        }
        return null;
    }
}

