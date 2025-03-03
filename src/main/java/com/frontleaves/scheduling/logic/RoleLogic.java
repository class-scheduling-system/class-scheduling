package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.services.RoleService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
     * 获取角色列表
     * @param page - 当前页数
     * @param size - 每页显示数量
     * @param isDesc - 是否降序
     * @param search - 搜索关键字
     * @return 角色列表
     */
    @Override
    public PageDTO<RoleDTO> getRoleList(@NotNull Integer page, @NotNull Integer size, Boolean isDesc, String search) {
        return roleDAO.getRoleDtoPageDTO(page, size, isDesc, search);
    }
}
