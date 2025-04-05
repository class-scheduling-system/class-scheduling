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
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.daos.CampusDAO;
import com.frontleaves.scheduling.models.dto.BackAddBuildingDTO;
import com.frontleaves.scheduling.models.entity.BuildingDO;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * BuildingDAO单元测试
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class BuildingTest {
    @Resource
    private BuildingDAO buildingDAO;

    @Resource
    private RedissonClient redisson;
    @Autowired
    private CampusDAO campusDAO;

    /**
     * 测试前清理相关缓存
     */
    @BeforeEach
    void setup() {
        // 清除可能存在的缓存，确保测试的独立性
        redisson.getKeys().deleteByPattern(StringConstant.Redis.BUILDING_KEY_LIST + "*");
    }

    /**
     * 测试根据关键字获取建筑列表 - 空关键字
     * 预期行为：应当返回所有建筑物（若有），没有则返回null
     */
    @Test
    void testGetBuildingListByKeyWithEmptyKeyword() {
        log.debug("测试获取建筑列表 - 空关键字");

        // 测试null关键字
        List<BuildingDO> buildingsWithNull = buildingDAO.getBuildingListByKey(null);
        log.debug("testGetBuildingListByKeyWithEmptyKeyword - null关键字: {}", buildingsWithNull);

        // 测试空字符串关键字
        List<BuildingDO> buildingsWithEmpty = buildingDAO.getBuildingListByKey("");
        log.debug("testGetBuildingListByKeyWithEmptyKeyword - 空字符串关键字: {}", buildingsWithEmpty);

        // 断言：两种情况应当有相同的结果
        if (buildingsWithNull == null) {
            Assertions.assertNull(buildingsWithEmpty, "空关键字和null关键字应当返回相同的结果");
        } else {
            if (buildingsWithEmpty != null) {
                Assertions.assertEquals(buildingsWithNull.size(), buildingsWithEmpty.size(), "空关键字和null关键字应当返回相同数量的结果");
            }
        }
    }

    /**
     * 测试根据关键字获取建筑列表 - 有效关键字
     * 预期行为：应当返回包含关键字的建筑物列表
     */
    @Test
    void testGetBuildingListByKeyWithValidKeyword() {
        log.debug("测试获取建筑列表 - 有效关键字");

        // 假设数据库中存在一个名为"教学楼"的建筑物
        String testKeyword = "教学";

        // 第一次调用，走数据库查询
        List<BuildingDO> buildings = buildingDAO.getBuildingListByKey(testKeyword);
        log.debug("testGetBuildingListByKeyWithValidKeyword - 从数据库查询: {}", buildings);

        // 如果结果不为null，测试是否所有结果都包含关键字
        if (buildings != null && !buildings.isEmpty()) {
            for (BuildingDO building : buildings) {
                Assertions.assertTrue(
                    building.getBuildingName().contains(testKeyword),
                    "查询结果中的建筑物名称应当包含关键字"
                );
            }

            // 第二次调用，走缓存查询
            List<BuildingDO> cachedBuildings = buildingDAO.getBuildingListByKey(testKeyword);
            log.debug("testGetBuildingListByKeyWithValidKeyword - 从缓存查询: {}", cachedBuildings);

            // 断言：缓存结果与数据库结果应当一致
            if (cachedBuildings != null) {
                Assertions.assertEquals(buildings.size(), cachedBuildings.size(), "缓存结果与数据库结果数量应当一致");
            }
        } else {
            log.debug("数据库中没有包含关键字 '{}' 的建筑物", testKeyword);
        }
    }

    /**
     * 测试根据关键字获取建筑列表 - 无匹配结果
     * 预期行为：应当返回null
     */
    @Test
    void testGetBuildingListByKeyWithNoMatch() {
        log.debug("测试获取建筑列表 - 无匹配结果");

        // 使用一个不太可能存在的关键字
        String nonExistingKeyword = "非常不可能存在的建筑物名称XYZABC123";

        List<BuildingDO> buildings = buildingDAO.getBuildingListByKey(nonExistingKeyword);
        log.debug("testGetBuildingListByKeyWithNoMatch - 无匹配结果: {}", buildings);

        // 断言：应当返回null
        Assertions.assertNull(buildings, "不存在的关键字应当返回null");
    }

    /**
     * 测试缓存功能
     * 预期行为：第一次查询后结果应被缓存，第二次查询应从缓存中获取
     */
    @Test
    void testCaching() {
        log.debug("测试缓存功能");

        String testKeyword = "教学";

        // 确保缓存不存在
        RList<BuildingDO> rList = redisson.getList(StringConstant.Redis.BUILDING_KEY_LIST + testKeyword);
        Assertions.assertFalse(rList.isExists(), "测试前缓存不应存在");

        // 第一次调用，走数据库查询并缓存结果
        List<BuildingDO> firstCallResult = buildingDAO.getBuildingListByKey(testKeyword);
        log.debug("testCaching - 第一次调用结果: {}", firstCallResult);

        // 验证缓存是否已创建
        Assertions.assertTrue(rList.isExists(), "第一次调用后缓存应当存在");

        // 第二次调用，应从缓存中获取数据
        List<BuildingDO> secondCallResult = buildingDAO.getBuildingListByKey(testKeyword);
        log.debug("testCaching - 第二次调用结果: {}", secondCallResult);

        // 断言：两次调用结果应当一致
        if (firstCallResult == null) {
            Assertions.assertNull(secondCallResult, "如果第一次调用返回null，第二次调用也应当返回null");
        } else {
            if (secondCallResult != null) {
                Assertions.assertEquals(firstCallResult.size(), secondCallResult.size(), "两次调用结果数量应当一致");
            }
        }
    }


    @Test
    void testSaveBuildingBackError() {
        // 创建一个新的建筑物对象
        BuildingDO newBuilding = new BuildingDO();
        newBuilding.setBuildingUuid(UuidUtil.generateUuidNoDash())
                .setBuildingName("测试教学楼")
                .setCampusUuid(campusDAO.lambdaQuery().list().get(0).getCampusUuid())
                .setStatus(true);
        buildingDAO.saveBuildingBackError(newBuilding, 1);
        BuildingDO building = buildingDAO.lambdaQuery()
                .eq(BuildingDO::getBuildingUuid, newBuilding.getBuildingUuid())
                .one();
        Assertions.assertNotNull(building);
        buildingDAO.deleteBuilding(newBuilding);
    }

    @Test
    void testSaveBuildingBackErrorWithError(){
        BuildingDO buildingDO = new BuildingDO();
        buildingDO.setBuildingUuid(UuidUtil.generateUuidNoDash())
                  .setBuildingName("测试教学楼")
                 .setCampusUuid(UuidUtil.generateUuidNoDash())
                 .setStatus(true);
        Assertions.assertThrows(BusinessException.class, () -> buildingDAO.saveBuildingBackError(buildingDO, 1), "保存教学楼时应抛出异常·1");

        buildingDO.setBuildingUuid(UuidUtil.generateUuidNoDash())
                .setBuildingName("测试教学楼")
                .setCampusUuid(campusDAO.lambdaQuery().list().get(0).getCampusUuid())
                .setStatus(true);
        Assertions.assertThrows(BusinessException.class, () -> {
            buildingDAO.saveBuildingBackError(buildingDO, 1);
            buildingDAO.saveBuildingBackError(buildingDO, 1);
        }, "保存教学楼时应抛出异常·2");

        buildingDO.setBuildingUuid(UuidUtil.generateUuidNoDash())
                .setBuildingName(null)
                .setCampusUuid(campusDAO.lambdaQuery().list().get(0).getCampusUuid())
                .setStatus(true);
        Assertions.assertThrows(BusinessException.class, () -> buildingDAO.saveBuildingBackError(buildingDO, 1), "保存教学楼时应抛出异常");
    }

   @Test
    void testSaveBuildingIgnoreError() {
       // 创建一个新的建筑物对象
       BuildingDO newBuilding = new BuildingDO();
       newBuilding.setBuildingUuid(UuidUtil.generateUuidNoDash())
               .setBuildingName("行政楼")
               .setCampusUuid(campusDAO.lambdaQuery().list().get(0).getCampusUuid())
               .setStatus(true);
       List<BackAddBuildingDTO.FailedDetail> failedDetails = buildingDAO.saveBuildingIgnoreError(newBuilding, 1);
       BuildingDO buildingDO = buildingDAO.lambdaQuery()
               .eq(BuildingDO::getBuildingUuid, newBuilding.getBuildingUuid())
               .one();
       Assertions.assertTrue(failedDetails.isEmpty());
         Assertions.assertNotNull(buildingDO);
         buildingDAO.deleteBuilding(newBuilding);

   }

    @Test
    void testSaveBuildingIgnoreError2() {
        // 创建一个新的建筑物对象
        BuildingDO newBuilding = new BuildingDO();
        newBuilding.setBuildingUuid(UuidUtil.generateUuidNoDash())
                .setBuildingName(null)
                .setCampusUuid(campusDAO.lambdaQuery().list().get(0).getCampusUuid())
                .setStatus(true);
        List<BackAddBuildingDTO.FailedDetail> failedDetails = buildingDAO.saveBuildingIgnoreError(newBuilding, 1);
        BuildingDO buildingDO = buildingDAO.lambdaQuery()
                .eq(BuildingDO::getBuildingUuid, newBuilding.getBuildingUuid())
                .one();
        Assertions.assertFalse(failedDetails.isEmpty());
        Assertions.assertNull(buildingDO);

    }
}
