package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.models.dto.lite.UnitCategoryLiteDTO;
import com.frontleaves.scheduling.models.entity.base.UnitCategoryDO;
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
class UnitCategoryTest {
    @Resource
    private UnitCategoryDAO unitCategoryDAO;
    @Resource
    private RedissonClient redisson;
    private UnitCategoryDO setupUnitCategory;

    @BeforeEach
    void setUp() {
        log.debug("UnitCategoryDAO单元测试初始化");
        setupUnitCategory = new UnitCategoryDO();
        setupUnitCategory.setUnitCategoryUuid(UuidUtil.generateUuidNoDash())
                .setName("测试单位类别")
                .setEnglishName("Test Unit Category")
                .setShortName("TUC")
                .setOrder(1);
        if (unitCategoryDAO.lambdaQuery().eq(UnitCategoryDO::getName, setupUnitCategory.getName()).one() != null) {
            unitCategoryDAO.lambdaUpdate().eq(UnitCategoryDO::getName, setupUnitCategory.getName()).remove();
        }
        unitCategoryDAO.save(setupUnitCategory);
    }

    @Test
    void testGetUnitCategoryByUuid() {
        // 清除Redis缓存
        redisson.getKeys().delete(StringConstant.Redis.UNIT_CATEGORY_UUID + setupUnitCategory.getUnitCategoryUuid());

        // 测试从数据库获取
        UnitCategoryDO unitCategoryDO = unitCategoryDAO.getUnitCategoryByUuid(setupUnitCategory.getUnitCategoryUuid());
        Assertions.assertNotNull(unitCategoryDO, "通过UUID获取单位类别不应为空");
        Assertions.assertEquals(setupUnitCategory.getName(), unitCategoryDO.getName(), "单位类别名称应匹配");

        // 验证Redis缓存是否已创建
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.UNIT_CATEGORY_UUID + setupUnitCategory.getUnitCategoryUuid());
        Assertions.assertTrue(map.isExists(), "Redis缓存应该已创建");

        // 测试从Redis缓存获取
        UnitCategoryDO cachedUnitCategory = unitCategoryDAO.getUnitCategoryByUuid(setupUnitCategory.getUnitCategoryUuid());
        Assertions.assertNotNull(cachedUnitCategory, "从Redis缓存获取的单位类别不应为空");
        Assertions.assertEquals(setupUnitCategory.getName(), cachedUnitCategory.getName(), "从缓存获取的单位类别名称应匹配");
    }

    @Test
    void testGetUnitCategoryByName() {
        // 清除Redis缓存
        redisson.getKeys().delete(StringConstant.Redis.UNIT_CATEGORY_NAME + setupUnitCategory.getName());

        // 测试从数据库获取
        UnitCategoryDO unitCategoryDO = unitCategoryDAO.getUnitCategoryByName(setupUnitCategory.getName());
        Assertions.assertNotNull(unitCategoryDO, "通过名称获取单位类别不应为空");
        Assertions.assertEquals(setupUnitCategory.getUnitCategoryUuid(), unitCategoryDO.getUnitCategoryUuid(), "单位类别UUID应匹配");

        // 验证Redis缓存是否已创建
        RBucket<String> bucket = redisson.getBucket(
                StringConstant.Redis.UNIT_CATEGORY_NAME + setupUnitCategory.getName());
        Assertions.assertTrue(bucket.isExists(), "Redis缓存应该已创建");

        // 测试从Redis缓存获取
        UnitCategoryDO cachedUnitCategory = unitCategoryDAO.getUnitCategoryByName(setupUnitCategory.getName());
        Assertions.assertNotNull(cachedUnitCategory, "从Redis缓存获取的单位类别不应为空");
        Assertions.assertEquals(setupUnitCategory.getUnitCategoryUuid(), cachedUnitCategory.getUnitCategoryUuid(), "从缓存获取的单位类别UUID应匹配");
    }

    @Test
    void testGetUnitCategoryByNameExceptUuid() {
        // 测试获取除指定UUID外的单位类别
        UnitCategoryDO unitCategoryDO = unitCategoryDAO.getUnitCategoryByNameExceptUuid(
                setupUnitCategory.getName(), "different-uuid");
        Assertions.assertNotNull(unitCategoryDO, "应能找到指定名称的单位类别");
        Assertions.assertEquals(setupUnitCategory.getUnitCategoryUuid(), unitCategoryDO.getUnitCategoryUuid(), "单位类别UUID应匹配");

        // 测试获取除自身UUID外的单位类别（应返回null）
        UnitCategoryDO result = unitCategoryDAO.getUnitCategoryByNameExceptUuid(
                setupUnitCategory.getName(), setupUnitCategory.getUnitCategoryUuid());
        Assertions.assertNull(result, "排除自身UUID后不应找到其他单位类别");
    }

    @Test
    void testDeleteUnitCategoryCache() {
        // 先确保缓存存在
        unitCategoryDAO.getUnitCategoryByUuid(setupUnitCategory.getUnitCategoryUuid());
        unitCategoryDAO.getUnitCategoryByName(setupUnitCategory.getName());

        // 删除缓存
        unitCategoryDAO.deleteUnitCategoryCache(setupUnitCategory);

        // 验证缓存是否已被删除
        RMap<String, String> uuidMap = redisson.getMap(
                StringConstant.Redis.UNIT_CATEGORY_UUID + setupUnitCategory.getUnitCategoryUuid());
        RBucket<String> nameBucket = redisson.getBucket(
                StringConstant.Redis.UNIT_CATEGORY_NAME + setupUnitCategory.getName());

        Assertions.assertFalse(uuidMap.isExists(), "UUID缓存应该已被删除");
        Assertions.assertFalse(nameBucket.isExists(), "名称缓存应该已被删除");
    }

    @Test
    void testGetPageOfUnitCategory() {
        // 清除分页缓存
        redisson.getKeys().deleteByPattern(StringConstant.Redis.UNIT_CATEGORY_PAGE + "*");

        // 测试分页查询
        int page = 1;
        int size = 10;
        boolean isDesc = true;
        String keyword = "测试";

        Page<UnitCategoryDO> pageResult = unitCategoryDAO.getPageOfUnitCategory(page, size, isDesc, keyword);
        Assertions.assertNotNull(pageResult, "分页结果不应为空");
        Assertions.assertTrue(pageResult.getTotal() > 0, "应该至少有一条记录");

        // 验证缓存是否已创建
        RMap<String, String> pageMap = redisson.getMap(
                StringConstant.Redis.UNIT_CATEGORY_PAGE + page + ":" + size + ":" + isDesc + ":" + keyword);
        Assertions.assertTrue(pageMap.isExists(), "分页缓存应该已创建");

        // 测试从缓存获取
        Page<UnitCategoryDO> cachedPage = unitCategoryDAO.getPageOfUnitCategory(page, size, isDesc, keyword);
        Assertions.assertEquals(pageResult.getTotal(), cachedPage.getTotal(), "从缓存获取的总记录数应匹配");
    }

    @Test
    void testGetUnitCategoryList() {
        // 清除列表缓存
        redisson.getKeys().delete(StringConstant.Redis.UNIT_CATEGORY_LIST);

        // 测试获取列表
        List<UnitCategoryLiteDTO> categoryList = unitCategoryDAO.getUnitCategoryList();
        Assertions.assertNotNull(categoryList, "单位类别列表不应为空");
        Assertions.assertFalse(categoryList.isEmpty(), "单位类别列表应该包含数据");

        // 验证缓存是否已创建
        RList<UnitCategoryLiteDTO> cachedList = redisson.getList(StringConstant.Redis.UNIT_CATEGORY_LIST);
        Assertions.assertTrue(cachedList.isExists(), "列表缓存应该已创建");

        // 测试从缓存获取
        List<UnitCategoryLiteDTO> cachedCategoryList = unitCategoryDAO.getUnitCategoryList();
        Assertions.assertEquals(categoryList.size(), cachedCategoryList.size(), "从缓存获取的列表大小应匹配");
    }
}
