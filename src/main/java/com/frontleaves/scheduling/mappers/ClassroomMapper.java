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

package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.ClassroomDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教室表映射器
 * <p>
 * 该类用于定义教室表映射器，继承自 MyBatis-Plus 的 {@code BaseMapper} 接口。
 * 通过此接口可以实现对教室信息的增删改查等基本数据库操作。
 * 对应的实体类为 {@code ClassroomDO}，该类表示教室信息，包含教室的基本属性如编号、名称、容量等，
 * 以及一些附加属性如是否是考场、是否有空调等。教室信息存储在数据库表 `cs_classroom` 中。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see ClassroomDO
 * @since v1.0.0
 */
@Mapper
public interface ClassroomMapper extends BaseMapper<ClassroomDO> {
}
