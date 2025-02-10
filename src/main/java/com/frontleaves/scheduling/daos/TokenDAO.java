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
 * 该类用于对 Token 数据进行访问。
 * </p>
 *
 * @version v1.0.0
 * @since v1.0.0
 * @author xiao_lfeng
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TokenDAO {

    private final Jedis jedis;

    /**
     * 创建 Token
     *
     * @param userDO 用户信息
     * @return TokenDTO
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
        // TokenDTO 转为 HMap
        jedis.hmset(StringConstant.Redis.TOKEN + getNewTokenUuid, ConvertUtil.convertObjectToMapString(tokenDTO));
        jedis.expire(StringConstant.Redis.TOKEN + getNewTokenUuid, 86400);
        return tokenDTO;
    }

    /**
     * 验证 Token
     *
     * @param token Token
     * @return boolean
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
        // 检查当前令牌是否有效
        if (getToken == null || getToken.isEmpty()) {
            return false;
        }
        // 检查 Token 是否是当前用户
        if (!getToken.get(0).equals(userDO.getUserUuid())) {
            return false;
        }
        // 检查 Token 是否过期
        if (Long.parseLong(getToken.get(4)) < System.currentTimeMillis()) {
            return false;
        }
        // 检查 RefreshToken 是否过期
        if (Long.parseLong(getToken.get(5)) < System.currentTimeMillis()) {
            // 如果 RefreshToken 过期删除 Token
            jedis.del(StringConstant.Redis.TOKEN + token);
            return false;
        }
        return true;
    }
}
