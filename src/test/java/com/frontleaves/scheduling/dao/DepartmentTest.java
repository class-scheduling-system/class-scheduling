package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class DepartmentTest {
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private RedissonClient redisson;

    // 测试在没有Redis缓存的情况下，通过UUID获取部门信息
    @Test
    void testGetDepartmentByUuidNoRedis() {
        // 从数据库中查询第一个部门记录
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        // 删除Redis中与该部门UUID相关的缓存
        redisson.getKeys().delete(StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid());
        // 通过UUID从数据库中获取部门信息
        DepartmentDO departmentDO1 = departmentDAO.getDepartmentByUuid(departmentDO.getDepartmentUuid());
        // 断言获取到的部门信息不为空
        Assertions.assertNotNull(departmentDO1);
        // 获取Redis中与该部门UUID相关的缓存映射
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid());
        // 断言缓存映射存在
        Assertions.assertTrue(map.isExists());
    }
}
