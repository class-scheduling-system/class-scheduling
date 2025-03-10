/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色测试类
 * <p>
 * 该类用于测试角色相关功能;
 * </p>
 *
 * @version v1.0.0
 * @since v1.0.0
 */
@SpringBootTest
@RequiredArgsConstructor
class RoleTest {
    @Resource
    private RoleDAO roleDAO;

    @Test
    void testGetRoleByName() {
        // 测试角色
        RoleDTO roleDO = roleDAO.getRoleByName("管理员");
        Assertions.assertNotNull(roleDO);
    }

    @Test
    void testGetRoleByUuid() {
        RoleDO roleDO = roleDAO.lambdaQuery().eq(RoleDO::getRoleName, "管理员").one();
        assert roleDO != null;
        // 测试角色
        RoleDTO getRole = roleDAO.getRoleByUuid(roleDO.getRoleUuid());
        Assertions.assertNotNull(getRole);
    }

    @Test
    void testGetNullRoleByUuid() {
        // 测试角色
        RoleDTO getRole = roleDAO.getRoleByUuid("null");
        Assertions.assertNull(getRole);
    }

    @Test
    void testGetRoleDtoPageDTOAsc() {
        // 测试角色
        PageDTO<RoleDTO> roleDTOPageDTO = roleDAO.getRoleDtoPageDTO(1, 10, false, "");
        Assertions.assertNotNull(roleDTOPageDTO);
        Assertions.assertTrue(roleDTOPageDTO.getTotal() >= 1, "总页数应该大于等于 1");
        // 判断是否为正序（根据 createdAt 字段）
        List<RoleDTO> content = roleDTOPageDTO.getRecords();
        for (int i = 0; i < content.size() - 1; i++) {
            Timestamp currentTimestamp = content.get(i).getCreatedAt();
            Timestamp nextTimestamp = content.get(i + 1).getCreatedAt();
            // 将 Timestamp 转换为 LocalDateTime
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            // 检查当前项的 createdAt 是否严格大于下一项的 createdAt
            Assertions.assertFalse(currentCreatedAt.isAfter(nextCreatedAt),
                    "数据应该按正序排列，当前数据顺序错误");
        }
    }

    @Test
    void testGetRoleDtoPageDTODesc() {
        // 测试角色
        PageDTO<RoleDTO> roleDTOPageDTO = roleDAO.getRoleDtoPageDTO(1, 10, true, "");
        Assertions.assertNotNull(roleDTOPageDTO);
        Assertions.assertTrue(roleDTOPageDTO.getTotal() >= 1, "总页数应该大于等于 1");
        // 判断是否为正序（根据 createdAt 字段）
        List<RoleDTO> content = roleDTOPageDTO.getRecords();
        for (int i = 0; i < content.size() - 1; i++) {
            Timestamp currentTimestamp = content.get(i).getCreatedAt();
            Timestamp nextTimestamp = content.get(i + 1).getCreatedAt();
            // 将 Timestamp 转换为 LocalDateTime
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            // 检查当前项的 createdAt 是否严格大于下一项的 createdAt
            Assertions.assertFalse(currentCreatedAt.isBefore(nextCreatedAt),
                    "数据应该按降序排列，当前数据顺序错误");
        }
    }

    @Test
    void testGetRoleDtoPageDTOAscBySearch() {
        // 测试角色
        String search = "管理员";
        PageDTO<RoleDTO> roleDTOPageDTO = roleDAO.getRoleDtoPageDTO(1, 10, true, search);
        Assertions.assertNotNull(roleDTOPageDTO);
        Assertions.assertTrue(roleDTOPageDTO.getTotal() >= 1, "总页数应该大于等于 1");
        for (RoleDTO roleDTO : roleDTOPageDTO.getRecords()) {
            // 断言用记录中包含关键字
            boolean containsKeyword = roleDTO.getRoleName().contains(search);
            Assertions.assertTrue(containsKeyword, "应包含关键字");
        }
        // 判断是否为正序（根据 createdAt 字段）
        List<RoleDTO> content = roleDTOPageDTO.getRecords();
        for (int i = 0; i < content.size() - 1; i++) {
            Timestamp currentTimestamp = content.get(i).getCreatedAt();
            Timestamp nextTimestamp = content.get(i + 1).getCreatedAt();
            // 将 Timestamp 转换为 LocalDateTime
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            // 检查当前项的 createdAt 是否严格大于下一项的 createdAt
            Assertions.assertFalse(currentCreatedAt.isAfter(nextCreatedAt),
                    "数据应该按正序排列，当前数据顺序错误");
        }
    }

    @Test
    void testGetActiveRoles() {
        // 测试获取有效角色列表
        List<RoleDO> activeRoles = roleDAO.getActiveRoles();
        Assertions.assertNotNull(activeRoles);
        Assertions.assertFalse(activeRoles.isEmpty(), "有效角色列表不应为空");

        // 验证所有角色的状态都为 1
        for (RoleDO roleDO : activeRoles) {
            Assertions.assertNotNull(roleDO.getRoleUuid(), "角色UUID不应为空");
            Assertions.assertNotNull(roleDO.getRoleName(), "角色名称不应为空");
            Assertions.assertEquals(1, roleDO.getRoleStatus(), "角色状态应为有效(1)");
        }
    }

    @Test
    void testGetActiveRolesExcludesInactiveRoles() {
        // 获取所有角色（包括未激活的）
        List<RoleDO> allRoles = roleDAO.lambdaQuery().list();

        // 获取激活角色列表
        List<RoleDO> activeRoles = roleDAO.getActiveRoles();

        // 确保任何状态为0的角色都没有出现在结果中
        long inactiveRolesCount = allRoles.stream()
                .filter(role -> role.getRoleStatus() == 0)
                .count();

        // 如果存在未激活角色，验证它们是否被排除
        if (inactiveRolesCount > 0) {
            // 获取所有未激活角色的UUID
            List<String> inactiveRoleUuids = allRoles.stream()
                    .filter(role -> role.getRoleStatus() == 0)
                    .map(RoleDO::getRoleUuid)
                    .toList();

            // 确保激活角色列表中没有未激活角色的UUID
            if (activeRoles != null) {
                for (RoleDO activeRole : activeRoles) {
                    Assertions.assertFalse(inactiveRoleUuids.contains(activeRole.getRoleUuid()),
                            "未激活角色不应出现在列表中");
                }
            }
        }
    }
}
