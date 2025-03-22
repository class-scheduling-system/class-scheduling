package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 邮箱验证令牌数据传输对象
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class EmailVerificationTokenDTO {
    /**
     * 用户UUID
     */
    private String userUuid;

    /**
     * 邮箱地址
     */
    private String email;

    /**
     * 验证令牌
     */
    private String token;

    /**
     * 创建时间（毫秒时间戳）
     */
    private Long createdAt;

    /**
     * 过期时间（毫秒时间戳）
     */
    private Long expireTime;
}

