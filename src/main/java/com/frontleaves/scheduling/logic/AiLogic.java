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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.services.AiService;
import com.frontleaves.scheduling.services.RoleService;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.ws.WebSocketSessionManager;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 逻辑处理类
 * <p>
 * 该类用于处理与 AI 相关的业务逻辑。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiLogic implements AiService {
    private final UserService userService;
    private final RoleService roleService;
    private final WebSocketSessionManager webSocketSessionManager;

    // 创建线程池处理 AI 流式响应
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    /**
     * 发送路由跳转
     * <p>
     * 该方法用于处理路由跳转请求。
     * </p>
     *
     * @param userInput 用户输入的路由路径
     * @param html      HTML 内容
     * @param request   HTTP 请求对象
     */
    @Override
    public void sendRouteJump(String userInput, String html, @NotNull HttpServletRequest request) {
        String aiFrontApiKey = /* systemDAO.getSystemInfo("ai_front_api_key"); */ "app-PI6ZLJbaYnwDQBllEI50cP8c";

        RoleDTO getRole = Optional.ofNullable(userService.getUserByRequest(request))
                .map(data -> roleService.getRole(data.getRoleUuid()))
                .orElseThrow(() -> new BusinessException("用户信息不存在", ErrorCode.NOT_EXIST));

        String requestUrl = UrlBuilder.of()
                .setScheme("http")
                .setHost("ai.x-lf.com")
                .addPath("/v1")
                .addPath("/workflows/run")
                .build();
        log.info("请求地址: {}", requestUrl);

        HttpResponse response = HttpRequest.post(requestUrl)
                .addHeaders(Map.of(
                        "Authorization", "Bearer " + aiFrontApiKey,
                        "Content-Type", "application/json",
                        "Accept", "application/json",
                        "Accept-Charset", "utf-8",
                        "User-Agent", request.getHeader("User-Agent")
                ))
                .body(JSONUtil.toJsonStr(Map.of(
                        "inputs", Map.of(
                                "user_input", userInput,
                                "html", html != null ? HtmlUtil.escape(html) : "",
                                "role", getRole.getRoleName()
                        ),
                        "response_mode", "streaming",
                        "user", "uuid_" + userService.getUserByRequest(request).getUserUuid() + "_" + System.currentTimeMillis()
                )))
                .execute();
        if (response.getStatus() != 200) {
            response.close();
            throw new BusinessException("请求出现不正确返回", ErrorCode.OPERATION_FAILED, JSONUtil.parse(response.body()));
        }

        response.close();
    }
}
