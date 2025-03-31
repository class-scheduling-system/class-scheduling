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

package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.annotations.IgnoreLog;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.RequestLogMapper;
import com.frontleaves.scheduling.models.entity.RequestLogDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

/**
 * 请求日志数据访问对象
 * <p>
 * 该类扩展了 {@code ServiceImpl}��并实现了 {@code IService<RequestLogDO>} 接口，
 * 用于对请求日志实体 {@code RequestLogDO} 进行数据库操作。通过继承自定义的
 * {@code RequestLogMapper}，提供了对请求日志表的基本 CRUD 操作以及其他业务逻辑方法。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see RequestLogDO
 * @see RequestLogMapper
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RequestLogDAO extends ServiceImpl<RequestLogMapper, RequestLogDO> {

    private final RedissonClient redisson;

    /**
     * 添加请求日志
     * <p>
     * 该方法用于向 Redis 缓存中添加请求日志记录；通过定时器将缓存中的日志记录持久化到数据库中。
     * 如果直接调用应该调用该方法，最后再调用 {@code mergeRequestLog} 方法将缓存中的日志记录持久化到数据库中。
     *
     * @param requestLogDO 请求日志实体
     * @return 添加结果
     */
    @IgnoreLog
    public boolean addRequestLog(RequestLogDO requestLogDO) {
        try {
            RList<RequestLogDO> cacheList = redisson.getList(StringConstant.REQUEST_LOG_CACHE);
            cacheList.add(requestLogDO);
            return true;
        } catch (Exception e) {
            log.error("[LOG] 添加日志记录失败：{}", e.getMessage());
            return false;
        }
    }

    /**
     * 合并请求日志
     * <p>
     * 该方法用于将 Redis 缓存中的请求日志记录持久化到数据库中。
     */
    public void mergeRequestLog() {
        RList<RequestLogDO> cacheList = redisson.getList(StringConstant.REQUEST_LOG_CACHE);
        if (cacheList.isExists()) {
            log.info("[LOG] 缓存中存在 {} 条日志记录，开始持久化...", cacheList.size());
            cacheList.stream().peek(requestLogDO -> {
                        try {
                            this.save(requestLogDO);
                        } catch (Exception e) {
                            log.error("[LOG] 日志记录持久化失败：{}", e.getMessage());
                        }
                    })
                    .forEach(cacheList::remove);
            log.info("[LOG] 日志记录持久化完成");
            cacheList.delete();
        } else {
            log.info("[LOG] 缓存中不存在日志记录，无需持久化");
        }
    }
}
