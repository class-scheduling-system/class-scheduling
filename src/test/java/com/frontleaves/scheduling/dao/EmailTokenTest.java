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
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TokenDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.email.EmailVerificationTokenDTO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * TokenDAO中与邮箱验证令牌相关方法的测试类
 *
 * @author yourname
 */
@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmailTokenTest {
    @Resource
    private UserDAO userDAO;

    @Resource
    private TokenDAO tokenDAO;

    @Resource
    private RedissonClient redisson;

    private UserDO testUser;

    @BeforeEach
    void setUp() {
        // 获取一个用于测试的用户
        testUser = userDAO.lambdaQuery().eq(UserDO::getName, "test").one();
        if (testUser == null) {
            log.warn("在数据库中未找到测试用户，测试可能会失败");
        } else {
            log.debug("测试用户信息: {}", testUser);
        }
    }

    /**
     * 测试创建邮箱验证令牌
     */
    @Test
    void testCreateEmailToken() {
        // 确保测试用户存在且有邮箱地址
        Assertions.assertNotNull(testUser, "测试用户不能为空");
        Assertions.assertNotNull(testUser.getEmail(), "测试用户必须有邮箱地址");

        // 创建邮箱验证令牌
        EmailVerificationTokenDTO tokenDTO = tokenDAO.createEmailToken(testUser);

        // 验证返回的令牌DTO不为空且包含正确的信息
        Assertions.assertNotNull(tokenDTO, "创建的令牌不能为空");
        Assertions.assertEquals(testUser.getUserUuid(), tokenDTO.getUserUuid(), "令牌中的用户UUID应当与测试用户一致");
        Assertions.assertEquals(testUser.getEmail(), tokenDTO.getEmail(), "令牌中的邮箱地址应当与测试用户一致");
        Assertions.assertNotNull(tokenDTO.getToken(), "令牌字符串不能为空");
        Assertions.assertNotNull(tokenDTO.getCreatedAt(), "令牌创建时间不能为空");
        Assertions.assertNotNull(tokenDTO.getExpireTime(), "令牌过期时间不能为空");

        // 验证过期时间是否正确（约15分钟）
        long expectedExpiry = tokenDTO.getCreatedAt() + 900000;
        Assertions.assertEquals(expectedExpiry, tokenDTO.getExpireTime(), 100, "令牌过期时间应为创建时间+15分钟");

        // 验证Redis中是否正确存储了数据
        String redisKey = StringConstant.Redis.EMAIL_TOKEN + tokenDTO.getToken();
        RMap<String, String> map = redisson.getMap(redisKey);
        Assertions.assertTrue(map.isExists(), "Redis中应当存在令牌信息");

        // 验证邮箱到令牌的映射
        String emailToTokenKey = StringConstant.Redis.EMAIL_TO_TOKEN + testUser.getEmail();
        RBucket<String> emailToTokenBucket = redisson.getBucket(emailToTokenKey);
        Assertions.assertEquals(tokenDTO.getToken(), emailToTokenBucket.get(), "邮箱到令牌的映射应当存在并正确");

        // 清理测试数据
        redisson.getKeys().delete(redisKey);
        redisson.getKeys().delete(emailToTokenKey);
    }

    /**
     * 测试验证邮箱令牌的有效性
     */
    @Test
    void testVerifyEmailToken() {
        // 创建邮箱验证令牌
        EmailVerificationTokenDTO tokenDTO = tokenDAO.createEmailToken(testUser);

        try {
            // 验证令牌，应返回用户UUID
            String userUuid = tokenDAO.verifyEmailToken(tokenDTO.getToken());
            Assertions.assertEquals(testUser.getUserUuid(), userUuid, "验证令牌应返回正确的用户UUID");

            // 测试空令牌
            Assertions.assertNull(tokenDAO.verifyEmailToken(""), "空令牌应返回null");

            // 使用 Lambda 表达式测试不存在的令牌
            BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                    tokenDAO.verifyEmailToken("non-existent-token")
            );
            Assertions.assertTrue(exception.getMessage().contains(StringConstant.EMAIL_VERIFICATION_TOKEN_INVALID),
                    "不存在的令牌应当抛出相应异常");

        } finally {
            // 清理测试数据
            redisson.getKeys().delete(StringConstant.Redis.EMAIL_TOKEN + tokenDTO.getToken());
            redisson.getKeys().delete(StringConstant.Redis.EMAIL_TO_TOKEN + testUser.getEmail());
        }
    }

    /**
     * 测试令牌过期逻辑 - 使用已过期的令牌
     * 这个测试通过直接创建一个已经过期的令牌（过期时间在过去）来验证系统能正确识别过期令牌。
     */
    @Test
    void testExpiredToken() {
        // 创建一个已经过期的令牌 - 过期时间设为过去的时间点
        String expiredToken = "expired-token-" + System.currentTimeMillis();
        long pastTime = System.currentTimeMillis() - 60000; // 1分钟前

        // 手动将已过期的令牌存入Redis
        RMap<String, Object> expiredMap = redisson.getMap(StringConstant.Redis.EMAIL_TOKEN + expiredToken);
        expiredMap.put("userUuid", testUser.getUserUuid());
        expiredMap.put("email", testUser.getEmail());
        expiredMap.put("token", expiredToken);
        expiredMap.put("createdAt", String.valueOf(pastTime));
        expiredMap.put("expireTime", String.valueOf(pastTime + 1000)); // 创建后1秒过期，现在已过期

        try {
            // 验证已过期的令牌，应抛出异常
            BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                    tokenDAO.verifyEmailToken(expiredToken)
            );
            Assertions.assertTrue(exception.getMessage().contains(StringConstant.EMAIL_VERIFICATION_TOKEN_EXPIRED),
                    "已过期的令牌应当抛出过期异常");
        } finally {
            // 清理测试数据
            redisson.getKeys().delete(StringConstant.Redis.EMAIL_TOKEN + expiredToken);
        }
    }

    /**
     * 测试令牌未过期的情况
     * <p>
     * 这个测试验证系统能正确识别还未过期的令牌
     */
    @Test
    void testNonExpiredToken() {
        // 创建一个未过期的令牌 - 过期时间设在将来的某个时间点
        String validToken = "valid-token-" + System.currentTimeMillis();
        long currentTime = System.currentTimeMillis();
        long futureTime = currentTime + 3600000; // 1小时后过期

        // 手动将有效的令牌存入Redis
        RMap<String, Object> validMap = redisson.getMap(StringConstant.Redis.EMAIL_TOKEN + validToken);
        validMap.put("userUuid", testUser.getUserUuid());
        validMap.put("email", testUser.getEmail());
        validMap.put("token", validToken);
        validMap.put("createdAt", String.valueOf(currentTime));
        validMap.put("expireTime", String.valueOf(futureTime));

        try {
            // 验证未过期的令牌应该成功
            String userUuid = tokenDAO.verifyEmailToken(validToken);
            Assertions.assertEquals(testUser.getUserUuid(), userUuid, "未过期的令牌应返回正确的用户UUID");
        } finally {
            // 清理测试数据
            redisson.getKeys().delete(StringConstant.Redis.EMAIL_TOKEN + validToken);
        }
    }

    /**
     * 测试删除邮箱验证令牌
     */
    @Test
    void testDeleteEmailToken() {
        // 创建邮箱验证令牌
        EmailVerificationTokenDTO tokenDTO = tokenDAO.createEmailToken(testUser);
        String token = tokenDTO.getToken();

        // 验证令牌存在
        String redisKey = StringConstant.Redis.EMAIL_TOKEN + token;
        Assertions.assertTrue(redisson.getMap(redisKey).isExists(), "令牌应当在Redis中存在");

        // 删除令牌
        tokenDAO.deleteEmailToken(token);

        // 验证令牌已被删除
        Assertions.assertFalse(redisson.getMap(redisKey).isExists(), "令牌应当已从Redis中删除");

        // 清理剩余测试数据
        redisson.getKeys().delete(StringConstant.Redis.EMAIL_TO_TOKEN + testUser.getEmail());
    }

    /**
     * 测试获取邮件验证令牌的创建时间
     */
    @Test
    void testGetEmailTokenCreatAt() {
        // 创建邮箱验证令牌
        EmailVerificationTokenDTO tokenDTO = tokenDAO.createEmailToken(testUser);

        try {
            // 获取令牌创建时间
            long createdAt = tokenDAO.getEmailTokenCreatedAt(testUser);

            // 验证获取的创建时间与创建令牌时的时间相符
            Assertions.assertEquals(tokenDTO.getCreatedAt(), createdAt, 100,
                    "获取的令牌创建时间应与创建时的时间一致");

            // 测试不存在的令牌
            UserDO nonExistentUser = new UserDO().setEmail("non-existent@example.com");
            Assertions.assertEquals(0, tokenDAO.getEmailTokenCreatedAt(nonExistentUser),
                    "对于不存在的令牌应返回0");

        } finally {
            // 清理测试数据
            redisson.getKeys().delete(StringConstant.Redis.EMAIL_TOKEN + tokenDTO.getToken());
            redisson.getKeys().delete(StringConstant.Redis.EMAIL_TO_TOKEN + testUser.getEmail());
        }
    }
}
