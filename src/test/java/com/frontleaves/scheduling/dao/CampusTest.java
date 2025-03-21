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
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.ListOfCampusDTO;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class CampusTest {
    @Resource
    private CampusDAO campusDAO;
    @Resource
    private RedissonClient redisson;

    /**
     * 测试分页查询校园数据的功能
     * <p>
     * 该方法首先删除与校园分页列表相关的所有 Redis 键，然后调用 {@code campusDAO.getPageOfCampus} 方法获取第一页的校园数据。
     * 如果返回的数据总数大于0，则再次调用该方法，传入关键词 "铁" 进行查询，并断言两次查询的结果不同。
     * 如果返回的数据总数为0，则断言结果为空。
     */
    @Test
    void testPageOfCampus() {
        // 清理测试相关的缓存数据
        redisson.getKeys().deleteByPattern(StringConstant.Redis.CAMPUS_PAGE_OF_LIST + "*");

        // 测试不带关键词的分页查询
        int pageNum = 1;
        int pageSize = 20;
        boolean isValid = true;
        Page<CampusDO> pageOfCampus = campusDAO.getPageOfCampus(pageNum, pageSize, isValid, null);

        if (pageOfCampus.getTotal() > 0) {
            // 当有数据时，测试带关键词的查询结果应与不带关键词的结果不同
            String keyword = "铁库";
            Page<CampusDO> keywordCampus = campusDAO.getPageOfCampus(pageNum, pageSize, isValid, keyword);

            // 断言带关键词和不带关键词的结果应不同
            Assertions.assertNotEquals(pageOfCampus.getRecords(), keywordCampus.getRecords(),
                    "使用关键词查询的结果应与不使用关键词的结果不同");
        } else {
            // 当没有数据时，确认结果列表为空
            Assertions.assertTrue(pageOfCampus.getRecords().isEmpty(),
                    "没有符合条件的校区数据时，返回列表应为空");
        }
    }

    /**
     * 测试校区列表获取功能
     * <p>
     * 该方法用于测试 {@code campusDAO.getCampusList} 方法的功能。具体步骤如下：
     * <ul>
     *     <li>调用 {@code campusDAO.getCampusList} 方法获取校区列表。</li>
     *     <li>断言返回的校区列表不为 {@code null}。</li>
     *     <li>断言返回的校区列表不为空。</li>
     * </ul>
     * 通过这些断言，确保从 Redis 缓存或数据库中获取的校区列表是有效的，并且包含至少一个校区信息。
     * </p>
     */
    @Test
    void testCampusList() {
        // 获取校区列表数据
        List<ListOfCampusDTO> campusList = campusDAO.getCampusList();

        // 验证返回的列表不为空
        Assertions.assertNotNull(campusList, "校区列表不应为null");
        Assertions.assertFalse(campusList.isEmpty(), "校区列表不应为空");

        // 验证返回的校区数据是否包含必要字段
        ListOfCampusDTO firstCampus = campusList.get(0);
        Assertions.assertNotNull(firstCampus.getCampusCode(), "校区ID不应为空");
        Assertions.assertNotNull(firstCampus.getCampusName(), "校区名称不应为空");
    }

    @Test
    void testDeleteCampus() {
        // 1.1 创建虚假校区数据
        CampusDO fakeCampus = new CampusDO();
        fakeCampus.setCampusUuid(UuidUtil.generateUuidNoDash()); // 生成32位UUID
        fakeCampus.setCampusName("测试校区");
        fakeCampus.setCampusCode("TEST_CAMPUS");
        fakeCampus.setCampusDesc("这是一个测试校区");
        fakeCampus.setCampusStatus(true); // 启用状态
        fakeCampus.setCampusAddress("测试地址");
        fakeCampus.setLatitude(30.1234567);
        fakeCampus.setLongitude(120.1234567);

        campusDAO.save(fakeCampus);

        // 2. 调用 deleteCampus 方法删除校园信息
        campusDAO.deleteCampus(fakeCampus);

        // 3. 验证数据库中是否已删除该校园信息
        Assertions.assertNull(campusDAO.getById(fakeCampus.getCampusUuid()));

        // 4. 检查 Redis 缓存中的相关数据是否被删除
        RMap<String, String> rMap = redisson.getMap(
                StringConstant.Redis.CAMPUS_UUID + fakeCampus.getCampusUuid());
        RBucket<String> rBucket = redisson.getBucket(
                StringConstant.Redis.CAMPUS_CODE + fakeCampus.getCampusCode());
        RBucket<String> rBucket1 = redisson.getBucket(
                StringConstant.Redis.CAMPUS_NAME + fakeCampus.getCampusName());
        RList<ListOfCampusDTO> rList = redisson.getList(StringConstant.Redis.CAMPUS_LIST + "*");
        RMap<String, String> rPage = redisson.getMap(
                StringConstant.Redis.CAMPUS_PAGE_OF_LIST + "*");
        // 5. 断言 Redis 缓存中的数据不存在
        Assertions.assertFalse(rList.isExists());
        Assertions.assertFalse(rMap.isExists());
        Assertions.assertFalse(rBucket.isExists());
        Assertions.assertFalse(rBucket1.isExists());
        Assertions.assertFalse(rPage.isExists());
    }
}
