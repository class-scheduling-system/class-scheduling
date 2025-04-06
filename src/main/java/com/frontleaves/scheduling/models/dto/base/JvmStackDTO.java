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

package com.frontleaves.scheduling.models.dto.base;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * JVM 堆栈信息数据传输对象
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
public class JvmStackDTO {
    /**
     * JVM 总内存（字节）
     */
    private Long totalMemory;

    /**
     * JVM 已用内存（字节）
     */
    private Long usedMemory;

    /**
     * JVM 最大内存（字节）
     */
    private Long maxMemory;

    /**
     * JVM 空闲内存（字节）
     */
    private Long freeMemory;

    /**
     * 系统属性
     */
    private Map<String, String> systemProperties;

    /**
     * 活动线程数
     */
    private Integer activeThreadCount;

    /**
     * 线程堆栈信息
     */
    private List<ThreadInfo> threadInfos;

    /**
     * 线程信息内部类
     */
    @Data
    public static class ThreadInfo {
        /**
         * 线程名称
         */
        private String threadName;

        /**
         * 线程状态
         */
        private String threadState;

        /**
         * 线程优先级
         */
        private Integer priority;

        /**
         * 是否为守护线程
         */
        private Boolean isDaemon;

        /**
         * 线程堆栈跟踪
         */
        private List<String> stackTrace;
    }
}
