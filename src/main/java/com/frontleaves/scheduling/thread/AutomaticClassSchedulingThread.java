package com.frontleaves.scheduling.thread;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.scheduling.AutomaticClassSchedulingBaseDTO;
import com.frontleaves.scheduling.models.dto.scheduling.ScheduleResultDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.GeneticSchedulingService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 自动排课线程
 * 使用遗传算法进行智能排课
 *
 * @author AI Assistant
 */
@Slf4j
public class AutomaticClassSchedulingThread extends Thread {
    @Resource
    private RedissonClient redisson;

    @Resource
    private GeneticSchedulingService geneticSchedulingService;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private boolean running = false;

    private UserDO user;

    @Override
    public void run() {
        log.info("自动排课线程启动，等待任务...");

        while (running) {
            lock.lock();
            try {
                // 从Redis获取排课基础数据
                RBucket<AutomaticClassSchedulingBaseDTO> cacheData =
                        redisson.getBucket(StringConstant.Redis.SCHEDULE_LESSONS + user.getUserUuid());
                if (!cacheData.isExists()) {
                    throw new BusinessException("缓存数据不存在", ErrorCode.BODY_ERROR);
                }
                AutomaticClassSchedulingBaseDTO baseData = cacheData.get();

                // 执行遗传算法排课
                log.info("开始执行遗传算法排课...");
                ScheduleResultDTO result = geneticSchedulingService.executeGeneticAlgorithm(baseData);

                // 保存排课结果到Redis
                RBucket<ScheduleResultDTO> resultCache =
                        redisson.getBucket(StringConstant.Redis.SCHEDULE_RESULT + user.getUserUuid());
                resultCache.set(result);

                log.info("排课完成，适应度：{}", result.getFitness());
                running = false;

            } catch (Exception e) {
                log.error("排课过程发生错误：", e);
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
        }

        log.info("排课线程结束运行");
    }

    /**
     * 启动排课任务
     */
    public void startUp(UserDO user) {
        this.user = user;
        try {
            condition.signal();
            running = true;
            log.info("已通知线程执行排课任务");
        } finally {
            lock.unlock();
        }
    }
}
