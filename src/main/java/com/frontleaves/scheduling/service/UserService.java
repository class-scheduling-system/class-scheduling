package com.frontleaves.scheduling.service;

import com.frontleaves.scheduling.models.dto.UserLoginDTO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;

/**
 * 用户服务接口
 * @author FLASHLACK
 */
public interface UserService {
    /**
     * 检查登录数据
     * @param userLoginVO 用户登录数据
     */
    UserLoginDTO checkLoginData(
            UserLoginVO userLoginVO);
}
