package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().eq(DepartmentDO::getDepartmentOrder,1).one();
        DepartmentDO departmentDO1 = departmentDAO.getDepartmentByUuid(departmentDO.getDepartmentUuid());
        Assertions.assertNotNull(departmentDO1);
        Map<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid());
        Assertions.assertNotNull(map);
    }
}
