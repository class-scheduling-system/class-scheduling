package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.RoleDTO;
import com.frontleaves.scheduling.models.dto.lite.RoleLiteDTO;

import java.util.List;

/**
 * 角色服务接口
 * @author FLASHLACK
 */
public interface RoleService {
    /**
     * 获取角色信息
     * @param role 角色ID或角色名称
     * @return 角色详细信息
     */
    RoleDTO getRole(String role);

    /**
     * 获取角色列表
     * @param page 当前页数
     * @param size 每页显示数量
     */
    void checkPageAndSize(
            Integer page,
            Integer size);

    /**
     * 获取角色分页列表
     * @param page - 当前页数
     * @param size - 每页显示数量
     * @param isDesc - 是否降序
     * @param search - 搜索关键字
     * @return 角色分页列表
     */
    PageDTO<RoleDTO> getRolePage(
            Integer page,
            Integer size,
            Boolean isDesc,
            String search);

    /**
     * 获取角色列表（不分页）
     * @return 角色精简列表，只包含 roleUuid 和 roleName，且只返回 roleStatus 为 1 的角色
     */
    List<RoleLiteDTO> getRoleList();
}
