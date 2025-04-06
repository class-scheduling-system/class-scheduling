package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TeacherTypeDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherTypeDTO;
import com.frontleaves.scheduling.models.entity.base.TeacherTypeDO;
import com.frontleaves.scheduling.services.TeacherTypeService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
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
 * TeacherTypeLogic 单元测试类
 */
@SpringBootTest
@Slf4j
class TeacherTypeEnumTest {
    @Resource
    private TeacherTypeService teacherTypeService;
    @Resource
    private TeacherTypeDAO teacherTypeDAO;
    @Resource
    private RedissonClient redisson;
    private TeacherTypeDO setUpTeacherType;

    @BeforeEach
    void setUp() {
        log.debug("TeacherTypeLogic单元测试初始化");

        // 创建测试教师类型
        setUpTeacherType = new TeacherTypeDO();
        setUpTeacherType.setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName("TestTeacherType")
                .setTypeEnglishName("TestTeacherTypeEnglish")
                .setTypeDesc("这是一个测试教师类型");

        // 清除可能存在的重复教师类型
        TeacherTypeDO existingType = teacherTypeDAO.getTeacherTypeByName("TestTeacherType");
        if (existingType != null) {
            teacherTypeDAO.removeById(existingType.getTeacherTypeUuid());
        }

        // 保存教师类型
        teacherTypeDAO.save(setUpTeacherType);

        // 将教师类型信息放入Redis缓存
        RMap<String, String> typeMap = redisson.getMap(
                StringConstant.Redis.TEACHER_TYPE_UUID + setUpTeacherType.getTeacherTypeUuid());
        typeMap.putAll(com.xlf.utility.util.ConvertUtil.convertObjectToMapString(setUpTeacherType));
        typeMap.expire(Duration.ofSeconds(86400));
    }

    @AfterEach
    @Transactional
    void tearDown() {
        log.debug("TeacherTypeLogic单元测试结束，清理测试数据");

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
    void testGetTeacherType() {
        log.debug("测试获取教师类型");

        // 调用获取教师类型方法
        TeacherTypeDTO teacherTypeDTO = teacherTypeService.getTeacherType(setUpTeacherType.getTeacherTypeUuid());

        // 验证返回结果
        Assertions.assertNotNull(teacherTypeDTO, "教师类型DTO不应为空");
        Assertions.assertEquals(setUpTeacherType.getTeacherTypeUuid(), teacherTypeDTO.getTeacherTypeUuid(), "教师类型UUID应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeName(), teacherTypeDTO.getTypeName(), "教师类型名称应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeEnglishName(), teacherTypeDTO.getTypeEnglishName(), "教师类型英文名称应该匹配");
        Assertions.assertEquals(setUpTeacherType.getTypeDesc(), teacherTypeDTO.getTypeDesc(), "教师类型描述应该匹配");
    }

    /**
     * 测试获取不存在的教师类型
     */
    @Test
    void testGetNonExistentTeacherType() {
        log.debug("测试获取不存在的教师类型");

        // 生成一个不存在的教师类型UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 验证调用获取教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.getTeacherType(nonExistentUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals("教师类型不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试使用无效格式的UUID获取教师类型
     */
    @Test
    void testGetTeacherTypeWithInvalidUuidFormat() {
        log.debug("测试使用无效格式的UUID获取教师类型");

        // 使用带连字符的UUID，这应该不符合无连字符UUID正则表达式
        String invalidUuid = java.util.UUID.randomUUID().toString(); // 带连字符的UUID

        // 验证调用获取教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.getTeacherType(invalidUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals("教师类型UUID格式不正确", exception.getMessage());
        Assertions.assertEquals(ErrorCode.PARAMETER_ERROR, exception.getErrorCode());
    }

    /**
     * 测试获取教师类型分页列表
     */
    @Test
    void testGetTeacherTypePage() {
        log.debug("测试获取教师类型分页列表");

        // 调用获取教师类型分页列表方法
        PageDTO<TeacherTypeDTO> pageDTO = teacherTypeService.getTeacherTypePage(1, 10, true, null);

        // 验证返回结果
        Assertions.assertNotNull(pageDTO, "分页DTO不应为空");
        Assertions.assertNotNull(pageDTO.getRecords(), "记录列表不应为null");

        // 验证分页参数
        Assertions.assertEquals(1, pageDTO.getCurrent(), "当前页应该是1");
        Assertions.assertEquals(10, pageDTO.getSize(), "每页大小应该是10");
    }

    /**
     * 测试获取教师类型分页列表，按名称搜索
     */
    @Test
    void testGetTeacherTypePageSearchByName() {
        log.debug("测试获取教师类型分页列表，按名称搜索");

        // 使用测试教师类型的名称进行搜索
        PageDTO<TeacherTypeDTO> pageDTO = teacherTypeService.getTeacherTypePage(
                1, 10, true, setUpTeacherType.getTypeName());

        // 验证返回结果
        Assertions.assertNotNull(pageDTO, "分页DTO不应为空");
        Assertions.assertNotNull(pageDTO.getRecords(), "记录列表不应为null");

        // 验证搜索结果包含测试教师类型
        boolean foundTestType = false;
        for (TeacherTypeDTO typeDTO : pageDTO.getRecords()) {
            if (typeDTO.getTeacherTypeUuid().equals(setUpTeacherType.getTeacherTypeUuid())) {
                foundTestType = true;
                break;
            }
        }
        Assertions.assertTrue(foundTestType, "搜索结果应该包含测试教师类型");
    }

    /**
     * 测试获取所有教师类型列表
     */
    @Test
    void testGetTeacherTypeList() {
        log.debug("测试获取所有教师类型列表");

        // 调用获取教师类型列表方法
        List<TeacherTypeDTO> typeList = teacherTypeService.getTeacherTypeList();

        // 验证返回结果
        Assertions.assertNotNull(typeList, "类型列表不应为空");

        // 验证列表包含测试教师类型
        boolean foundTestType = false;
        for (TeacherTypeDTO typeDTO : typeList) {
            if (typeDTO.getTeacherTypeUuid().equals(setUpTeacherType.getTeacherTypeUuid())) {
                foundTestType = true;
                break;
            }
        }
        Assertions.assertTrue(foundTestType, "列表应该包含测试教师类型");
    }

    /**
     * 测试添加教师类型
     */
    @Test
    void testAddTeacherType() {
        log.debug("测试添加教师类型");

        // 创建新的教师类型信息
        String typeName = "NewTestTeacherType";
        String typeEnglishName = "NewTestTeacherTypeEnglish";
        String typeDesc = "这是一个新添加的测试教师类型";

        // 清除可能存在的重复教师类型
        TeacherTypeDO existingType = teacherTypeDAO.getTeacherTypeByName(typeName);
        if (existingType != null) {
            teacherTypeDAO.removeById(existingType.getTeacherTypeUuid());
        }

        // 调用添加教师类型方法
        TeacherTypeDTO addedType = teacherTypeService.addTeacherType(typeName, typeEnglishName, typeDesc);

        // 验证返回结果
        Assertions.assertNotNull(addedType, "添加的教师类型DTO不应为空");
        Assertions.assertEquals(typeName, addedType.getTypeName(), "教师类型名称应该匹配");
        Assertions.assertEquals(typeEnglishName, addedType.getTypeEnglishName(), "教师类型英文名称应该匹配");
        Assertions.assertEquals(typeDesc, addedType.getTypeDesc(), "教师类型描述应该匹配");

        // 验证UUID是否被正确生成
        Assertions.assertNotNull(addedType.getTeacherTypeUuid(), "教师类型UUID不应为空");
        Assertions.assertTrue(addedType.getTeacherTypeUuid().matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION),
                "教师类型UUID应符合无连字符UUID格式");

        // 验证教师类型是否已添加到数据库
        TeacherTypeDO savedType = teacherTypeDAO.getTeacherTypeByUuid(addedType.getTeacherTypeUuid());
        Assertions.assertNotNull(savedType, "教师类型应该已添加到数据库");

        // 清理测试数据
        teacherTypeDAO.removeById(addedType.getTeacherTypeUuid());
    }

    /**
     * 测试添加教师类型时名称为空
     */
    @Test
    void testAddTeacherTypeWithEmptyName() {
        log.debug("测试添加教师类型时名称为空");

        // 验证调用添加教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.addTeacherType("", "EnglishName", "Description"));

        // 验证异常信息和错误码
        Assertions.assertEquals("类型名称不能为空", exception.getMessage());
        Assertions.assertEquals(ErrorCode.PARAMETER_ERROR, exception.getErrorCode());
    }

    /**
     * 测试添加教师类型时英文名称为空
     */
    @Test
    void testAddTeacherTypeWithEmptyEnglishName() {
        log.debug("测试添加教师类型时英文名称为空");

        // 验证调用添加教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.addTeacherType("TypeName", "", "Description"));

        // 验证异常信息和错误码
        Assertions.assertEquals("类型英文名称不能为空", exception.getMessage());
        Assertions.assertEquals(ErrorCode.PARAMETER_ERROR, exception.getErrorCode());
    }

    /**
     * 测试添加教师类型时名称已存在
     */
    @Test
    void testAddTeacherTypeWithDuplicateName() {
        log.debug("测试添加教师类型时名称已存在");

        // 验证调用添加教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.addTeacherType(
                        setUpTeacherType.getTypeName(), // 使用已存在的名称
                        "NewEnglishName",
                        "New Description"
                ));

        // 验证异常信息和错误码
        Assertions.assertEquals("类型名称已存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.EXISTED, exception.getErrorCode());
    }

    /**
     * 测试添加教师类型时英文名称已存在
     */
    @Test
    void testAddTeacherTypeWithDuplicateEnglishName() {
        log.debug("测试添加教师类型时英文名称已存在");

        // 验证调用添加教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.addTeacherType(
                        "NewTypeName",
                        setUpTeacherType.getTypeEnglishName(), // 使用已存在的英文名称
                        "New Description"
                ));

        // 验证异常信息和错误码
        Assertions.assertEquals("类型英文名称已存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.EXISTED, exception.getErrorCode());
    }

    /**
     * 测试更新教师类型
     */
    @Test
    void testUpdateTeacherType() {
        log.debug("测试更新教师类型");

        // 更新信息
        String updatedName = "UpdatedTestType";
        String updatedEnglishName = "UpdatedTestTypeEnglish";
        String updatedDesc = "这是更新后的测试教师类型描述";

        // 调用更新教师类型方法
        TeacherTypeDTO updatedType = teacherTypeService.updateTeacherType(
                setUpTeacherType.getTeacherTypeUuid(),
                updatedName,
                updatedEnglishName,
                updatedDesc
        );

        // 验证返回结果
        Assertions.assertNotNull(updatedType, "更新后的教师类型DTO不应为空");
        Assertions.assertEquals(setUpTeacherType.getTeacherTypeUuid(), updatedType.getTeacherTypeUuid(), "教师类型UUID应该匹配");
        Assertions.assertEquals(updatedName, updatedType.getTypeName(), "教师类型名称应该已更新");
        Assertions.assertEquals(updatedEnglishName, updatedType.getTypeEnglishName(), "教师类型英文名称应该已更新");
        Assertions.assertEquals(updatedDesc, updatedType.getTypeDesc(), "教师类型描述应该已更新");

        // 验证数据库中的数据是否已更新
        TeacherTypeDO dbType = teacherTypeDAO.getTeacherTypeByUuid(setUpTeacherType.getTeacherTypeUuid());
        Assertions.assertEquals(updatedName, dbType.getTypeName(), "数据库中的名称应该已更新");
        Assertions.assertEquals(updatedEnglishName, dbType.getTypeEnglishName(), "数据库中的英文名称应该已更新");
        Assertions.assertEquals(updatedDesc, dbType.getTypeDesc(), "数据库中的描述应该已更新");
    }

    /**
     * 测试更新不存在的教师类型
     */
    @Test
    void testUpdateNonExistentTeacherType() {
        log.debug("测试更新不存在的教师类型");

        // 生成一个不存在的教师类型UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 验证调用更新教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.updateTeacherType(
                        nonExistentUuid,
                        "UpdatedName",
                        "UpdatedEnglishName",
                        "Updated Description"
                ));

        // 验证异常信息和错误码
        Assertions.assertEquals("教师类型不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试更新教师类型时使用已存在的名称
     */
    @Test
    void testUpdateTeacherTypeWithDuplicateName() {
        log.debug("测试更新教师类型时使用已存在的名称");

        // 创建另一个教师类型用于测试
        TeacherTypeDO anotherType = new TeacherTypeDO();
        anotherType.setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName("AnotherType")
                .setTypeEnglishName("AnotherTypeEnglish")
                .setTypeDesc("这是另一个教师类型");

        // 保存到数据库
        teacherTypeDAO.save(anotherType);

        // 验证调用更新教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.updateTeacherType(
                        setUpTeacherType.getTeacherTypeUuid(),
                        anotherType.getTypeName(), // 使用已存在的名称
                        "UpdatedEnglishName",
                        "Updated Description"
                ));

        // 验证异常信息和错误码
        Assertions.assertEquals("类型名称已存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.EXISTED, exception.getErrorCode());

        // 清理测试数据
        teacherTypeDAO.removeById(anotherType.getTeacherTypeUuid());
    }

    /**
     * 测试删除教师类型
     */
    @Test
    void testDeleteTeacherType() {
        log.debug("测试删除教师类型");

        // 创建要删除的教师类型
        TeacherTypeDO typeToDelete = new TeacherTypeDO();
        typeToDelete.setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName("TypeToDelete")
                .setTypeEnglishName("TypeToDeleteEnglish")
                .setTypeDesc("这是要删除的教师类型");

        // 保存到数据库
        teacherTypeDAO.save(typeToDelete);

        // 调用删除教师类型方法
        boolean deleted = teacherTypeService.deleteTeacherType(typeToDelete.getTeacherTypeUuid());

        // 验证返回结果
        Assertions.assertTrue(deleted, "删除操作应该成功");

        // 验证教师类型是否已从数据库中删除
        TeacherTypeDO deletedType = teacherTypeDAO.getTeacherTypeByUuid(typeToDelete.getTeacherTypeUuid());
        Assertions.assertNull(deletedType, "教师类型应该已从数据库中删除");
    }

    /**
     * 测试删除不存在的教师类型
     */
    @Test
    void testDeleteNonExistentTeacherType() {
        log.debug("测试删除不存在的教师类型");

        // 生成一个不存在的教师类型UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 验证调用删除教师类型方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherTypeService.deleteTeacherType(nonExistentUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals("删除教师类型失败，可能不存在或被引用", exception.getMessage());
        Assertions.assertEquals(ErrorCode.OPERATION_FAILED, exception.getErrorCode());
    }
}
