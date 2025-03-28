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

package com.frontleaves.scheduling.utils;

import cn.hutool.core.util.URLUtil;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 请求工具类
 * <p>
 * 该类提供了一些用于处理请求的工具方法，如提取响应状态码、构建查询参数等。
 * </p>
 * <p>
 * 该类中的方法均为静态方法，可以直接通过类名调用。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
public class RequestUtil {

    private RequestUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 从响应结果中提取状态码
     *
     * @param result 响应结果
     * @return 状态码
     */
    public static int extractStatusCode(Object result) {
        if (result == null) {
            return 500;
        }
        try {
            if (result instanceof ResponseEntity<?> responseEntity) {
                return responseEntity.getStatusCode().value();
            }
        } catch (Exception e) {
            log.warn("[LOG] 提取响应状态码时发生异常: {}", e.getMessage());
        }
        return 200;
    }

    /**
     * 构建查询参数字符串为JSON格式
     *
     * @param request HTTP请求
     * @return 查询参数的JSON字符串
     */
    public static String buildQueryParams(@NotNull HttpServletRequest request) {
        try {
            if (request.getQueryString() != null) {
                // 使用Hutool解析查询字符串为Map
                Map<String, String> queryMap = Arrays.stream(request.getQueryString().split("&"))
                        .map(param -> param.split("="))
                        .collect(Collectors.toMap(
                                param -> URLUtil.decode(param[0]),
                                param -> URLUtil.decode(param[1])
                        ));
                return JSONUtil.toJsonStr(queryMap);
            }

            // 如果没有查询字符串，从参数构建Map
            Map<String, String> paramMap = new HashMap<>();
            Collections.list(request.getParameterNames()).forEach(name ->
                    paramMap.put(name, request.getParameter(name))
            );
            return cn.hutool.json.JSONUtil.toJsonStr(paramMap);
        } catch (Exception e) {
            log.warn("[LOG] 构建查询参数时发生异常: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    public static String getClientIp(@NotNull HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || StringConstant.Common.Hump.UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || StringConstant.Common.Hump.UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || StringConstant.Common.Hump.UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || StringConstant.Common.Hump.UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || StringConstant.Common.Hump.UNKNOWN.equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
