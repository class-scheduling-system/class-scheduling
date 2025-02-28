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

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.annotations.IgnoreLog;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.SystemMapper;
import com.frontleaves.scheduling.models.entity.SystemDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 系统表数据访问对象
 * <p>
 * 该类用于定义系统表数据访问对象。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SystemDAO extends ServiceImpl<SystemMapper, SystemDO> implements IService<SystemDO> {
    /**
     * Redis 缓存
     */
    private final RedissonClient redisson;

    /**
     * 获取系统信息
     * <p>
     * 该方法用于获取系统表的信息;
     * 输入系统键，返回系统值;
     * 查询首先从 Redis 缓存中查询，如果没有则从数据库中查询「当从数据库中查询到数据时，将数据存入 Redis 缓存中」。
     *
     * @param key 系统键
     * @return 系统值
     */
    @IgnoreLog
    public String getSystemInfo(String key) {
        RMap<String, String> getValue = redisson.getMap(StringConstant.Redis.SYSTEM + "info");
        if (getValue.isExists()) {
            return getValue.get(key);
        } else {
            SystemDO systemDO = this.lambdaQuery()
                    .eq(SystemDO::getSystemKey, key)
                    .one();
            if (systemDO != null) {
                getValue.put(systemDO.getSystemKey(), systemDO.getSystemVal());
                return systemDO.getSystemVal();
            } else {
                return null;
            }
        }
    }

    /**
     * 设置系统信息
     * <p>
     * 该方法用于设置系统表的信息;
     * 输入系统键和系统值，返回系统值;
     * 设置完成后将数据存入 Redis 缓存中，并更新数据库中的数据。
     *
     * @param key   系统键
     * @param value 系统值
     * @return 系统值
     */
    public String setSystemInfo(String key, String value) {
        this.lambdaUpdate()
                .eq(SystemDO::getSystemKey, key)
                .set(SystemDO::getSystemVal, value)
                .update();
        redisson.getMap(StringConstant.Redis.SYSTEM + "info")
                .put(key, value);
        return value;
    }

    /**
     * 添加系统信息
     * <p>
     * 该方法用于添加系统表的信息;
     * 输入系统键和系统值，无返回值;
     * 添加完成后将数据存入 Redis 缓存中，并更新数据库中的数据。
     *
     * @param key   系统键
     * @param value 系统值
     */
    public void addSystemInfo(String key, String value) {
        SystemDO systemDO = new SystemDO()
                .setSystemKey(key)
                .setSystemVal(value);
        this.save(systemDO);
        redisson.getMap(StringConstant.Redis.SYSTEM + "info")
                .put(key, value);
    }

    /**
     * 获取系统信息列表
     * <p>
     * 该方法用于获取系统表中的所有系统信息。首先从 Redis 缓存中查询，如果缓存中不存在数据，则从数据库中查询并将结果存入 Redis 缓存中。
     * 返回的列表包含所有的系统值。
     * </p>
     *
     * @return 系统值的列表
     */
    public Map<String, String> getSystemInfoList() {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.SYSTEM + "info");
        if (!map.isExists()) {
            List<SystemDO> systemDOList = this.list();
            for (SystemDO systemDO : systemDOList) {
                map.put(systemDO.getSystemKey(), systemDO.getSystemVal());
            }
        }
        return map.readAllMap();
    }
}
