package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.services.RoleService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
