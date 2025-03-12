package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

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
}
