package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import java.util.Date;
import java.util.UUID;

@SpringBootTest
@Slf4j
 class DepartmentTest {
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private DepartmentService departmentService;

    private DepartmentDO testDepartment;
    private String nonExistentUuid;

    @Test
    void testAddDepartment() {
        log.debug("测试添加部门");

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
                "33b46a1003384ee8bf5cbbe56caada26",                     // 单位类别
                "d862471154aa49dba4495d47b4d439dc",                     // 单位办别
                "3055db155b1f41baba70c6ab2cadfd6d",                     // 上级部门
                "037ccda638d548edbef145a6cefc3aa3",                     // 分配教学楼
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
                "33b46a1003384ee8bf5cbbe56caada26",                     // 单位类别
                "d862471154aa49dba4495d47b4d439dc",                     // 单位办别
                "3055db155b1f41baba70c6ab2cadfd6d",                     // 上级部门
                "037ccda638d548edbef145a6cefc3aa3",                     // 分配教学楼
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
        Assertions.assertThrows(NullPointerException.class, () ->
                departmentService.getDepartment(null));
    }


}
