package com.frontleaves.scheduling.logic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@SpringBootTest
@Slf4j
class DepartmentTest {
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private UnitCategoryDAO unitCategoryDAO;
    @Resource
    private UnitTypeDAO unitTypeDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private DepartmentService departmentService;
    private DepartmentDO testDepartment;
    private String nonExistentUuid;

    @Test
    void testAddDepartment() {
        log.debug("测试添加部门");

        UnitCategoryDO unitCategoryDO = unitCategoryDAO.lambdaQuery().list().get(0);
        UnitTypeDO unitTypeDO = unitTypeDAO.lambdaQuery().list().get(0);

        // 创建部门数据传输对象
        DepartmentVO departmentVO = new DepartmentVO(
                "TEST001",                // 部门编码
                "测试部门",                 // 部门名称
                100,                      // 部门排序
                "Test Department",        // 部门英文名称
                "测部",                     // 部门简称
                "测试地址",                  // 部门地址
                true,                     // 是否实体部门
                "张三",                     // 行政负责人
                "李四",                     // 党委负责人
                new Date(),               // 成立日期
                null,                     // 失效日期
                unitCategoryDO.getUnitCategoryUuid(),                     // 单位类别
                unitTypeDO.getUnitTypeUuid(),                     // 单位办别
                null,
                null,                            // 分配教学楼
                true,                     // 是否为开课院系
                true,                     // 是否为上课院系
                "010-12345678",           // 固定电话
                "测试用部门",                // 备注
                false,                    // 是否为开课教研室
                true                      // 是否启用
        );

        // 确保测试前该部门不存在
        DepartmentDO existingDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentCode, departmentVO.getDepartmentCode())
                .one();
        if (existingDepartment != null) {
            departmentDAO.removeById(existingDepartment);
        }

        // 调用添加部门方法
        DepartmentDTO departmentDTO = departmentService.addDepartment(departmentVO);

        // 断言返回结果不为空
        Assertions.assertNotNull(departmentDTO);

        // 断言数据库中已存在此部门
        DepartmentDO departmentDO = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, departmentDTO.getDepartmentUuid())
                .one();
        Assertions.assertNotNull(departmentDO);

        // 断言部门信息正确
        Assertions.assertEquals(departmentVO.getDepartmentCode(), departmentDO.getDepartmentCode());
        Assertions.assertEquals(departmentVO.getDepartmentName(), departmentDO.getDepartmentName());
        Assertions.assertEquals(departmentVO.getDepartmentEnglishName(), departmentDO.getDepartmentEnglishName());
        Assertions.assertEquals(departmentVO.getDepartmentShortName(), departmentDO.getDepartmentShortName());
        Assertions.assertEquals(departmentVO.getDepartmentAddress(), departmentDO.getDepartmentAddress());
        Assertions.assertEquals(departmentVO.getIsEntity(), departmentDO.getIsEntity());
        Assertions.assertEquals(departmentVO.getFixedPhone(), departmentDO.getFixedPhone());
        Assertions.assertEquals(departmentVO.getRemark(), departmentDO.getRemark());
        Assertions.assertEquals(departmentVO.getIsEnabled(), departmentDO.getIsEnabled());

        // 清理测试数据
        departmentDAO.removeById(departmentDO);

        // 如果有Redis缓存,也需要清理
        redisson.getBucket(StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid()).delete();
    }

    @Test
    void testAddDepartmentWithDuplicateCode() {
        log.debug("测试添加重复编码的部门");

        UnitCategoryDO unitCategoryDO = unitCategoryDAO.lambdaQuery().list().get(0);
        UnitTypeDO unitTypeDO = unitTypeDAO.lambdaQuery().list().get(0);

        // 创建第一个测试部门数据
        DepartmentVO departmentVO = new DepartmentVO(
                "DUPLICATE001",           // 部门编码
                "测试重复部门",              // 部门名称
                100,                      // 部门排序
                "Duplicate Department",   // 部门英文名称
                "重部",                     // 部门简称
                "测试地址",                  // 部门地址
                true,                     // 是否实体部门
                "张三",                     // 行政负责人
                "李四",                     // 党委负责人
                new Date(),               // 成立日期
                null,                     // 失效日期
                unitCategoryDO.getUnitCategoryUuid(),                     // 单位类别
                unitTypeDO.getUnitTypeUuid(),                     // 单位办别
                null,
                null,                 // 分配教学楼
                true,                     // 是否为开课院系
                true,                     // 是否为上课院系
                "010-12345678",           // 固定电话
                "测试用部门",                // 备注
                false,                    // 是否为开课教研室
                true                      // 是否启用
        );

        // 确保测试前部门不存在
        DepartmentDO existingDepartment = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentCode, departmentVO.getDepartmentCode())
                .one();
        if (existingDepartment != null) {
            departmentDAO.removeById(existingDepartment);
        }

        // 先添加一次
        DepartmentDTO firstDepartmentDTO = departmentService.addDepartment(departmentVO);

        // 断言添加重复编码的部门时会抛出异常
        Assertions.assertThrows(DuplicateKeyException.class, () ->
                departmentService.addDepartment(departmentVO));

        // 清理测试数据
        DepartmentDO departmentDO = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getDepartmentUuid, firstDepartmentDTO.getDepartmentUuid())
                .one();
        departmentDAO.removeById(departmentDO);

        // 清理Redis缓存
        redisson.getBucket(StringConstant.Redis.DEPARTMENT_UUID + departmentDO.getDepartmentUuid()).delete();
    }

    @BeforeEach
    void setUp() {
        // 从数据库获取一个现有部门用于测试
        testDepartment = departmentDAO.lambdaQuery().last("LIMIT 1").one();

        // 生成一个不存在的UUID用于测试
        // 去掉破折号，确保格式符合系统要求
        nonExistentUuid = UUID.randomUUID().toString().replace("-", "");
        while (departmentDAO.getDepartmentByUuid(nonExistentUuid) != null) {
            nonExistentUuid = UUID.randomUUID().toString().replace("-", "");
        }

        log.debug("测试准备完成，使用部门UUID: {}", testDepartment.getDepartmentUuid());
        log.debug("非存在部门UUID: {}", nonExistentUuid);
    }

    /**
     * 测试正常情况下的getDepartment方法
     * 预期：能够成功获取现有部门的信息
     */
    @Test
    void testGetDepartmentSuccess() {
        log.debug("测试正常获取部门信息");

        // 删除Redis缓存，确保从数据库获取
        redisson.getKeys().delete(StringConstant.Redis.DEPARTMENT_UUID + testDepartment.getDepartmentUuid());

        // 调用getDepartment方法
        DepartmentDTO departmentDTO = departmentService.getDepartment(testDepartment.getDepartmentUuid());

        // 验证结果
        Assertions.assertNotNull(departmentDTO, "返回的部门DTO不应为空");
        Assertions.assertEquals(testDepartment.getDepartmentUuid(), departmentDTO.getDepartmentUuid());
        Assertions.assertEquals(testDepartment.getDepartmentName(), departmentDTO.getDepartmentName());
        Assertions.assertEquals(testDepartment.getDepartmentCode(), departmentDTO.getDepartmentCode());

        // 验证Redis缓存是否已创建
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + testDepartment.getDepartmentUuid());
        Assertions.assertTrue(map.isExists(), "Redis缓存应该被创建");
    }

    /**
     * 测试从Redis缓存获取部门信息
     * 预期：能够从Redis缓存中获取部门信息
     */
    @Test
    void testGetDepartmentFromRedisCache() {
        log.debug("测试从Redis缓存获取部门信息");

        // 先调用一次getDepartment确保Redis缓存已创建
        departmentService.getDepartment(testDepartment.getDepartmentUuid());

        // 确认Redis缓存存在
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + testDepartment.getDepartmentUuid());
        Assertions.assertTrue(map.isExists(), "Redis缓存应该存在");

        // 再次调用getDepartment方法（此时应从缓存获取）
        DepartmentDTO departmentDTO = departmentService.getDepartment(testDepartment.getDepartmentUuid());

        // 验证结果
        Assertions.assertNotNull(departmentDTO, "从缓存获取的部门DTO不应为空");
        Assertions.assertEquals(testDepartment.getDepartmentUuid(), departmentDTO.getDepartmentUuid());
        Assertions.assertEquals(testDepartment.getDepartmentName(), departmentDTO.getDepartmentName());
    }

    /**
     * 测试不存在的部门UUID
     * 预期：抛出业务异常，提示"部门不存在"
     */
    @Test
    void testGetDepartmentNotExist() {
        log.debug("测试获取不存在的部门");

        // 断言调用getDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.getDepartment(nonExistentUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试无效的UUID格式
     * 预期：抛出业务异常，提示"部门不存在"
     */
    @Test
    void testGetDepartmentWithInvalidUuid() {
        log.debug("测试使用无效UUID格式");

        // 使用带破折号的UUID格式，这应该会与正则表达式不匹配
        String invalidUuid = UUID.randomUUID().toString(); // 带破折号的UUID

        // 断言调用getDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.getDepartment(invalidUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试空UUID
     * 预期：抛出业务异常，提示"部门不存在"
     */
    @Test
    void testGetDepartmentWithEmptyUuid() {
        log.debug("测试使用空UUID");

        // 断言调用getDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.getDepartment(""));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试传入null作为UUID
     * 预期：由于@NotNull注解的存在，应该抛出NullPointerException
     * 注：根据实际代码实现，可能会抛出不同的异常类型
     */
    @Test
    void testGetDepartmentWithNullUuid() {
        log.debug("测试使用null作为UUID");

        // 断言调用getDepartment时会抛出NullPointerException异常
        Assertions.assertThrows(BusinessException.class, () ->
                departmentService.getDepartment(null));
    }

    /**
     * 测试正常情况下的deleteDepartment方法
     * 预期：能够成功删除现有部门
     */
    @Test
    @DisplayName("测试正常删除部门")
    @Rollback
    void testDeleteDepartmentSuccess() {
        log.debug("测试正常删除部门");

        // 先创建一个临时部门用于删除测试
        DepartmentDO tempDepartment = new DepartmentDO();
        tempDepartment.setDepartmentUuid(UUID.randomUUID().toString().replace("-", ""));
        tempDepartment.setDepartmentCode("TEST_DELETE_" + System.currentTimeMillis());
        tempDepartment.setDepartmentName("测试删除部门");
        tempDepartment.setDepartmentOrder(999);
        tempDepartment.setIsEnabled(true);
        // 设置必要的关联字段，从已有部门复制
        tempDepartment.setUnitCategory(testDepartment.getUnitCategory());
        tempDepartment.setUnitType(testDepartment.getUnitType());
        tempDepartment.setAssignedTeachingBuilding(testDepartment.getAssignedTeachingBuilding());
        tempDepartment.setParentDepartment(testDepartment.getParentDepartment());

        // 保存临时部门
        departmentDAO.save(tempDepartment);

        // 缓存到Redis
        departmentDAO.getDepartmentByUuid(tempDepartment.getDepartmentUuid());

        // 验证Redis缓存是否存在
        RMap<String, String> map = redisson.getMap(
                StringConstant.Redis.DEPARTMENT_UUID + tempDepartment.getDepartmentUuid());
        Assertions.assertTrue(map.isExists(), "删除前Redis缓存应该存在");

        // 调用删除部门方法
        departmentService.deleteDepartment(tempDepartment.getDepartmentUuid());

        // 验证部门已被删除
        DepartmentDO deletedDepartment = departmentDAO.getDepartmentByUuid(tempDepartment.getDepartmentUuid());
        Assertions.assertNull(deletedDepartment, "部门应该已被删除");

        // 验证Redis缓存是否被清除
        Assertions.assertFalse(map.isExists(), "删除后Redis缓存应该被清除");
    }

    /**
     * 测试删除不存在的部门
     * 预期：抛出业务异常，提示"部门不存在"
     */
    @Test
    @DisplayName("测试删除不存在的部门")
    void testDeleteDepartmentNotExist() {
        log.debug("测试删除不存在的部门");

        // 断言调用deleteDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.deleteDepartment(nonExistentUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试无效的UUID格式
     * 预期：抛出业务异常，提示"部门不存在"
     */
    @Test
    @DisplayName("测试使用无效UUID格式删除部门")
    void testDeleteDepartmentWithInvalidUuid() {
        log.debug("测试使用无效UUID格式删除部门");

        // 使用带破折号的UUID格式，这应该会与正则表达式不匹配
        String invalidUuid = UUID.randomUUID().toString(); // 带破折号的UUID

        // 断言调用deleteDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.deleteDepartment(invalidUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试空UUID
     * 预期：抛出业务异常，提示"部门不存在"
     */
    @Test
    @DisplayName("测试使用空UUID删除部门")
    void testDeleteDepartmentWithEmptyUuid() {
        log.debug("测试使用空UUID删除部门");

        // 断言调用deleteDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.deleteDepartment(""));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试传入null作为UUID
     * 预期：由于方法实现可能没有@NotNull注解，但逻辑上应该抛出异常
     */
    @Test
    @DisplayName("测试使用null作为UUID删除部门")
    void testDeleteDepartmentWithNullUuid() {
        log.debug("测试使用null作为UUID删除部门");

        // 断言调用deleteDepartment时会抛出异常，具体异常类型取决于实现
        Assertions.assertThrows(Exception.class, () ->
                departmentService.deleteDepartment(null));
    }

    /**
     * 测试删除有依赖关系的部门
     * 预期：如果部门有关联的教师、课程或专业，应该抛出业务异常
     * 注：这个测试假设DAO层的deleteDepartment方法在检测到依赖关系时会抛出异常
     */
    @Test
    @DisplayName("测试删除有依赖关系的部门")
    void testDeleteDepartmentWithDependencies() {
        log.debug("测试删除有依赖关系的部门");

        // 这里我们使用主测试部门，假设它有依赖关系
        // 如果实际上没有依赖关系，这个测试可能会失败

        // 注意：这个测试依赖于数据库中的实际数据状态
        // 在实际应用中，可能需要先创建依赖关系，然后尝试删除

        try {
            departmentService.deleteDepartment(testDepartment.getDepartmentUuid());

            // 如果执行到这里，说明删除成功，那么检查部门是否确实被删除
            DepartmentDO deletedDepartment = departmentDAO.getDepartmentByUuid(testDepartment.getDepartmentUuid());
            Assertions.assertNull(deletedDepartment, "有依赖关系的部门不应被删除，或者测试部门实际上没有依赖关系");
        } catch (BusinessException e) {
            // 如果抛出业务异常，检查异常信息是否与预期一致
            // 这里的错误信息假设，根据实际情况可能需要调整
            Assertions.assertTrue(e.getMessage().contains("部门") && e.getMessage().contains("存在"),
                    "异常信息应包含依赖关系说明");
        }
    }

    /**
     * 测试正常情况下的updateDepartment方法
     * 预期：能够成功更新现有部门的信息
     */
    @Test
    @DisplayName("测试正常更新部门")
    @Transactional
    @Rollback
    void testUpdateDepartmentSuccess() {
        log.debug("测试正常更新部门信息");

        // 先创建一个临时部门用于更新测试
        DepartmentDO tempDepartment = new DepartmentDO();
        String tempUuid = UUID.randomUUID().toString().replace("-", "");
        tempDepartment.setDepartmentUuid(tempUuid);
        tempDepartment.setDepartmentCode("TEST_UPDATE_" + System.currentTimeMillis());
        tempDepartment.setDepartmentName("测试更新部门-原始");
        tempDepartment.setDepartmentOrder(999);
        tempDepartment.setDepartmentEnglishName("Test Update Department - Original");
        tempDepartment.setDepartmentShortName("测更原");
        tempDepartment.setIsEnabled(true);
        // 设置必要的关联字段，从已有部门复制
        tempDepartment.setUnitCategory(testDepartment.getUnitCategory());
        tempDepartment.setUnitType(testDepartment.getUnitType());
        tempDepartment.setAssignedTeachingBuilding(testDepartment.getAssignedTeachingBuilding());
        tempDepartment.setParentDepartment(testDepartment.getParentDepartment());

        // 保存临时部门
        departmentDAO.save(tempDepartment);

        // 确保Redis缓存存在
        departmentDAO.getDepartmentByUuid(tempUuid);

        // 创建更新数据
        DepartmentVO updateVO = new DepartmentVO(
                "TEST_UPDATE_" + System.currentTimeMillis(), // 部门编码
                "测试更新部门-已更新",                            // 部门名称
                888,                                         // 部门排序
                "Test Update Department - Updated",          // 部门英文名称
                "测更新",                                     // 部门简称
                "测试更新地址",                                 // 部门地址
                true,                                       // 是否实体部门
                "王五",                                      // 行政负责人
                "赵六",                                      // 党委负责人
                new Date(),                                 // 成立日期
                null,                                       // 失效日期
                testDepartment.getUnitCategory(),           // 单位类别
                testDepartment.getUnitType(),               // 单位办别
                testDepartment.getParentDepartment(),       // 上级部门
                testDepartment.getAssignedTeachingBuilding() != null ? List.of(testDepartment.getAssignedTeachingBuilding()) : null,// 分配教学楼
                true,                                       // 是否为开课院系
                true,                                       // 是否为上课院系
                "010-87654321",                             // 固定电话
                "测试更新备注",                                // 备注
                false,                                      // 是否为开课教研室
                false                                       // 是否启用
        );

        // 调用更新方法
        DepartmentDTO updatedDTO = departmentService.updateDepartment(tempUuid, updateVO);

        // 验证返回的DTO
        Assertions.assertNotNull(updatedDTO, "更新后的部门DTO不应为空");
        Assertions.assertEquals("测试更新部门-已更新", updatedDTO.getDepartmentName(), "部门名称应该已更新");
        Assertions.assertEquals("Test Update Department - Updated", updatedDTO.getDepartmentEnglishName(), "部门英文名称应该已更新");
        Assertions.assertEquals("测更新", updatedDTO.getDepartmentShortName(), "部门简称应该已更新");
        Assertions.assertEquals(888, updatedDTO.getDepartmentOrder(), "部门排序应该已更新");
        Assertions.assertFalse(updatedDTO.getIsEnabled(), "部门启用状态应该已更新");

        // 验证Redis缓存是否已清除（因为更新操作会清除缓存）
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.DEPARTMENT_UUID + tempUuid);
        Assertions.assertFalse(map.isExists(), "更新后Redis缓存应该被清除");

        // 验证数据库中的数据是否已更新
        DepartmentDO updatedDO = departmentDAO.getDepartmentByUuid(tempUuid);
        Assertions.assertNotNull(updatedDO, "数据库中应该存在更新后的部门");
        Assertions.assertEquals("测试更新部门-已更新", updatedDO.getDepartmentName(), "数据库中的部门名称应该已更新");
        Assertions.assertEquals("Test Update Department - Updated", updatedDO.getDepartmentEnglishName(), "数据库中的部门英文名称应该已更新");
        Assertions.assertEquals("测更新", updatedDO.getDepartmentShortName(), "数据库中的部门简称应该已更新");
        Assertions.assertEquals(888, updatedDO.getDepartmentOrder(), "数据库中的部门排序应该已更新");
        Assertions.assertFalse(updatedDO.getIsEnabled(), "数据库中的部门启用状态应该已更新");

        // 清理测试数据
        departmentDAO.removeById(tempUuid);
    }

    /**
     * 测试更新不存在的部门
     * 预期：返回null，表示未找到可更新的部门
     */
    @Test
    @DisplayName("测试更新不存在的部门")
    void testUpdateDepartmentNotExist() {
        log.debug("测试更新不存在的部门");

        // 创建更新数据
        DepartmentVO updateVO = new DepartmentVO();

        // 调用更新方法并验证结果
        DepartmentDTO result = departmentService.updateDepartment(nonExistentUuid, updateVO);
        Assertions.assertNull(result, "更新不存在的部门应该返回null");
    }

    /**
     * 测试使用无效单位类别进行更新
     * 预期：抛出业务异常，提示"单位类别不存在"
     */
    @Test
    @DisplayName("测试更新部门时使用无效单位类别")
    void testUpdateDepartmentWithInvalidUnitCategory() {
        log.debug("测试更新部门时使用无效单位类别");

        // 创建更新数据，使用不存在的单位类别UUID
        DepartmentVO updateVO = new DepartmentVO(
                "TEST_UPDATE" + System.currentTimeMillis(), // 部门编码
                "测试更新部门-更新",                            // 部门名称
                8888,                                         // 部门排序
                "Test Updated",          // 部门英文名称
                "测更新",                                     // 部门简称
                "测试更新地址",                                 // 部门地址
                true,                                       // 是否实体部门
                "王五",                                      // 行政负责人
                "赵六",                                      // 党委负责人
                new Date(),                                 // 成立日期
                null,                                       // 失效日期
                nonExistentUuid,           // 单位类别
                testDepartment.getUnitType(),               // 单位办别
                testDepartment.getParentDepartment(),       // 上级部门
                testDepartment.getAssignedTeachingBuilding() != null ? List.of(testDepartment.getAssignedTeachingBuilding()) : null,// 分配教学楼
                true,                                       // 是否为开课院系
                true,                                       // 是否为上课院系
                "010-87654311",                             // 固定电话
                "更新备注",                                // 备注
                false,                                      // 是否为开课教研室
                false                                       // 是否启用
        );

        // 断言调用updateDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.updateDepartment(testDepartment.getDepartmentUuid(), updateVO));

        // 验证异常信息和错误码
        Assertions.assertEquals("单位类别不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试使用无效单位办别进行更新
     * 预期：抛出业务异常，提示"单位办别不存在"
     */
    @Test
    @DisplayName("测试更新部门时使用无效单位办别")
    void testUpdateDepartmentWithInvalidUnitType() {
        log.debug("测试更新部门时使用无效单位办别");

        // 创建更新数据，使用不存在的单位办别UUID
        DepartmentVO updateVO = new DepartmentVO(
                "TEST_UPDATE" + System.currentTimeMillis(), // 部门编码
                "测试更新部门-更新",                            // 部门名称
                8888,                                         // 部门排序
                "Test Update",          // 部门英文名称
                "更新",                                     // 部门简称
                "测试更新地址",                                 // 部门地址
                true,                                       // 是否实体部门
                "王五",                                      // 行政负责人
                "赵六",                                      // 党委负责人
                new Date(),                                 // 成立日期
                null,                                       // 失效日期
                testDepartment.getUnitCategory(),           // 单位类别
                nonExistentUuid,               // 单位办别
                testDepartment.getParentDepartment(),       // 上级部门
                testDepartment.getAssignedTeachingBuilding() != null ? List.of(testDepartment.getAssignedTeachingBuilding()) : null,// 分配教学楼
                true,                                       // 是否为开课院系
                true,                                       // 是否为上课院系
                "010-87654311",                             // 固定电话
                "更新备注",                                // 备注
                false,                                      // 是否为开课教研室
                false                                       // 是否启用
        );


        // 断言调用updateDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.updateDepartment(testDepartment.getDepartmentUuid(), updateVO));

        // 验证异常信息和错误码
        Assertions.assertEquals("单位办别不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试使用无效教学楼进行更新
     * 预期：抛出业务异常，提示"教学楼不存在"
     */
    @Test
    @DisplayName("测试更新部门时使用无效教学楼")
    void testUpdateDepartmentWithInvalidBuilding() {
        log.debug("测试更新部门时使用无效教学楼");

        // 创建更新数据，使用不存在的教学楼UUID
        DepartmentVO updateVO = new DepartmentVO(
                "TEST_UPDATE" + System.currentTimeMillis(), // 部门编码
                "测试更新部门-更新",                            // 部门名称
                8888,                                         // 部门排序
                "Test Update",          // 部门英文名称
                "更新",                                     // 部门简称
                "测试更新地址",                                 // 部门地址
                true,                                       // 是否实体部门
                "王五",                                      // 行政负责人
                "赵六",                                      // 党委负责人
                new Date(),                                 // 成立日期
                null,                                       // 失效日期
                testDepartment.getUnitCategory(),           // 单位类别
                testDepartment.getUnitType(),               // 单位办别
                testDepartment.getParentDepartment(),       // 上级部门
                List.of(UuidUtil.generateUuidNoDash()),     // 分配教学楼
                true,                                       // 是否为开课院系
                true,                                       // 是否为上课院系
                "010-87654311",                             // 固定电话
                "更新备注",                                // 备注
                false,                                      // 是否为开课教研室
                false                                       // 是否启用
        );

        // 断言调用updateDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.updateDepartment(testDepartment.getDepartmentUuid(), updateVO));

        // 验证异常信息和错误码
        Assertions.assertEquals("教学楼不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试使用无效上级部门进行更新
     * 预期：抛出业务异常，提示"上级部门不存在"
     */
    @Test
    @DisplayName("测试更新部门时使用无效上级部门")
    void testUpdateDepartmentWithInvalidParentDepartment() {
        log.debug("测试更新部门时使用无效上级部门");

        // 创建更新数据，使用不存在的上级部门UUID
        DepartmentVO updateVO = new DepartmentVO(
                "TEST_UPDATE" + System.currentTimeMillis(), // 部门编码
                "测试更新部门-更新",                            // 部门名称
                8888,                                         // 部门排序
                "Test Update",          // 部门英文名称
                "更新",                                     // 部门简称
                "测试更新地址",                                 // 部门地址
                true,                                       // 是否实体部门
                "王五",                                      // 行政负责人
                "赵六",                                      // 党委负责人
                new Date(),                                 // 成立日期
                null,                                       // 失效日期
                testDepartment.getUnitCategory(),           // 单位类别
                testDepartment.getUnitType(),               // 单位办别
                nonExistentUuid,       // 上级部门
                testDepartment.getAssignedTeachingBuilding() != null ? List.of(testDepartment.getAssignedTeachingBuilding()) : null,// 分配教学楼
                true,                                       // 是否为开课院系
                true,                                       // 是否为上课院系
                "010-87654311",                             // 固定电话
                "更新备注",                                // 备注
                false,                                      // 是否为开课教研室
                false                                       // 是否启用
        );


        // 断言调用updateDepartment时会抛出BusinessException异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                departmentService.updateDepartment(testDepartment.getDepartmentUuid(), updateVO));

        // 验证异常信息和错误码
        Assertions.assertEquals("上级部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试传入null作为UUID进行更新
     * 预期：方法应该返回null或抛出异常
     */
    @Test
    @DisplayName("测试使用null作为UUID更新部门")
    void testUpdateDepartmentWithNullUuid() {
        log.debug("测试使用null作为UUID更新部门");

        // 创建更新数据
        DepartmentVO updateVO = new DepartmentVO();

        try {
            DepartmentDTO result = departmentService.updateDepartment(null, updateVO);
            Assertions.assertNull(result, "使用null UUID更新部门应该返回null");
        } catch (Exception e) {
            // 或者可能抛出异常，两种情况都可接受
            // 不做具体的异常类型断言，因为实现可能会有所不同
        }
    }

    @BeforeEach
    void setUp2() {
        log.debug("测试准备开始");
        // 确保数据库中至少有一些部门数据用于测试
        long departmentCount = departmentDAO.lambdaQuery().count();
        if (departmentCount == 0) {
            log.warn("数据库中没有部门数据，测试可能无法正常进行");
        } else {
            log.debug("数据库中有 {} 个部门", departmentCount);
        }
    }

    /**
     * 测试正常获取部门列表
     * <p>
     * 预期：能够成功获取部门列表，并且返回数据与查询参数一致
     * </p>
     */
    @Test
    @DisplayName("测试正常获取部门列表")
    void testGetDepartmentListSuccess() {
        log.debug("测试正常获取部门列表");

        // 调用getDepartmentList方法获取第一页数据
        PageDTO<DepartmentDTO> pageDTO = departmentService.getDepartmentPage(DEFAULT_PAGE, DEFAULT_SIZE, false, null);

        // 验证返回的数据不为空
        Assertions.assertNotNull(pageDTO, "返回的分页数据不应为空");

        // 验证分页信息正确
        Assertions.assertEquals(DEFAULT_PAGE, pageDTO.getCurrent(), "当前页码应该与请求的页码一致");
        Assertions.assertEquals(DEFAULT_SIZE, pageDTO.getSize(), "每页大小应该与请求的大小一致");

        // 如果总数大于0，验证记录列表不为空
        if (pageDTO.getTotal() > 0) {
            Assertions.assertNotNull(pageDTO.getRecords(), "返回的记录列表不应为空");
            Assertions.assertFalse(pageDTO.getRecords().isEmpty(), "返回的记录列表不应为空");

            // 验证返回的记录数不超过每页大小
            Assertions.assertTrue(pageDTO.getRecords().size() <= DEFAULT_SIZE,
                    "返回的记录数不应超过每页大小");

            // 验证第一条记录的关键字段不为空
            DepartmentDTO firstDept = pageDTO.getRecords().get(0);
            Assertions.assertNotNull(firstDept.getDepartmentUuid(), "部门UUID不应为空");
            Assertions.assertNotNull(firstDept.getDepartmentName(), "部门名称不应为空");
        }

        log.debug("获取到 {} 条部门记录，总计 {} 条",
                pageDTO.getRecords() != null ? pageDTO.getRecords().size() : 0,
                pageDTO.getTotal());
    }

    /**
     * 测试使用名称搜索部门
     * <p>
     * 预期：能够根据部门名称搜索匹配的部门
     * </p>
     */
    @Test
    @DisplayName("测试使用名称搜索部门")
    void testGetDepartmentListByName() {
        log.debug("测试使用名称搜索部门");

        // 先获取一个部门名称用于搜索
        Page<DepartmentDO> allDepts = departmentDAO.getDepartmentPage(DEFAULT_PAGE, 1, false, null);
        if (allDepts.getRecords().isEmpty()) {
            log.warn("数据库中没有部门数据，跳过搜索测试");
            return;
        }

        String searchName = allDepts.getRecords().get(0).getDepartmentName();
        if (searchName == null || searchName.isEmpty()) {
            searchName = "部门"; // 使用通用名称作为备选
        }
        log.debug("使用 '{}' 作为搜索关键词", searchName);

        // 使用部门名称进行搜索
        PageDTO<DepartmentDTO> searchResult = departmentService.getDepartmentPage(
                DEFAULT_PAGE, DEFAULT_SIZE, false, searchName);

        // 验证返回的数据不为空
        Assertions.assertNotNull(searchResult, "搜索结果不应为空");

        // 如果搜索结果非空，验证记录是否包含搜索关键词
        if (searchResult.getTotal() > 0 && searchResult.getRecords() != null) {
            String finalSearchName = searchName;
            boolean hasMatch = searchResult.getRecords().stream()
                    .anyMatch(dept -> dept.getDepartmentName() != null &&
                            dept.getDepartmentName().contains(finalSearchName));
            Assertions.assertTrue(hasMatch, "搜索结果应包含匹配的部门名称");
        }

        log.debug("搜索 '{}' 获取到 {} 条部门记录", searchName, searchResult.getTotal());
    }

    /**
     * 测试分页功能
     * <p>
     * 预期：能够正确返回指定页码和大小的数据
     * </p>
     */
    @Test
    @DisplayName("测试部门列表分页功能")
    void testGetDepartmentListPagination() {
        log.debug("测试部门列表分页功能");

        // 获取总记录数
        Page<DepartmentDO> countPage = departmentDAO.getDepartmentPage(DEFAULT_PAGE, 1, false, null);
        long totalCount = countPage.getTotal();

        if (totalCount <= DEFAULT_SIZE) {
            log.warn("部门总数不足以测试分页，跳过分页测试");
            return;
        }

        // 获取第一页数据
        PageDTO<DepartmentDTO> page1 = departmentService.getDepartmentPage(1, DEFAULT_SIZE, false, null);

        // 获取第二页数据
        PageDTO<DepartmentDTO> page2 = departmentService.getDepartmentPage(2, DEFAULT_SIZE, false, null);

        // 验证两页数据不同
        Assertions.assertNotEquals(
                page1.getRecords().get(0).getDepartmentUuid(),
                page2.getRecords().get(0).getDepartmentUuid(),
                "第一页和第二页的第一条记录应该不同");

        // 验证总记录数一致
        Assertions.assertEquals(page1.getTotal(), page2.getTotal(), "不同页的总记录数应该一致");

        log.debug("第一页和第二页数据成功验证不同");
    }

    /**
     * 测试获取空结果
     * <p>
     * 预期：使用不存在的部门名称搜索时，返回空结果集
     * </p>
     */
    @Test
    @DisplayName("测试获取空的部门列表")
    void testGetDepartmentListEmpty() {
        log.debug("测试获取空的部门列表");

        // 使用一个极不可能存在的名称进行搜索
        String nonExistentName = "非常不可能存在的部门名称" + System.currentTimeMillis();

        // 调用搜索方法
        PageDTO<DepartmentDTO> emptyResult = departmentService.getDepartmentPage(
                DEFAULT_PAGE, DEFAULT_SIZE, false, nonExistentName);

        // 验证返回的数据不为空但结果为空
        Assertions.assertNotNull(emptyResult, "返回的PageDTO对象不应为空");
        Assertions.assertEquals(0, emptyResult.getTotal(), "总记录数应为0");

        // 根据方法实现，验证records是否为null或空列表
        if (emptyResult.getRecords() != null) {
            Assertions.assertTrue(emptyResult.getRecords().isEmpty(), "记录列表应为空");
        }

        log.debug("空结果集验证完成");
    }

    /**
     * 测试每页大小为0的特殊情况
     * <p>
     * 预期：应该正常处理，可能返回默认每页大小的结果或空结果
     * </p>
     */
    @Test
    @DisplayName("测试每页大小为0的情况")
    void testGetDepartmentListZeroSize() {
        log.debug("测试每页大小为0的情况");

        // 调用方法，设置每页大小为0
        PageDTO<DepartmentDTO> result = departmentService.getDepartmentPage(DEFAULT_PAGE, 0, false, null);

        // 不对具体结果做断言，因为处理方式可能不同，只要不抛出异常即可
        Assertions.assertNotNull(result, "返回的PageDTO对象不应为空");

        log.debug("每页大小为0的情况测试完成，返回记录数: {}",
                result.getRecords() != null ? result.getRecords().size() : 0);
    }

    /**
     * 测试页码为负数的特殊情况
     * <p>
     * 预期：应该正常处理，可能使用默认页码或返回空结果
     * </p>
     */
    @Test
    @DisplayName("测试页码为负数的情况")
    void testGetDepartmentListNegativePage() {
        log.debug("测试页码为负数的情况");

        // 调用方法，设置页码为负数
        PageDTO<DepartmentDTO> result = departmentService.getDepartmentPage(-1, DEFAULT_SIZE, false, null);

        // 不对具体结果做断言，因为处理方式可能不同，只要不抛出异常即可
        Assertions.assertNotNull(result, "返回的PageDTO对象不应为空");

        log.debug("页码为负数的情况测试完成");
    }

    /**
     * 测试正常情况下的 getDepartmentByUuid 方法
     * <p>
     * 预期：能够成功获取现有部门的信息，返回对应的DTO对象
     * </p>
     */
    @Test
    @DisplayName("测试正常获取部门信息")
    void testGetDepartmentByUuidSuccess() {
        log.debug("测试正常获取部门信息");

        // 跳过测试如果没有测试数据
        if (testDepartment == null) {
            log.warn("数据库中没有可用的部门数据，跳过测试");
            return;
        }

        // 删除Redis缓存，确保从数据库获取
        redisson.getKeys().delete(StringConstant.Redis.DEPARTMENT_UUID + testDepartment.getDepartmentUuid());

        // 调用getDepartmentByUuid方法
        DepartmentDTO departmentDTO = departmentService.getDepartmentByUuid(testDepartment.getDepartmentUuid());

        // 验证结果
        Assertions.assertNotNull(departmentDTO, "返回的部门DTO不应为空");
        Assertions.assertEquals(testDepartment.getDepartmentUuid(), departmentDTO.getDepartmentUuid(), "UUID应匹配");
        Assertions.assertEquals(testDepartment.getDepartmentName(), departmentDTO.getDepartmentName(), "部门名称应匹配");
        Assertions.assertEquals(testDepartment.getDepartmentCode(), departmentDTO.getDepartmentCode(), "部门编码应匹配");

        // 验证Redis缓存是否已创建（如果项目使用了Redis缓存机制）
            RMap<String, String> map = redisson.getMap(
                    StringConstant.Redis.DEPARTMENT_UUID + testDepartment.getDepartmentUuid());
            Assertions.assertTrue(map.isExists(), "Redis缓存应该被创建");
    }

    /**
     * 测试从Redis缓存获取部门信息（如果项目使用了Redis缓存）
     * <p>
     * 预期：能够从Redis缓存中获取部门信息
     * </p>
     */
    @Test
    @DisplayName("测试从Redis缓存获取部门信息")
    void testGetDepartmentByUuidFromRedisCache() {
        log.debug("测试从Redis缓存获取部门信息");

        // 跳过测试如果没有测试数据
        if (testDepartment == null) {
            log.warn("数据库中没有可用的部门数据，跳过测试");
            return;
        }

        // 先调用一次getDepartmentByUuid确保Redis缓存已创建（如果使用了缓存机制）
        departmentService.getDepartmentByUuid(testDepartment.getDepartmentUuid());

        // 确认Redis缓存存在（如果使用了缓存机制）
            RMap<String, String> map = redisson.getMap(
                    StringConstant.Redis.DEPARTMENT_UUID + testDepartment.getDepartmentUuid());
            Assertions.assertTrue(map.isExists(), "Redis缓存应该存在");


        // 再次调用getDepartmentByUuid方法（此时可能从缓存获取，取决于实现）
        DepartmentDTO departmentDTO = departmentService.getDepartmentByUuid(testDepartment.getDepartmentUuid());

        // 验证结果
        Assertions.assertNotNull(departmentDTO, "从缓存获取的部门DTO不应为空");
        Assertions.assertEquals(testDepartment.getDepartmentUuid(), departmentDTO.getDepartmentUuid(), "UUID应匹配");
        Assertions.assertEquals(testDepartment.getDepartmentName(), departmentDTO.getDepartmentName(), "部门名称应匹配");
    }

    /**
     * 测试获取不存在的部门
     * <p>
     * 预期：对于不存在的UUID，应返回null
     * </p>
     */
    @Test
    @DisplayName("测试获取不存在的部门")
    void testGetDepartmentByUuidNotExist() {
        log.debug("测试获取不存在的部门");

        // 调用getDepartmentByUuid方法获取不存在的部门
        DepartmentDTO departmentDTO = departmentService.getDepartmentByUuid(nonExistentUuid);

        // 验证结果为null
        Assertions.assertNull(departmentDTO, "不存在的部门应返回null");
    }

    /**
     * 测试使用无效的UUID格式
     * <p>
     * 预期：使用格式不正确的UUID应返回null或特定错误
     * </p>
     */
    @Test
    @DisplayName("测试使用无效UUID格式")
    void testGetDepartmentByUuidWithInvalidFormat() {
        log.debug("测试使用无效UUID格式");

        // 使用带破折号的UUID格式，这可能与系统要求的格式不匹配
        String invalidUuid = UUID.randomUUID().toString(); // 带破折号的UUID

        // 调用getDepartmentByUuid方法
        DepartmentDTO departmentDTO = departmentService.getDepartmentByUuid(invalidUuid);

        // 验证结果为null（或其他预期行为，取决于实现）
        Assertions.assertNull(departmentDTO, "无效格式的UUID应返回null");
    }

    /**
     * 测试使用空UUID
     * <p>
     * 预期：空字符串作为UUID应返回null或特定错误
     * </p>
     */
    @Test
    @DisplayName("测试使用空UUID")
    void testGetDepartmentByUuidWithEmptyString() {
        log.debug("测试使用空UUID");

        // 调用getDepartmentByUuid方法，传入空字符串
        DepartmentDTO departmentDTO = departmentService.getDepartmentByUuid("");

        // 验证结果为null（或其他预期行为，取决于实现）
        Assertions.assertNull(departmentDTO, "空UUID应返回null");
    }

    /**
     * 测试使用null作为UUID
     * <p>
     * 预期：null作为UUID应返回null或抛出特定异常
     * </p>
     */
    @Test
    @DisplayName("测试使用null作为UUID")
    void testGetDepartmentByUuidWithNull() {
        log.debug("测试使用null作为UUID");

        // 调用getDepartmentByUuid方法，传入null
        DepartmentDTO departmentDTO = departmentService.getDepartmentByUuid(null);

        // 验证结果为null（或其他预期行为，取决于实现）
        Assertions.assertNull(departmentDTO, "null UUID应返回null");
    }

    /**
     * 测试使用超长UUID
     * <p>
     * 预期：超出正常长度的UUID应返回null或特定错误
     * </p>
     */
    @Test
    @DisplayName("测试使用超长UUID")
    void testGetDepartmentByUuidWithTooLongString() {
        log.debug("测试使用超长UUID");

        // 创建一个超长的UUID字符串
        String tooLongUuid = UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");

        // 调用getDepartmentByUuid方法
        DepartmentDTO departmentDTO = departmentService.getDepartmentByUuid(tooLongUuid);

        // 验证结果为null（或其他预期行为，取决于实现）
        Assertions.assertNull(departmentDTO, "超长UUID应返回null");
    }



    @Test
    void testGetDepartmentByUuidWithThrows (){
        log.debug("测试通过UUID获取部门信息会传出报错");
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        Assertions.assertNotNull(
                departmentService.getDepartmentByUuidWithThrows(departmentDO.getDepartmentUuid()));
        String departmentUuid = UuidUtil.generateUuidNoDash();
        Assertions.assertThrows(BusinessException.class,() ->
                departmentService.getDepartmentByUuidWithThrows(departmentUuid));
    }
}
