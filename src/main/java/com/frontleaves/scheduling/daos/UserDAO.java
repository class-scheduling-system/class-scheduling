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
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.UserMapper;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.redisson.api.*;
import org.springframework.stereotype.Repository;

import java.time.Duration;

/**
 * 用户数据访问对象
 * <p>
 * 该类提供了对用户数据的基本操作，包括通过 UUID、用户名、邮箱和电话号码获取用户信息。
 * 所有方法都优先尝试从 Redis 缓存中读取数据，如果缓存中没有，则从数据库中查询，并将结果缓存到 Redis 中以提高性能。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDAO extends ServiceImpl<UserMapper, UserDO> {
    private final RedissonClient redisson;


    /**
     * 根据用户 UUID 获取用户信息
     * <p>
     * 该方法通过传入的用户 UUID 从 Redis 中获取用户信息，如果 Redis 中不存在，则从数据库中查询并缓存到 Redis。
     * 如果在 Redis 和数据库中都未找到对应用户，则返回 null。
     *
     * @param userUuid 用户的唯一标识符 UUID
     * @return 返回与给定 UUID 对应的 {@code UserDO} 对象，如果未找到则返回 null
     * @throws ServerInternalErrorException 当数据库操作失败时抛出此异常
     */
    public UserDO getUserByUuid(String userUuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.USER_UUID + userUuid);
        if (!map.isExists()) {
            UserDO userDO = this.getById(userUuid);
            if (userDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(userDO));
                map.expire(Duration.ofSeconds(86400));
                return userDO;
            }
        } else {
            return BeanUtil.toBean(map, UserDO.class);
        }
        return null;
    }

    /**
     * 通过用户名获取用户信息
     * <p>
     * 该方法根据提供的用户名从 Redis 缓存或数据库中查询对应的用户信息。首先尝试从 Redis 缓存中获取用户的 UUID，
     * 如果缓存中不存在，则从数据库中查询用户信息，并将查询结果存入 Redis 缓存中，设置过期时间为一天。
     * 如果在 Redis 或数据库中均未找到对应用户，则返回 {@code null}。
     *
     * @param name 用户名
     * @return 返回与用户名匹配的 {@code UserDO} 对象，如果未找到则返回 {@code null}
     * @throws ServerInternalErrorException 如果在操作过程中发生服务器内部错误
     */
    public UserDO getUserByName(String name) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            RBucket<String> tryGetUuid = transaction.getBucket(StringConstant.Redis.USER_NAME + name);
            return this.getUserInternal(
                    transaction, tryGetUuid,
                    this.lambdaQuery().eq(UserDO::getName, name)
            );
        } catch (Exception e) {
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 通过邮箱获取用户信息
     * <p>
     * 该方法根据提供的邮箱地址查询用户信息。首先尝试从 Redis 缓存中读取用户 UUID，如果缓存中没有找到，则直接从数据库中查询。
     * 如果在数据库中查找到了用户信息，则将用户的 UUID 和邮箱信息存储到 Redis 中，并设置过期时间为一天（86400秒）。
     * 最后返回查找到的用户信息。如果在整个过程中出现异常，将抛出 {@code ServerInternalErrorException} 异常。
     *
     * @param mail 用户的邮箱地址
     * @return 返回与邮箱匹配的用户信息，如果没有找到则返回 null
     * @throws ServerInternalErrorException 在数据库操作或 Redis 操作失败时抛出
     */
    public UserDO getUserByMail(String mail) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            RBucket<String> tryGetUuid = transaction.getBucket(StringConstant.Redis.USER_MAIL + mail);
            return this.getUserInternal(
                    transaction, tryGetUuid,
                    this.lambdaQuery().eq(UserDO::getEmail, mail)
            );
        } catch (Exception e) {
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 通过电话号码获取用户信息
     * <p>
     * 该方法根据提供的电话号码 {@code tel} 查询用户信息。首先尝试从 Redis 缓存中获取用户的 UUID，
     * 如果缓存中没有找到，则从数据库中查询用户信息，并将结果存储到 Redis 缓存中，设置过期时间为一天。
     * 如果在数据库中也未找到用户信息，则返回 null。
     *
     * @param tel 用户的电话号码
     * @return 返回与电话号码关联的用户信息，如果未找到则返回 null
     * @throws ServerInternalErrorException 如果在操作过程中发生服务器内部错误
     */
    public UserDO getUserByTel(String tel) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            RBucket<String> tryGetUuid = transaction.getBucket(StringConstant.Redis.USER_TEL + tel);
            return this.getUserInternal(
                    transaction, tryGetUuid,
                    this.lambdaQuery().eq(UserDO::getPhone, tel)
            );
        } catch (Exception e) {
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 从数据库中获取用户信息并存储到 Redis 中
     * <p>
     * 此方法首先检查 Redis 中是否已存在指定的用户 UUID。如果不存在，则从数据库查询用户信息，
     * 并将用户信息存储到 Redis 中，同时设置过期时间为 24 小时。如果 Redis 中已存在用户 UUID，则直接从 Redis 中获取用户信息。
     * 如果在数据库中没有找到用户信息，则回滚事务。
     *
     * @param transaction Redis 事务对象，用于执行一系列操作
     * @param tryGetUuid  用于尝试获取或存储用户 UUID 的 Redis 存储桶
     * @param eq          查询条件链，用于构建查询条件以从数据库中获取用户信息
     * @return 返回用户信息，如果没有找到则返回 null
     */
    @Nullable
    private UserDO getUserInternal(
            RTransaction transaction,
            @NotNull RBucket<String> tryGetUuid,
            LambdaQueryChainWrapper<UserDO> eq
    ) {
        if (!tryGetUuid.isExists()) {
            UserDO userDO = eq.one();
            if (userDO != null) {
                tryGetUuid.set(userDO.getUserUuid());
                tryGetUuid.expire(Duration.ofSeconds(86400));
                RMap<String, String> map = transaction.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid());
                map.putAll(ConvertUtil.convertObjectToMapString(userDO));
                map.expire(Duration.ofSeconds(86400));
                transaction.commit();
                return userDO;
            }
            transaction.rollback();
        } else {
            return this.getUserByUuid(tryGetUuid.get());
        }
        return null;
    }

    /**
     * 删除用户并且删除token
     * <p>
     * 该方法用于删除用户信息，首先通过用户 UUID 获取用户信息，然后删除用户信息。
     * 如果用户信息存在，则删除 Redis 中与用户相关的所有数据。
     * 如果用户信息不存在或者删除失败，则抛出 {@code ServerInternalErrorException} 异常。
     * </p>
     *
     * @param userDO 用户实体
     */
    public void deleteUser(UserDO userDO) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            this.lambdaUpdate().eq(UserDO::getUserUuid, userDO.getUserUuid()).remove();
            this.deleteUserRedis(userDO, transaction);
        } catch (Exception e) {
            transaction.rollback();
            log.debug("删除用户失败", e);
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 删除用户 Redis 数据
     *
     * @param userDO      用户实体
     * @param transaction 事务
     */
    private void deleteUserRedis(@NotNull UserDO userDO, @NotNull RTransaction transaction) {
        transaction.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
        transaction.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
        transaction.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
        transaction.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
        transaction.commit();
    }

    /**
     * 更新用户信息
     *
     * @param userOldDO 旧的用户实体
     *                  用于删除 Redis 中的旧数据
     * @param userNewDO 新的用户实体
     * @throws ServerInternalErrorException 如果更新过程中发生服务器内部错误
     */
    public void updateUser(UserDO userOldDO, UserDO userNewDO) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        if (userOldDO == null || userNewDO == null) {
            throw new BusinessException("UserDAO.updateUser: userOldDO or userNewDO is null",
                    ErrorCode.OPERATION_ERROR);
        }
        if (userNewDO.getUserUuid() == null) {
            throw new BusinessException("UserDAO.updateUser: userNewDO.getUserUuid() is null",
                    ErrorCode.OPERATION_ERROR);
        }
        try {
            this.updateById(userNewDO);
            this.deleteUserRedis(userOldDO, transaction);
        } catch (Exception e) {
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }


    /**
     * 获取用户列表
     * <p>
     * 该方法用于从数据库中查询用户列表，并支持分页、关键词搜索以及排序。根据传入的参数，可以实现对用户名称、邮箱或电话进行模糊匹配，
     * 并按照创建时间升序或降序排列结果。
     *
     * @param page    当前页码，必须为正整数
     * @param size    每页显示的记录数，必须为正整数
     * @param keyword 可选参数，用于在用户的姓名、邮箱和电话字段中进行模糊搜索
     * @param isDesc  布尔值，指定结果是否按创建时间降序排列；如果为 {@code true} 则降序，否则升序
     * @return 返回一个包含当前请求页面数据及总条目数等信息的 {@code Page<UserDO>} 对象
     */
    public Page<UserDO> getUserDoPage(
            @NotNull Integer page,
            @NotNull Integer size,
            String keyword,
            boolean isDesc
    ) {
        if (page <= 0 || size <= 0) {
            throw new BusinessException("UserDAO内page和size为空", ErrorCode.OPERATION_ERROR);
        }

        LambdaQueryChainWrapper<UserDO> query = this.lambdaQuery();
        if (keyword != null && !keyword.isEmpty()) {
            query
                    .like(UserDO::getName, keyword).or()
                    .like(UserDO::getEmail, keyword).or()
                    .like(UserDO::getPhone, keyword);
        }
        if (Boolean.TRUE.equals(isDesc)) {
            query.orderByDesc(UserDO::getCreatedAt);
        } else {
            query.orderByAsc(UserDO::getCreatedAt);
        }
        return query.page(new Page<>(page, size));
    }
}
