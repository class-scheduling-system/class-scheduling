package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.services.RoleService;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class RoleTest {
    @Resource
    private RoleService roleService;

    @Test
    void testGetRoleWithUuid() {
        RoleDTO role = roleService.getRole(SystemConstant.getRoleAdmin());
        log.debug("role-Uuid: {}", role);
        Assertions.assertNotNull(role);
    }

    @Test
    void testGetRoleWithName() {
        RoleDTO role = roleService.getRole("学生");
        log.debug("role-Name: {}", role);
        Assertions.assertNotNull(role);
    }

    @Test
    void testGetRoleNoMatching() {
        Assertions.assertThrows(BusinessException.class, () -> roleService.getRole("not-exist"));
    }

    @Test
    void testCheckPageAndSizeNotThrow() {
        Assertions.assertDoesNotThrow(() -> roleService.checkPageAndSize(1, 10));
    }

    @Test
    void testCheckPageAndSizeThrow() {
        Assertions.assertThrows(BusinessException.class, () -> roleService.checkPageAndSize(0, 10));
    }

}
