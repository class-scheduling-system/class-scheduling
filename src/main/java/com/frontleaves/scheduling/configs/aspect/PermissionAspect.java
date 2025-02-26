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

package com.frontleaves.scheduling.configs.aspect;

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.entity.PermissionDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.PermissionService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.Objects;

/**
 * 权限切面类
 * <p>
 * 该类提供了基于 AOP 的权限检查功能，包括角色和权限的验证。通过使用 {@code @Aspect} 和 {@code @Component} 注解，
 * 使得该类可以作为 Spring 框架中的一个切面组件运行。主要依赖于 {@link RoleDAO}、{@link UserService} 和 {@link PermissionService}
 * 这三个服务来实现具体的权限管理逻辑。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final RoleDAO roleDAO;
    private final UserService userService;
    private final PermissionService permissionService;

    /**
     * 检查用户是否具有指定的权限
     * <p>
     * 该方法用于验证给定的用户是否拥有特定的权限。首先，它会通过 {@code permissionDAO} 从数据库中获取指定的权限信息。
     * 如果该权限不存在，则抛出 {@link ServerInternalErrorException} 异常。接着，将用户的权限字符串解析为一个包含 {@link PermissionDO} 对象的列表，
     * 并检查该列表中是否存在与所需权限键匹配的对象。
     * </p>
     *
     * @param permission 需要检查的权限标识符
     * @param getUser    用户实体对象，包含用户的权限信息
     * @return 如果用户拥有指定的权限，则返回 {@code true}；否则返回 {@code false}
     * @throws ServerInternalErrorException 如果指定的权限在数据库中不存在
     */
    private boolean checkPermission(String permission, UserDO getUser) {
        return permissionService.checkPermission(permission, getUser);
    }

    /**
     * 检查请求是否已登录
     * <p>
     * 该方法作为切面逻辑，在带有 {@code @RequestLogin} 注解的方法执行前检查当前请求是否已登录。
     * 具体步骤如下：
     * </p>
     * <ol>
     *     <li>从当前请求上下文中获取 {@link HttpServletRequest} 对象。</li>
     *     <li>通过 {@link UserService#getUserByRequest(HttpServletRequest)} 方法根据请求获取用户信息。</li>
     *     <li>如果用户信息为空，则抛出 {@link UserAuthenticationException} 异常，表示用户未登录。</li>
     * </ol>
     * <p><strong>注意事项：</strong></p>
     * <ul>
     *     <li>确保调用该方法的前置条件是请求上下文已建立，且 {@code @RequestLogin} 注解已正确应用在目标方法上。</li>
     *     <li>方法内部实现了对用户登录状态的检查。</li>
     * </ul>
     *
     * @throws UserAuthenticationException 如果用户未登录，则抛出此异常。
     */
    @Before("@annotation(com.frontleaves.scheduling.annotations.RequestLogin)")
    public void requestLoginCheck() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        UserDO getUser = userService.getUserByRequest(request);
        if (getUser == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_LOGIN, request);
        }
        log.debug(LogConstant.ASPECT + "用户登录检查通过，用户信息：{}", getUser);
    }

    /**
     * 在方法执行前检查请求的角色与权限。
     * <p>
     * 该方法作为切面逻辑，用于验证调用者是否具备执行特定方法所需的角色和权限。具体步骤如下：
     * </p>
     * <ol>
     *     <li>从连接点获取方法签名，并读取 {@link RequestRole} 注解的值，即所需角色和权限的标识。</li>
     *     <li>根据角色标识查询数据库确认角色存在。</li>
     *     <li>获取当前请求的用户信息。</li>
     *     <li>如果权限标识为空，则继续检查角色；否则检查用户是否具有指定的权限。</li>
     *     <li>验证用户直接拥有的角色中是否包含所需角色。</li>
     *     <li>若最终未找到匹配角色或权限，则抛出权限拒绝异常 {@link UserAuthenticationException}。</li>
     * </ol>
     * <p><strong>注意事项：</strong></p>
     * <ul>
     *     <li>确保调用该方法的前置条件是请求上下文已建立，且 {@link RequestRole} 注解已正确应用在目标方法上。</li>
     *     <li>方法内部实现了详细的权限检查逻辑，包括直接角色和权限的检查。</li>
     * </ul>
     *
     * @param joinPoint 连接点对象，包含正在执行的方法信息。
     *                  通过此参数可以获取到注解 {@link RequestRole} 配置的角色和权限标识符。
     * @throws ServerInternalErrorException 如果所需角色在数据库中不存在，抛出此异常。
     * @throws UserAuthenticationException  如果用户未登录或没有足够权限，抛出此异常。
     */
    @Before("@annotation(com.frontleaves.scheduling.annotations.RequestRole)")
    public void requestRoleCheck(@NotNull JoinPoint joinPoint)
            throws ServerInternalErrorException, UserAuthenticationException {
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        List<String> role = List.of(methodSignature.getMethod().getAnnotation(RequestRole.class).value());
        String permission = methodSignature.getMethod().getAnnotation(RequestRole.class).permission();

        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        UserDO getUser = userService.getUserByRequest(request);

        // 检查角色
        if (!permission.isEmpty() && this.checkPermission(permission, getUser)) {
            log.debug(LogConstant.ASPECT + "用户权限检查通过，所需权限：{}", getUser.getPermission());
            return;
        }

        // 可以继续后续的权限检查逻辑，如查询数据库等
        List<RoleDTO> roleNameList = role.stream().map(roleDAO::getRoleByName)
                .filter(Objects::nonNull)
                .toList();
        if (role.size() != roleNameList.size()) {
            role.stream()
                    .filter(roleName -> roleNameList.stream().noneMatch(roleDTO -> roleDTO.getRoleName().equals(roleName)))
                    .findFirst()
                    .ifPresent(roleName -> {
                        throw new ServerInternalErrorException("角色 " + roleName + " 不存在，请检查接口" + methodSignature.getName() + "的角色配置");
                    });
        }
        roleNameList.stream()
                .filter(roleDTO -> getUser != null && getUser.getRoleUuid().equals(roleDTO.getRoleUuid()))
                .findFirst()
                .orElseThrow(() -> new UserAuthenticationException(UserAuthenticationException.ErrorType.PERMISSION_DENIED, request));
        log.debug(LogConstant.ASPECT + "用户角色检查通过，所需角色：{}", role);
    }
}
