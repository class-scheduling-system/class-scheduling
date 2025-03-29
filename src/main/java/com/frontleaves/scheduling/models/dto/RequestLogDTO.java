package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 请求日志数据传输对象
 * <p>
 * 用于返回请求日志相关信息，传输的是请求日志的基本信息。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RequestLogDTO {
    /**
     * 请求日志主键
     */
    private String requestLogUuid;

    /**
     * 用户UUID
     */
    private String userUuid;

    /**
     * 请求IP地址
     */
    private String requestIp;

    /**
     * 用户代理信息
     */
    private String userAgent;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求方法(GET/POST等)
     */
    private String requestMethod;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * 请求体
     */
    private String requestBody;

    /**
     * 响应状态码
     */
    private Integer responseCode;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间(毫秒)
     */
    private Long executionTime;

    /**
     * 请求时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp requestTime;

    /**
     * 响应时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp responseTime;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;
}