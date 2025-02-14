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

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.lang.reflect.Parameter;

/**
 * 数据处理切面类，用于在带有@DataWrangling注解的方法执行前记录数据访问参数。
 * 该切面通过AOP机制实现在方法调用前自动记录日志，帮助监控和调试数据处理过程中的参数情况。
 *
 * <p>
 * 功能包括：
 * - 在标注了{@link com.frontleaves.scheduling.annotations.DataWrangling}的方法被调用前，
 *   通过日志系统记录下即将被处理的数据参数信息。
 * </p>
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author xiao_lfeng
 */
@Slf4j
@Aspect
@Component
public class DataWranglingAspect {

    /**
     * 在带有@DataWrangling注解的方法执行前，记录方法访问所需的数据参数。
     * 此切面确保在数据处理操作开始前，相关参数会被记录到日志中，便于跟踪和调试。
     *
     * @param joinPoint 连接点对象，提供对当前执行方法的信息访问。
     *                  通过此对象可以获取方法签名、参数等详细信息。
     */
    @Before("@annotation(com.frontleaves.scheduling.annotations.DataWrangling)")
    public void dataReadsBeforeAccess(@NotNull JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Parameter[] parameters = signature.getMethod().getParameters();
        Object[] args = joinPoint.getArgs();
        log.debug("[DATA] 获取 {} 数据参数:", signature.getName());
        for (int i = 0; i < args.length; i++) {
            log.debug("\t> [{}]: {}", parameters[i].getName(), args[i]);
        }
    }
}
