package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.AcademicAffairsPermissionDAO;
import com.frontleaves.scheduling.models.dto.base.AcademicAffairsPermissionDTO;
import com.frontleaves.scheduling.models.entity.base.AcademicAffairsPermissionDO;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 教务权限逻辑测试类
 *
 * @author FLASHLACK
 */
@SpringBootTest
class AcademicAffairsPermissionTest {

    @Resource
    private AcademicAffairsPermissionLogic academicAffairsPermissionLogic;

    @Resource
    private AcademicAffairsPermissionDAO academicAffairsPermissionDAO;

    /**
     * 测试获取教务权限 - 正常情况
     */
    @Test
    void testGetAcademicAffairsPermission_Success() {
        // 准备测试数据
        AcademicAffairsPermissionDO academicAffairsPermissionDO =
                academicAffairsPermissionDAO.lambdaQuery().list().get(0);
        String userUuid = academicAffairsPermissionDO.getAuthorizedUser();
        // 执行测试
        AcademicAffairsPermissionDTO result = academicAffairsPermissionLogic.getAcademicAffairsPermission(userUuid);
        Assertions.assertNotNull(result);
    }

    /**
     * 测试获取教务权限 - 权限不存在的情况
     */
    @Test
    void testGetAcademicAffairsPermission_NotExist() {
        // 准备测试数据
        String userUuid = UuidUtil.generateUuidNoDash();

        // 执行测试并验证异常
        Assertions.assertThrows(
                BusinessException.class,
                () -> academicAffairsPermissionLogic.getAcademicAffairsPermission(userUuid)
        );
    }
}
