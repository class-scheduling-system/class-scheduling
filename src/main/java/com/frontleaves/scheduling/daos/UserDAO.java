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
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Map;

/**
 * 用户数据访问对象
 * <p>
 * 该类用于对用户数据进行访问。
 * </p>
 *
 * @version v1.0.0
 * @since v1.0.0
 * @author xiao_lfeng
 */
@Repository
@RequiredArgsConstructor
public class UserDAO extends ServiceImpl<UserMapper, UserDO> implements IService<UserDO> {
    private final Jedis jedis;

    /**
     * 根据用户UUID获取用户数据对象
     * <p>
     * 本方法首先尝试从Redis缓存中通过用户UUID查找用户信息。如果缓存中没有找到，
     * 则查询数据库并将查询到的数据存入Redis，同时返回该用户数据对象。
     * 如果缓存中存在用户信息，则直接转换为UserDO对象并返回。
     * </p>
     *
     * @param userUuid 用户的UUID字符串，用于唯一标识用户
     * @return 匹配用户UUID的UserDO对象，如果没有找到则返回null
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
     * 根据用户名获取用户数据对象
     * <p>
     * 本方法首先尝试从Redis缓存中通过用户名查找用户信息。如果缓存中没有找到，
     * 则查询数据库并将查询到的数据存入Redis，同时返回该用户数据对象。
     * 如果缓存中存在用户信息，则直接转换为UserDO对象并返回。
     * </p>
     *
     * @param name 用户名
     * @return 匹配用户名的UserDO对象，如果没有找到则返回null
     */
    public UserDO getUserByName(String name) {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.USER_NAME + name);
        if (map.isEmpty()) {
            UserDO userDO = this.lambdaQuery().eq(UserDO::getName, name).one();
            if (userDO != null) {
                Transaction transaction = jedis.multi();
                transaction.hset(StringConstant.Redis.USER_NAME + userDO.getName(), ConvertUtil.convertObjectToMapString(userDO));
                transaction.expire(StringConstant.Redis.USER_NAME + userDO.getName(), 86400);
                transaction.exec();
                return userDO;
            }
        } else {
            return BeanUtil.toBean(map, UserDO.class);
        }
        return null;
    }

    /**
     * 根据用户邮箱获取用户数据对象
     * <p>
     * 本方法首先尝试从Redis缓存中通过用户邮箱查找用户信息。如果缓存中没有找到，
     * 则查询数据库并将查询到的数据存入Redis，同时返回该用户数据对象。
     * 如果缓存中存在用户信息，则直接转换为UserDO对象并返回。
     * </p>
     *
     * @param mail 用户邮箱
     * @return 匹配用户邮箱的UserDO对象，如果没有找到则返回null
     */
    public UserDO getUserByMail(String mail) {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.USER_MAIL + mail);
        if (map.isEmpty()) {
            UserDO userDO = this.lambdaQuery().eq(UserDO::getEmail, mail).one();
            if (userDO != null) {
                Transaction transaction = jedis.multi();
                transaction.hset(StringConstant.Redis.USER_MAIL + userDO.getEmail(), ConvertUtil.convertObjectToMapString(userDO));
                transaction.expire(StringConstant.Redis.USER_MAIL + userDO.getEmail(), 86400);
                transaction.exec();
                return userDO;
            }
        } else {
            return BeanUtil.toBean(map, UserDO.class);
        }
        return null;
    }

    /**
     * 根据用户电话号码获取用户数据对象
     * <p>
     * 本方法首先尝试从Redis缓存中通过用户电话号码查找用户信息。如果缓存中没有找到，
     * 则查询数据库并将查询到的数据存入Redis，同时返回该用户数据对象。
     * 如果缓存中存在用户信息，则直接转换为UserDO对象并返回。
     * </p>
     *
     * @param tel 用户电话号码
     * @return 匹配用户电话号码的UserDO对象，如果没有找到则返回null
     */
    public UserDO getUserByTel(String tel) {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.USER_TEL + tel);
        if (map.isEmpty()) {
            UserDO userDO = this.lambdaQuery().eq(UserDO::getPhone, tel).one();
            if (userDO != null) {
                Transaction transaction = jedis.multi();
                transaction.hset(StringConstant.Redis.USER_TEL + userDO.getPhone(), ConvertUtil.convertObjectToMapString(userDO));
                transaction.expire(StringConstant.Redis.USER_TEL + userDO.getPhone(), 86400);
                transaction.exec();
                return userDO;
            }
        } else {
            return BeanUtil.toBean(map, UserDO.class);
        }
        return null;
    }
}
