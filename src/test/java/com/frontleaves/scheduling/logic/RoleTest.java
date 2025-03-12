package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.dto.RoleLiteDTO;
import com.frontleaves.scheduling.services.RoleService;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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
    
    @Test
    void testGetRoleListNotEmpty() {
        List<RoleLiteDTO> roleList = roleService.getRoleList();
        log.debug("role-list: {}", roleList);
        Assertions.assertNotNull(roleList, "角色列表不应为空");
        Assertions.assertFalse(roleList.isEmpty(), "角色列表应至少包含一个角色");
    }
    
    @Test
    void testGetRoleListContainsOnlyRequiredFields() {
        List<RoleLiteDTO> roleList = roleService.getRoleList();
        
        for (RoleLiteDTO role : roleList) {
            // 确保每个角色都有UUID和名称
            Assertions.assertNotNull(role.getRoleUuid(), "角色UUID不应为空");
            Assertions.assertNotNull(role.getRoleName(), "角色名称不应为空");
            
            // 通过反射确认对象只有两个字段
            Assertions.assertEquals(2, role.getClass().getDeclaredFields().length,
                    "RoleLiteDTO应该只包含roleUuid和roleName两个字段");
        }
    }
    
    @Test
    void testGetRoleListWithFullRoleInfo() {
        List<RoleLiteDTO> roleListLite = roleService.getRoleList();
        
        // 随机选择一个角色，通过UUID获取完整信息进行比较
        if (!roleListLite.isEmpty()) {
            RoleLiteDTO roleLite = roleListLite.get(0);
            RoleDTO fullRole = roleService.getRole(roleLite.getRoleUuid());
            
            // 验证精简DTO中的信息与完整DTO一致
            Assertions.assertEquals(roleLite.getRoleUuid(), fullRole.getRoleUuid(), "UUID应一致");
            Assertions.assertEquals(roleLite.getRoleName(), fullRole.getRoleName(), "角色名称应一致");
            
            // 验证完整DTO中的状态为有效
            Assertions.assertEquals(true, fullRole.getRoleStatus(), "角色状态应为有效(1)");
        }
    }
}
