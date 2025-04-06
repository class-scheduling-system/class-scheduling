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

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.ClassroomDAO;
import com.frontleaves.scheduling.models.entity.base.ClassroomDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 教室相关功能测试
 * <p>
 * 该类用于对教室相关的功能进行单元测试，确保各项操作符合预期。
 * 测试内容可能包括但不限于教室的创建、修改、查询和删除等。
 * 所有测试方法都应确保在干净的数据环境下运行，以避免测试之间的相互影响。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClassroomTest {
    @Resource
    private ClassroomDAO classroomDAO;
    @Resource
    private RedissonClient redisson;

    /**
     * 测试分页查询教室信息
     * <p>
     * 该方法用于测试 {@code ClassroomDAO} 的分页查询功能。具体来说，它会执行两次查询：
     * 1. 不带关键字的分页查询，获取所有符合条件的教室信息。
     * 2. 带关键字 "室" 的分页查询，获取名称或编号中包含 "室" 的教室信息。
     * 每次查询后，都会记录查询到的总记录数和每条记录的具体信息。
     * </p>
     * <p>
     * 通过这些测试，可以验证分页查询功能是否按预期工作，并确保在不同条件下都能正确返回结果。
     * </p>
     */
    @Test
    void testPageQuery() {
        AtomicReference<Page<ClassroomDO>> noKeywordPage = new AtomicReference<>();
        AtomicReference<Page<ClassroomDO>> hasKeywordPage = new AtomicReference<>();
        Optional.ofNullable(classroomDAO.getClassroomPage(1, 20, true, null, null, null))
                .ifPresent(page -> {
                    log.info("Total No Keyword: {}", page.getTotal());
                    page.getRecords().forEach(classroomDO -> log.info("{}", classroomDO));
                    noKeywordPage.set(page);
                });
        Optional.ofNullable(classroomDAO.getClassroomPage(1, 20, true, "室", null, null))
                .ifPresent(page -> {
                    log.info("Total Has Keyword: {}", page.getTotal());
                    page.getRecords().forEach(classroomDO -> log.info("{}", classroomDO));
                    hasKeywordPage.set(page);
                });
        if (noKeywordPage.get().hasNext()) {
            Assertions.assertNotEquals(noKeywordPage.get().getRecords(), hasKeywordPage.get().getRecords());
        } else {
            Assertions.assertTrue(true);
        }
    }

    /**
     * 测试通过UUID查询教室信息
     * <p>
     * 该方法用于测试通过UUID查询教室信息的功能。具体来说，它会执行以下步骤：
     * 1. 从数据库中获取一个教室实体 {@code ClassroomDO}。
     * 2. 删除Redis中与该教室UUID相关的缓存。
     * 3. 第一次查询：不使用Redis缓存，直接从数据库中查询教室信息，并记录查询时间。
     * 4. 第二次查询：使用Redis缓存，再次查询教室信息，并记录查询时间。
     * 5. 比较两次查询的结果，确保它们一致。
     * </p>
     * <p>
     * 通过这些测试，可以验证通过UUID查询教室信息的功能是否按预期工作，并确保在不同条件下（有无缓存）都能正确返回结果。
     * </p>
     */
    @Test
    void testClassroomByUuid() {
        ClassroomDO classroomDO = classroomDAO.lambdaQuery().list().get(0);
        redisson.getKeys().delete(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getClassroomUuid());

        AtomicReference<ClassroomDO> classroomNoRedis = new AtomicReference<>();
        AtomicReference<ClassroomDO> classroomHasRedis = new AtomicReference<>();

        long noRedisNowTime = System.currentTimeMillis();
        Optional.ofNullable(classroomDAO.getClassroomByUuid(classroomDO.getClassroomUuid()))
                .ifPresent(classroomNoRedis::set);
        log.info("[ClassroomUUID] No Redis Time: {}ms", System.currentTimeMillis() - noRedisNowTime);
        long redisNowTime = System.currentTimeMillis();
        Optional.ofNullable(classroomDAO.getClassroomByUuid(classroomDO.getClassroomUuid()))
                .ifPresent(classroomHasRedis::set);
        log.info("[ClassroomUUID] Redis Time: {}ms", System.currentTimeMillis() - redisNowTime);
        Assertions.assertEquals(classroomNoRedis.get(), classroomHasRedis.get());
    }

    /**
     * 测试通过教室编号获取教室信息的功能
     * <p>
     * 该测试方法首先从数据库中获取一个教室对象，并删除与之关联的 Redis 缓存。然后，分别在没有 Redis 缓存和有 Redis 缓存的情况下，通过教室编号获取教室信息，并记录两次请求的时间差。最后，断言两次获取到的教室信息是否一致。
     * <p>
     * 此方法用于验证 Redis 缓存在查询教室信息时的性能提升效果。
     */
    @Test
    void testClassroomByNumber() {
        ClassroomDO classroomDO = classroomDAO.lambdaQuery().list().get(0);
        redisson.getKeys().delete(StringConstant.Redis.CLASSROOM_NUMBER + classroomDO.getNumber());
        redisson.getKeys().delete(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getClassroomUuid());

        AtomicReference<ClassroomDO> classroomNoRedis = new AtomicReference<>();
        AtomicReference<ClassroomDO> classroomHasRedis = new AtomicReference<>();

        long noRedisNowTime = System.currentTimeMillis();
        Optional.ofNullable(classroomDAO.getClassroomByNumber(classroomDO.getNumber()))
                .ifPresent(classroomNoRedis::set);
        Assertions.assertNotNull(redisson.getBucket(StringConstant.Redis.CLASSROOM_NUMBER + classroomDO.getNumber()).get());
        log.info("[ClassroomNumber] No Redis Time: {}ms", System.currentTimeMillis() - noRedisNowTime);
        long redisNowTime = System.currentTimeMillis();
        Optional.ofNullable(classroomDAO.getClassroomByNumber(classroomDO.getNumber()))
                .ifPresent(classroomHasRedis::set);
        log.info("[ClassroomNumber] Redis Time: {}ms", System.currentTimeMillis() - redisNowTime);
        Assertions.assertEquals(classroomNoRedis.get(), classroomHasRedis.get());
    }

    /**
     * 测试编辑教室信息
     * <p>
     * 该方法用于测试编辑教室信息的功能。它首先从数据库中获取一个教室对象，然后检查Redis中是否存在与该教室相关的缓存。
     * 如果存在，则通过调用 {@code updateClassroom} 方法更新教室信息，并再次检查Redis缓存是否已被正确清除。
     * 该测试确保了在编辑教室信息后，相关缓存能够被正确地删除，以保证数据的一致性。
     */
    @Test
    void testEditClassroom() {
        ClassroomDO classroomDO = classroomDAO.lambdaQuery().list().get(0);
        classroomDAO.getClassroomByUuid(classroomDO.getClassroomUuid());
        classroomDAO.getClassroomByNumber(classroomDO.getNumber());
        Assertions.assertTrue(redisson.getMap(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getClassroomUuid()).isExists());
        Assertions.assertTrue(redisson.getBucket(StringConstant.Redis.CLASSROOM_NUMBER + classroomDO.getNumber()).isExists());
        Optional.of(classroomDO)
                .ifPresent(data -> {
                    classroomDAO.updateClassroom(data);
                    Assertions.assertFalse(redisson.getBucket(StringConstant.Redis.CLASSROOM_NUMBER + data.getNumber()).isExists());
                    Assertions.assertFalse(redisson.getMap(StringConstant.Redis.CLASSROOM_UUID + data.getClassroomUuid()).isExists());
                });
    }

    /**
     * 测试删除教室功能
     * <p>
     * 该测试方法验证了删除教室的功能是否正常工作。具体步骤如下：
     * <ol>
     *     <li>从数据库中获取一个教室信息。</li>
     *     <li>通过教室的UUID和编号在Redis中检查是否存在相应的键值对。</li>
     *     <li>调用 {@code deleteClassroom} 方法删除指定的教室。</li>
     *     <li>再次检查Redis中相应的键值对是否已被删除。</li>
     *     <li>通过UUID检查数据库中是否已成功删除该教室记录。</li>
     * </ol>
     * <p>
     * 如果所有检查都通过，则表示删除教室功能正常。
     */
    @Test
    void testDeleteClassroom() {
        ClassroomDO classroomDO = classroomDAO.lambdaQuery().list().get(0);
        classroomDAO.getClassroomByUuid(classroomDO.getClassroomUuid());
        classroomDAO.getClassroomByNumber(classroomDO.getNumber());
        Assertions.assertTrue(redisson.getMap(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getClassroomUuid()).isExists());
        Assertions.assertTrue(redisson.getBucket(StringConstant.Redis.CLASSROOM_NUMBER + classroomDO.getNumber()).isExists());
        classroomDAO.deleteClassroom(classroomDO.getClassroomUuid());
        Assertions.assertFalse(redisson.getBucket(StringConstant.Redis.CLASSROOM_NUMBER + classroomDO.getNumber()).isExists());
        Assertions.assertFalse(redisson.getMap(StringConstant.Redis.CLASSROOM_UUID + classroomDO.getClassroomUuid()).isExists());
        classroomDAO.getOptById(classroomDO.getClassroomUuid())
                .ifPresentOrElse(classroom -> {
                    log.info("Delete Classroom Failed: {}", classroom);
                    Assertions.fail();
                }, () -> {
                    log.info("Delete Classroom Success");
                    Assertions.assertTrue(true);
                });

    }
}
