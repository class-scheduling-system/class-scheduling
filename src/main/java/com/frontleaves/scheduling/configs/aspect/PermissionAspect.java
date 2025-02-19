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

import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.annotations.RequestPermission;
import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.daos.PermissionDAO;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.daos.TokenDAO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.entity.PermissionDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

/**
 * 权限控制切面类，用于实现系统中的权限验证逻辑。
 * 该切面通过AOP（面向切面编程）技术，动态地在方法执行前后织入权限检查代码，
 * 确保只有具备相应权限的用户或服务才能访问受保护的资源。
 *
 * <p>
 * 注解使用：
 * <ul>
 *     <li>{@link Aspect} 标记该类为一个切面，让Spring识别并应用其切面逻辑。</li>
 *     <li>{@link Component} 将该类作为Spring的一个Bean进行管理，便于自动装配。</li>
 *     <li>{@link RequiredArgsConstructor} Lombok注解，自动生成含有必需参数的构造函数，以配合依赖注入使用。</li>
 * </ul>
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionDAO permissionDAO;
    private final TokenDAO tokenDAO;
    private final RoleDAO roleDAO;

    /**
     * 在方法执行前检查请求的权限。
     * 该方法作为切面逻辑，用于验证调用者是否具备执行特定方法所需的权限。
     *
     * <p>权限检查的流程如下：</p>
     * <ol>
     *     <li>从连接点获取方法签名，并读取 {@link RequestPermission} 注解的值，即所需权限的标识。</li>
     *     <li>根据权限标识查询数据库确认权限存在。</li>
     *     <li>获取当前请求的用户 Token，进而获取用户信息。</li>
     *     <li>验证用户直接拥有的权限中是否包含所需权限。</li>
     *     <li>如用户直接权限不包含，则进一步检查其角色所拥有的权限。</li>
     *     <li>若最终未找到匹配权限，则抛出权限拒绝异常 {@link UserAuthenticationException}。</li>
     * </ol>
     *
     * <p><strong>注意事项：</strong></p>
     * <ul>
     *     <li>确保调用该方法的前置条件是请求上下文已建立，且 {@link RequestPermission} 注解已正确应用在目标方法上。</li>
     *     <li>方法内部实现了详细的权限检查逻辑，包括直接权限和角色继承权限的检查。</li>
     * </ul>
     *
     * @param joinPoint 连接点对象，包含正在执行的方法信息。
     *                 通过此参数可以获取到注解 {@link RequestPermission} 配置的权限标识符。
     *
     * @throws ServerInternalErrorException 如果所需权限在数据库中不存在，抛出此异常。
     * @throws UserAuthenticationException   如果用户未登录或没有足够权限，抛出此异常。
     */
    @Before("@annotation(com.frontleaves.scheduling.annotations.RequestPermission)")
    public void requestPermissionCheck(@NotNull JoinPoint joinPoint) {
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String permission = methodSignature.getMethod().getAnnotation(RequestPermission.class).value();

        // 可以继续后续的权限检查逻辑，如查询数据库等
        PermissionDO foundPermission = permissionDAO.getPermissionKey(permission);
        if (foundPermission == null) {
            throw new ServerInternalErrorException("权限 " + permission + " 不存在，请检查接口" + methodSignature.getName() + "的权限配置");
        }
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String userToken = HeaderUtil.getAuthorizeUserUuidString(request);
        UserDO getUser = tokenDAO.getTokenUser(userToken);
        if (getUser == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_LOGIN, request);
        } else {
            // 获取权限信息
            JSONUtil.toList(JSONUtil.parseArray(getUser.getPermission()), PermissionDO.class).stream()
                    .filter(permissionDO -> permissionDO.getPermissionKey().equals(permission))
                    .findFirst()
                    .ifPresentOrElse(
                            permissionDO -> {},
                            () -> {
                                // 不具备权限获取所在角色
                                RoleDTO roleDTO = roleDAO.getRoleByUuid(getUser.getRoleUuid());
                                assert roleDTO != null;
                                roleDTO.getPermission().stream()
                                        .filter(permissionDO -> {
                                            PermissionDO rolePermissionDO = permissionDAO.getPermissionKey(permission);
                                            return rolePermissionDO != null && rolePermissionDO.getPermissionKey().equals(permission);
                                        })
                                        .findFirst()
                                        .orElseThrow(() -> new UserAuthenticationException(UserAuthenticationException.ErrorType.PERMISSION_DENIED, request));
                            }
                    );
        }
    }

    /**
     * 在方法执行前检查请求的登录状态。
     * 该方法作为切面逻辑，用于验证调用者是否已经登录。
     *
     * <p>登录检查的流程如下：</p>
     * <ol>
     *     <li>从当前请求上下文中获取 {@link HttpServletRequest} 对象。</li>
     *     <li>从请求头中提取用户Token。</li>
     *     <li>通过用户Token查询数据库，获取用户信息。</li>
     *     <li>如果未找到对应用户，则抛出未登录异常 {@link UserAuthenticationException}。</li>
     * </ol>
     *
     * <p><strong>注意事项：</strong></p>
     * <ul>
     *     <li>确保调用该方法的前置条件是请求上下文已建立，且 {@link RequestLogin} 注解已正确应用在目标方法上。</li>
     *     <li>方法内部实现了详细的登录状态检查逻辑。</li>
     * </ul>
     *
     * @throws UserAuthenticationException 如果用户未登录，抛出此异常。
     */
    @Before("@annotation(com.frontleaves.scheduling.annotations.RequestLogin)")
    public void requestLoginCheck() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String userToken = HeaderUtil.getAuthorizeUserUuidString(request);
        UserDO getUser = tokenDAO.getTokenUser(userToken);
        if (getUser == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_LOGIN, request);
        }
    }

    /**
     * 在方法执行前检查请求的角色权限。
     * 该方法作为切面逻辑，用于验证调用者是否具备执行特定方法所需的角色权限。
     *
     * <p>角色权限检查的流程如下：</p>
     * <ol>
     *     <li>从连接点获取方法签名，并读取 {@link RequestRole} 注解的值，即所需角色的标识。</li>
     *     <li>根据角色标识查询数据库确认角色存在。</li>
     *     <li>获取当前请求的用户 Token，进而获取用户信息。</li>
     *     <li>验证用户的角色是否与所需角色匹配。</li>
     *     <li>若用户角色不匹配，则抛出权限拒绝异常 {@link UserAuthenticationException}。</li>
     * </ol>
     *
     * <p><strong>注意事项：</strong></p>
     * <ul>
     *     <li>确保调用该方法的前置条件是请求上下文已建立，且 {@link RequestRole} 注解已正确应用在目标方法上。</li>
     *     <li>方法内部实现了详细的角色权限检查逻辑。</li>
     * </ul>
     *
     * @param joinPoint 连接点对象，包含正在执行的方法信息。
     *                  通过此参数可以获取到注解 {@link RequestRole} 配置的角色标识符。
     *
     * @throws ServerInternalErrorException 如果所需角色在数据库中不存在，抛出此异常。
     * @throws UserAuthenticationException   如果用户未登录或没有足够的角色权限，抛出此异常。
     */
    @Before("@annotation(com.frontleaves.scheduling.annotations.RequestRole)")
    public void requestRoleCheck(@NotNull JoinPoint joinPoint) {
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String role = methodSignature.getMethod().getAnnotation(RequestRole.class).value();

        // 可以继续后续的权限检查逻辑，如查询数据库等
        RoleDTO foundRole = roleDAO.getRoleByName(role);
        if (foundRole == null) {
            throw new ServerInternalErrorException("角色 " + role + " 不存在，请检查接口" + methodSignature.getName() + "的角色配置");
        }
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String userToken = HeaderUtil.getAuthorizeUserUuidString(request);
        UserDO getUser = tokenDAO.getTokenUser(userToken);
        if (getUser == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_LOGIN, request);
        } else {
            // 获取权限信息
            if (!getUser.getRoleUuid().equals(foundRole.getRoleUuid())) {
                throw new UserAuthenticationException(UserAuthenticationException.ErrorType.PERMISSION_DENIED, request);
            }
        }
    }
}
