package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
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
        StudentDO studentDO = studentDAO.lambdaQuery().eq(StudentDO::getUserUuid,
                userDO.getUserUuid()).one();
        if (studentDO != null) {
            userLoginDTO.setTeacher(null);
            StudentDTO studentDTO = new StudentDTO();
            BeanUtils.copyProperties(studentDO, studentDTO);
            userLoginDTO.setStudent(studentDTO);
        }
        //校验教师
        TeacherDO teacherDO = teacherDAO.lambdaQuery().eq(TeacherDO::getUserUuid,
                userDO.getUserUuid()).one();
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

    /**
     * 用户注册信息检查
     * @param userDO 用户实体
     */
    private void checkUser(UserDO userDO){
        log.info("检测用户信息是否重复");
        if (userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one() != null) {
            throw new BusinessException("用户名已存在", ErrorCode.BODY_ERROR);
        }
        if (userDAO.lambdaQuery().eq(UserDO::getEmail, userDO.getEmail()).one() != null) {
            throw new BusinessException("邮箱已存在", ErrorCode.BODY_ERROR);
        }
        if (userDAO.lambdaQuery().eq(UserDO::getPhone, userDO.getPhone()).one() != null) {
            throw new BusinessException("手机号已存在", ErrorCode.BODY_ERROR);
        }
    }
    @Override
    public UserLoginDTO checkLoginData(UserLoginVO userLoginVO) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        UserDO userDO = null;
        // 依次查询用户名、手机号、邮箱
        log.info("正常查询是否为用户名，手机号或者邮箱登录");
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
            log.info("确认为非学号登录，正则检查密码是否正确");
            if (userDO.getPassword().equals(userLoginVO.getPassword())) {
                userLoginDTO = loginReturn(userDO);
            } else {
                throw new BusinessException("用户名或密码错误", ErrorCode.BODY_ERROR);
            }
        } else {
            //检测是否初始化过
            log.info("确认为是否为学号登录，检查是否初始化");
            StudentDO studentDO = studentDAO.getStudentById(userLoginVO.getUser());
            if (studentDO != null) {
                //校验是否初始化
                log.info("确认为学号登录，检查是否初始化");
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
                        log.info("确认为学号登录，正在初始化");
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
                log.info("确认为工号号登录，检查是否初始化");
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
                        log.info("确认为工号登录，正在初始化");
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

    @Override
    public void userRegistered(UserLoginVO userLoginVO, UserInitializationVO userInitializationVO) {
        //查明是否为学生或者老师
        if (userLoginVO.getPassword().startsWith("stu")) {
            log.info("确认为学生初始化");
            StudentDO studentDO = studentDAO.getStudentById(userLoginVO.getUser());
            if (studentDO == null) {
                throw new BusinessException("学生信息不存在", ErrorCode.BODY_ERROR);
            }
            if (studentDO.getUserUuid() != null) {
                throw new BusinessException("用户已注册", ErrorCode.BODY_ERROR);
            }
            UserDO userDO = new UserDO();
            //传递了用户名，密码，邮箱，手机号
            BeanUtils.copyProperties(userInitializationVO, userDO);
            checkUser(userDO);
            RoleDO roleDO = roleDAO.getRoleByName("学生");
            //初始化用户
            userDO.setRoleUuid(roleDO.getRoleUuid())
                    .setPermission(roleDO.getPermission());
            //检查用户是否创建成功
            if (userDAO.save(userDO)) {
                //查询用户UUID
                log.info("确认学号用户是否创建成功");
                UserDO userNewDO = userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one();
                if (userNewDO == null) {
                    throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                }
                log.info("更新学生表");
                studentDO.setUserUuid(userNewDO.getUserUuid());
                if (!studentDAO.updateById(studentDO)) {
                    throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                }
                //检查学生是否更新成功
                if (!studentDAO.updateById(studentDO)) {
                    throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                }
            } else {
                throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
            }

        } else {
            log.info("确认为老师初始化");
            TeacherDO teacherDO = teacherDAO.getTeacherById(userLoginVO.getUser());
            if (teacherDO == null) {
                throw new BusinessException("教师信息不存在", ErrorCode.BODY_ERROR);
            }
            if (teacherDO.getUserUuid() != null) {
                throw new BusinessException("用户已注册", ErrorCode.BODY_ERROR);
            }
            UserDO userDO = new UserDO();
            //传递了用户名，密码，邮箱，手机号
            BeanUtils.copyProperties(userInitializationVO, userDO);
            checkUser(userDO);
            RoleDO roleDO = roleDAO.getRoleByName("教师");
            //初始化用户
            userDO.setRoleUuid(roleDO.getRoleUuid())
                    .setPermission(roleDO.getPermission());
            //检测用户是否创建成功
            if (userDAO.save(userDO)) {
                //查询用户UUID
                log.info("确认用户是否创建成功");
                UserDO userNewDO = userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one();
                if (userNewDO == null) {
                    throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                }
                //更新教师表
                teacherDO.setUserUuid(userNewDO.getUserUuid());
                if (!teacherDAO.updateById(teacherDO)) {
                    throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                }
                log.info("更新教师表");
                if (!teacherDAO.updateById(teacherDO)) {
                    throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
                }
            } else {
                throw new BusinessException("系统错误", ErrorCode.OPERATION_ERROR);
            }
        }

    }
}
