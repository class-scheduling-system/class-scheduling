package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.UnitTypeDTO;
import com.frontleaves.scheduling.models.dto.lite.UnitTypeLiteDTO;
import com.frontleaves.scheduling.models.entity.base.DepartmentDO;
import com.frontleaves.scheduling.models.entity.base.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.UnitTypeVO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@SpringBootTest
@Transactional
class UnitTypeLogicTest {
    @Autowired
    private UnitTypeDAO unitTypeDAO;
    @Autowired
    private DepartmentDAO departmentDAO;
    @Autowired
    private UnitTypeLogic unitTypeLogic;

    private UnitTypeVO setupUnitTypeVO;
    private UnitTypeDO setupUnitTypeDO;
    @Autowired
    private UnitCategoryDAO unitCategoryDAO;

    @BeforeEach
    void setUp() {
        log.debug("UnitTypeLogic单元测试初始化");
        setupUnitTypeVO = new UnitTypeVO(
                "测试单位办别",
                "Test Unit Type",
                "TUT",
                1
        );

        setupUnitTypeDO = new UnitTypeDO();
        setupUnitTypeDO.setUnitTypeUuid(UuidUtil.generateUuidNoDash())
                .setName(setupUnitTypeVO.getName())
                .setEnglishName(setupUnitTypeVO.getEnglishName())
                .setShortName(setupUnitTypeVO.getShortName())
                .setOrder(setupUnitTypeVO.getOrder());

        // 保存测试数据到数据库
        unitTypeDAO.save(setupUnitTypeDO);
    }

    @Test
    void testCheckAddUnitTypeVO() {
        // 测试添加新单位办别（使用新名称）
        UnitTypeVO newUnitTypeVO = new UnitTypeVO(
                "新测试单位办别",
                "New Test Unit Type",
                "NTUT",
                2
        );
        Assertions.assertDoesNotThrow(() -> unitTypeLogic.checkAddUnitTypeVO(newUnitTypeVO));

        // 测试添加已存在的单位办别
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkAddUnitTypeVO(setupUnitTypeVO));
        Assertions.assertEquals(ErrorCode.PARAMETER_INVALID, exception.getErrorCode());
    }

    @Test
    void testAddUnitType() {
        // 使用 Logic 层方法进行添加测试
        UnitTypeVO newUnitTypeVO = new UnitTypeVO(
                "新测试单位办别",
                "New Test Unit Type",
                "NTUT",
                2
        );
        UnitTypeDTO result = unitTypeLogic.addUnitType(newUnitTypeVO);

        // 验证返回结果
        Assertions.assertNotNull(result);
        Assertions.assertEquals(newUnitTypeVO.getName(), result.getName());
        Assertions.assertEquals(newUnitTypeVO.getEnglishName(), result.getEnglishName());
        Assertions.assertEquals(newUnitTypeVO.getShortName(), result.getShortName());
        Assertions.assertEquals(newUnitTypeVO.getOrder(), result.getOrder());

        // 从数据库中查询并验证
        UnitTypeDO savedUnitType = unitTypeDAO.getById(result.getUnitTypeUuid());
        Assertions.assertNotNull(savedUnitType);
        Assertions.assertEquals(newUnitTypeVO.getName(), savedUnitType.getName());
        Assertions.assertEquals(newUnitTypeVO.getEnglishName(), savedUnitType.getEnglishName());
        Assertions.assertEquals(newUnitTypeVO.getShortName(), savedUnitType.getShortName());
        Assertions.assertEquals(newUnitTypeVO.getOrder(), savedUnitType.getOrder());
    }

    @Test
    void testCheckUpdateUnitTypeVO() {
        // 测试更新不存在的单位办别
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkUpdateUnitTypeVO("non-existent-uuid", setupUnitTypeVO));
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());

        // 创建另一个单位办别用于测试名称冲突
        UnitTypeDO anotherUnitType = new UnitTypeDO()
                .setUnitTypeUuid(UuidUtil.generateUuidNoDash())
                .setName("另一个单位办别")
                .setEnglishName("Another Unit Type")
                .setShortName("AUT")
                .setOrder(2);
        unitTypeDAO.save(anotherUnitType);

        // 测试更新为已存在的名称
        UnitTypeVO conflictVO = new UnitTypeVO(
                anotherUnitType.getName(),
                "Updated Test Unit Type",
                "UTUT",
                3
        );
        exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkUpdateUnitTypeVO(setupUnitTypeDO.getUnitTypeUuid(), conflictVO));
        Assertions.assertEquals(ErrorCode.PARAMETER_INVALID, exception.getErrorCode());
    }

    @Test
    void testUpdateUnitType() {
        // 准备更新数据
        UnitTypeVO updateVO = new UnitTypeVO(
                "更新后的单位办别",
                "Updated Unit Type",
                "UUT",
                3
        );

        // 执行更新
        UnitTypeDTO result = unitTypeLogic.updateUnitType(updateVO, setupUnitTypeDO);

        // 验证返回结果
        Assertions.assertNotNull(result);
        Assertions.assertEquals(updateVO.getName(), result.getName());
        Assertions.assertEquals(updateVO.getEnglishName(), result.getEnglishName());
        Assertions.assertEquals(updateVO.getShortName(), result.getShortName());
        Assertions.assertEquals(updateVO.getOrder(), result.getOrder());

        // 验证数据库更新
        UnitTypeDO updatedType = unitTypeDAO.getById(setupUnitTypeDO.getUnitTypeUuid());
        Assertions.assertNotNull(updatedType);
        Assertions.assertEquals(updateVO.getName(), updatedType.getName());
        Assertions.assertEquals(updateVO.getEnglishName(), updatedType.getEnglishName());
        Assertions.assertEquals(updateVO.getShortName(), updatedType.getShortName());
        Assertions.assertEquals(updateVO.getOrder(), updatedType.getOrder());
    }

    @Test
    void testCheckDeleteUnitType() {
        // 测试删除不存在的单位办别
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkDeleteUnitType("non-existent-uuid"));
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());

        // 创建关联的部门
        DepartmentDO department = new DepartmentDO();
        department.setDepartmentUuid(UuidUtil.generateUuidNoDash())
                .setDepartmentName("测试部门")
                .setDepartmentCode("TT10110")
                .setUnitType(setupUnitTypeDO.getUnitTypeUuid())
                .setUnitCategory(unitCategoryDAO.list().get(0).getUnitCategoryUuid());
        departmentDAO.save(department);

        // 测试正常删除
        UnitTypeDO result = unitTypeLogic.checkDeleteUnitType(setupUnitTypeDO.getUnitTypeUuid());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(setupUnitTypeDO.getUnitTypeUuid(), result.getUnitTypeUuid());
    }

    @Test
    void testDeleteUnitType() {
        // 执行删除操作
        Assertions.assertDoesNotThrow(() -> unitTypeLogic.deleteUnitType(setupUnitTypeDO));

        // 验证是否已被删除
        UnitTypeDO deletedType = unitTypeDAO.getById(setupUnitTypeDO.getUnitTypeUuid());
        Assertions.assertNull(deletedType);
    }

    @Test
    void testGetPageOfUnitType() {
        // 添加多个测试数据
        UnitTypeDO unitType2 = new UnitTypeDO()
                .setUnitTypeUuid(UuidUtil.generateUuidNoDash())
                .setName("测试单位办别2")
                .setEnglishName("Test Unit Type 2")
                .setShortName("TUT2")
                .setOrder(2);
        unitTypeDAO.save(unitType2);

        // 测试分页查询
        PageDTO<UnitTypeDTO> result = unitTypeLogic.getPageOfUnitType(1, 10, true, "测试");

        // 验证结果
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getTotal() >= 2); // 至少包含两条测试数据
        Assertions.assertTrue(result.getRecords().size() >= 2);

        // 验证查询结果包含测试数据
        boolean foundSetup = false;
        boolean foundType2 = false;
        for (UnitTypeDTO dto : result.getRecords()) {
            if (dto.getName().equals(setupUnitTypeDO.getName())) {
                foundSetup = true;
            }
            if (dto.getName().equals(unitType2.getName())) {
                foundType2 = true;
            }
        }
        Assertions.assertTrue(foundSetup && foundType2);
    }

    @Test
    void testGetUnitTypeList() {
        // 添加另一个测试数据
        UnitTypeDO unitType2 = new UnitTypeDO()
                .setUnitTypeUuid(UuidUtil.generateUuidNoDash())
                .setName("测试单位办别2")
                .setEnglishName("Test Unit Type 2")
                .setShortName("TUT2")
                .setOrder(2);
        unitTypeDAO.save(unitType2);

        // 获取列表
        List<UnitTypeLiteDTO> result = unitTypeLogic.getUnitTypeList();

        // 验证结果
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.size() >= 2); // 至少包含两条测试数据

        // 验证结果包含测试数据
        boolean foundSetup = false;
        boolean foundType2 = false;
        for (UnitTypeLiteDTO dto : result) {
            if (dto.getName().equals(setupUnitTypeDO.getName())) {
                foundSetup = true;
            }
            if (dto.getName().equals(unitType2.getName())) {
                foundType2 = true;
            }
        }
        Assertions.assertTrue(foundSetup && foundType2);
    }
}
