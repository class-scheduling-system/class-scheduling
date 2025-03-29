package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.lite.UnitTypeLiteDTO;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
class UnitTypeTest {
    @Resource
    private UnitTypeDAO unitTypeDAO;
    @Resource
    private RedissonClient redisson;
    private UnitTypeDO setupUnitType;

    @BeforeEach
    void setUp() {
        log.debug("UnitTypeDAO单元测试初始化");
        setupUnitType = new UnitTypeDO();
        setupUnitType.setUnitTypeUuid(UuidUtil.generateUuidNoDash())
                .setName("测试单位类型")
                .setEnglishName("Test Unit Type")
                .setShortName("TUT");
        if (unitTypeDAO.lambdaQuery().eq(UnitTypeDO::getName, setupUnitType.getName()).one() != null) {
            unitTypeDAO.lambdaUpdate().eq(UnitTypeDO::getName, setupUnitType.getName()).remove();
        }
        unitTypeDAO.save(setupUnitType);
    }

    @Test
    void testGetUnitTypeByUuid() {
        // 清除Redis缓存
        redisson.getKeys().delete(StringConstant.Redis.UNIT_TYPE_UUID + setupUnitType.getUnitTypeUuid());

        // 测试从数据库获取
        UnitTypeDO unitTypeDO = unitTypeDAO.getUnitTypeByUuid(setupUnitType.getUnitTypeUuid());
        Assertions.assertNotNull(unitTypeDO, "通过UUID获取单位类型不应为空");
        Assertions.assertEquals(setupUnitType.getName(), unitTypeDO.getName(), "单位类型名称应匹配");

        // 验证Redis缓存是否已创建
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.UNIT_TYPE_UUID + setupUnitType.getUnitTypeUuid());
        Assertions.assertTrue(map.isExists(), "Redis缓存应该已创建");

        // 测试从Redis缓存获取
        UnitTypeDO cachedUnitType = unitTypeDAO.getUnitTypeByUuid(setupUnitType.getUnitTypeUuid());
        Assertions.assertNotNull(cachedUnitType, "从Redis缓存获取的单位类型不应为空");
        Assertions.assertEquals(setupUnitType.getName(), cachedUnitType.getName(), "从缓存获取的单位类型名称应匹配");
    }

    @Test
    void testGetUnitTypeByName() {
        // 清除Redis缓存
        redisson.getKeys().delete(StringConstant.Redis.UNIT_TYPE_NAME + setupUnitType.getName());

        // 测试从数据库获取
        UnitTypeDO unitTypeDO = unitTypeDAO.getUnitTypeByName(setupUnitType.getName());
        Assertions.assertNotNull(unitTypeDO, "通过名称获取单位类型不应为空");
        Assertions.assertEquals(setupUnitType.getUnitTypeUuid(), unitTypeDO.getUnitTypeUuid(), "单位类型UUID应匹配");

        // 验证Redis缓存是否已创建
        RBucket<String> bucket = redisson.getBucket(
                StringConstant.Redis.UNIT_TYPE_NAME + setupUnitType.getName());
        Assertions.assertTrue(bucket.isExists(), "Redis缓存应该已创建");

        // 测试从Redis缓存获取
        UnitTypeDO cachedUnitType = unitTypeDAO.getUnitTypeByName(setupUnitType.getName());
        Assertions.assertNotNull(cachedUnitType, "从Redis缓存获取的单位类型不应为空");
        Assertions.assertEquals(setupUnitType.getUnitTypeUuid(), cachedUnitType.getUnitTypeUuid(), "从缓存获取的单位类型UUID应匹配");
    }

    @Test
    void testDeleteUnitTypeCache() {
        // 先确保缓存存在
        unitTypeDAO.getUnitTypeByUuid(setupUnitType.getUnitTypeUuid());
        unitTypeDAO.getUnitTypeByName(setupUnitType.getName());

        // 删除缓存
        unitTypeDAO.deleteUnitTypeCache(setupUnitType);

        // 验证缓存是否已被删除
        RMap<String, String> uuidMap = redisson.getMap(
                StringConstant.Redis.UNIT_TYPE_UUID + setupUnitType.getUnitTypeUuid());
        RBucket<String> nameBucket = redisson.getBucket(
                StringConstant.Redis.UNIT_TYPE_NAME + setupUnitType.getName());

        Assertions.assertFalse(uuidMap.isExists(), "UUID缓存应该已被删除");
        Assertions.assertFalse(nameBucket.isExists(), "名称缓存应该已被删除");
    }

    @Test
    void testGetPageOfUnitType() {
        // 清除分页缓存
        redisson.getKeys().deleteByPattern(StringConstant.Redis.UNIT_TYPE_PAGE_OF_LIST + "*");

        // 测试分页查询
        int page = 1;
        int size = 10;
        boolean isDesc = true;
        String keyword = "测试";

        Page<UnitTypeDO> pageResult = unitTypeDAO.getPageOfUnitType(page, size, isDesc, keyword);
        Assertions.assertNotNull(pageResult, "分页结果不应为空");
        Assertions.assertTrue(pageResult.getTotal() > 0, "应该至少有一条记录");

        // 验证缓存是否已创建
        RMap<String, String> pageMap = redisson.getMap(
                StringConstant.Redis.UNIT_TYPE_PAGE_OF_LIST + page + ":" + size + ":" + isDesc + ":" + keyword);
        Assertions.assertTrue(pageMap.isExists(), "分页缓存应该已创建");

        // 测试从缓存获取
        Page<UnitTypeDO> cachedPage = unitTypeDAO.getPageOfUnitType(page, size, isDesc, keyword);
        Assertions.assertEquals(pageResult.getTotal(), cachedPage.getTotal(), "从缓存获取的总记录数应匹配");
    }

    @Test
    void testGetUnitTypeList() {
        // 清除列表缓存
        redisson.getKeys().delete(StringConstant.Redis.UNIT_TYPE_LIST);

        // 测试获取列表
        List<UnitTypeLiteDTO> typeList = unitTypeDAO.getUnitTypeList();
        Assertions.assertNotNull(typeList, "单位类型列表不应为空");
        Assertions.assertFalse(typeList.isEmpty(), "单位类型列表应该包含数据");

        // 验证缓存是否已创建
        RList<UnitTypeLiteDTO> cachedList = redisson.getList(StringConstant.Redis.UNIT_TYPE_LIST);
        Assertions.assertTrue(cachedList.isExists(), "列表缓存应该已创建");

        // 测试从缓存获取
        List<UnitTypeLiteDTO> cachedTypeList = unitTypeDAO.getUnitTypeList();
        Assertions.assertEquals(typeList.size(), cachedTypeList.size(), "从缓存获取的列表大小应匹配");
    }
}
