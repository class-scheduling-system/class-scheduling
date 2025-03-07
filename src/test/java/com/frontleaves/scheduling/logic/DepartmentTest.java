package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

import java.util.Date;

@SpringBootTest
@Slf4j
 class DepartmentTest {
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private DepartmentService departmentService;

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
}
