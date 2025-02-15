package com.frontleaves.scheduling.service;

import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.models.entity.SiteDO;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;

/**
 * @author fanfan187
 */

@Service
public class SiteInfoService {

    private final SystemDAO systemDAO;
    @Getter
    private final Jedis jedis;

    @Autowired
    public SiteInfoService(SystemDAO systemDAO) {
        this.systemDAO = systemDAO;
        this.jedis = new Jedis("localhost");
    }

    /**
     * 获取站点信息
     * 使用 Redis 缓存，首先从缓存中获取数据，如果没有则从数据库中获取
     *
     * @return SiteDO 站点信息
     */
    @Cacheable(value = "system:info",
            key = "'system:info'+'#key",
            unless = "#result == null")
    public SiteDO getSiteInfo() {
        // 从数据库或 Redis 获取信息
        String name = systemDAO.getSystemInfo("name");
        String description = systemDAO.getSystemInfo("description");
        String icp = systemDAO.getSystemInfo("icp");
        String securityRecord = systemDAO.getSystemInfo("security-record");

        // 如果某个值为 null，说明数据库中没有该数据，可能需要处理
        if (name == null || description == null || icp == null || securityRecord == null) {
            // 错误处理或默认值，视你的需求决定
            throw new RuntimeException("系统信息获取失败");
        }

        // 创建 SiteDO 对象并返回
        return new SiteDO(name, description, icp, securityRecord);
    }

    /**
     * 从 Redis 缓存中获取站点信息
     * 如果缓存中没有，则从数据库中获取，并将其存入缓存
     */
    private String getFromCache(String key) {
        // 构建缓存键名：system:info:web_<key>
        String cacheKey = "system:info:" + key;
        String value = jedis.get(cacheKey);
        if (value == null) {
            // 如果缓存中没有，从数据库中查询
            value = systemDAO.getSystemInfo(key);
            if (value != null) {
                // 如果数据库中有，将数据存入 Redis，设置缓存过期时间 7 天
                jedis.setex(cacheKey, 7 * 24 * 60 * 60, value);
            }
        }
        return value;
    }

}
