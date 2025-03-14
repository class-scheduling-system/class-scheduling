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

package com.frontleaves.scheduling.exceptions;

import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.PublicExceptionHandlerAbstract;
import jakarta.validation.UnexpectedTypeException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.IOException;
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
public class PublicExceptionHandler extends PublicExceptionHandlerAbstract {

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
    public ResponseEntity<BaseResponse<Map<String, String>>> handleIOException(@NotNull IOException e) {
        log.error("IO异常: {}", e.getMessage(), e);
        return ResultUtil.error(ErrorCode.OPERATION_ERROR, "IO操作异常", Map.of("message", e.getMessage()));
    }
}
