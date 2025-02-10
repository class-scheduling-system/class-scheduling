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

package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.InitVO;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 初始化控制器
 * <p>
 * 该类用于定义初始化控制器;
 * 用于系统在于初始化时进行调用的控制器接口。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/init")
@RequiredArgsConstructor
public class InitController {

    private final UserDAO userDAO;
    private final SystemDAO systemDAO;
    private final RoleDAO roleDAO;

    /**
     * 系统初始化
     * <p>
     * 该方法用于系统初始化;
     * 用于系统在启动时进行初始化操作。
     */
    @PostMapping("")
    public ResponseEntity<BaseResponse<Void>> systemInit(@RequestBody @Validated InitVO initVO) {
        if ("true".equals(SystemConstant.getIsInitMode())) {
            // 获取管理员角色
            RoleDO getAdminRole = roleDAO.getRoleByUuid(SystemConstant.getRoleAdmin());
            if (getAdminRole == null) {
                throw new BusinessException("管理员角色不存在", ErrorCode.SERVER_INTERNAL_ERROR);
            }
            // 创建超级管理员用户
            UserDO newUser = new UserDO();
            newUser
                    .setRoleUuid(getAdminRole.getRoleUuid())
                    .setName(initVO.getUsername())
                    .setPassword(PasswordUtil.encrypt(initVO.getPassword()))
                    .setEmail(initVO.getEmail())
                    .setPhone(initVO.getPhone());
            userDAO.save(newUser);
            UserDO adminUser = userDAO.lambdaQuery().eq(UserDO::getName, initVO.getUsername()).one();
            if (adminUser != null) {
                // 添加超级管理员 UUID
                systemDAO.addSystemInfo("system_admin_uuid", adminUser.getUserUuid());
                // 设置初始化结束
                SystemConstant.setIsInitMode(systemDAO.setSystemInfo("system_init_mode", "false"));
                return ResultUtil.success("系统初始化成功");
            } else {
                throw new BusinessException("初始化失败", ErrorCode.SERVER_INTERNAL_ERROR);
            }
        } else {
            throw new BusinessException("当前系统不处于初始化模式", ErrorCode.FORBIDDEN);
        }
    }
}
