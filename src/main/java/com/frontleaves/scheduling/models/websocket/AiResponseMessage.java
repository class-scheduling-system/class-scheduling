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

package com.frontleaves.scheduling.models.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 响应消息
 * <p>
 * 该类用于封装 AI 响应的消息，通过 WebSocket 发送给客户端。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiResponseMessage {
    
    /**
     * 消息类型
     * <p>
     * 可能的值:
     * - "content": 内容片段
     * - "completed": 完成标记
     * - "error": 错误信息
     * </p>
     */
    private String type;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 是否是流式响应的结束
     */
    private boolean isEnd;
    
    /**
     * 创建内容消息
     *
     * @param content 响应内容
     * @param isEnd   是否是流式响应的结束
     * @return 返回 AI 响应消息对象
     */
    public static AiResponseMessage content(String content, boolean isEnd) {
        return new AiResponseMessage("content", content, isEnd);
    }
    
    /**
     * 创建完成消息
     *
     * @return 返回 AI 响应完成消息对象
     */
    public static AiResponseMessage completed() {
        return new AiResponseMessage("completed", "AI 响应已完成", true);
    }
    
    /**
     * 创建错误消息
     *
     * @param errorMessage 错误信息
     * @return 返回 AI 响应错误消息对象
     */
    public static AiResponseMessage error(String errorMessage) {
        return new AiResponseMessage("error", errorMessage, true);
    }
}