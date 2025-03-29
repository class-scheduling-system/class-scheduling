package com.frontleaves.scheduling.thread;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class AutomaticClassSchedulingThread extends Thread {
    @Resource
    private RedissonClient redisson;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private boolean running = false;

    private UserDO user;


    @Override
    public void run() {
        log.info("线程启动，等待任务...");

        while (running) {
            lock.lock();
            try {
                RBucket<AutomaticClassSchedulingBaseDTO> cacheData = redisson.getBucket(StringConstant.Redis.SCHEDULE_LESSONS + user.getUserUuid());
                if (!cacheData.isExists()) {
                    throw new BusinessException("缓存数据不存在", ErrorCode.BODY_ERROR);
                }
                AutomaticClassSchedulingBaseDTO baseData = cacheData.get();


            } catch (Exception e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
        }

        log.info("线程结束运行");
    }


    /**
     * 执行具体的任务
     */
    public void startUp(UserDO user) {
        this.user = user;
        try {
            condition.signal();
            running = true;
            log.info("已通知线程执行任务");
        } finally {
            lock.unlock();
        }
    }
}
