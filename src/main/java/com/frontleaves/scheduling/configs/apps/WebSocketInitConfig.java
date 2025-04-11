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

package com.frontleaves.scheduling.configs.apps;

import com.frontleaves.scheduling.daos.TokenDAO;
import com.frontleaves.scheduling.services.AiService;
import com.frontleaves.scheduling.ws.AiFrontWebSocketComponent;
import jakarta.websocket.server.ServerContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

/**
 * WebSocket 初始化配置类
 * <p>
 * 该类用于初始化 WebSocket 组件，设置用户服务和令牌数据访问对象。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Order(2)
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketInitConfig {
    private final TokenDAO tokenDAO;
    private final AiService aiService;

    /**
     * 创建 WebSocket 组件
     * <p>
     * 创建一个新的 {@link AiFrontWebSocketComponent} 实例，并设置用户服务和令牌数据访问对象。
     * </p>
     *
     * @return {@link AiFrontWebSocketComponent} 实例
     */
    @Bean
    public AiFrontWebSocketComponent webSocketComponent() {
        AiFrontWebSocketComponent webSocket = new AiFrontWebSocketComponent();
        AiFrontWebSocketComponent.setTokenDAO(tokenDAO);
        AiFrontWebSocketComponent.setAiService(aiService);
        return webSocket;
    }

    /**
     * 自定义 Tomcat 容器，设置 WebSocket 缓冲区大小
     *
     * @return TomcatContextCustomizer 实例
     */
    @Bean
    public TomcatContextCustomizer tomcatContextCustomizer() {
        return context -> {
            context.addServletContainerInitializer((c, ctx) -> {
                ServerContainer container = (ServerContainer) ctx.getAttribute(ServerContainer.class.getName());
                if (container != null) {
                    // 设置WebSocket缓冲区大小为512KB
                    container.setDefaultMaxTextMessageBufferSize(524288);
                    container.setDefaultMaxBinaryMessageBufferSize(524288);
                    log.info("配置WebSocket消息缓冲区大小为: 512KB");
                }
            }, null);
        };
    }
}
