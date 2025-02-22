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
import com.frontleaves.scheduling.mappers.UserMapper;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Map;

/**
 * 用户数据访问对象
 * <p>
 * 该类提供了对用户数据的基本操作，包括通过 UUID、用户名、邮箱和电话号码获取用户信息。
 * 所有方法都优先尝试从 Redis 缓存中读取数据，如果缓存中没有，则从数据库中查询，并将结果缓存到 Redis 中以提高性能。
 * </p>
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author xiao_lfeng
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class UserDAO extends ServiceImpl<UserMapper, UserDO> implements IService<UserDO> {
    private final Jedis jedis;
    private final TokenDAO tokenDAO;

    /**
     * 通过用户 UUID 获取用户信息。
     * <p>
     * 该方法首先尝试从 Redis 中获取用户信息，如果 Redis 中不存在，则从数据库中查询用户信息并将其存入 Redis。
     * 如果在 Redis 和数据库中都未找到用户信息，则返回 null。
     *
     * @param userUuid 用户的 UUID
     * @return 返回用户信息，如果未找到则返回 null
     */
    public UserDO getUserByUuid(String userUuid) {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.USER_UUID + userUuid);
        if (map.isEmpty()) {
            UserDO userDO = this.lambdaQuery().eq(UserDO::getUserUuid, userUuid).one();
            if (userDO != null) {
                Transaction transaction = jedis.multi();
                transaction.hset(StringConstant.Redis.USER_UUID + userDO.getUserUuid(), ConvertUtil.convertObjectToMapString(userDO));
                transaction.expire(StringConstant.Redis.USER_UUID + userDO.getUserUuid(), 86400);
                transaction.exec();
                return userDO;
            }
        } else {
            return BeanUtil.toBean(map, UserDO.class);
        }
        return null;
    }

    /**
     * 通过用户名获取用户信息。
     * <p>
     * 该方法首先尝试从 Redis 中获取用户的 UUID，如果 Redis 中不存在，则从数据库中查询用户信息。
     * 如果在数据库中找到了用户信息，则将其存入 Redis 并设置过期时间为 24 小时，最后返回用户信息。
     * 如果在 Redis 中找到了用户的 UUID，则直接通过 UUID 获取用户信息并返回。
     * 如果用户不存在，则返回 null。
     *
     * @param name 用户名
     * @return 用户信息，如果用户不存在则返回 null
     */
    public UserDO getUserByName(String name) {
        String tryGetUuid = jedis.get(StringConstant.Redis.USER_NAME + name);
        if (tryGetUuid == null) {
            UserDO userDO = this.lambdaQuery().eq(UserDO::getName, name).one();
            if (userDO != null) {
                Transaction transaction = jedis.multi();
                transaction.set(StringConstant.Redis.USER_NAME + userDO.getName(), userDO.getUserUuid());
                transaction.expire(StringConstant.Redis.USER_NAME + userDO.getName(), 86400);
                transaction.exec();
                return userDO;
            }
        } else {
            return this.getUserByUuid(tryGetUuid);
        }
        return null;
    }

    /**
     * 通过邮箱获取用户信息。
     * <p>
     * 该方法首先尝试从 Redis 中获取用户的 UUID，如果 Redis 中存在该用户的 UUID，则直接返回该用户的信息。
     * 如果 Redis 中不存在该用户的 UUID，则从数据库中查询该用户的信息，并将查询结果缓存到 Redis 中，缓存有效期为 24 小时。
     * 如果在数据库中也未找到该用户，则返回 null。
     *
     * @param mail 用户的邮箱地址
     * @return 返回用户信息，如果未找到用户则返回 null
     */
    public UserDO getUserByMail(String mail) {
        String tryGetUuid = jedis.get(StringConstant.Redis.USER_MAIL + mail);
        if (tryGetUuid == null) {
            UserDO userDO = this.lambdaQuery().eq(UserDO::getEmail, mail).one();
            if (userDO != null) {
                Transaction transaction = jedis.multi();
                transaction.set(StringConstant.Redis.USER_MAIL + userDO.getEmail(), userDO.getUserUuid());
                transaction.expire(StringConstant.Redis.USER_MAIL + userDO.getEmail(), 86400);
                transaction.exec();
                return userDO;
            }
        } else {
            return this.getUserByUuid(tryGetUuid);
        }
        return null;
    }

    /**
     * 通过电话号码获取用户信息。
     * <p>
     * 该方法首先尝试从 Redis 中获取用户的 UUID，如果未找到，则通过数据库查询用户信息。
     * 如果在数据库中找到了用户信息，会将用户信息缓存到 Redis 中，并设置过期时间为一天。
     * </p>
     *
     * @param tel 用户的电话号码
     * @return 用户信息，如果未找到则返回 null
     */
    public UserDO getUserByTel(String tel) {
        String tryGetUuid = jedis.get(StringConstant.Redis.USER_TEL + tel);
        if (tryGetUuid == null) {
            UserDO userDO = this.lambdaQuery().eq(UserDO::getPhone, tel).one();
            if (userDO != null) {
                Transaction transaction = jedis.multi();
                transaction.set(StringConstant.Redis.USER_TEL + userDO.getPhone(), userDO.getUserUuid());
                transaction.expire(StringConstant.Redis.USER_TEL + userDO.getPhone(), 86400);
                transaction.exec();
                return userDO;
            }
        } else {
            return this.getUserByUuid(tryGetUuid);
        }
        return null;
    }

    /**
     * 删除用户并且删除token
     * @param userDO 用户实体
     */
    public void deleteUser(UserDO userDO){
        this.lambdaUpdate().eq(UserDO::getUserUuid,userDO.getUserUuid()).remove();
        tokenDAO.clearUserToken(userDO);
    }
}
