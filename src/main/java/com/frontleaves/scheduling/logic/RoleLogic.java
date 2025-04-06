package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.RoleDTO;
import com.frontleaves.scheduling.models.dto.lite.RoleLiteDTO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.services.RoleService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 角色服务逻辑
 * @author FLASHLACK
 */
@Service
@RequiredArgsConstructor
public class RoleLogic implements RoleService {
    private final RoleDAO roleDAO;

    @Override
    public RoleDTO getRole(@NotNull String role) {
        RoleDTO roleDTO;
        if (role.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            roleDTO = roleDAO.getRoleByUuid(role);
        } else {
            roleDTO = roleDAO.getRoleByName(role);
        }
        if (roleDTO == null) {
            throw new BusinessException("角色不存在", ErrorCode.NOT_EXIST);
        }
        return roleDTO;
    }

    @Override
    public void checkPageAndSize(Integer page, Integer size) {
        if (page == null || page < 1) {
            throw new BusinessException("页数参数错误", ErrorCode.PARAMETER_ERROR);
        }
        if (size == null || size < 1) {
            throw new BusinessException("每页显示数量参数错误", ErrorCode.PARAMETER_ERROR);
        }
    }

    /**
     * 获取角色分页列表
     * @param page - 当前页数
     * @param size - 每页显示数量
     * @param isDesc - 是否降序
     * @param search - 搜索关键字
     * @return 角色分页列表
     */
    @Override
    public PageDTO<RoleDTO> getRolePage(@NotNull Integer page, @NotNull Integer size, Boolean isDesc, String search) {
        return roleDAO.getRoleDtoPageDTO(page, size, isDesc, search);
    }

    /**
     * 获取角色列表（不分页）
     * @return 角色精简列表，只包含 roleUuid 和 roleName，且只返回 roleStatus 为 1 的角色
     */
    @Override
    @Nullable
    public List<RoleLiteDTO> getRoleList() {
        List<RoleDO> activeRoles = roleDAO.getActiveRoles();
        if (activeRoles == null) {
            return List.of();
        }
        return BeanUtil.copyToList(activeRoles, RoleLiteDTO.class);
    }
}
