package com.frontleaves.scheduling.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 使用 Condition 机制的线程
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author xiao_lfeng
 */
public class ConditionThread extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(ConditionThread.class);

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private volatile boolean running = true;
    private volatile boolean hasTask = false;

    public ConditionThread(String name) {
        super(name);
        setDaemon(false);
    }

    @Override
    public void run() {
        logger.info("线程启动，等待任务...");

        while (running) {
            lock.lock();
            try {
                while (!hasTask && running) {
                    logger.info("线程进入等待状态");
                    condition.await();  // 等待被唤醒
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
            } finally {
                lock.unlock();
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
        lock.lock();
        try {
            hasTask = true;
            condition.signal();  // 唤醒等待的线程
            logger.info("已通知线程执行任务");
        } finally {
            lock.unlock();
        }
    }

    /**
     * 停止线程
     */
    public void stopThread() {
        lock.lock();
        try {
            running = false;
            condition.signal();  // 唤醒线程以检查running状态
        } finally {
            lock.unlock();
        }
    }
}
