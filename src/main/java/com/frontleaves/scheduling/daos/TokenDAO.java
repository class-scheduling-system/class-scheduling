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
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Map;
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

    private final Jedis jedis;
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
        try (Transaction transaction = jedis.multi()) {
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
            transaction.hmset(StringConstant.Redis.TOKEN + getNewTokenUuid, ConvertUtil.convertObjectToMapString(tokenDTO));
            transaction.expire(StringConstant.Redis.TOKEN + getNewTokenUuid, 86400);
            transaction.exec();
            return tokenDTO;
        } catch (Exception e) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 验证令牌有效性
     * <p>
     * 该方法用于验证给定的令牌是否有效，并且是否属于指定用户。如果令牌为空或无效，将返回 {@code false}。
     * 如果令牌有效但已过期，或者刷新时间已过期，则从 Redis 中删除该令牌并返回 {@code false}。
     * 如果令牌有效且未过期，则返回 {@code true}。
     * <p>
     * 在处理过程中，如果遇到任何异常，将抛出 {@link ServerInternalErrorException}。
     *
     * @param token  要验证的令牌字符串
     * @param userDO 用户对象，包含用户唯一标识符等信息
     * @return 如果令牌有效且未过期，返回 {@code true}；否则返回 {@code false}
     * @throws ServerInternalErrorException 如果在处理过程中发生内部服务器错误
     */
    public boolean verifyToken(String token, UserDO userDO) throws ServerInternalErrorException {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.TOKEN + token);
        try (Transaction transaction = jedis.multi()) {
            if (!map.isEmpty()) {
                TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
                if (tokenDTO.getUserUuid() == null || !tokenDTO.getUserUuid().equals(userDO.getUserUuid())) {
                    throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
                }
                if (Long.parseLong(map.get(StringConstant.Common.Hump.EXPIRE_TIME)) < System.currentTimeMillis()) {
                    return false;
                }
                if (Long.parseLong(map.get(StringConstant.Common.Hump.REFRESH_EXPIRE_TIME)) < System.currentTimeMillis()) {
                    transaction.del(StringConstant.Redis.TOKEN + token);
                    transaction.exec();
                    return false;
                }
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }
    }

    /**
     * 刷新访问令牌
     * <p>
     * 该方法用于刷新给定的访问令牌。如果令牌存在且未过期，并且与指定用户关联，则会更新其过期时间并返回更新后的令牌信息。
     * 如果令牌不存在、已过期或不与指定用户关联，则抛出相应的异常。
     *
     * @param token  待刷新的访问令牌字符串
     * @param userDO 用户数据对象，包含用户唯一标识符等信息
     * @return 更新后的令牌数据传输对象，包含新的过期时间和刷新过期时间
     * @throws ServerInternalErrorException 如果在执行数据库操作时发生内部错误
     * @throws BusinessException            如果令牌不存在、已过期或不与指定用户关联
     */
    public TokenDTO refreshToken(String token, UserDO userDO) throws ServerInternalErrorException, BusinessException {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.TOKEN + token);
        try (Transaction transaction = jedis.multi()) {
            if (!map.isEmpty()) {
                TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
                if (tokenDTO.getUserUuid() == null || !tokenDTO.getUserUuid().equals(userDO.getUserUuid())) {
                    throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
                }
                if (Long.parseLong(map.get(StringConstant.Common.Hump.EXPIRE_TIME)) < System.currentTimeMillis()) {
                    transaction.del(StringConstant.Redis.TOKEN + token);
                    throw new BusinessException("刷新令牌已过期", ErrorCode.SERVER_INTERNAL_ERROR);
                }
                long newExpireTime = System.currentTimeMillis() + 3600000;
                long newRefreshExpireTime = System.currentTimeMillis() + 86400000;
                transaction.hset(StringConstant.Redis.TOKEN + token, StringConstant.Common.Hump.EXPIRE_TIME, String.valueOf(newExpireTime));
                transaction.hset(StringConstant.Redis.TOKEN + token, StringConstant.Common.Hump.REFRESH_EXPIRE_TIME, String.valueOf(newRefreshExpireTime));
                transaction.expire(StringConstant.Redis.TOKEN + token, 86400);
                transaction.exec();
                return tokenDTO.setExpireTime(newExpireTime)
                        .setRefreshExpireTime(newRefreshExpireTime);
            } else {
                throw new BusinessException("令牌不存在", ErrorCode.SERVER_INTERNAL_ERROR);
            }
        } catch (Exception e) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
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
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.TOKEN + token);
        try (Transaction transaction = jedis.multi()) {
            if (!map.isEmpty()) {
                TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
                if (tokenDTO.getUserUuid() == null || !tokenDTO.getUserUuid().equals(userDO.getUserUuid())) {
                    throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
                } else {
                    transaction.del(StringConstant.Redis.TOKEN + token);
                    transaction.exec();
                }
            } else {
                throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.NOT_EXIST);
            }
        } catch (Exception e) {
            throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
        }
    }

    /**
     * 通过用户令牌获取用户信息
     * <p>
     * 该方法首先尝试从 Redis 中获取与给定用户令牌关联的用户信息。如果在 Redis 中找到相关信息，
     * 则将其转换为 {@code TokenDTO} 对象，并使用其中的用户 UUID 从数据库中查询对应的用户信息。
     * 如果在 Redis 或数据库中未找到相关信息，则返回 null。
     * </p>
     *
     * @param userToken 用户令牌
     * @return 返回与令牌关联的用户信息，如果未找到则返回 null
     */
    public UserDO getTokenUser(String userToken) {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.TOKEN + userToken);
        if (!map.isEmpty()) {
            TokenDTO tokenDTO = BeanUtil.toBean(map, TokenDTO.class);
            if (tokenDTO.getUserUuid() != null && !tokenDTO.getUserUuid().isEmpty()) {
                return userDAO.getUserByUuid(tokenDTO.getUserUuid());
            }
        }
        return null;
    }
}
