package com.frontleaves.scheduling.dao;

import cn.hutool.core.bean.BeanUtil;
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

import java.util.Map;

@SpringBootTest
@Slf4j
class DepartmentTest {
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private RedissonClient redisson;

    @Test
     void testGetDepartmentByUuidNoRedis() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        redisson.getKeys().delete(StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid());
        DepartmentDO departmentDO1 = departmentDAO.getDepartmentByUuid(departmentDO.getDepartmentUuid());
        Assertions.assertNotNull(departmentDO1);
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid());
        Assertions.assertTrue(map.isExists());
    }
}
