package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.RoleDTO;

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
     * 获取角色列表
     * @param page - 当前页数
     * @param size - 每页显示数量
     * @param isDesc - 是否降序
     * @param search - 搜索关键字
     * @return 角色列表
     */
    PageDTO<RoleDTO> getRoleList(
            Integer page,
            Integer size,
            Boolean isDesc,
            String search);
}
