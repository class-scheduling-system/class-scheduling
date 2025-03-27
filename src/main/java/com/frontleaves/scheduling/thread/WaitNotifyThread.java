package com.frontleaves.scheduling.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 使用 wait/notify 机制的线程
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public class WaitNotifyThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(WaitNotifyThread.class);

    private final Object lock = new Object();
    private volatile boolean running = true;
    private volatile boolean hasTask = false;

    public WaitNotifyThread(String name) {
        super(name);
        setDaemon(false);
    }

    @Override
    public void run() {
        logger.info("线程启动，等待任务...");

        while (running) {
            synchronized (lock) {
                try {
                    while (!hasTask && running) {
                        logger.info("线程进入等待状态");
                        lock.wait();  // 等待被唤醒
                    }

                    if (!running) {
                        break;
                    }

                    // 执行任务
                    doWork();

                    // 任务完成，重置状态
                    hasTask = false;

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("线程结束运行");
    }

    /**
     * 执行具体的任务
     */
    private void doWork() {
        logger.info("开始执行任务");
        try {
            // 模拟任务执行
            Thread.sleep(1000);
            logger.info("任务执行完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 唤醒线程执行任务
     */
    public void wakeUpToWork() {
        synchronized (lock) {
            hasTask = true;
            lock.notifyAll();
            logger.info("已通知线程执行任务");
        }
    }

    /**
     * 停止线程
     */
    public void stopThread() {
        synchronized (lock) {
            running = false;
            lock.notifyAll();
        }
    }
}
