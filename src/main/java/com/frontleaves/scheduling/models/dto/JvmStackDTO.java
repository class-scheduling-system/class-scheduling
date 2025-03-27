package com.frontleaves.scheduling.models.dto;

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