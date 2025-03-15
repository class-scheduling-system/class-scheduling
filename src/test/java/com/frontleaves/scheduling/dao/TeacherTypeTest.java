package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TeacherTypeDAO;
import com.frontleaves.scheduling.models.entity.TeacherTypeDO;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * TeacherTypeDAO 单元测试类
 */
@SpringBootTest
@Slf4j
class TeacherTypeTest {
    @Resource
    private TeacherTypeDAO teacherTypeDAO;
    @Resource
    private RedissonClient redisson;
    private TeacherTypeDO setUpTeacherType;

    @BeforeEach
    void setUp() {
        log.debug("TeacherTypeDAO单元测试初始化");
        
        // 创建测试教师类型
        setUpTeacherType = new TeacherTypeDO();
        setUpTeacherType.setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName("DAOTestTeacherType")
                .setTypeEnglishName("DAOTestTeacherTypeEnglish")
                .setTypeDesc("这是一个用于DAO测试的教师类型");

        // 清除可能存在的重复教师类型
        TeacherTypeDO existingType = teacherTypeDAO.lambdaQuery()
                .eq(TeacherTypeDO::getTypeName, setUpTeacherType.getTypeName())
                .one();
        if (existingType != null) {
            teacherTypeDAO.removeById(existingType.getTeacherTypeUuid());
        }

        // 保存教师类型
        teacherTypeDAO.save(setUpTeacherType);
    }

    @AfterEach
    @Transactional
    void tearDown() {
        log.debug("TeacherTypeDAO单元测试结束，清理测试数据");

        // 删除测试教师类型
        teacherTypeDAO.removeById(setUpTeacherType.getTeacherTypeUuid());

        // 清理Redis缓存
        redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + setUpTeacherType.getTeacherTypeUuid()).delete();
        redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST).delete();
        redisson.getKeys().deleteByPattern(StringConstant.Redis.TEACHER_TYPE_LIST + ":page:*");
    }

    /**
     * 测试根据UUID获取教师类型
     */
    @Test
    void testGetTeacherTypeByUuid() {
        log.debug("测试通过UUID获取教师类型");

        // 调用方法获取教师类型
        TeacherTypeDO teacherType = teacherTypeDAO.getTeacherTypeByUuid(setUpTeacherType.getTeacherTypeUuid());

        // 验证返回结果
        Assertions.assertNotNull(teacherType, "教师类型不应为空");
        Assertions.assertEquals(setUpTeacherType.getTeacherTypeUuid(), teacherType.getTeacherTypeUuid(), "教师类型UUID应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeName(), teacherType.getTypeName(), "教师类型名称应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeEnglishName(), teacherType.getTypeEnglishName(), "教师类型英文名称应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeDesc(), teacherType.getTypeDesc(), "教师类型描述应该匹配");

        // 验证Redis缓存是否创建
        RMap<String, String> typeMap = redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + setUpTeacherType.getTeacherTypeUuid());
        Assertions.assertTrue(typeMap.isExists(), "Redis缓存应该存在");
    }

    /**
     * 测试从缓存获取教师类型
     */
    @Test
    void testGetTeacherTypeByUuidFromCache() {
        log.debug("测试从缓存获取教师类型");

        // 创建Redis缓存
        RMap<String, String> typeMap = redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + setUpTeacherType.getTeacherTypeUuid());
        typeMap.putAll(com.xlf.utility.util.ConvertUtil.convertObjectToMapString(setUpTeacherType));
        typeMap.expire(Duration.ofSeconds(86400));

        // 调用方法获取教师类型
        TeacherTypeDO teacherType = teacherTypeDAO.getTeacherTypeByUuid(setUpTeacherType.getTeacherTypeUuid());

        // 验证返回结果
        Assertions.assertNotNull(teacherType, "教师类型不应为空");
        Assertions.assertEquals(setUpTeacherType.getTeacherTypeUuid(), teacherType.getTeacherTypeUuid(), "教师类型UUID应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeName(), teacherType.getTypeName(), "教师类型名称应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeEnglishName(), teacherType.getTypeEnglishName(), "教师类型英文名称应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeDesc(), teacherType.getTypeDesc(), "教师类型描述应该匹配");
    }

    /**
     * 测试根据名称获取教师类型
     */
    @Test
    void testGetTeacherTypeByName() {
        log.debug("测试通过名称获取教师类型");

        // 调用方法获取教师类型
        TeacherTypeDO teacherType = teacherTypeDAO.getTeacherTypeByName(setUpTeacherType.getTypeName());

        // 验证返回结果
        Assertions.assertNotNull(teacherType, "教师类型不应为空");
        Assertions.assertEquals(setUpTeacherType.getTeacherTypeUuid(), teacherType.getTeacherTypeUuid(), "教师类型UUID应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeName(), teacherType.getTypeName(), "教师类型名称应该匹配");
    }

    /**
     * 测试根据英文名称获取教师类型
     */
    @Test
    void testGetTeacherTypeByEnglishName() {
        log.debug("测试通过英文名称获取教师类型");

        // 调用方法获取教师类型
        TeacherTypeDO teacherType = teacherTypeDAO.getTeacherTypeByEnglishName(setUpTeacherType.getTypeEnglishName());

        // 验证返回结果
        Assertions.assertNotNull(teacherType, "教师类型不应为空");
        Assertions.assertEquals(setUpTeacherType.getTeacherTypeUuid(), teacherType.getTeacherTypeUuid(), "教师类型UUID应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeEnglishName(), teacherType.getTypeEnglishName(), "教师类型英文名称应该匹配");
    }

    /**
     * 测试获取所有教师类型
     */
    @Test
    void testGetAllTeacherTypes() {
        log.debug("测试获取所有教师类型");

        // 调用方法获取所有教师类型
        List<TeacherTypeDO> teacherTypes = teacherTypeDAO.getAllTeacherTypes();

        // 验证返回结果
        Assertions.assertNotNull(teacherTypes, "教师类型列表不应为空");
        Assertions.assertFalse(teacherTypes.isEmpty(), "教师类型列表不应为空");
        
        // 验证列表中包含测试教师类型
        boolean contains = teacherTypes.stream()
                .anyMatch(type -> type.getTeacherTypeUuid().equals(setUpTeacherType.getTeacherTypeUuid()));
        Assertions.assertTrue(contains, "教师类型列表应包含测试教师类型");
    }

    /**
     * 测试缓存教师类型列表
     */
    @Test
    void testGetAllTeacherTypesFromCache() {
        log.debug("测试从缓存获取所有教师类型");

        // 先获取一次，建立缓存
        teacherTypeDAO.getAllTeacherTypes();
        
        // 添加新的教师类型，但不清除缓存
        TeacherTypeDO newType = new TeacherTypeDO();
        newType.setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName("NewCacheTestType")
                .setTypeEnglishName("NewCacheTestTypeEnglish")
                .setTypeDesc("这是一个测试缓存的新教师类型");
        teacherTypeDAO.save(newType);

        // 再次获取，应该返回缓存数据
        List<TeacherTypeDO> cachedTypes = teacherTypeDAO.getAllTeacherTypes();
        
        // 查看是否不包含新添加的类型
        boolean containsNew = cachedTypes.stream()
                .anyMatch(type -> type.getTeacherTypeUuid().equals(newType.getTeacherTypeUuid()));
                
        // 注意：这个测试可能会因缓存实现的差异而失败
        // 如果实现总是从数据库获取最新数据，那么期望值应改为true
        Assertions.assertFalse(containsNew, "缓存的教师类型列表不应包含新添加的类型");
        
        // 清理测试数据
        teacherTypeDAO.removeById(newType.getTeacherTypeUuid());
    }

    /**
     * 测试分页获取教师类型
     */
    @Test
    void testGetTeacherTypePage() {
        log.debug("测试分页获取教师类型");

        // 调用方法获取教师类型分页数据
        Page<TeacherTypeDO> page = teacherTypeDAO.getTeacherTypePage(1, 10, true, null);

        // 验证返回结果
        Assertions.assertNotNull(page, "分页对象不应为空");
        Assertions.assertNotNull(page.getRecords(), "分页记录不应为空");
        Assertions.assertFalse(page.getRecords().isEmpty(), "分页记录不应为空");
        
        // 验证分页参数
        Assertions.assertEquals(1, page.getCurrent(), "当前页应该是1");
        Assertions.assertEquals(10, page.getSize(), "每页大小应该是10");
    }

    /**
     * 测试根据名称模糊查询教师类型
     */
    @Test
    void testGetTeacherTypeByNameLike() {
        log.debug("测试根据名称模糊查询教师类型");

        // 使用部分名称模糊查询
        String partialName = setUpTeacherType.getTypeName().substring(0, 5);
        List<TeacherTypeDO> types = teacherTypeDAO.getTeacherTypeByNameLike(partialName);

        // 验证返回结果
        Assertions.assertNotNull(types, "教师类型列表不应为空");
        Assertions.assertFalse(types.isEmpty(), "教师类型列表不应为空");
        
        // 验证列表中包含测试教师类型
        boolean contains = types.stream()
                .anyMatch(type -> type.getTeacherTypeUuid().equals(setUpTeacherType.getTeacherTypeUuid()));
        Assertions.assertTrue(contains, "查询结果应包含测试教师类型");
    }

    /**
     * 测试添加教师类型及缓存清理
     */
    @Test
    void testAddTeacherType() {
        log.debug("测试添加教师类型");

        // 创建新的教师类型
        TeacherTypeDO newType = new TeacherTypeDO();
        newType.setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName("AddTestType")
                .setTypeEnglishName("AddTestTypeEnglish")
                .setTypeDesc("这是用于测试添加的教师类型");

        // 先获取一次列表，建立缓存
        teacherTypeDAO.getAllTeacherTypes();
        
        // 添加教师类型
        TeacherTypeDO addedType = teacherTypeDAO.addTeacherType(newType);

        // 验证返回结果
        Assertions.assertNotNull(addedType, "添加的教师类型不应为空");
        Assertions.assertEquals(newType.getTeacherTypeUuid(), addedType.getTeacherTypeUuid(), "教师类型UUID应该匹配");
        Assertions.assertEquals(newType.getTypeName(), addedType.getTypeName(), "教师类型名称应该匹配");
        
        // 验证缓存是否被清除
        Assertions.assertFalse(redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST).isExists(), 
                "教师类型列表缓存应该被清除");
        
        // 从数据库查询验证
        TeacherTypeDO dbType = teacherTypeDAO.getById(newType.getTeacherTypeUuid());
        Assertions.assertNotNull(dbType, "数据库中应存在新添加的教师类型");
        
        // 清理测试数据
        teacherTypeDAO.removeById(newType.getTeacherTypeUuid());
    }

    /**
     * 测试更新教师类型及缓存清理
     */
    @Test
    void testUpdateTeacherType() {
        log.debug("测试更新教师类型");

        // 修改测试教师类型
        TeacherTypeDO updateType = new TeacherTypeDO();
        updateType.setTeacherTypeUuid(setUpTeacherType.getTeacherTypeUuid())
                .setTypeName("UpdatedTestType")
                .setTypeEnglishName("UpdatedTestTypeEnglish")
                .setTypeDesc("这是更新后的教师类型描述");

        // 先获取一次，建立缓存
        teacherTypeDAO.getTeacherTypeByUuid(setUpTeacherType.getTeacherTypeUuid());
        teacherTypeDAO.getAllTeacherTypes();
        
        // 更新教师类型
        boolean updated = teacherTypeDAO.updateTeacherType(updateType);

        // 验证更新结果
        Assertions.assertTrue(updated, "更新应该成功");
        
        // 验证缓存是否被清除
        Assertions.assertFalse(redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + 
                setUpTeacherType.getTeacherTypeUuid()).isExists(), "教师类型缓存应该被清除");
        Assertions.assertFalse(redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST).isExists(), 
                "教师类型列表缓存应该被清除");
        
        // 从数据库查询验证
        TeacherTypeDO dbType = teacherTypeDAO.getById(setUpTeacherType.getTeacherTypeUuid());
        Assertions.assertNotNull(dbType, "数据库中应存在更新后的教师类型");
        Assertions.assertEquals(updateType.getTypeName(), dbType.getTypeName(), "教师类型名称应已更新");
        Assertions.assertEquals(updateType.getTypeEnglishName(), dbType.getTypeEnglishName(), "教师类型英文名称应已更新");
        Assertions.assertEquals(updateType.getTypeDesc(), dbType.getTypeDesc(), "教师类型描述应已更新");
    }

    /**
     * 测试删除教师类型及缓存清理
     */
    @Test
    void testDeleteTeacherType() {
        log.debug("测试删除教师类型");

        // 创建要删除的教师类型
        TeacherTypeDO typeToDelete = new TeacherTypeDO();
        typeToDelete.setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName("DeleteTestType")
                .setTypeEnglishName("DeleteTestTypeEnglish")
                .setTypeDesc("这是要删除的教师类型");
        teacherTypeDAO.save(typeToDelete);

        // 先获取一次，建立缓存
        teacherTypeDAO.getTeacherTypeByUuid(typeToDelete.getTeacherTypeUuid());
        teacherTypeDAO.getAllTeacherTypes();
        
        // 删除教师类型
        boolean deleted = teacherTypeDAO.deleteTeacherType(typeToDelete.getTeacherTypeUuid());

        // 验证删除结果
        Assertions.assertTrue(deleted, "删除应该成功");
        
        // 验证缓存是否被清除
        Assertions.assertFalse(redisson.getMap(StringConstant.Redis.TEACHER_TYPE_UUID + 
                typeToDelete.getTeacherTypeUuid()).isExists(), "教师类型缓存应该被清除");
        Assertions.assertFalse(redisson.getList(StringConstant.Redis.TEACHER_TYPE_LIST).isExists(), 
                "教师类型列表缓存应该被清除");
        
        // 从数据库查询验证
        TeacherTypeDO dbType = teacherTypeDAO.getById(typeToDelete.getTeacherTypeUuid());
        Assertions.assertNull(dbType, "数据库中不应存在已删除的教师类型");
    }
}
