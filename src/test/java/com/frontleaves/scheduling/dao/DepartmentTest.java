package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.MajorDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
class DepartmentTest {
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private RedissonClient redisson;
    @Autowired
    private TeacherDAO teacherDAO;
    @Autowired
    private CourseLibraryDAO courseLibraryDAO;
    @Autowired
    private MajorDAO majorDAO;

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


    @Test
    void testDeleteDepartmentNotUser() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        // 删除所有教师记录
        teacherDAO.lambdaUpdate()
                .eq(TeacherDO::getUnitUuid, departmentDO.getDepartmentUuid())
                .remove();
        courseLibraryDAO.lambdaUpdate()
                .eq(CourseLibraryDO::getDepartment, departmentDO.getDepartmentUuid())
                .remove();
        majorDAO.lambdaUpdate()
                .eq(MajorDO::getDepartmentUuid, departmentDO.getDepartmentUuid())
                .remove();
        // 查询并获取第一个部门对象，用于后续的删除操作
        // 执行删除部门的操作
        departmentDAO.deleteDepartment(departmentDO);
        // 尝试根据部门的唯一标识符获取被删除的部门信息
        DepartmentDO getDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, departmentDO.getDepartmentUuid())
                .one();
        // 验证删除操作是否成功，即验证数据库中是否真的不再存在该部门
        Assertions.assertNull(getDepartment);
    }

    @Test
    void testDeleteDepartmentHasUser() {
        MajorDO majorDO = majorDAO.lambdaQuery().list().get(0);
        DepartmentDO departmentDO = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, majorDO.getDepartmentUuid())
                .one();
        // 查询并获取第一个部门对象，用于后续的删除操作
        Assertions.assertThrows(BusinessException.class, () -> {
            departmentDAO.deleteDepartment(departmentDO);
        });
    }

    @Test
    void testUpdateDepartment() {
        // 查询并获取第一个部门对象，用于后续的更新操作
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);

        // 修改部门名称
        departmentDO.setDepartmentName("测试");

        // 执行更新部门的操作
        departmentDAO.updateDepartment(departmentDO);

        // 尝试根据部门的唯一标识符获取被更新的部门信息
        DepartmentDO getDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, departmentDO.getDepartmentUuid())
                .one();

        // 验证更新操作是否成功，即验证数据库中是否真的存在该部门，并且名称已被修改
        Assertions.assertNotNull(getDepartment);
        Assertions.assertEquals("测试", getDepartment.getDepartmentName());
    }


}
