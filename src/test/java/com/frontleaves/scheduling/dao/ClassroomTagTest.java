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

package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.ClassroomTagDAO;
import com.frontleaves.scheduling.models.entity.base.ClassroomTagDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 教室标签测试类
 * <p>
 * 该类用于测试 {@code ClassroomTagDAO} 中的方法，特别是对教室标签数据的获取功能。
 * 通过 Spring 的 {@code @SpringBootTest} 注解，该测试类可以在完整的应用上下文中运行，确保了与实际环境的一致性。
 * 测试过程中，首先会清除 Redis 缓存中的教室标签列表，然后从数据库中获取教室标签，并再次尝试从缓存中获取这些标签，最后验证两次获取的结果是否一致。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@SpringBootTest
class ClassroomTagTest {

    @Resource
    private ClassroomTagDAO classroomTagDAO;
    @Resource
    private RedissonClient redisson;

    /**
     * 获取教室标签
     * <p>
     * 该方法用于测试从数据库和缓存中获取教室标签的功能。首先，它会清除 Redis 缓存中的教室标签列表。
     * 然后，从数据库中获取教室标签，并记录调试信息。接着，再次尝试从缓存中获取这些标签，并记录调试信息。
     * 最后，验证两次获取的结果是否一致，确保数据的一致性和缓存的正确性。
     * </p>
     */
    @Test
    void testGetClassroomTag() {
        redisson.getKeys().delete(StringConstant.Redis.CLASSROOM_TAG_LIST);
        List<ClassroomTagDO> tags = classroomTagDAO.getTags();
        log.debug(LogConstant.DEBUG + "数据库获取 tags: {}", tags);
        List<ClassroomTagDO> tags2 = classroomTagDAO.getTags();
        log.debug(LogConstant.DEBUG + "缓存获取 tags: {}", tags2);
        Assertions.assertEquals(tags, tags2);
    }

    /**
     * 通过 UUID 获取教室标签
     * <p>
     * 该方法用于测试从数据库和缓存中通过 UUID 获取教室标签的功能。首先，它会清除 Redis 缓存中与指定 UUID 对应的教室标签。
     * 然后，从数据库中获取该教室标签，并记录调试信息。接着，再次尝试从缓存中获取该标签，并记录调试信息。
     * 最后，验证两次获取的结果是否一致，确保数据的一致性和缓存的正确性。
     * </p>
     */
    @Test
    void testGetClassroomTagByUuid() {
        ClassroomTagDO classroomTagDO = classroomTagDAO.lambdaQuery().list().get(0);
        redisson.getKeys().delete(StringConstant.Redis.CLASSROOM_TAG_UUID + classroomTagDO.getClassTagUuid());

        AtomicReference<ClassroomTagDO> classroomNoRedis = new AtomicReference<>();
        AtomicReference<ClassroomTagDO> classroomHasRedis = new AtomicReference<>();

        long noRedisNowTime = System.currentTimeMillis();
        Optional.ofNullable(classroomTagDAO.getTagByUuid(classroomTagDO.getClassTagUuid()))
                .ifPresent(classroomNoRedis::set);
        log.info("[ClassroomTagUUID] No Redis Time: {}ms", System.currentTimeMillis() - noRedisNowTime);
        long redisNowTime = System.currentTimeMillis();
        Optional.ofNullable(classroomTagDAO.getTagByUuid(classroomTagDO.getClassTagUuid()))
                .ifPresent(classroomHasRedis::set);
        log.info("[ClassroomTagUUID] Redis Time: {}ms", System.currentTimeMillis() - redisNowTime);
        Assertions.assertEquals(classroomNoRedis.get(), classroomHasRedis.get());
    }
}
