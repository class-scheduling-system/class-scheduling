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
import com.frontleaves.scheduling.mappers.CampusMapper;
import com.frontleaves.scheduling.models.entity.CampusDO;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.Map;

/**
 * 校区数据访问对象
 * <p>
 * 该类是校区表的数据访问对象，继承自 MyBatis-Plus 的 {@code ServiceImpl} 类，并实现了 {@code IService} 接口。
 * 通过此类可以实现对 {@code CampusDO} 实体类的数据库操作，包括增删改查等基本操作。
 * 该类依赖于 {@code CampusMapper} 映射器进行数据库交互。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class CampusDAO extends ServiceImpl<CampusMapper, CampusDO> implements IService<CampusDO> {
    private final Jedis jedis;

    /**
     * 通过校区UUID获取校区信息
     * <p>
     * 该方法根据传入的校区唯一标识符（{@code campusUuid}）从Redis缓存中查询校区信息。
     * 如果缓存中不存在，则从数据库中获取，并将结果存储到Redis缓存中，设置缓存有效期为24小时。
     * 如果在数据库中也未找到对应的校区信息，则返回 {@code null}。
     * </p>
     *
     * @param campusUuid 校区的唯一标识符
     * @return 返回与给定 {@code campusUuid} 对应的 {@code CampusDO} 对象，如果未找到则返回 {@code null}
     * @throws ServerInternalErrorException 如果在操作过程中发生异常，则抛出此异常
     */
    public CampusDO getCampusByUuid(String campusUuid) throws ServerInternalErrorException {
        Map<String, String> map = jedis.hgetAll(StringConstant.Redis.CAMPUS_UUID + campusUuid);
        if (map.isEmpty()) {
            CampusDO campusDO = getById(campusUuid);
            if (campusDO != null) {
                try (Transaction transaction = jedis.multi()) {
                    transaction.hset(StringConstant.Redis.CAMPUS_UUID + campusUuid, ConvertUtil.convertObjectToMapString(campusDO));
                    transaction.expire(StringConstant.Redis.CAMPUS_UUID + campusUuid, 86400);
                    transaction.exec();
                } catch (Exception e) {
                    throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
                }
                return campusDO;
            }
        } else {
            return BeanUtil.toBean(map, CampusDO.class);
        }
        return null;
    }

    /**
     * 通过校区名称获取校区信息
     * <p>
     * 该方法根据传入的校区名称（{@code campusName}）从Redis缓存中查询校区信息。如果缓存中不存在，则从数据库中获取，并将结果存储到Redis缓存中，设置缓存有效期为24小时。
     * 如果在数据库中也未找到对应的校区信息，则返回 {@code null}。
     * </p>
     *
     * @param campusName 校区名称
     * @return 返回与给定 {@code campusName} 对应的 {@code CampusDO} 对象，如果未找到则返回 {@code null}
     * @throws ServerInternalErrorException 如果在操作过程中发生异常，则抛出此异常
     */
    public CampusDO getCampusByName(String campusName) throws ServerInternalErrorException {
        String value = jedis.get(StringConstant.Redis.CAMPUS_NAME + campusName);
        if (value == null) {
            CampusDO campusDO = lambdaQuery().eq(CampusDO::getCampusName, campusName).one();
            if (campusDO != null) {
                try (Transaction transaction = jedis.multi()) {
                    transaction.set(StringConstant.Redis.CAMPUS_NAME + campusName, campusDO.getCampusUuid());
                    transaction.expire(StringConstant.Redis.CAMPUS_NAME + campusName, 86400);
                    transaction.hset(StringConstant.Redis.CAMPUS_UUID + campusDO.getCampusUuid(), ConvertUtil.convertObjectToMapString(campusDO));
                    transaction.expire(StringConstant.Redis.CAMPUS_UUID + campusDO.getCampusUuid(), 86400);
                    transaction.exec();
                } catch (Exception e) {
                    throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
                }
                return campusDO;
            }
        } else {
            return this.getCampusByUuid(value);
        }
        return null;
    }

    /**
     * 通过校区编码获取校区信息
     * <p>
     * 该方法根据传入的校区编码（{@code campusCode}）从Redis缓存中查询校区信息。如果缓存中不存在，则从数据库中获取，并将结果存储到Redis缓存中，设置缓存有效期为24小时。
     * 如果在数据库中也未找到对应的校区信息，则返回 {@code null}。
     * </p>
     *
     * @param campusCode 校区编码
     * @return 返回与给定 {@code campusCode} 对应的 {@code CampusDO} 对象，如果未找到则返回 {@code null}
     * @throws ServerInternalErrorException 如果在操作过程中发生异常，则抛出此异常
     */
    public CampusDO getCampusByCode(String campusCode) throws ServerInternalErrorException {
        String value = jedis.get(StringConstant.Redis.CAMPUS_CODE + campusCode);
        if (value == null) {
            CampusDO campusDO = lambdaQuery().eq(CampusDO::getCampusCode, campusCode).one();
            if (campusDO != null) {
                try (Transaction transaction = jedis.multi()) {
                    transaction.set(StringConstant.Redis.CAMPUS_CODE + campusCode, campusDO.getCampusUuid());
                    transaction.expire(StringConstant.Redis.CAMPUS_CODE + campusCode, 86400);
                    transaction.hset(StringConstant.Redis.CAMPUS_UUID + campusDO.getCampusUuid(), ConvertUtil.convertObjectToMapString(campusDO));
                    transaction.expire(StringConstant.Redis.CAMPUS_UUID + campusDO.getCampusUuid(), 86400);
                    transaction.exec();
                } catch (Exception e) {
                    throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
                }
                return campusDO;
            }
        } else {
            return this.getCampusByUuid(value);
        }
        return null;
    }
}
