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
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
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

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TablesChairsTypeMapper;
import com.frontleaves.scheduling.models.entity.base.TablesChairsTypeDO;
import com.frontleaves.scheduling.services.TablesChairsTypeService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RKeys;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * 桌椅类型数据访问对象
 * <p>
 * 该类是 {@code TablesChairsTypeService} 接口的具体实现，用于提供桌椅类型相关的数据访问操作。
 * 继承自 {@code ServiceImpl<TablesChairsTypeMapper, TablesChairsTypeDO>}，提供了基本的 CRUD 操作，并且可以扩展更多的业务逻辑。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see TablesChairsTypeService
 * @see TablesChairsTypeMapper
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TablesChairsTypeDAO extends ServiceImpl<TablesChairsTypeMapper, TablesChairsTypeDO> {
    private final RedissonClient redisson;

    /**
     * 从 Redis 中删除指定桌椅类型的相关缓存数据
     * <p>
     * 该方法根据传入的桌椅类型信息，删除与之相关的所有 Redis 缓存条目。具体来说，它会删除以下几类缓存：
     * <ul>
     *     <li>以 {@code StringConstant.Redis.TABLES_CHAIRS_TYPE_LIST + "*"} 为模式的所有缓存条目</li>
     *     <li>以 {@code StringConstant.Redis.TABLES_CHAIRS_TYPE_PAGE + "*"} 为模式的所有缓存条目</li>
     *     <li>以 {@code StringConstant.Redis.TABLES_CHAIRS_TYPE_UUID + tablesChairsTypeDO.getUuid()} 为键的缓存条目</li>
     *     <li>以 {@code StringConstant.Redis.TABLES_CHAIRS_TYPE_NAME + tablesChairsTypeDO.getName()} 为键的缓存条目</li>
     * </ul>
     * 删除完成后，会记录删除的总条数。
     *
     * @param tablesChairsTypeDO 桌椅类型的数据对象，包含桌椅类型的 UUID、名称等信息
     */
    private void deleteTablesChairsTypeRedis(TablesChairsTypeDO tablesChairsTypeDO) {
        RKeys keys = redisson.getKeys();
        long checkTotal = 0;
        checkTotal += keys.deleteByPattern(StringConstant.Redis.TABLES_CHAIRS_LIST + "*");
        checkTotal += keys.deleteByPattern(StringConstant.Redis.TABLES_CHAIRS_PAGE + "*");
        checkTotal += keys.delete(StringConstant.Redis.TABLES_CHAIRS_UUID + tablesChairsTypeDO.getTablesChairsTypeUuid());
        checkTotal += keys.delete(StringConstant.Redis.TABLES_CHAIRS_NAME + tablesChairsTypeDO.getName());
        log.debug("删除桌椅类型缓存数据，共删除 {} 条数据", checkTotal);
    }

    /**
     * 根据 UUID 获取桌椅类型信息
     * <p>
     * 该方法通过传入的 {@code uuid} 参数，从 Redis 缓存中获取对应的桌椅类型信息。如果缓存中不存在，则从数据库中查询并将其放入缓存。
     * 如果在数据库中也找不到对应记录，则返回 {@code null}。
     * </p>
     *
     * @param uuid 桌椅类型的唯一标识符，用于从缓存或数据库中查找对应的桌椅类型信息
     * @return 返回与给定 UUID 对应的 {@link TablesChairsTypeDO} 对象，如果没有找到则返回 {@code null}
     */
    public TablesChairsTypeDO getTablesChairsTypeByUuid(String uuid) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TABLES_CHAIRS_UUID + uuid);
        if (!map.isExists()) {
            TablesChairsTypeDO typeDO = this.getById(uuid);
            if (typeDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(typeDO));
                map.expire(Duration.ofSeconds(3600));
                return typeDO;
            }
        } else {
            return BeanUtil.toBean(map, TablesChairsTypeDO.class);
        }
        return null;
    }

    /**
     * 根据名称获取桌椅类型信息
     * <p>
     * 该方法根据传入的桌椅类型名称从数据库中查询对应的 {@code TablesChairsTypeDO} 对象。如果找到匹配的记录，则将其相关信息缓存到 Redis 中，并设置过期时间为 1 小时。
     * 如果 Redis 中已经存在对应的缓存数据，则直接返回缓存中的数据。如果未找到匹配的记录，则返回 {@code null}。
     * </p>
     *
     * @param name 桌椅类型名称
     * @return 返回与指定名称匹配的 {@code TablesChairsTypeDO} 对象，如果没有找到则返回 {@code null}
     */
    public TablesChairsTypeDO getTablesChairsTypeByName(String name) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.TABLES_CHAIRS_NAME + name);
        if (!map.isExists()) {
            TablesChairsTypeDO typeDO = this.lambdaQuery().eq(TablesChairsTypeDO::getName, name).one();
            if (typeDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(typeDO));
                map.expire(Duration.ofSeconds(3600));
                return typeDO;
            }
        } else {
            return BeanUtil.toBean(map, TablesChairsTypeDO.class);
        }
        return null;
    }

    /**
     * 获取包含关键字的桌椅类型列表
     * <p>
     * 该方法用于根据给定的关键字从数据库中查询桌椅类型列表，并支持分页和排序。首先尝试从 Redis 缓存中读取数据，如果缓存中没有数据，则从数据库查询并缓存结果。
     * </p>
     *
     * @param page    分页的页码
     * @param size    每页的大小
     * @param isDesc  是否降序排列，默认为升序
     * @param keyword 查询关键字，用于匹配桌椅类型名称
     * @return 返回包含桌椅类型信息的分页对象
     */
    public Page<TablesChairsTypeDO> getTablesChairsTypePage(int page, int size, boolean isDesc, String keyword) {
        String cacheKey = StringConstant.Redis.TABLES_CHAIRS_PAGE + page + ":" + size + ":" + isDesc + ":" + keyword;
        RMap<String, String> map = redisson.getMap(cacheKey);
        if (!map.isExists()) {
            LambdaQueryChainWrapper<TablesChairsTypeDO> queryWrapper = this.lambdaQuery();
            if (isDesc) {
                queryWrapper.orderByDesc(TablesChairsTypeDO::getCreatedAt);
            } else {
                queryWrapper.orderByAsc(TablesChairsTypeDO::getCreatedAt);
            }
            if (keyword != null) {
                queryWrapper.like(TablesChairsTypeDO::getName, keyword);
            }
            return ProjectUtil.queryAndCache(queryWrapper, page, size, map);
        } else {
            return ProjectUtil.convertMapToPage(map, TablesChairsTypeDO.class);
        }
    }

    /**
     * 获取所有桌椅类型列表
     * <p>
     * 该方法用于获取系统中所有的桌椅类型信息。首先尝试从 Redis 缓存中读取数据，如果缓存中没有数据，则从数据库查询并缓存结果。
     * </p>
     *
     * @return 返回包含所有桌椅类型信息的列表
     */
    public List<TablesChairsTypeDO> getTablesChairsTypeList() {
        RList<TablesChairsTypeDO> list = redisson.getList(StringConstant.Redis.TABLES_CHAIRS_LIST);
        if (!list.isExists()) {
            List<TablesChairsTypeDO> typeDOList = this.lambdaQuery()
                    .orderByAsc(TablesChairsTypeDO::getName)
                    .list();
            if (!typeDOList.isEmpty()) {
                list.addAll(typeDOList);
                list.expire(Duration.ofSeconds(3600));
                return typeDOList;
            }
            return List.of();
        } else {
            return list.readAll();
        }
    }

    /**
     * 更新桌椅类型信息
     * <p>
     * 该方法用于更新指定的桌椅类型信息。首先从 Redis 中删除与该桌椅类型相关的所有缓存数据，然后在数据库中更新桌椅类型信息。
     * 通过事务管理确保操作的一致性。
     * </p>
     *
     * @param tablesChairsTypeDO 桌椅类型实体对象，包含需要更新的桌椅类型信息
     */
    public void updateTablesChairsType(TablesChairsTypeDO tablesChairsTypeDO) {
        this.deleteTablesChairsTypeRedis(tablesChairsTypeDO);
        this.updateById(tablesChairsTypeDO);
    }

    /**
     * 删除桌椅类型信息
     * <p>
     * 该方法用于删除指定的桌椅类型信息。首先从 Redis 中删除与该桌椅类型相关的所有缓存数据，然后从数据库中删除对应的记录。
     * 整个过程在一个事务中进行，确保数据的一致性。
     * </p>
     *
     * @param tablesChairsTypeDO 待删除的桌椅类型实体对象，包含需要删除的信息
     */
    public void deleteTablesChairsType(TablesChairsTypeDO tablesChairsTypeDO) {
        this.deleteTablesChairsTypeRedis(tablesChairsTypeDO);
        this.removeById(tablesChairsTypeDO.getTablesChairsTypeUuid());
    }

    /**
     * 添加桌椅类型信息
     * <p>
     * 该方法用于向系统中添加一个新的桌椅类型信息。在添加新的桌椅类型之前，会先清除 Redis 中所有与桌椅类型列表相关的缓存数据，以确保数据的一致性。
     * 随后，将新的桌椅类型信息保存到数据库中。
     * </p>
     *
     * @param tablesChairsTypeDO 包含桌椅类型详细信息的对象 {@code TablesChairsTypeDO}
     */
    public void addTablesChairsType(TablesChairsTypeDO tablesChairsTypeDO) {
        RKeys keys = redisson.getKeys();
        keys.deleteByPattern(StringConstant.Redis.TABLES_CHAIRS_LIST + "*");
        keys.deleteByPattern(StringConstant.Redis.TABLES_CHAIRS_PAGE + "*");
        this.save(tablesChairsTypeDO);
    }
}
