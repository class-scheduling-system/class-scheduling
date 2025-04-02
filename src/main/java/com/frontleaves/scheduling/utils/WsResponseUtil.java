package com.frontleaves.scheduling.utils;

import cn.hutool.json.JSONUtil;

import java.util.Map;

/**
 * WsResponseUtil
 * <p>
 * WebSocket 响应工具类
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public class WsResponseUtil {

    private WsResponseUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static String success(String output, String type, Map<String, Object> data) {
        return JSONUtil.toJsonStr(
                Map.of(
                        "output", output,
                        "success", true,
                        "type", type,
                        "data", data
                )
        );
    }

    public static String error(String output, String errorMessage, Map<String, Object> data) {
        return JSONUtil.toJsonStr(
                Map.of(
                        "output", output,
                        "success", false,
                        "error_message", errorMessage,
                        "type", "error",
                        "data", data
                )
        );
    }

}
