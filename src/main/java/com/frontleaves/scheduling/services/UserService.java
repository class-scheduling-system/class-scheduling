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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.UserInfoDTO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.exception.library.UserAuthenticationException;
import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;

/**
 * 用户服务接口，定义了与用户相关的操作方法。
 * 该接口可以被具体实现类继承，以提供具体的业务逻辑。
 * <p>
 * 本接口中的方法主要用于处理用户的增删改查等基本操作，
 * 以及可能的其他扩展功能。具体实现细节由实现类决定。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface UserService {
    /**
     * 根据请求中的用户Token获取用户信息。
     * <p>
     * 该方法首先从请求头中提取用户Token，然后通过Token在数据库中查找对应的用户信息。
     * 如果Token有效且用户存在，则返回用户信息；如果Token无效或用户不存在，则抛出相应的异常。
     * </p>
     *
     * @param request HTTP请求对象，用于从中提取用户Token
     * @return UserDO 用户信息对象，包含用户的详细信息
     * @throws UserAuthenticationException 如果Token过期或用户不存在时抛出
     */
    UserDO getUserByRequest(HttpServletRequest request);

    /**
     *获取当前登录用户的三方信息
     * <p>
     * 该方法首先通过用户信息(如用户UUID)获取用户详细信息，
     * 然后根据用户信息查询并返回用户对应的角色信息。
     * 如果用户信息有效且角色存在，则返回用户的完整信息（包括角色信息）。
     * 如果用户信息无效或角色不存在，则抛出相应的异常。
     * </p>
     *
     * @param userByRequest 用户信息对象，包含当前用户的详细信息
     * @return UserInfoDTO 返回包含用户详细信息及角色信息的DTO对象
     * @throws UserAuthenticationException 如果用户信息无效或角色不存在时抛出
     */
    UserInfoDTO getUserInfoWithRole(@NotNull UserDO userByRequest);

}
