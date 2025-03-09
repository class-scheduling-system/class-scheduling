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
import com.frontleaves.scheduling.mappers.ClassroomTypeMapper;
import com.frontleaves.scheduling.models.entity.ClassroomTypeDO;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.xlf.utility.util.ConvertUtil;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

/**
 * 教室类型数据访问对象
 * <p>
 * 该类是 {@code ClassroomTypeDO} 实体类的数据访问对象，通过继承 {@code ServiceImpl<ClassroomTypeMapper, ClassroomTypeDO>}，
 * 实现了对教室类型表的基本 CRUD 操作。此类提供了与数据库交互的方法，用于管理教室类型的相关信息。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see ClassroomTypeDO
 * @see ClassroomTypeMapper
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class ClassroomTypeDAO extends ServiceImpl<ClassroomTypeMapper, ClassroomTypeDO> implements IService<ClassroomTypeDO> {
    private final RedissonClient redisson;

    /**
     * 获取所有教室类型
     * <p>
     * 该方法用于从 Redis 缓存中获取所有的 {@code ClassroomTypeDO} 对象列表。如果缓存中不存在，则从数据库中查询并加载到缓存中。
     * 如果数据库中也不存在任何记录，则返回空列表。
     * </p>
     *
     * @return 返回一个包含所有教室类型的 {@code List<ClassroomTypeDO>}，如果没有找到任何类型则返回 {@code null}
     */
    public List<ClassroomTypeDO> getTypes() {
        RList<ClassroomTypeDO> types = redisson.getList(StringConstant.Redis.CLASSROOM_TYPE_LIST);
        if (!types.isExists()) {
            List<ClassroomTypeDO> getList = this.lambdaQuery().list();
            if (!getList.isEmpty()) {
                types.addAll(getList);
                types.expire(Duration.ofSeconds(3600));
                return getList;
            }
        } else {
            return types.readAll();
        }
        return List.of();
    }

    /**
     * 通过 UUID 获取教室类型
     * <p>
     * 该方法用于根据给定的 {@code type} 参数从 Redis 缓存中获取对应的 {@code ClassroomTypeDO} 对象。
     * 如果缓存中不存在，则从数据库中查询并加载到缓存中。如果在数据库中也未找到对应记录，则返回 {@code null}。
     * </p>
     *
     * @param type 教室类型的 UUID
     * @return 返回与给定 UUID 对应的 {@code ClassroomTypeDO} 对象，如果没有找到则返回 {@code null}
     */
    @Nullable
    public ClassroomTypeDO getTypeByUuid(String type) {
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.CLASSROOM_TYPE_UUID + type);
        if (!map.isExists()) {
            ClassroomTypeDO classroomTypeDO = this.getById(type);
            if (classroomTypeDO != null) {
                map.putAll(ConvertUtil.convertObjectToMapString(classroomTypeDO));
                map.expire(Duration.ofSeconds(86400));
                return classroomTypeDO;
            }
        } else {
            return BeanUtil.toBean(map, ClassroomTypeDO.class, ProjectOption.stringBlankToNull());
        }
        return null;
    }
}
