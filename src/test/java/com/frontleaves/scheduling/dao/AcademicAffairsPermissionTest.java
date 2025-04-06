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

import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.AcademicAffairsPermissionDAO;
import com.frontleaves.scheduling.models.entity.base.AcademicAffairsPermissionDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

/**
 * 教务权限测试类
 * <p>
 * 该类用于测试教务权限相关的功能，特别是权限信息的缓存机制。通过集成测试，确保在有无 Redis 缓存的情况下，
 * 教务权限信息能够正确获取，并且缓存能够显著提高数据获取的性能。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcademicAffairsPermissionTest {
    @Resource
    private AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    @Resource
    private RedissonClient redisson;

    /**
     * 测试根据UUID获取教务权限信息
     * <p>
     * 该方法用于测试在有无 Redis 缓存的情况下，获取教务权限信息的性能和一致性。
     * 具体步骤如下：
     * <ol>
     *     <li>删除与教务权限相关的所有 Redis 缓存条目。</li>
     *     <li>记录当前时间戳，调用 {@code academicAffairsPermissionDAO.getAcademicAffairsPermissionByUuid} 方法获取无缓存情况下的权限信息，并计算执行时间。</li>
     *     <li>再次记录当前时间戳，调用 {@code academicAffairsPermissionDAO.getAcademicAffairsPermissionByUuid} 方法获取有缓存情况下的权限信息，并计算执行时间。</li>
     *     <li>断言两次获取的权限信息是否一致，以验证缓存的有效性和数据的一致性。</li>
     * </ol>
     */
    @Test
    void testGetAcademicAffairsPermissionByUuid() {
        String uuid = UUID.randomUUID().toString();
        redisson.getKeys().deleteByPattern(StringConstant.Redis.ACADEMIC_AFFAIRS_PERMISSION_UUID + "*");

        long nowTime = System.currentTimeMillis();
        AcademicAffairsPermissionDO noRedisPermission = academicAffairsPermissionDAO.getAcademicAffairsPermissionByUuid(uuid);
        log.debug(LogConstant.TEST + "<无缓存>根据UUID获取教务权限 {}ms", System.currentTimeMillis() - nowTime);

        nowTime = System.currentTimeMillis();
        AcademicAffairsPermissionDO redisPermission = academicAffairsPermissionDAO.getAcademicAffairsPermissionByUuid(uuid);
        log.debug(LogConstant.TEST + "<有缓存>根据UUID获取教务权限 {}ms", System.currentTimeMillis() - nowTime);

        Assertions.assertEquals(noRedisPermission, redisPermission);
    }

    /**
     * 测试根据用户UUID获取教务权限信息
     * <p>
     * 该方法用于测试在有无 Redis 缓存的情况下，根据用户UUID获取教务权限信息的性能和一致性。
     * 具体步骤如下：
     * <ol>
     *     <li>删除与教务权限相关的所有 Redis 缓存条目。</li>
     *     <li>记录当前时间戳，调用 {@code academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid} 方法获取无缓存情况下的权限信息，并计算执行时间。</li>
     *     <li>再次记录当前时间戳，调用 {@code academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid} 方法获取有缓存情况下的权限信息，并计算执行时间。</li>
     *     <li>断言两次获取的权限信息是否一致，以验证缓存的有效性和数据的一致性。</li>
     * </ol>
     */
    @Test
    void testGetAcademicAffairsPermissionByUserUuid() {
        String userUuid = UUID.randomUUID().toString();
        redisson.getKeys().deleteByPattern(StringConstant.Redis.ACADEMIC_AFFAIRS_PERMISSION_USER_UUID + "*");

        long nowTime = System.currentTimeMillis();
        AcademicAffairsPermissionDO noRedisPermission = academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(userUuid);
        log.debug(LogConstant.TEST + "<无缓存>根据用户UUID获取教务权限 {}ms", System.currentTimeMillis() - nowTime);

        nowTime = System.currentTimeMillis();
        AcademicAffairsPermissionDO redisPermission = academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(userUuid);
        log.debug(LogConstant.TEST + "<有缓存>根据用户UUID获取教务权限 {}ms", System.currentTimeMillis() - nowTime);

        Assertions.assertEquals(noRedisPermission, redisPermission);
    }
}
