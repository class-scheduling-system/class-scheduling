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
import com.frontleaves.scheduling.daos.GradeDAO;
import com.frontleaves.scheduling.models.entity.GradeDO;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
class GradeDAOTest {

    @Resource
    private GradeDAO gradeDAO;

    @Resource
    private RedissonClient redisson;

    /**
     * 测试根据UUID获取年级
     * <p>
     * 本测试需要数据库中已存在年级数据
     * </p>
     */
    @Test
    void testGetGradeByUuid() {
        // 获取数据库中第一条年级数据作为测试数据
        GradeDO gradeDO = gradeDAO.lambdaQuery().list().get(0);
        String existingUuid = gradeDO.getGradeUuid();
        
        log.debug("测试获取年级信息(UUID)");
        GradeDO gradeData = gradeDAO.getGradeByUuid(existingUuid);
        
        // 如果年级在数据库中存在，应该不为null
        Assertions.assertNotNull(gradeData);
        
        log.debug("删除缓存并再次获取");
        // 删除Redis缓存
        redisson.getMap(StringConstant.Redis.GRADE_UUID + existingUuid).delete();
        
        // 再次获取，这次应该从数据库获取并重新缓存
        GradeDO gradeData2 = gradeDAO.getGradeByUuid(existingUuid);
        
        // 验证仍然能获到数据
        Assertions.assertNotNull(gradeData2);
        
        // 清理测试后的缓存
        redisson.getMap(StringConstant.Redis.GRADE_UUID + existingUuid).delete();
    }

    /**
     * 测试获取不存在的年级
     */
    @Test
    void testGetNonExistingGrade() {
        log.debug("测试获取不存在的年级");
        
        // 使用一个肯定不存在的UUID
        String nonExistingUuid = UuidUtil.generateUuidNoDash();
        
        // 尝试获取不存在的年级
        GradeDO gradeData = gradeDAO.getGradeByUuid(nonExistingUuid);
        
        // 应该返回null
        Assertions.assertNull(gradeData);
    }
    @Test
    void testGetGradeListForUpdate() {
        // 先清除Redis中的年级列表缓存
        redisson.getKeys().delete(StringConstant.Redis.GRADE_LIST);

        // 第一次调用getGradeList方法，应该从数据库获取数据并缓存到Redis
        List<GradeDO> gradeList1 = gradeDAO.getGradeListForUpdate();

        // 断言从数据库获取的年级列表不为空
        Assertions.assertNotNull(gradeList1);
        Assertions.assertFalse(gradeList1.isEmpty());

        // 验证Redis中是否已缓存年级列表
        RList<GradeDO> redisCache = redisson.getList(StringConstant.Redis.GRADE_LIST);
        Assertions.assertTrue(redisCache.isExists());

        // 记录第一次查询结果的大小
        int firstResultSize = gradeList1.size();

        // 第二次调用getGradeList方法，应该从Redis缓存中获取数据
        List<GradeDO> gradeList2 = gradeDAO.getGradeListForUpdate();

        // 断言第二次获取的结果与第一次结果大小相同
        Assertions.assertEquals(firstResultSize, gradeList2.size());
    }
}
