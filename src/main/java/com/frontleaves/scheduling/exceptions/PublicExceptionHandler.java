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

package com.frontleaves.scheduling.exceptions;

import com.frontleaves.scheduling.daos.RequestLogDAO;
import com.frontleaves.scheduling.models.entity.RequestLogDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.RequestUtil;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.PublicExceptionHandlerAbstract;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.UnexpectedTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;

/**
 * 公共异常处理器
 * <p>
 * 该类用于处理公共异常;
 * 该类使用 {@link ControllerAdvice} 注解标记;
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class PublicExceptionHandler extends PublicExceptionHandlerAbstract {

    private final RequestLogDAO requestLogDAO;
    private final UserService userService;

    /**
     * 处理 HttpMessageNotReadableException 异常
     * <p>
     * 该方法用于处理在请求体无法读取时抛出的 {@code HttpMessageNotReadableException} 异常。
     * 当请求体格式不正确或无法解析时，此异常会被触发。该方法返回一个包含错误信息的响应实体。
     *
     * @param e 发生的 {@code HttpMessageNotReadableException} 异常实例
     * @return 包含错误码和错误消息的响应实体
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleHttpMessageNotReadableException(@NotNull HttpMessageNotReadableException e) {
        // 记录异常日志
        logException(e, ErrorCode.BODY_ERROR);
        return ResultUtil.error(ErrorCode.BODY_ERROR, "消息不可读", Map.of("message", e.getMessage()));
    }

    /**
     * 处理 UnexpectedTypeException 异常
     * <p>
     * 该方法用于处理在参数类型不符合预期时抛出的 {@code UnexpectedTypeException} 异常。
     * 当请求中的参数类型错误或无法解析时，此异常会被触发。该方法返回一个包含错误信息的响应实体。
     *
     * @param e 发生的 {@code UnexpectedTypeException} 异常实例
     * @return 包含错误码和错误消息的响应实体
     */
    @ExceptionHandler(UnexpectedTypeException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleUnexpectedTypeException(@NotNull UnexpectedTypeException e) {
        // 记录异常日志
        logException(e, ErrorCode.OPERATION_INVALID);
        return ResultUtil.error(ErrorCode.OPERATION_INVALID, "参数类型错误", Map.of("message", e.getMessage()));
    }

    /**
     * 处理 IOException 异常
     * <p>
     * 该方法用于处理文件读写或网络传输等IO操作中抛出的 {@code IOException} 异常。
     * 当文件无法读取、写入或网络连接中断时，此异常会被触发。该方法返回一个包含错误信息的响应实体。
     *
     * @param e 发生的 {@code IOException} 异常实例
     * @return 包含错误码和错误消息的响应实体
     */
    @ExceptionHandler(IOException.class)
    public ResponseEntity<BaseResponse<Map<String, String>>> handleIoException(@NotNull IOException e) {
        log.error("IO异常: {}", e.getMessage(), e);
        // 记录异常日志
        logException(e, ErrorCode.OPERATION_ERROR);
        return ResultUtil.error(ErrorCode.OPERATION_ERROR, "IO操作异常", Map.of("message", e.getMessage()));
    }

    /**
     * 记录异常日志
     * <p>
     * 该方法统一处理异常日志记录逻辑，确保所有通过全局异常处理器捕获的异常都被记录。
     *
     * @param e        异常对象
     * @param errorCode 错误代码
     */
    private void logException(Exception e, ErrorCode errorCode) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();
            RequestLogDO requestLog = new RequestLogDO();

            // 记录请求时间和当前时间
            long currentTime = System.currentTimeMillis();
            requestLog.setRequestTime(new Timestamp(currentTime));
            requestLog.setResponseTime(new Timestamp(currentTime));
            requestLog.setCreatedAt(new Timestamp(currentTime));

            // 计算从请求开始到异常发生的执行时间
            // 注意：这里我们无法准确获取请求开始时间，所以设置为0
            requestLog.setExecutionTime(0L);

            // 设置请求基本信息
            requestLog.setRequestUrl(request.getRequestURI());
            requestLog.setRequestMethod(request.getMethod());
            requestLog.setRequestIp(RequestUtil.getClientIp(request));
            requestLog.setUserAgent(request.getHeader("User-Agent"));

            // 获取用户信息
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer")) {
                UserDO getUser = userService.getUserByRequest(request);
                if (getUser != null) {
                    requestLog.setUserUuid(getUser.getUserUuid());
                }
            }

            // 记录请求参数
            requestLog.setRequestParams(RequestUtil.buildQueryParams(request));

            // 设置响应状态和错误信息
            requestLog.setResponseCode(errorCode.getCode());
            requestLog.setErrorMessage(e.getMessage());

            // 异步保存日志
            requestLogDAO.addRequestLog(requestLog);
        } catch (Exception ex) {
            log.error("记录异常日志时发生错误: {}", ex.getMessage(), ex);
        }
    }
}
