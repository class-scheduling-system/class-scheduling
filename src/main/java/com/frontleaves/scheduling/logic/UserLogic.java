package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
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
    private final TokenDAO tokenDAO;

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
        //校验学生
        StudentDO studentDO = studentDAO.lambdaQuery().eq(StudentDO::getUserUuid, userDO.getUserUuid()).one();
        if (studentDO != null) {
            userLoginDTO.setTeacher(null);
            StudentDTO studentDTO = new StudentDTO();
            BeanUtils.copyProperties(studentDO, studentDTO);
            userLoginDTO.setStudent(studentDTO);
        }
        //校验教师
        TeacherDO teacherDO = teacherDAO.lambdaQuery().eq(TeacherDO::getUserUuid, userDO.getUserUuid()).one();
        if (teacherDO != null) {
            userLoginDTO.setStudent(null);
            TeacherDTO teacherDTO = new TeacherDTO();
            BeanUtils.copyProperties(teacherDO, teacherDTO);
            userLoginDTO.setTeacher(teacherDTO);
        }
        TokenDTO tokenDTO = tokenDAO.createToken(userDO);
        userLoginDTO.setToken(tokenDTO);
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
                throw new BusinessException("用户名或密码错误", ErrorCode.BODY_ERROR);
            }
        } else {
            //检测是否初始化过
            StudentDO studentDO = studentDAO.getStudentById(userLoginVO.getUser());
            if (studentDO != null) {
                //校验是否初始化
                if (studentDO.getUserUuid() != null) {
                    UserDO userDoS = userDAO.getById(studentDO.getUserUuid());
                    if (userDoS != null) {
                        if (userDoS.getPassword().equals(userLoginVO.getPassword())) {
                            //正常登录
                            loginReturn(userDO);
                        } else {
                            throw new BusinessException("用户名或密码错误", ErrorCode.BODY_ERROR);
                        }
                    } else {
                        throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                    }
                } else {
                    if (userLoginVO.getPassword().equals("stu" + userLoginVO.getUser())) {
                        //进行初始化操作
                        userLoginDTO.setInitialization(true);
                        StudentDTO studentDTO = new StudentDTO();
                        BeanUtils.copyProperties(studentDO, studentDTO);
                        userLoginDTO.setTeacher(null);
                        userLoginDTO.setStudent(studentDTO);
                    } else {
                        throw new BusinessException("用户名或密码错误", ErrorCode.BODY_ERROR);
                    }
                }
            }
            TeacherDO teacherDO = teacherDAO.getTeacherById(userLoginVO.getUser());
            if (teacherDO != null) {
                if (teacherDO.getUserUuid() != null) {
                    UserDO userDoS = userDAO.getById(teacherDO.getUserUuid());
                    if (userDoS != null) {
                        if (userDoS.getPassword().equals(userLoginVO.getPassword())) {
                            //正常登录
                            loginReturn(userDO);
                        } else {
                            throw new BusinessException("用户名或密码错误", ErrorCode.BODY_ERROR);
                        }
                    } else {
                        throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                    }
                } else {
                    if (userLoginVO.getPassword().equals("te" + userLoginVO.getUser())) {
                        userLoginDTO.setInitialization(true);
                        TeacherDTO teacherDTO = new TeacherDTO();
                        BeanUtils.copyProperties(teacherDO, teacherDTO);
                        userLoginDTO.setStudent(null);
                        userLoginDTO.setTeacher(teacherDTO);
                    } else {
                        throw new BusinessException("用户名或密码错误", ErrorCode.BODY_ERROR);
                    }
                }
            }


        }
        return userLoginDTO;
    }
}
