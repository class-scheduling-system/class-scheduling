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

package com.frontleaves.scheduling.thread;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 演示不同类型的线程
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
public class ThreadTypeDemo {
    private static final Logger logger = LoggerFactory.getLogger(ThreadTypeDemo.class);

    // 自定义线程池
    private final Executor userThreadExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "自定义用户线程");
        thread.setDaemon(false); // 明确设置为用户线程
        return thread;
    });

    private final Executor daemonThreadExecutor = Executors.newFixedThreadPool(2, r -> {
        Thread thread = new Thread(r, "自定义守护线程");
        thread.setDaemon(true); // 设置为守护线程
        return thread;
    });

    @PostConstruct
    public void init() {
        // 1. 打印主线程信息
        logger.info("主线程: {}, 是否为守护线程: {}",
                Thread.currentThread().getName(),
                Thread.currentThread().isDaemon());

        // 2. 启动自定义用户线程
        CompletableFuture.runAsync(() -> {
            logger.info("自定义用户线程: {}, 是否为守护线程: {}",
                    Thread.currentThread().getName(),
                    Thread.currentThread().isDaemon());

            // 模拟长时间运行的任务
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                    logger.info("用户线程仍在运行...");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, userThreadExecutor);

        // 3. 启动自定义守护线程
        CompletableFuture.runAsync(() -> {
            logger.info("自定义守护线程: {}, 是否为守护线程: {}",
                    Thread.currentThread().getName(),
                    Thread.currentThread().isDaemon());

            // 模拟长时间运行的任务
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(5000);
                    logger.info("守护线程仍在运行...");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, daemonThreadExecutor);
    }

    /**
     * Spring的@Async异步方法（默认使用SimpleAsyncTaskExecutor）
     */
    @Async
    public void asyncMethod() {
        logger.info("Spring @Async线程: {}, 是否为守护线程: {}",
                Thread.currentThread().getName(),
                Thread.currentThread().isDaemon());
    }

    /**
     * Spring的定时任务
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void scheduledTask() {
        logger.info("Spring @Scheduled线程: {}, 是否为守护线程: {}",
                Thread.currentThread().getName(),
                Thread.currentThread().isDaemon());
    }
}
