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

package com.frontleaves.scheduling.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.models.dto.UserDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import org.jetbrains.annotations.NotNull;

/**
 * 项目工具类
 * <p>
 * 提供了一系列的静态方法，用于在项目中进行一些通用的操作。该类中的方法主要用于对象之间的转换、数据处理等。
 * </p>
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author xiao_lfeng
 */
public class ProjectUtil {

    /**
     * 将用户实体对象转换为用户数据传输对象
     * <p>
     * 该方法接收一个 {@code UserDO} 对象，并将其转换为对应的 {@code UserDTO} 对象。
     * 转换过程中，如果用户权限不为空，则会将权限从 JSON 格式字符串转换为字符串列表。
     * 如果权限为空，则在 {@code UserDTO} 中设置权限为 null。
     * </p>
     *
     * @param userDO 用户实体对象，不能为空
     * @return 转换后的用户数据传输对象
     */
    @NotNull
    public static UserDTO convertUserDoToUserDTO(@NotNull UserDO userDO) {
        UserDTO userDTO = BeanUtil.toBean(userDO, UserDTO.class);
        if (userDO.getPermission() != null && !userDO.getPermission().isEmpty()) {
            userDTO.setPermission(JSONUtil.toList(userDO.getPermission(), String.class));
        } else {
            userDTO.setPermission(null);
        }
        return userDTO;
    }
}
