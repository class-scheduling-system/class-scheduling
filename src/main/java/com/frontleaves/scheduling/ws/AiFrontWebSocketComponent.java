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

package com.frontleaves.scheduling.ws;

import cn.hutool.json.JSONObject;
import com.frontleaves.scheduling.configs.apps.WebSocketConfig;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TokenDAO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.AiService;
import com.frontleaves.scheduling.utils.WsResponseUtil;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 会话管理器
 * <p>
 * 该类用于管理 WebSocket 会话，保存用户ID和会话ID的映射关系。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Component
@EqualsAndHashCode
@NoArgsConstructor
@ServerEndpoint(value = "/ws/ai/response/front", configurator = WebSocketConfig.class)
public class AiFrontWebSocketComponent {
    @Setter
    private static TokenDAO tokenDAO;
    @Setter
    private static AiService aiService;

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     * 虽然@Component默认是单例模式的，但springboot还是会为每个websocket连接初始化一个bean，所以可以用一个静态set保存起来。
     */
    private static final CopyOnWriteArraySet<AiFrontWebSocketComponent> SESSION_MANAGER = new CopyOnWriteArraySet<>();
    /**
     * 用来存在线连接用户信息
     */
    private static final ConcurrentHashMap<String, Session> SESSION_POOL = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 用户ID
     */
    private String userUuid;

    private UserDO user;

    /**
     * 链接建立成功调用的方法
     *
     * @param session WebSocket 会话
     */
    @OnOpen
    public void onOpen(@NotNull Session session) {
        Map<String, List<String>> getAuthorizationToken = session.getRequestParameterMap();
        log.info("{}建立连接 [{}]", LogConstant.WS, session.getRequestURI().toString());
        try {
            if (getAuthorizationToken == null || getAuthorizationToken.isEmpty()) {
                session.getAsyncRemote().sendText(WsResponseUtil.error("Failed", "用户未登录", Map.of()));
                log.info("{}关闭连接: [{}]", LogConstant.WS, "用户令牌为空");
                session.close();
                return;
            }
            String getToken = getAuthorizationToken.get("token").get(0).replace("Bearer ", "");
            if (!getToken.matches(StringConstant.Regular.UUID_REGULAR_EXPRESSION)) {
                session.getAsyncRemote().sendText(WsResponseUtil.error("Failed", "正则表达式不匹配", Map.of()));
                log.info("{}关闭连接: [{}]", LogConstant.WS, "正则表达式不匹配");
                session.close();
                return;
            }
            // token 与用户进行匹配
            if (tokenDAO == null) {
                session.getAsyncRemote().sendText(WsResponseUtil.error("Failed", "服务器内部错误", Map.of()));
                log.info("{}关闭连接: [{}]", LogConstant.WS, "服务器内部错误");
                session.close();
                return;
            }
            UserDO getUser = tokenDAO.getTokenUser(getToken);
            if (getUser == null || getUser.getUserUuid() == null || getUser.getUserUuid().isEmpty()) {
                session.getAsyncRemote().sendText(WsResponseUtil.error("Failed", "用户不存在", Map.of()));
                log.info("{}关闭连接: [{}]", LogConstant.WS, "用户不存在");
                session.close();
                return;
            }
            this.user = getUser;
        } catch (IOException e) {
            log.error("{}建立连接时发生错误: {}", LogConstant.WS, e.getMessage());
        }

        this.session = session;
        this.userUuid = user.getUserUuid();
        SESSION_MANAGER.add(this);
        SESSION_POOL.put(userUuid, session);

        // 发送连接成功消息
        session.getAsyncRemote().sendText(WsResponseUtil.success("Success", "connected", Map.of(
            "message", "连接成功"
        )));
        log.debug("{}建立与[{}]的消息提醒计数连接", LogConstant.WS, user.getName());
    }

    /**
     * 链接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (this.userUuid != null) {
            try {
                Session remove = SESSION_POOL.remove(this.userUuid);
                SESSION_MANAGER.remove(this);
                log.info("{}关闭与{}连接: [{}]", LogConstant.WS, this.userUuid, "常规关闭");
                this.userUuid = null;
                this.session = null;

                remove.close();
            } catch (Exception e) {
                log.warn("{}关闭 Session 出现错误: {}", LogConstant.WS, e.getMessage());
            }
        }
    }

    @OnMessage
    public void onMessage(String message) {
        String userAgent = SESSION_POOL.get(userUuid).getUserProperties().get("user-agent").toString();
        try {
            log.debug("{}接收到消息: [{}]", LogConstant.WS, message);
            JSONObject getJson = new JSONObject(message);
            log.debug("{}接收到消息: [{}]", LogConstant.WS, getJson);
            aiService.sendRouteJump(
                getJson.getStr("user_input"),
                getJson.getStr("html"),
                getJson.getStr("role"),
                getJson.getStr("form"),
                getJson.getStr("other_data"),
                getJson.getStr("record"),
                getJson.getStr("this_page"),
                getJson.getStr("chat"),
                user,
                userAgent
            );
        } catch (Exception e) {
            this.sendMessage(userUuid, WsResponseUtil.error("Failed", "消息处理失败", Map.of(
                "message", e.getMessage()
            )));
            log.error("{}处理消息时发生错误: {}", LogConstant.WS, e.getMessage());
        }
    }

    /**
     * 单人单播消息
     *
     * @param userUuid 用户UUID
     * @param message  消息内容
     */
    public void sendMessage(String userUuid, String message) {
        Session session = SESSION_POOL.get(userUuid);
        if (session != null && session.isOpen()) {
            session.getAsyncRemote().sendText(message);
        }
    }
}
