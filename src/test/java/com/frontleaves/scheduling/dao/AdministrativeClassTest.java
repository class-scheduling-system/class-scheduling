package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
@Slf4j
class AdministrativeClassTest {

    @Resource
    private AdministrativeClassDAO administrativeClassDAO;

    @Resource
    private RedissonClient redisson;

    /**
     * 测试根据UUID获取行政班级
     * <p>
     * 本测试需要数据库中已存在行政班级数据
     * </p>
     */
    @Test
    void testGetAdministrativeClassByUuid() {
        // 假设数据库中已有一条行政班级数据，这里需要替换为实际存在的UUID
        AdministrativeClassDO administrativeClassDO = administrativeClassDAO.lambdaQuery().list().get(0);
        String existingUuid = administrativeClassDO.getAdministrativeClassUuid();
        log.debug("测试获取行政班级信息(UUID)");
        AdministrativeClassDO classData = administrativeClassDAO.getAdministrativeClassByUuid(existingUuid);
        // 如果行政班级在数据库中存在，应该不为null
        Assertions.assertNotNull(classData);
        log.debug("删除缓存并再次获取");
        // 删除Redis缓存
        redisson.getMap(StringConstant.Redis.ADMINISTRATIVE_CLASS_UUID + existingUuid).delete();
        // 再次获取，这次应该从数据库获取并重新缓存
        AdministrativeClassDO classData2 = administrativeClassDAO.getAdministrativeClassByUuid(existingUuid);
        // 验证仍然能获到数据
        Assertions.assertNotNull(classData2);
        // 清理测试后的缓存
        redisson.getMap(StringConstant.Redis.ADMINISTRATIVE_CLASS_UUID + existingUuid).delete();
    }

    /**
     * 测试获取不存在的行政班级
     */
    @Test
    void testGetNonExistingAdministrativeClass() {
        log.debug("测试获取不存在的行政班级");
        // 使用一个肯定不存在的UUID
        String nonExistingUuid = UuidUtil.generateUuidNoDash();
        // 尝试获取不存在的行政班级
        AdministrativeClassDO classData = administrativeClassDAO.getAdministrativeClassByUuid(nonExistingUuid);
        // 应该返回null
        Assertions.assertNull(classData);
    }
    @Test
    void testGetAdministrativeClassList() {
        // 先清除Redis中的管理班级列表缓存
        redisson.getKeys().delete(StringConstant.Redis.ADMINISTRATIVE_CLASS_LIST);

        // 第一次调用getAdministrativeClassList方法，应该从数据库获取数据并缓存到Redis
        List<AdministrativeClassDO> classList1 = administrativeClassDAO.getAdministrativeClassList();

        // 断言从数据库获取的管理班级列表不为空
        Assertions.assertNotNull(classList1);
        Assertions.assertFalse(classList1.isEmpty());

        // 验证Redis中是否已缓存管理班级列表
        RList<AdministrativeClassDO> redisCache = redisson.getList(StringConstant.Redis.ADMINISTRATIVE_CLASS_LIST);
        Assertions.assertTrue(redisCache.isExists());

        // 记录第一次查询结果的大小
        int firstResultSize = classList1.size();

        // 第二次调用getAdministrativeClassList方法，应该从Redis缓存中获取数据
        List<AdministrativeClassDO> classList2 = administrativeClassDAO.getAdministrativeClassList();

        // 断言第二次获取的结果与第一次结果大小相同
        Assertions.assertEquals(firstResultSize, classList2.size());
    }
    @Test
    void testGetAdministrativeClassListByDepartmentForUpdate (){
        String departmentUuid = administrativeClassDAO.lambdaQuery().list().get(0).getDepartmentUuid();
        redisson.getList(
                StringConstant.Redis.ADMINISTRATIVE_CLASS_LIST_BY_DEPARTMENT + departmentUuid).delete();
        List<AdministrativeClassDO> administrativeClassDOList =
                administrativeClassDAO.getAdministrativeClassListByDepartmentForUpdate(departmentUuid);
        Assertions.assertFalse(administrativeClassDOList.isEmpty());
        RList<AdministrativeClassDO> redisCache = redisson.getList(
                StringConstant.Redis.ADMINISTRATIVE_CLASS_LIST_BY_DEPARTMENT + departmentUuid);
        Assertions.assertTrue(redisCache.isExists());
    }

    @Test
    void testGetAdministrativeClassMappingByClazz() {
        // 1.假设数据库中至少有一条数据
        AdministrativeClassDO dbRecord = administrativeClassDAO.lambdaQuery().list().get(0);
        String clazzUuid = dbRecord.getAdministrativeClassUuid();

        // 构造缓存键，并清除缓存，确保测试从数据库获取数据
        String cacheKey = StringConstant.Redis.ADMINISTRATIVE_CLASS_MAPPING_BY_CALZZ + clazzUuid;
        redisson.getMap(cacheKey).delete();

        // 第一次调用：应从数据库中查询，并缓存数据
        AdministrativeClassDO fetched = administrativeClassDAO.getAdministrativeClassMappingByClazz(clazzUuid);
        Assertions.assertNotNull(fetched, "存在的行政班级首次查询应返回数据");
        Assertions.assertEquals(dbRecord.getAdministrativeClassUuid(), fetched.getAdministrativeClassUuid(),
                "返回的数据应与数据库记录一致");

        // 验证缓存已写入
        RMap<String, String> rMap = redisson.getMap(cacheKey);
        Assertions.assertTrue(rMap.isExists(), "缓存应已存在");

        // 第二次调用：应从缓存中获取数据
        AdministrativeClassDO cachedFetched = administrativeClassDAO.getAdministrativeClassMappingByClazz(clazzUuid);
        Assertions.assertNotNull(cachedFetched, "从缓存中查询应返回数据");
        Assertions.assertEquals(dbRecord.getAdministrativeClassUuid(), cachedFetched.getAdministrativeClassUuid(),
                "缓存中的数据应与数据库记录一致");

        // 清理缓存
        redisson.getMap(cacheKey).delete();

        // 2.不存在行政班级
        String nonExistingClazz = UuidUtil.generateUuidNoDash();
        String nonExistCacheKey = StringConstant.Redis.ADMINISTRATIVE_CLASS_MAPPING_BY_CALZZ + nonExistingClazz;
        redisson.getMap(nonExistCacheKey).delete();

        // 查询不存在的数据，应返回 null
        AdministrativeClassDO result = administrativeClassDAO.getAdministrativeClassMappingByClazz(nonExistingClazz);
        Assertions.assertNull(result, "不存在的行政班级应返回 null");

        // 确认缓存没有被创建
        RMap<String, String> nonExistMap = redisson.getMap(nonExistCacheKey);
        Assertions.assertFalse(nonExistMap.isExists(), "不存在的数据不应在缓存中");
    }
}
