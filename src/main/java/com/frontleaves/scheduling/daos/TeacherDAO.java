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
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Map;

/**
 * 教师数据访问对象
 * <p>
 * 此类继承自IService接口，专门用于实现对TeacherDO实体的数据库操作。
 * 通过使用MyBatis-Plus的ServiceImpl简化了对教师信息的CRUD操作。
 * 实现了获取单个教师信息的方法，根据教师编号查询教师详细资料。
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherDAO extends ServiceImpl<TeacherMapper, TeacherDO> implements IService<TeacherDO> {
    private final Jedis jedis;

    /**
     * 根据教师ID获取教师信息
     * <p>
     * 该方法首先尝试从Redis缓存中根据提供的教师ID获取教师信息。如果缓存中没有找到，则从数据库中查询，并将查询结果存入Redis缓存中以提高后续访问的速度。
     * 如果在数据库中也未找到对应的教师记录，返回 {@code null}。此过程可能抛出 {@link ServerInternalErrorException} 异常，表示服务器内部错误。
     * <p>
     * 缓存中的数据有效期为一天（86400秒）。
     *
     * @param id 教师的唯一标识符
     * @return 返回与给定ID匹配的 {@code TeacherDO} 对象，若不存在则返回 {@code null}
     * @throws ServerInternalErrorException 当操作数据库或Redis时发生异常
     */
    @Nullable
    public TeacherDO getTeacherById(String id) throws ServerInternalErrorException {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.TEACHER_ID + id);
        if (map.isEmpty()) {
            TeacherDO teacherDO = this.lambdaQuery().eq(TeacherDO::getId, id).one();
            if (teacherDO != null) {
                try (Transaction transaction = jedis.multi()) {
                    transaction.hset(StringConstant.Redis.TEACHER_ID + id, ConvertUtil.convertObjectToMapString(teacherDO));
                    transaction.expire(StringConstant.Redis.TEACHER_ID + id, 86400);
                    transaction.exec();
                } catch (Exception e) {
                    throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
                }
            }
        } else {
            return BeanUtil.toBean(map, TeacherDO.class);
        }
        return null;
    }

    /**
     * 根据教师UUID获取教师信息
     * <p>
     * 该方法通过给定的教师UUID从Redis缓存中查找教师信息。如果在Redis中未找到，则会尝试从数据库中查询。
     * 如果数据库中有对应的记录，会将该记录添加到Redis缓存中，并设置过期时间为24小时。如果在整个过程中发生任何异常，
     * 将抛出{@code ServerInternalErrorException}。
     *
     * @param teacherUuid 教师的唯一标识符 {@code String}
     * @return 返回与给定UUID匹配的教师信息，如果没有找到则返回null
     * @throws ServerInternalErrorException 当操作数据库或Redis时发生内部错误
     */
    @Nullable
    public TeacherDO getTeacherByUuid(String teacherUuid) throws ServerInternalErrorException{
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.TEACHER_UUID + teacherUuid);
        if (map.isEmpty()) {
            TeacherDO teacherDO = this.lambdaQuery().eq(TeacherDO::getTeacherUuid, teacherUuid).one();
            if (teacherDO != null) {
                try (Transaction transaction = jedis.multi()) {
                    transaction.hset(StringConstant.Redis.TEACHER_UUID + teacherUuid, ConvertUtil.convertObjectToMapString(teacherDO));
                    transaction.expire(StringConstant.Redis.TEACHER_UUID + teacherUuid, 86400);
                    transaction.exec();
                } catch (Exception e) {
                    throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
                }
            }
        } else {
            return BeanUtil.toBean(map, TeacherDO.class);
        }
        return null;
    }

    /**
     * 更新教师的用户 UUID
     * <p>
     * 该方法用于更新指定教师的用户 UUID。首先通过教师 ID 获取教师信息，如果找到对应的教师，则在 Redis 中删除与该教师关联的旧数据，并更新数据库中教师的用户 UUID。
     * 如果未找到对应的教师信息，则抛出 {@code BusinessException} 异常。任何其他异常将被捕获并抛出 {@code ServerInternalErrorException} 异常。
     *
     * @param userUuid 新的用户 UUID
     * @param teacherId 教师 ID
     * @throws BusinessException 如果未找到对应的教师信息
     * @throws ServerInternalErrorException 如果更新过程中发生其他异常
     */
    public void updateUserUuid(String userUuid, String teacherId) throws BusinessException, ServerInternalErrorException {
        try (Transaction transaction = jedis.multi()) {
            TeacherDO teacherDO = this.getTeacherById(teacherId);
            if (teacherDO != null) {
                transaction.del(StringConstant.Redis.TEACHER_ID + teacherDO.getId());
                transaction.del(StringConstant.Redis.TEACHER_UUID + teacherDO.getTeacherUuid());
                this.lambdaUpdate()
                        .eq(TeacherDO::getTeacherUuid, teacherDO.getTeacherUuid())
                        .set(TeacherDO::getUserUuid, userUuid)
                        .update();
                transaction.exec();
            } else {
                throw new BusinessException("未找到对应的教师信息", ErrorCode.NOT_EXIST);
            }
        } catch (Exception e) {
            log.error("更新教师信息失败", e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }
}
