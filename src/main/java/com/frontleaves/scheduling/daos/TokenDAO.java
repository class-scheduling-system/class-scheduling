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
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.EmailVerificationTokenDTO;
import com.frontleaves.scheduling.models.dto.TokenDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.redisson.api.*;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.UUID;

/**
 * Token 数据访问对象
 * <p>
 * 该类用于处理 Token 的创建、验证、刷新和删除操作。通过 Redis 存储 Token 数据，实现对用户 Token 的管理。
 * 具体操作包括：
 * <ul>
 *   <li>创建 Token：生成新的 Token 和 RefreshToken，并设置相应的过期时间，将其存储在 Redis 中；</li>
 *   <li>验证 Token：检查 Token 是否存在、是否属于当前用户、是否过期；</li>
 *   <li>刷新 Token：更新 Token 和 RefreshToken 的过期时间；</li>
 *   <li>删除 Token：删除 Redis 中存储的 Token 数据。</li>
 * </ul>
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TokenDAO {

    private final RedissonClient redisson;
    private final UserDAO userDAO;

    /**
     * 生成新的令牌
     * <p>
     * 该方法接收一个用户对象 {@code UserDO}，并为其生成一个新的访问令牌和刷新令牌。
     * 生成的令牌信息将被存储在一个 {@code TokenDTO} 对象中，并将其保存在 Redis 数据库中。
     * 访问令牌的有效期为 1 小时，刷新令牌的有效期为 24 小时。
     * 如果操作过程中出现异常，将抛出 {@code ServerInternalErrorException}。
     *
     * @param userDO 用户对象，不能为空
     * @return 包含新生成的令牌信息的 {@code TokenDTO} 对象
     * @throws ServerInternalErrorException 如果在操作过程中发生错误
     */
    public TokenDTO createToken(@NotNull UserDO userDO) throws ServerInternalErrorException {
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            TokenDTO tokenDTO = new TokenDTO();
            String getNewTokenUuid = UuidUtil.generateStringUuid();
            String newRefreshToken = UUID.randomUUID().toString();
            tokenDTO
                    .setUserUuid(userDO.getUserUuid())
                    .setToken(getNewTokenUuid)
                    .setRefreshToken(newRefreshToken)
                    .setCreatedAt(System.currentTimeMillis())
                    .setExpireTime(System.currentTimeMillis() + 3600000)
                    .setRefreshExpireTime(System.currentTimeMillis() + 86400000);
            // 将 TokenDTO 对象转换为 Map 格式存储在 Redis 中
            RMap<String, String> map = transaction.getMap(StringConstant.Redis.TOKEN + getNewTokenUuid);
            map.putAll(ConvertUtil.convertObjectToMapString(tokenDTO));
            map.expire(Duration.ofSeconds(86400));
            transaction.commit();
            return tokenDTO;
        } catch (Exception e) {
            transaction.rollback();
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 验证令牌有效性
     * <p>
     * 该方法用于验证给定的令牌是否有效。首先检查令牌是否为空或空白，然后从 Redis 中获取与令牌关联的数据。
     * 如果数据存在，则进一步验证用户 UUID 是否匹配以及令牌和刷新令牌是否过期。如果所有条件都满足，则返回 true 表示令牌有效。
     * 否则，返回 false 或抛出异常。
     *
     * @param token  待验证的令牌字符串
     * @param userDO 用户对象，包含用户信息
     * @return 如果令牌有效且未过期，则返回 {@code true}；否则返回 {@code false}
     * @throws BusinessException 当令牌归属不匹配时抛出业务异常
     */
    public boolean verifyToken(String token, UserDO userDO) throws BusinessException {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TOKEN + token);
        if (map.isExists()) {
            TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
            if (tokenDTO.getUserUuid() == null || !tokenDTO.getUserUuid().equals(userDO.getUserUuid())) {
                throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
            }
            if (Long.parseLong(map.get(StringConstant.Common.Hump.EXPIRE_TIME)) < System.currentTimeMillis()) {
                return false;
            }
            if (Long.parseLong(map.get(StringConstant.Common.Hump.REFRESH_EXPIRE_TIME)) < System.currentTimeMillis()) {
                map.delete();
                return false;
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * 刷新访问令牌
     * <p>
     * 该方法用于刷新用户的访问令牌。首先，它会检查提供的令牌和刷新令牌的有效性。如果这些令牌有效且未过期，则生成新的访问令牌和刷新令牌，并更新存储在 Redis 中的相关信息。如果原始令牌不存在或已过期，则抛出相应的业务异常。
     *
     * @param token        当前的访问令牌
     * @param refreshToken 当前的刷新令牌
     * @param userDO       用户数据对象
     * @return 包含新生成的访问令牌和刷新令牌的 {@code TokenDTO} 对象
     * @throws BusinessException 如果令牌归属错误、令牌已过期、刷新令牌错误或刷新令牌已过期等情况下抛出
     */
    public TokenDTO refreshToken(String token, String refreshToken, UserDO userDO) throws BusinessException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TOKEN + token);
        if (!map.isExists()) {
            TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
            // 检查令牌
            if (tokenDTO.getUserUuid() == null || !tokenDTO.getUserUuid().equals(userDO.getUserUuid())) {
                throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
            }
            if (Long.parseLong(map.get(StringConstant.Common.Hump.EXPIRE_TIME)) < System.currentTimeMillis()) {
                map.delete();
                throw new BusinessException(StringConstant.TOKEN_EXPIRED, ErrorCode.SERVER_INTERNAL_ERROR);
            }
            // 检查刷新令牌
            if (!refreshToken.equals(tokenDTO.getRefreshToken())) {
                throw new BusinessException(StringConstant.REFRESH_TOKEN_ERROR, ErrorCode.OPERATION_DENIED);
            }
            if (Long.parseLong(map.get(StringConstant.Common.Hump.REFRESH_EXPIRE_TIME)) < System.currentTimeMillis()) {
                map.delete();
                throw new BusinessException(StringConstant.REFRESH_TOKEN_EXPIRED, ErrorCode.SERVER_INTERNAL_ERROR);
            }
            long newExpireTime = System.currentTimeMillis() + 3600000;
            long newRefreshExpireTime = System.currentTimeMillis() + 86400000;
            String newToken = UuidUtil.generateStringUuid();
            String newRefreshToken = UuidUtil.generateStringUuid();
            tokenDTO
                    .setToken(newToken)
                    .setRefreshToken(newRefreshToken)
                    .setExpireTime(newExpireTime)
                    .setRefreshExpireTime(newRefreshExpireTime);
            map.putAll(ConvertUtil.convertObjectToMapString(tokenDTO));
            map.expire(Duration.ofSeconds(86400));
            return tokenDTO
                    .setExpireTime(newExpireTime)
                    .setRefreshExpireTime(newRefreshExpireTime);
        } else {
            throw new BusinessException(StringConstant.TOKEN_NOT_EXIST, ErrorCode.OPERATION_DENIED);
        }
    }

    /**
     * 删除用户令牌
     * <p>
     * 该方法用于删除指定用户的令牌。首先，通过提供的令牌 {@code token} 从 Redis 中获取与之关联的令牌数据。
     * 如果令牌存在且其所属用户 UUID 与传入的用户对象 {@code userDO} 的 UUID 匹配，则删除该令牌。
     * 若令牌不存在或不匹配，则抛出相应的业务异常。整个操作在一个事务中进行以确保数据的一致性。
     * <p>
     *
     * @param token  待删除的令牌字符串
     * @param userDO 用户对象，包含用户的 UUID 等信息
     * @throws BusinessException            当令牌归属错误、令牌不存在或内部服务器错误时抛出
     * @throws ServerInternalErrorException 当发生未预期的内部服务器错误时抛出
     */
    public void deleteToken(String token, UserDO userDO) throws BusinessException, ServerInternalErrorException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TOKEN + token);
        if (!map.isExists()) {
            TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
            if (tokenDTO.getUserUuid() == null || !tokenDTO.getUserUuid().equals(userDO.getUserUuid())) {
                throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
            } else {
                map.delete();
            }
        } else {
            throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.NOT_EXIST);
        }
    }

    /**
     * 获取与指定用户令牌关联的用户信息
     * <p>
     * 该方法通过提供的用户令牌 {@code userToken} 从 Redis 中获取相关联的用户信息。
     * 首先，它会检查是否存在与给定令牌相关的缓存数据。如果存在，将尝试转换为 {@code TokenDTO} 对象并验证其有效性。
     * 如果令牌已过期，则删除该缓存条目，并抛出一个 {@code BusinessException} 异常。
     * 如果令牌有效且包含有效的用户唯一标识符，则使用此标识符从数据库中检索对应的用户信息。
     *
     * @param userToken 用户令牌字符串
     * @return 返回与令牌关联的用户对象，若未找到或令牌无效则返回 null
     * @throws BusinessException 当令牌已过期时抛出此异常
     */
    public UserDO getTokenUser(String userToken) throws BusinessException {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TOKEN + userToken);
        if (map.isExists()) {
            TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
            if (Long.parseLong(map.get(StringConstant.Common.Hump.EXPIRE_TIME)) < System.currentTimeMillis()) {
                map.delete();
                throw new BusinessException(StringConstant.TOKEN_EXPIRED, ErrorCode.SERVER_INTERNAL_ERROR);
            }
            if (tokenDTO.getUserUuid() != null && !tokenDTO.getUserUuid().isEmpty()) {
                return userDAO.getUserByUuid(tokenDTO.getUserUuid());
            }
        }
        return null;
    }


    /**
     * 创建邮箱验证令牌
     * 该方法用于生成并保存邮箱验证令牌，包括设置令牌的有效期和关联的用户信息
     * 它利用Redis进行分布式事务处理，确保操作的原子性
     *
     * @param userDO 用户实体，包含用户UUID和邮箱信息，用于生成验证令牌
     * @return 返回生成的EmailVerificationTokenDTO对象，包含验证令牌和相关用户信息
     */
    public EmailVerificationTokenDTO createEmailToken(UserDO userDO) {
        // 创建Redis事务，用于确保后续操作的原子性
        RTransaction transaction = redisson.createTransaction(TransactionOptions.defaults());
        try {
            EmailVerificationTokenDTO emailVerificationTokenDTO = new EmailVerificationTokenDTO();
            // 设置验证码过期时间为15分钟
            long expirationTime = System.currentTimeMillis() + 900000;
            // 生成唯一验证码字符串
            String verificationToken = UuidUtil.generateStringUuid();
            // 填充EmailVerificationTokenDTO对象的属性
            emailVerificationTokenDTO.setUserUuid(userDO.getUserUuid())
                    .setEmail(userDO.getEmail())
                    .setToken(verificationToken)
                    .setCreatedAt(System.currentTimeMillis())
                    .setExpireTime(expirationTime);
            // 获取事务中的Map对象，用于存储验证令牌信息
            RMap<String, String> map = transaction.getMap(
                    StringConstant.Redis.EMAIL_TOKEN + verificationToken);
            // 将EmailVerificationTokenDTO对象转换为Map并保存到Redis
            map.putAll(ConvertUtil.convertObjectToMapString(emailVerificationTokenDTO));
            // 设置Map的过期时间为15分钟，与验证码过期时间一致
            map.expire(Duration.ofSeconds(900000));
            // 创建从email到token的映射，方便通过email查找
            RBucket<String> emailToTokenBucket = transaction.getBucket(
                    StringConstant.Redis.EMAIL_TO_TOKEN + userDO.getEmail());
            emailToTokenBucket.set(verificationToken);
            emailToTokenBucket.expire(Duration.ofMillis(900000));
            // 提交事务，确保所有操作生效
            transaction.commit();
            return emailVerificationTokenDTO;
        } catch (Exception e) {
            // 如果发生异常，回滚事务以撤销所有操作
            transaction.rollback();
            log.error("创建邮箱验证令牌失败", e);
            // 抛出内部服务器错误异常，表示数据库操作失败
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }


    /**
     * 验证电子邮件验证令牌的有效性]
     * @param token 验证令牌
     * @return 如果令牌有效且与电子邮件地址匹配，则返回用户UUID；否则返回null或抛出异常
     * @throws BusinessException 当令牌无效、过期或与电子邮件地址不匹配时抛出
     */
    public String verifyEmailToken(String token) throws BusinessException {
        // 检查令牌是否为空或仅包含空格
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        // 从Redis中获取与令牌关联的电子邮件验证信息
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.EMAIL_TOKEN + token);
        if (map.isExists()) {
            // 将从Redis中获取的数据转换为电子邮件验证令牌DTO对象
            EmailVerificationTokenDTO tokenDTO = BeanUtil.toBean(map, EmailVerificationTokenDTO.class);
            // 检查令牌是否过期
            if (Long.parseLong(map.get("expireTime")) < System.currentTimeMillis()) {
                map.delete();
                throw new BusinessException(StringConstant.EMAIL_VERIFICATION_TOKEN_EXPIRED,
                        ErrorCode.SERVER_INTERNAL_ERROR);
            }
            redisson.getKeys().delete(StringConstant.Redis.EMAIL_TO_TOKEN + tokenDTO.getEmail());
            // 返回用户UUID
            return tokenDTO.getUserUuid();
        } else {
            // 如果令牌在Redis中不存在，抛出异常
            throw new BusinessException(StringConstant.EMAIL_VERIFICATION_TOKEN_INVALID,
                    ErrorCode.NOT_EXIST);
        }
    }


    /**
     * 删除电子邮件验证令牌
     * 本方法通过将令牌从Redis中删除来实现令牌的删除操作Redis是一种支持网络、可持久化的键值对存储系统，
     * 在这里用于存储和管理电子邮件验证令牌通过删除指定的令牌，可以撤销对该令牌的验证状态
     *
     * @param token 要删除的电子邮件验证令牌字符串
     * @throws BusinessException 当删除操作失败时抛出的业务异常
     */
    public void deleteEmailToken(String token) throws BusinessException {
        // 从Redis中删除指定的电子邮件验证令牌
        redisson.getKeys().delete(StringConstant.Redis.EMAIL_TOKEN + token);
    }

    /**
     * 获取邮件验证令牌的创建时间
     *
     * @param userDO 用户实体对象
     * @return 验证令牌的创建时间，如果没有找到则返回0
     */
    public long getEmailTokenCreatAt(UserDO userDO) {
        try {
            // 首先通过邮箱获取token
            RBucket<String> emailToTokenBucket = redisson.getBucket(
                    StringConstant.Redis.EMAIL_TO_TOKEN + userDO.getEmail());
            String token = emailToTokenBucket.get();
            // 如果没有找到token，返回0
            if (token == null) {
                return 0;
            }
            // 通过token获取验证令牌信息的Map
            RMap<String, String> rMap = redisson.getMap(StringConstant.Redis.EMAIL_TOKEN + token);
            // 检查Map是否存在且有数据
            if (!rMap.isEmpty()) {
                // 将Map中的数据转换为EmailVerificationTokenDTO对象
                EmailVerificationTokenDTO oldEmailVerificationTokenDTO =
                        BeanUtil.toBean(rMap, EmailVerificationTokenDTO.class);
                // 返回验证令牌的创建时间
                return oldEmailVerificationTokenDTO.getCreatedAt();
            }
            // 如果Map为空，则返回0表示未找到验证令牌
            return 0;
        } catch (Exception e) {
            log.error("获取邮件验证令牌创建时间失败", e);
            return 0;
        }
    }

}
