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

package com.frontleaves.scheduling.configs.aspect;

import com.frontleaves.scheduling.annotations.IgnoreLog;
import com.frontleaves.scheduling.daos.RequestLogDAO;
import com.frontleaves.scheduling.models.entity.base.RequestLogDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.RequestUtil;
import com.xlf.utility.aspect.BusinessLogAspect;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.sql.Timestamp;
import java.util.Arrays;

/**
 * 日志切面
 * <p>
 * 该类用于定义日志切面;
 * 该类使用 {@link Aspect} 注解标记;
 * 该类使用 {@link Component} 注解标记;
 * 该类继承 {@link BusinessLogAspect} 类;
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect extends BusinessLogAspect {

    private final RequestLogDAO requestLogDAO;
    private final UserService userService;

    @Override
    @Before("execution(* com.frontleaves.scheduling.controllers..*.*(..))")
    public void beforeControllerLog(@NotNull JoinPoint joinPoint) {
        if (joinPoint.getTarget().getClass().isAnnotationPresent(IgnoreLog.class)) {
            return;
        }
        try {
            if (joinPoint.getSignature() instanceof MethodSignature signature
                    && signature.getMethod().isAnnotationPresent(IgnoreLog.class)) {
                return;
            }
        } catch (Exception ignored) {
            super.beforeControllerLog(joinPoint);
        }
        super.beforeControllerLog(joinPoint);
    }

    @Override
    @Before("execution(* com.frontleaves.scheduling.logic..*.*(..))")
    public void beforeServiceLog(@NotNull JoinPoint joinPoint) {
        if (joinPoint.getTarget().getClass().isAnnotationPresent(IgnoreLog.class)) {
            return;
        }
        try {
            if (joinPoint.getSignature() instanceof MethodSignature signature
                    && signature.getMethod().isAnnotationPresent(IgnoreLog.class)) {
                return;
            }
        } catch (Exception ignored) {
            super.beforeServiceLog(joinPoint);
        }
        super.beforeServiceLog(joinPoint);
    }

    @Override
    @Around("execution(* com.frontleaves.scheduling.daos..*.*(..))")
    public Object beforeDaoLog(@NotNull ProceedingJoinPoint pjp) throws Throwable {
        if (pjp.getTarget().getClass().isAnnotationPresent(IgnoreLog.class)) {
            return pjp.proceed();
        }
        try {
            if (pjp.getSignature() instanceof MethodSignature signature
                    && signature.getMethod().isAnnotationPresent(IgnoreLog.class)) {
                return pjp.proceed();
            }
        } catch (Exception ignored) {
            return super.beforeDaoLog(pjp);
        }
        return super.beforeDaoLog(pjp);
    }

    @Around("execution(* com.frontleaves.scheduling.controllers..*.*(..))")
    public Object aroundLogRecord(@NotNull ProceedingJoinPoint pjp) throws Throwable {
        // 获取请求信息
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return pjp.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        RequestLogDO requestLog = new RequestLogDO();

        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        Timestamp requestTime = new Timestamp(startTime);
        requestLog.setRequestTime(requestTime);

        // 设置请求基本信息 - 只获取路径，不要完整的URL
        requestLog.setRequestUrl(request.getRequestURI());
        requestLog.setRequestMethod(request.getMethod());
        requestLog.setRequestIp(RequestUtil.getClientIp(request));
        requestLog.setUserAgent(request.getHeader("User-Agent"));

        // 获取用户信息
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            try {
                UserDO getUser = userService.getUserByRequest(request);
                assert getUser != null;
                requestLog.setUserUuid(getUser.getUserUuid());
            } catch (Exception ignored) {}
        }

        // 记录请求参数
        requestLog.setRequestParams(RequestUtil.buildQueryParams(request));
        requestLog.setRequestBody(Arrays.toString(pjp.getArgs()));

        Object result = null;
        try {
            // 执行目标方法
            result = pjp.proceed();
            requestLog.setResponseCode(RequestUtil.extractStatusCode(result));
        } catch (Exception e) {
            // 记录异常信息
            requestLog.setResponseCode(RequestUtil.extractStatusCode(result));
            requestLog.setErrorMessage(e.getMessage());
            throw e;
        } finally {
            // 记录响应时间和执行时间
            long endTime = System.currentTimeMillis();
            requestLog.setResponseTime(new Timestamp(endTime));
            requestLog.setExecutionTime(endTime - startTime);
            requestLog.setCreatedAt(new Timestamp(System.currentTimeMillis()));

            // 异步保存日志
            requestLogDAO.addRequestLog(requestLog);
        }

        return result;
    }
}
