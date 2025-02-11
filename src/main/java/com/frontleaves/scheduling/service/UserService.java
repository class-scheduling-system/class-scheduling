package com.frontleaves.scheduling.service;

import com.frontleaves.scheduling.models.dto.UserLoginDTO;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;

/**
 * 用户服务接口
 * @author FLASHLACK
 */
public interface UserService {
    /**
     * 检查登录数据
     * @param userLoginVO 用户登录数据
     * @return 用户登录数据传输对象
     */
    UserLoginDTO checkLoginData(
            UserLoginVO userLoginVO);

    /**
     * 用户注册
     * @param userInitializationVO 用户初始化数据
     */
    void userRegistered(
            UserInitializationVO userInitializationVO);

    /**
     * 检查密码
     * @param userInitializationVO 用户初始化数据
     */
    void checkPassword(
            UserInitializationVO userInitializationVO);
}
