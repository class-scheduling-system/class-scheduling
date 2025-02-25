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
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.daos.TokenDAO;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 用户逻辑处理类，实现了 {@link UserService} 接口，提供了用户相关的业务逻辑。
 * <p>
 * 该类主要用于处理与用户相关的操作，如根据请求中的Token获取用户信息等。
 * 具体的实现细节包括从请求头中提取Token，并通过Token在数据库中查找对应的用户信息。
 * 如果Token有效且用户存在，则返回用户信息；如果Token无效或用户不存在，则抛出相应的异常。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserLogic implements UserService {
    private final TokenDAO tokenDAO;
    private final RoleDAO roleDAO;
    private final HttpServletRequest request;

    /**
     * 根据请求中的用户Token获取用户信息。
     * <p>
     * 该方法首先从请求头中提取用户Token，然后通过Token在数据库中查找对应的用户信息。
     * 如果Token有效且用户存在，则返回用户信息；如果Token无效或用户不存在，则抛出相应的异常。
     * </p>
     *
     * @param request HTTP请求对象，用于从中提取用户Token
     * @return UserDO 用户信息对象，包含用户的详细信息
     * @throws UserAuthenticationException 如果Token过期或用户不存在时抛出
     */
    @Override
    public UserDO getUserByRequest(HttpServletRequest request) {
        String getUserToken = HeaderUtil.getAuthorizeUserUuidString(request);
        if (getUserToken != null) {
            UserDO getUser = tokenDAO.getTokenUser(getUserToken);
            if (getUser != null) {
                return getUser;
            } else {
                throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
            }
        } else {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.TOKEN_EXPIRED, request);
        }
    }

    /**
     * 根据用户角色聚合三方信息(学生、教师或普通用户)
     *
     * @param userByRequest 当前登录用户实体
     * @return UserInfoDTO聚合后的用户信息
     */
    @Override
    public UserInfoDTO getUserInfoWithRole(UserDO userByRequest) {
        // 校验 roleUuid 是否为空或无效
        String roleUuid = userByRequest.getRoleUuid();
        if (roleUuid == null || roleUuid.trim().isEmpty()) {
            throw new UserAuthenticationException(
                    UserAuthenticationException.ErrorType.TOKEN_EXPIRED,
                    request
            );
        }
        
        // 获取用户的角色信息
        RoleDO role = roleDAO.getRoleByUuid(roleUuid);
        if (role == null) {
            throw new UserAuthenticationException(
                    UserAuthenticationException.ErrorType.USER_NOT_EXIST,
                    request
            );
        }

        // 创建 UserInfoDTO，返回基础的用户信息
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        // 填充基础用户信息
        userInfoDTO.setUser(new UserDTO()
                .setUserUuid(userByRequest.getUserUuid())
                .setName(userByRequest.getName())
                .setEmail(userByRequest.getEmail())
                .setPhone(userByRequest.getPhone())
                .setStatus(userByRequest.getStatus())
                .setBan(userByRequest.getBan())
                // 填充角色信息
                .setRole(new RoleDTO()
                        .setRoleUuid(role.getRoleUuid())
                        .setRoleName(role.getRoleName())
                        .setRoleStatus(role.getRoleStatus())
                        .setPermission(role.getPermission())
                        .setCreatedAt(role.getCreatedAt())
                        .setUpdatedAt(role.getUpdatedAt()))
                .setPermission(userByRequest.getPermission())
                .setCreatedAt(userByRequest.getCreatedAt())
                .setUpdatedAt(userByRequest.getUpdatedAt()));

        // 判断角色类型并填充对应的角色信息
        if ("学生".equals(role.getRoleName())) {
            userInfoDTO.setStudent(new StudentDTO());
        } else if ("教师".equals(role.getRoleName())) {
            userInfoDTO.setTeacher(new TeacherDTO());
        }

        return userInfoDTO;
    }
}
