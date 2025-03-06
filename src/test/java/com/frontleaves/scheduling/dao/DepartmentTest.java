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

import java.util.Date;

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
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        DepartmentDO newdepartmentDO = new DepartmentDO();
        newdepartmentDO.setDepartmentUuid(departmentDO.getDepartmentUuid())
                       .setDepartmentCode("1111")
                       .setDepartmentName("测试部门")
                       .setDepartmentOrder(99)
                       .setDepartmentEnglishName("Test Department")
                       .setDepartmentShortName("TD")
                       .setDepartmentAddress("测试地址")
                       .setIsEntity(true)
                       .setAdministrativeHead("测试负责人")
                       .setPartyCommitteeHead("测试党支部书记")
                       .setEstablishmentDate(new Date(System.currentTimeMillis()))
                       .setExpirationDate(new Date(System.currentTimeMillis() + 31536000000L))
                       .setIsTeachingCollege(true)
                       .setIsEnabled(true);
        departmentDAO.updateDepartment(newdepartmentDO);
        DepartmentDO getDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, newdepartmentDO.getDepartmentUuid())
                .one();
        Assertions.assertEquals(getDepartment.getDepartmentCode(), newdepartmentDO.getDepartmentCode());
        Assertions.assertEquals(getDepartment.getDepartmentName(), newdepartmentDO.getDepartmentName());
        Assertions.assertEquals(getDepartment.getDepartmentOrder(), newdepartmentDO.getDepartmentOrder());
        Assertions.assertEquals(getDepartment.getDepartmentEnglishName(), newdepartmentDO.getDepartmentEnglishName());
        Assertions.assertEquals(getDepartment.getDepartmentShortName(), newdepartmentDO.getDepartmentShortName());
        Assertions.assertEquals(getDepartment.getDepartmentAddress(), newdepartmentDO.getDepartmentAddress());
        Assertions.assertEquals(getDepartment.getIsEntity(), newdepartmentDO.getIsEntity());
        Assertions.assertEquals(getDepartment.getAdministrativeHead(), newdepartmentDO.getAdministrativeHead());
        Assertions.assertEquals(getDepartment.getPartyCommitteeHead(), newdepartmentDO.getPartyCommitteeHead());
        // 不涉及小时分钟秒，到日期截止
        Assertions.assertTrue(getDepartment.getEstablishmentDate().before(newdepartmentDO.getEstablishmentDate()));
        Assertions.assertTrue(getDepartment.getExpirationDate().before(newdepartmentDO.getExpirationDate()));
        Assertions.assertEquals(getDepartment.getIsTeachingCollege(), newdepartmentDO.getIsTeachingCollege());
        Assertions.assertEquals(getDepartment.getIsEnabled(), newdepartmentDO.getIsEnabled());
        // 验证Redis缓存是否被清除
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + newdepartmentDO.getDepartmentUuid());
        Assertions.assertFalse(map.isExists());
    }

}
