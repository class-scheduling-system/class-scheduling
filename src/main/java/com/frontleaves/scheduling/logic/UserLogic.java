package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.daos.StudentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.RoleDTO;
import com.frontleaves.scheduling.models.dto.UserDTO;
import com.frontleaves.scheduling.models.dto.UserLoginDTO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.frontleaves.scheduling.service.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户逻辑实现
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLogic implements UserService {
    private final UserDAO userDAO;
    private final StudentDAO studentDAO;
    private final TeacherDAO teacherDAO;
    private final RoleDAO roleDAO;

    /**
     * 用户登录信息交换
     *
     * @param userDO 用户实体
     * @return 用户登录信息
     */
    private UserLoginDTO loginReturn(UserDO userDO) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(userDO, userDTO);
        RoleDO roleDO = roleDAO.lambdaQuery().eq(RoleDO::getRoleUuid, userDO.getRoleUuid()).one();
        RoleDTO roleDTO = new RoleDTO();
        BeanUtils.copyProperties(roleDO, roleDTO);
        userDTO.setRole(roleDTO);
        userLoginDTO.setUser(userDTO);
        userLoginDTO.setInitialization(false);
        return userLoginDTO;
    }


    @Override
    public UserLoginDTO checkLoginData(UserLoginVO userLoginVO) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        UserDO userDO = null;
        // 依次查询用户名、手机号、邮箱
        for (UserDO user : List.of(
                userDAO.lambdaQuery().eq(UserDO::getName, userLoginVO.getUser()).one(),
                userDAO.lambdaQuery().eq(UserDO::getPhone, userLoginVO.getUser()).one(),
                userDAO.lambdaQuery().eq(UserDO::getEmail, userLoginVO.getUser()).one()
        )) {
            if (user != null) {
                userDO = user;
                break;
            }
        }
        if (userDO != null) {
            //检查密码是否正确
            if (userDO.getPassword().equals(userLoginVO.getPassword())) {
                userLoginDTO = loginReturn(userDO);
            } else {
                throw new BusinessException("密码错误", ErrorCode.BODY_ERROR);
            }
        } else {
            //检测是否为学生或者老师进行初始化操作
            if (userLoginVO.getPassword().equals("stu" + userLoginVO.getUser())) {

            } else if (userLoginVO.getPassword().equals("te" + userLoginVO.getUser())) {

            } else {
               //检测是否为通过学号或者工号进行登录（应是初始化后的）

            }
        }
        return userLoginDTO;
    }
}
