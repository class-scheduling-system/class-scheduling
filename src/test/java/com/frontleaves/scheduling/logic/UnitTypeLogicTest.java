package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.UnitTypeDTO;
import com.frontleaves.scheduling.models.dto.UnitTypeLiteDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.UnitTypeVO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@SpringBootTest
class UnitTypeLogicTest {
    @Mock
    private UnitTypeDAO unitTypeDAO;
    @Mock
    private DepartmentDAO departmentDAO;
    @InjectMocks
    private UnitTypeLogic unitTypeLogic;

    private UnitTypeVO setupUnitTypeVO;
    private UnitTypeDO setupUnitTypeDO;

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
    }

    @Test
    void testCheckAddUnitTypeVO() {
        // 测试添加新单位办别
        Mockito.when(unitTypeDAO.getUnitTypeByName(setupUnitTypeVO.getName())).thenReturn(null);
        Assertions.assertDoesNotThrow(() -> unitTypeLogic.checkAddUnitTypeVO(setupUnitTypeVO));

        // 测试添加已存在的单位办别
        Mockito.when(unitTypeDAO.getUnitTypeByName(setupUnitTypeVO.getName())).thenReturn(setupUnitTypeDO);
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkAddUnitTypeVO(setupUnitTypeVO));
        Assertions.assertEquals(ErrorCode.PARAMETER_INVALID, exception.getErrorCode());
    }

    @Test
    void testAddUnitType() {
        Mockito.when(unitTypeDAO.save(Mockito.any(UnitTypeDO.class))).thenReturn(true);
        UnitTypeDTO result = unitTypeLogic.addUnitType(setupUnitTypeVO);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(setupUnitTypeVO.getName(), result.getName());
    }

    @Test
    void testCheckUpdateUnitTypeVO() {
        // 测试更新不存在的单位办别
        Mockito.when(unitTypeDAO.getById(Mockito.anyString())).thenReturn(null);
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkUpdateUnitTypeVO("non-existent-uuid", setupUnitTypeVO));
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());

        // 测试更新为已存在的名称
        Mockito.when(unitTypeDAO.getById(setupUnitTypeDO.getUnitTypeUuid())).thenReturn(setupUnitTypeDO);
        Mockito.when(unitTypeDAO.getUnitTypeByName(setupUnitTypeVO.getName())).thenReturn(setupUnitTypeDO);
        exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkUpdateUnitTypeVO(setupUnitTypeDO.getUnitTypeUuid(), setupUnitTypeVO));
        Assertions.assertEquals(ErrorCode.PARAMETER_INVALID, exception.getErrorCode());
    }

    @Test
    void testUpdateUnitType() {
        Mockito.when(unitTypeDAO.updateById(Mockito.any(UnitTypeDO.class))).thenReturn(true);
        UnitTypeDTO result = unitTypeLogic.updateUnitType(setupUnitTypeVO, setupUnitTypeDO);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(setupUnitTypeVO.getName(), result.getName());
    }

    @Test
    void testCheckDeleteUnitType() {
        // 测试删除不存在的单位办别
        Mockito.when(unitTypeDAO.getById(Mockito.anyString())).thenReturn(null);
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkDeleteUnitType("non-existent-uuid"));
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());

        // 测试删除被部门使用的单位办别
        Mockito.when(unitTypeDAO.getById(setupUnitTypeDO.getUnitTypeUuid())).thenReturn(setupUnitTypeDO);
        Mockito.when(departmentDAO.getDepartmentByUuid(setupUnitTypeDO.getUnitTypeUuid())).thenReturn(new DepartmentDO());
        exception = Assertions.assertThrows(BusinessException.class,
                () -> unitTypeLogic.checkDeleteUnitType(setupUnitTypeDO.getUnitTypeUuid()));
        Assertions.assertEquals(ErrorCode.OPERATION_INVALID, exception.getErrorCode());

        // 测试正常删除
        Mockito.when(departmentDAO.getDepartmentByUuid(setupUnitTypeDO.getUnitTypeUuid())).thenReturn(null);
        UnitTypeDO result = unitTypeLogic.checkDeleteUnitType(setupUnitTypeDO.getUnitTypeUuid());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(setupUnitTypeDO.getUnitTypeUuid(), result.getUnitTypeUuid());
    }

    @Test
    void testDeleteUnitType() {
        Mockito.when(unitTypeDAO.removeById(setupUnitTypeDO.getUnitTypeUuid())).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> unitTypeLogic.deleteUnitType(setupUnitTypeDO));
    }

@Test
void testGetPageOfUnitType() {
    // Create mock data
    com.baomidou.mybatisplus.extension.plugins.pagination.Page<UnitTypeDO> page =
        new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();

    List<UnitTypeDO> records = new ArrayList<>();
    records.add(setupUnitTypeDO);
    page.setRecords(records);
    page.setTotal(1);

    // Mock DAO call
    Mockito.when(unitTypeDAO.getPageOfUnitType(Mockito.anyInt(), Mockito.anyInt(),
        Mockito.anyBoolean(), Mockito.anyString())).thenReturn(page);

    // Test the method
    PageDTO<UnitTypeDTO> result = unitTypeLogic.getPageOfUnitType(1, 10, true, "test");

    // Verify results
    Assertions.assertNotNull(result);
    Assertions.assertEquals(1, result.getTotal());
    Assertions.assertEquals(1, result.getRecords().size());
    Assertions.assertEquals(setupUnitTypeDO.getName(), result.getRecords().get(0).getName());
}

    @Test
    void testGetUnitTypeList() {
        List<UnitTypeLiteDTO> unitTypeLiteDTOList = new ArrayList<>();
        unitTypeLiteDTOList.add(new UnitTypeLiteDTO());

        Mockito.when(unitTypeDAO.getUnitTypeList()).thenReturn(unitTypeLiteDTOList);

        List<UnitTypeLiteDTO> result = unitTypeLogic.getUnitTypeList();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
    }
}
