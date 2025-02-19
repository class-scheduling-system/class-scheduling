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

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.PermissionMapper;
import com.frontleaves.scheduling.models.entity.PermissionDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Map;

/**
 * 权限数据访问对象
 * <p>
 * 该类继承自 ServiceImpl，专门处理 {@link PermissionDO} 的数据访问，
 * 实现了 {@link IService} 接口以支持 CRUD 操作。通过与 {@link PermissionMapper}
 * 结合，提供了对权限表的数据库操作能力。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class PermissionDAO extends ServiceImpl<PermissionMapper, PermissionDO> implements IService<PermissionDO> {

    private final Jedis jedis;

    /**
     * 根据权限键获取权限信息
     * <p>
     * 该方法首先尝试从 Redis 中获取指定权限键的权限信息。如果 Redis 中不存在该权限键的信息，
     * 则从数据库中查询并将其存入 Redis，最后返回对应的 {@code PermissionDO} 对象。
     * 如果在操作过程中出现异常，将抛出 {@code ServerInternalErrorException}。
     * </p>
     *
     * @param permissionKey 权限键，用于唯一标识一个权限
     * @return 返回与给定权限键关联的 {@code PermissionDO} 对象；如果未找到，则返回 {@code null}
     * @throws ServerInternalErrorException 如果在执行数据库或 Redis 操作时发生错误
     */
    public PermissionDO getPermissionKey(String permissionKey) throws ServerInternalErrorException {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.PERMISSION + permissionKey);
        if (map.isEmpty()) {
            PermissionDO permissionDO = this.lambdaQuery().eq(PermissionDO::getPermissionKey, permissionKey).one();
            try (Transaction transaction = jedis.multi()) {
                transaction.hmset(StringConstant.Redis.PERMISSION + permissionKey, ConvertUtil.convertObjectToMapString(permissionDO));
                transaction.exec();
            } catch (Exception e) {
                throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
            }
        } else {
            return BeanUtil.toBean(map, PermissionDO.class);
        }
        return null;
    }
}
