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

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.ClassroomMapper;
import com.frontleaves.scheduling.models.entity.ClassroomDO;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 教室标签数据访问对象
 * <p>
 * 该类扩展了 {@code ServiceImpl}，并实现了 {@code IService<ClassroomTagDO>} 接口，
 * 用于对教室标签实体 {@code ClassroomTagDO} 进行数据库操作。通过继承自定义的
 * {@code ClassroomTagMapper}，提供了对教室标签表的基本 CRUD 操作以及其他业务逻辑方法。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see ClassroomDO
 * @see ClassroomMapper
 * @since v1.0.0
 */
@Repository
@RequiredArgsConstructor
public class ClassroomDAO extends ServiceImpl<ClassroomMapper, ClassroomDO> implements IService<ClassroomDO> {

    /**
     * 获取教室分页数据
     * <p>
     * 该方法用于根据给定的分页参数、排序方式以及查询条件，从数据库中获取教室的分页数据。
     * 支持通过关键字、标签和类型进行过滤，并且可以指定结果的排序方式（升序或降序）。
     * </p>
     *
     * @param page 分页的页码，从1开始
     * @param size 每页显示的数据条数
     * @param isDesc 是否按创建时间降序排列，如果为 {@code true} 则降序，否则升序
     * @param keyword 查询的关键字，用于模糊匹配教室名称
     * @param tag 教室标签，用于精确匹配教室的标签
     * @param type 教室类型，用于精确匹配教室的类型
     * @return 返回一个包含分页信息和数据的 {@code Page<ClassroomDO>} 对象
     */
    public Page<ClassroomDO> getClassroomPage(
            int page,
            int size,
            boolean isDesc,
            String keyword,
            @Nullable String tag,
            @Nullable String type
    ) {
        LambdaQueryChainWrapper<ClassroomDO> queryWrapper = this.lambdaQuery();
        if (keyword != null) {
            queryWrapper.like(ClassroomDO::getName, keyword);
        }
        if (tag != null) {
            queryWrapper.eq(ClassroomDO::getTag, tag);
        }
        if (type != null) {
            queryWrapper.eq(ClassroomDO::getType, type);
        }
        if (isDesc) {
            queryWrapper.orderByDesc(ClassroomDO::getCreatedAt);
        } else {
            queryWrapper.orderByAsc(ClassroomDO::getCreatedAt);
        }

        return queryWrapper.page(new Page<>(page, size));
    }
}
