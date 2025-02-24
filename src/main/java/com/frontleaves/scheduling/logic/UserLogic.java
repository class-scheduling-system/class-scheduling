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

package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.TokenDAO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.HeaderUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户逻辑处理服务类，实现了 {@code UserService} 接口。该类主要负责处理用户登录验证、注册等核心业务逻辑。
 * 通过与多个 DAO 层对象协作，完成数据的存取操作，并基于这些数据构建响应体或进行有效性检查。
 * <p>
 * 此服务类使用了依赖注入来获取必要的 DAO 对象实例，确保能够访问到用户、学生、教师以及角色等相关信息。
 * 在执行具体业务时，会调用 DAO 提供的方法读写数据库，并根据需要转换实体间的数据格式以满足前端请求的需求。
 *
 * @author FLASHLACK | xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogic implements UserService {
    private final TokenDAO tokenDAO;

    /**
     * 根据传入的 {@code HttpServletRequest} 请求对象获取对应的用户信息。
     * <p>
     * 该方法首先通过请求头中的特定字段（如Authorization）获取用户的Token，然后使用这个Token从数据库中查询对应用户的信息。
     * 如果Token有效且存在对应的用户，则返回该用户信息；如果Token无效或没有找到对应的用户，则抛出相应的异常。
     *
     * @param request 包含用户认证信息的HTTP请求对象
     * @return 返回与请求中的Token关联的用户信息
     * @throws UserAuthenticationException 当Token过期或不存在对应的用户时抛出此异常
     */
    @Override
    public UserDO getUserByRequest(HttpServletRequest request) {
        String getUserToken = HeaderUtil.getAuthorizeUserUuidString(request);
        if (getUserToken != null) {
            UserDO getUser = tokenDAO.getTokenUser(getUserToken);
            if (getUser != null) {
                return getUser;
            } else {
                throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
            }
        } else {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.TOKEN_EXPIRED, request);
        }
    }
}
