package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.UnitCategoryDTO;
import com.frontleaves.scheduling.models.dto.UnitCategoryLiteDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.vo.UnitCategoryVO;
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
class UnitCategoryLogicTest {
    @Mock
    private UnitCategoryDAO unitCategoryDAO;
    @Mock
    private DepartmentDAO departmentDAO;
    @InjectMocks
    private UnitCategoryLogic unitCategoryLogic;

    private UnitCategoryVO setupUnitCategoryVO;
    private UnitCategoryDO setupUnitCategoryDO;

    @BeforeEach
    void setUp() {
        log.debug("UnitCategoryLogic单元测试初始化");
        setupUnitCategoryVO = new UnitCategoryVO(
                "测试单位类别",
                1,
                "Test Unit Category",
                "TUC",
                true
        );

        setupUnitCategoryDO = new UnitCategoryDO();
        setupUnitCategoryDO.setUnitCategoryUuid(UuidUtil.generateUuidNoDash())
                .setName(setupUnitCategoryVO.getName())
                .setEnglishName(setupUnitCategoryVO.getEnglishName())
                .setShortName(setupUnitCategoryVO.getShortName())
                .setOrder(setupUnitCategoryVO.getOrder())
                .setIsEntity(setupUnitCategoryVO.getIsEntity());
    }

    @Test
    void testCheckAddUnitCategoryVO() {
        // 测试添加新单位类别
        Mockito.when(unitCategoryDAO.getUnitCategoryByName(setupUnitCategoryVO.getName())).thenReturn(null);
        Assertions.assertDoesNotThrow(() -> unitCategoryLogic.checkAddUnitCategoryVO(setupUnitCategoryVO));

        // 测试添加已存在的单位类别
        Mockito.when(unitCategoryDAO.getUnitCategoryByName(setupUnitCategoryVO.getName())).thenReturn(setupUnitCategoryDO);
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitCategoryLogic.checkAddUnitCategoryVO(setupUnitCategoryVO));
        Assertions.assertEquals(ErrorCode.PARAMETER_INVALID, exception.getErrorCode());
    }

    @Test
    void testAddUnitCategory() {
        Mockito.when(unitCategoryDAO.saveUnitCategory(Mockito.any(UnitCategoryDO.class))).thenReturn(setupUnitCategoryDO);
        UnitCategoryDTO result = unitCategoryLogic.addUnitCategory(setupUnitCategoryVO);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(setupUnitCategoryVO.getName(), result.getName());
    }

    @Test
    void testCheckUpdateUnitCategoryVO() {
        // 测试更新不存在的单位类别
        Mockito.when(unitCategoryDAO.getById(Mockito.anyString())).thenReturn(null);
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitCategoryLogic.checkUpdateUnitCategoryVO("non-existent-uuid", setupUnitCategoryVO));
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());

        // 测试更新为已存在的名称
        Mockito.when(unitCategoryDAO.getById(setupUnitCategoryDO.getUnitCategoryUuid())).thenReturn(setupUnitCategoryDO);
        Mockito.when(unitCategoryDAO.getUnitCategoryByNameExceptUuid(setupUnitCategoryVO.getName(), setupUnitCategoryDO.getUnitCategoryUuid()))
                .thenReturn(setupUnitCategoryDO);
        exception = Assertions.assertThrows(BusinessException.class,
                () -> unitCategoryLogic.checkUpdateUnitCategoryVO(setupUnitCategoryDO.getUnitCategoryUuid(), setupUnitCategoryVO));
        Assertions.assertEquals(ErrorCode.PARAMETER_INVALID, exception.getErrorCode());
    }

    @Test
    void testUpdateUnitCategory() {
        Mockito.when(unitCategoryDAO.updateUnitCategory(Mockito.any(UnitCategoryDO.class))).thenReturn(setupUnitCategoryDO);
        UnitCategoryDTO result = unitCategoryLogic.updateUnitCategory(setupUnitCategoryVO, setupUnitCategoryDO);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(setupUnitCategoryVO.getName(), result.getName());
    }

    @Test
    void testCheckDeleteUnitCategory() {
        // 测试删除不存在的单位类别
        Mockito.when(unitCategoryDAO.getById(Mockito.anyString())).thenReturn(null);
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> unitCategoryLogic.checkDeleteUnitCategory("non-existent-uuid"));
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());

        // 测试删除被部门使用的单位类别
        Mockito.when(unitCategoryDAO.getById(setupUnitCategoryDO.getUnitCategoryUuid())).thenReturn(setupUnitCategoryDO);
        Mockito.when(departmentDAO.getDepartmentByUuid(setupUnitCategoryDO.getUnitCategoryUuid())).thenReturn(new DepartmentDO());
        exception = Assertions.assertThrows(BusinessException.class,
                () -> unitCategoryLogic.checkDeleteUnitCategory(setupUnitCategoryDO.getUnitCategoryUuid()));
        Assertions.assertEquals(ErrorCode.OPERATION_INVALID, exception.getErrorCode());

        // 测试正常删除
        Mockito.when(departmentDAO.getDepartmentByUuid(setupUnitCategoryDO.getUnitCategoryUuid())).thenReturn(null);
        UnitCategoryDO result = unitCategoryLogic.checkDeleteUnitCategory(setupUnitCategoryDO.getUnitCategoryUuid());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(setupUnitCategoryDO.getUnitCategoryUuid(), result.getUnitCategoryUuid());
    }

    @Test
    void testDeleteUnitCategory() {
        Mockito.when(unitCategoryDAO.removeById(setupUnitCategoryDO.getUnitCategoryUuid())).thenReturn(true);
        Assertions.assertDoesNotThrow(() -> unitCategoryLogic.deleteUnitCategory(setupUnitCategoryDO));
    }

    @Test
    void testGetPageOfUnitCategory() {
        // Create mock data
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UnitCategoryDO> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();

        List<UnitCategoryDO> records = new ArrayList<>();
        records.add(setupUnitCategoryDO);
        page.setRecords(records);
        page.setTotal(1);

        // Mock DAO call
        Mockito.when(unitCategoryDAO.getPageOfUnitCategory(Mockito.anyInt(), Mockito.anyInt(),
                Mockito.anyBoolean(), Mockito.anyString())).thenReturn(page);

        // Test the method
        PageDTO<UnitCategoryDTO> result = unitCategoryLogic.getPageOfUnitCategory(1, 10, true, "test");

        // Verify results
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotal());
        Assertions.assertEquals(1, result.getRecords().size());
        Assertions.assertEquals(setupUnitCategoryDO.getName(), result.getRecords().get(0).getName());
    }

    @Test
    void testGetUnitCategoryList() {
        List<UnitCategoryLiteDTO> unitCategoryLiteDTOList = new ArrayList<>();
        unitCategoryLiteDTOList.add(new UnitCategoryLiteDTO());

        Mockito.when(unitCategoryDAO.getUnitCategoryList()).thenReturn(unitCategoryLiteDTOList);

        List<UnitCategoryLiteDTO> result = unitCategoryLogic.getUnitCategoryList();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
    }
}
