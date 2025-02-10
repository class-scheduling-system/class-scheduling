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

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.TokenDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;

import java.util.List;
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

    /**
     * 创建新的 Token 并存储到 Redis 中。
     * <p>
     * 生成新的 Token 和 RefreshToken，设置 Token 过期时间为 1 小时，刷新令牌过期时间为 24 小时，
     * 并将这些信息封装在 TokenDTO 对象中。将 TokenDTO 转换成 HashMap 存入 Redis，
     * 同时设置 Redis 键的过期时间为 86400 秒。
     * </p>
     *
     * @param userDO 用户数据对象，包含用户唯一标识，不能为空
     * @return TokenDTO 包含生成的 Token、RefreshToken、创建时间、过期时间等信息
     */
    public TokenDTO createToken(@NotNull UserDO userDO) {
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
        jedis.hmset(StringConstant.Redis.TOKEN + getNewTokenUuid, ConvertUtil.convertObjectToMapString(tokenDTO));
        jedis.expire(StringConstant.Redis.TOKEN + getNewTokenUuid, 86400);
        return tokenDTO;
    }

    /**
     * 验证指定 Token 是否有效。
     * <p>
     * 该方法通过 Redis 获取存储的 Token 数据，并依次校验：
     * <ul>
     *   <li>Token 是否为空或不存在；</li>
     *   <li>Token 是否归属于当前用户；</li>
     *   <li>Token 是否已过期；</li>
     *   <li>RefreshToken 是否已过期（若已过期则删除 Token）。</li>
     * </ul>
     * </p>
     *
     * @param token  要验证的 Token 字符串
     * @param userDO 用户数据对象，包含当前用户的唯一标识
     * @return true 如果 Token 存在且有效；false 否则
     * @throws BusinessException 如果 Token 不属于当前用户，则抛出异常
     */
    public boolean verifyToken(String token, UserDO userDO) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        List<String> getToken = jedis.hmget(StringConstant.Redis.TOKEN + token,
                StringConstant.Common.USER_UUID,
                StringConstant.Common.TOKEN,
                StringConstant.Common.REFRESH_TOKEN,
                StringConstant.Common.CREATED_AT,
                StringConstant.Common.EXPIRE_TIME,
                StringConstant.Common.REFRESH_EXPIRE_TIME
        );
        // 检查 Token 是否存在
        if (getToken == null || getToken.isEmpty()) {
            return false;
        }
        // 检查 Token 是否属于传入的用户
        if (!getToken.get(0).equals(userDO.getUserUuid())) {
            throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
        }
        // 检查 Token 是否过期
        if (Long.parseLong(getToken.get(4)) < System.currentTimeMillis()) {
            return false;
        }
        // 检查 RefreshToken 是否过期，若已过期则删除 Token
        if (Long.parseLong(getToken.get(5)) < System.currentTimeMillis()) {
            jedis.del(StringConstant.Redis.TOKEN + token);
            return false;
        }
        return true;
    }

    /**
     * 刷新指定 Token 的过期时间。
     * <p>
     * 此方法首先验证 Token 是否存在且属于当前用户，再检查 RefreshToken 是否有效。
     * 如果 RefreshToken 未过期，则更新 Token 的过期时间（延长 1 小时）和 RefreshToken 的过期时间（延长 24 小时），
     * 同时更新 Redis 中对应的记录，并返回更新后的 TokenDTO 对象。
     * </p>
     *
     * @param token  当前的 Token 字符串
     * @param userDO 用户数据对象，包含当前用户的唯一标识
     * @return 更新后的 TokenDTO 对象，其中包含新的过期时间信息
     * @throws BusinessException 如果 Token 不存在、Token 不属于当前用户，或 RefreshToken 已过期
     */
    public TokenDTO refreshToken(String token, UserDO userDO) {
        List<String> getToken = jedis.hmget(StringConstant.Redis.TOKEN + token,
                StringConstant.Common.USER_UUID,
                StringConstant.Common.TOKEN,
                StringConstant.Common.REFRESH_TOKEN,
                StringConstant.Common.CREATED_AT,
                StringConstant.Common.EXPIRE_TIME,
                StringConstant.Common.REFRESH_EXPIRE_TIME
        );
        if (getToken == null || getToken.isEmpty()) {
            throw new BusinessException("令牌不存在", ErrorCode.SERVER_INTERNAL_ERROR);
        }
        if (!getToken.get(0).equals(userDO.getUserUuid())) {
            throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
        }
        if (Long.parseLong(getToken.get(5)) < System.currentTimeMillis()) {
            jedis.del(StringConstant.Redis.TOKEN + token);
            throw new BusinessException("刷新令牌已过期", ErrorCode.SERVER_INTERNAL_ERROR);
        }
        // 计算新的过期时间
        long newExpireTime = System.currentTimeMillis() + 3600000;
        long newRefreshExpireTime = System.currentTimeMillis() + 86400000;
        // 更新 Redis 中 Token 的过期时间和刷新令牌的过期时间
        jedis.hset(StringConstant.Redis.TOKEN + token, StringConstant.Common.EXPIRE_TIME, String.valueOf(newExpireTime));
        jedis.hset(StringConstant.Redis.TOKEN + token, StringConstant.Common.REFRESH_EXPIRE_TIME, String.valueOf(newRefreshExpireTime));
        jedis.expire(StringConstant.Redis.TOKEN + token, 86400);
        return new TokenDTO()
                .setUserUuid(userDO.getUserUuid())
                .setToken(token)
                .setRefreshToken(getToken.get(2))
                .setCreatedAt(Long.parseLong(getToken.get(3)))
                .setExpireTime(newExpireTime)
                .setRefreshExpireTime(newRefreshExpireTime);
    }

    /**
     * 删除指定 Token。
     * <p>
     * 该方法首先验证 Token 是否存在且属于当前用户，
     * 如果验证通过，则从 Redis 中删除对应的 Token 数据，并返回 true 表示删除成功。
     * 若 Token 不存在，则返回 false。
     * </p>
     *
     * @param token  要删除的 Token 字符串
     * @param userDO 用户数据对象，包含当前用户的唯一标识
     * @return true 如果 Token 成功删除；false 如果 Token 不存在
     * @throws BusinessException 如果 Token 不属于当前用户，则抛出异常
     */
    public boolean deleteToken(String token, UserDO userDO) throws BusinessException {
        List<String> getToken = jedis.hmget(StringConstant.Redis.TOKEN + token,
                StringConstant.Common.USER_UUID
        );
        if (getToken == null || getToken.isEmpty()) {
            return false;
        }
        if (!getToken.get(0).equals(userDO.getUserUuid())) {
            throw new BusinessException(StringConstant.TOKEN_ATTRIBUTION_ERROR, ErrorCode.SERVER_INTERNAL_ERROR);
        }
        jedis.del(StringConstant.Redis.TOKEN + token);
        return true;
    }
}
