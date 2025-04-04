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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.dto.UserDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.AiService;
import com.frontleaves.scheduling.services.RoleService;
import com.frontleaves.scheduling.utils.WsResponseUtil;
import com.frontleaves.scheduling.ws.AiFrontWebSocketComponent;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

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
    private final SystemDAO systemDAO;
    private final RoleService roleService;
    private final AiFrontWebSocketComponent aiFrontWebSocketComponent;

    /**
     * 发送路由跳转
     * <p>
     * 该方法用于处理路由跳转请求。
     * </p>
     *
     * @param userInput 用户输入的路由路径
     * @param role      用户角色
     * @param form      表单数据
     * @param otherData 其他数据
     * @param getRecord 记录数据
     * @param thisPage  当前页面
     * @param user      用户对象
     * @param userAgent 用户代理信息
     */
    @Override
    public void sendRouteJump(
            @NotNull String userInput,
            @Nullable String role,
            @Nullable String form,
            @Nullable String otherData,
            @Nullable String getRecord,
            @Nullable String thisPage,
            @Nullable String chat,
            @NotNull UserDO user,
            @NotNull String userAgent
    ) {
        String aiFrontApiKey = systemDAO.getSystemInfo("ai_front_api_key");

        RoleDTO getRole = Optional.of(user)
                .map(data -> roleService.getRole(data.getRoleUuid()))
                .orElseThrow(() -> new BusinessException("用户信息不存在", ErrorCode.NOT_EXIST));

        String requestUrl = UrlBuilder.of()
                .setScheme("http")
                .setHost("172.16.1.6")
                .addPath("/v1")
                .addPath("/workflows/run")
                .build();
        log.info("请求地址: {}", requestUrl);

        HttpResponse response = HttpRequest.post(requestUrl)
                .addHeaders(Map.of(
                        "Authorization", "Bearer " + aiFrontApiKey,
                        "Content-Type", "application/json",
                        "Accept", "text/event-stream",
                        "Accept-Charset", "utf-8",
                        "User-Agent", userAgent))
                .setReadTimeout(30000)
                .body(JSONUtil.toJsonStr(Map.of(
                        "inputs", Map.of(
                                "user_input", userInput,
                                "role", role != null ? role : getRole.getRoleName(),
                                "form", form != null ? form : "",
                                "other_data", otherData != null ? otherData : "",
                                "record", getRecord != null ? getRecord : "",
                                "this_page", thisPage != null ? thisPage : "",
                                "chat", chat != null ? chat : ""),
                        "response_mode", "streaming",
                        "user", "uuid_" + user.getUserUuid() + "_" + System.currentTimeMillis())))
                .executeAsync();

        if (response.getStatus() != 200) {
            response.close();
            throw new BusinessException("请求出现不正确返回", ErrorCode.OPERATION_FAILED,
                    JSONUtil.parse(response.bodyStream()));
        }

        try {
            // 使用 Scanner 处理 SSE 数据流
            try (InputStream inputStream = response.bodyStream();
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {

                // 设置分隔符为换行符
                scanner.useDelimiter("\n\n");

                while (scanner.hasNext()) {
                    String line = scanner.next().trim();

                    // 处理 SSE 数据
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        // 数据解析为 JSON 对象
                        JSONObject getData = JSONUtil.parseObj(data);
                        switch (getData.getStr("event")) {
                            case "node_started":
                                String getStep = getData.getByPath("data.title").toString();
                                aiFrontWebSocketComponent.sendMessage(
                                        user.getUserUuid(),
                                        WsResponseUtil.success("Success", "event", Map.of(
                                                "type", "node_started",
                                                "step", getStep
                                        )));
                                break;
                            case "workflow_finished":
                                String getResult = getData.getByPath("data.outputs.output").toString();
                                aiFrontWebSocketComponent.sendMessage(
                                        user.getUserUuid(),
                                        WsResponseUtil.success("Success", "event", Map.of(
                                                "type", "workflow_finished",
                                                "result", getResult
                                        )));
                                break;
                            default:
                                break;
                        }
                    } else if (line.startsWith("event:")) {
                        // 处理事件类型
                        String event = line.substring(6).trim();
                        log.debug("收到 SSE 事件: {}", event);
                    }

                    // 增加延迟时间，确保WebSocket消息有足够时间处理
                    Thread.sleep(25);
                }
            }
        } catch (Exception e) {
            log.error("处理 SSE 流时发生错误", e);
            aiFrontWebSocketComponent.sendMessage(
                    user.getUserUuid(),
                    WsResponseUtil.error("Failed", "流式内容读取失败", Map.of(
                            "message", e.getMessage()
                    )));
        } finally {
            // 确保响应被关闭
            response.close();
        }
    }

    /**
     * 发送 AI 聊天消息
     * <p>
     * 该方法用于发送 AI 聊天消息。
     * </p>
     *
     * @param userInput 用户输入的消息
     * @param user      用户对象
     * @param userAgent 用户代理信息
     */
    @Override
    public void sendAiChat(String userInput, String chat, UserDO user, String userAgent) {
        String aiMessageApiKey = systemDAO.getSystemInfo("ai_message_api_key");

        UserDTO getUser = BeanUtil.toBean(user, UserDTO.class);
        RoleDTO getRole = Optional.of(user)
                .map(data -> roleService.getRole(data.getRoleUuid()))
                .orElseThrow(() -> new BusinessException("用户信息不存在", ErrorCode.NOT_EXIST));

        String requestUrl = UrlBuilder.of()
                .setScheme("http")
                .setHost("172.16.1.6")
                .addPath("/v1")
                .addPath("/chat-messages")
                .build();
        log.info("请求地址: {}", requestUrl);

        HttpResponse response = HttpRequest.post(requestUrl)
                .addHeaders(Map.of(
                        "Authorization", "Bearer " + aiMessageApiKey,
                        "Content-Type", "application/json",
                        "Accept", "text/event-stream",
                        "Accept-Charset", "utf-8",
                        "User-Agent", userAgent))
                .setReadTimeout(30000)
                .body(JSONUtil.toJsonStr(Map.of(
                        "query", userInput,
                        "inputs", Map.of(
                                "role", getRole.getRoleName(),
                                "user", getUser.toString(),
                                "chat", chat),
                        "response_mode", "streaming",
                        "user", "uuid_" + user.getUserUuid() + "_" + System.currentTimeMillis())
                , JSONConfig.create().setIgnoreNullValue(true).setIgnoreError(true)))
                .executeAsync();

        if (response.getStatus() != 200) {
            response.close();
            log.debug("请求出现不正确返回: {}", response.getStatus());
            throw new BusinessException("请求出现不正确返回", ErrorCode.OPERATION_FAILED, response.bodyStream());
        }

        try {
            // 使用 Scanner 处理 SSE 数据流
            try (InputStream inputStream = response.bodyStream();
                 Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8)) {

                // 设置分隔符为换行符
                scanner.useDelimiter("\n\n");

                while (scanner.hasNext()) {
                    String line = scanner.next().trim();

                    // 处理 SSE 数据
                    if (line.startsWith("data:")) {
                        String data = line.substring(5).trim();
                        // 数据解析为 JSON 对象
                        JSONObject getData = JSONUtil.parseObj(data);
                        switch (getData.getStr("event")) {
                            case "agent_thought":
                                String getThought = getData.getByPath("thought") != null ? getData.getByPath("thought").toString() : "";
                                aiFrontWebSocketComponent.sendMessage(
                                        user.getUserUuid(),
                                        WsResponseUtil.success("Success", "event", Map.of(
                                                "type", "agent_thought",
                                                "thought", getThought
                                        )));
                                break;
                            case "agent_message":
                                String getMessage = getData.getByPath("answer") != null ? getData.getByPath("answer").toString() : "";
                                aiFrontWebSocketComponent.sendMessage(
                                        user.getUserUuid(),
                                        WsResponseUtil.success("Success", "event", Map.of(
                                                "type", "agent_message",
                                                "message", getMessage
                                        )));
                                break;
                            case "message_end":
                                String getMetadata = getData.getByPath("conversation_id") != null ? getData.getByPath("conversation_id").toString() : "";
                                aiFrontWebSocketComponent.sendMessage(
                                        user.getUserUuid(),
                                        WsResponseUtil.success("Success", "event", Map.of(
                                                "type", "message_end",
                                                "metadata", getMetadata
                                        )));
                                break;
                            default:
                                break;
                        }
                    } else if (line.startsWith("event:")) {
                        // 处理事件类型
                        String event = line.substring(6).trim();
                        log.debug("收到 SSE 事件: {}", event);
                    }

                    // 增加延迟时间，确保WebSocket消息有足够时间处理
                    Thread.sleep(50);
                }
            }
        } catch (Exception e) {
            log.error("处理 SSE 流时发生错误", e);
        } finally {
            response.close();
        }
    }
}
