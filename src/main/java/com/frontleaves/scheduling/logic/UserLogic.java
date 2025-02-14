package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
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
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

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
     *
     * @param userDO 用户实体
     */
    private void checkUser(UserDO userDO) {
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
    public UserLoginDTO checkLoginData(UserLoginVO userLoginVO, HttpServletRequest request) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        UserDO userDO = new UserDO();
        //检查是否为邮箱登录
        if (userLoginVO.getUser().matches(StringConstant.Common.EMAIL_REGRLAR_EXPRESSION)) {
            log.info("确认为邮箱登录");
            userDO = userDAO.lambdaQuery().eq(UserDO::getEmail, userLoginVO.getUser()).one();
        }
        if (userLoginVO.getUser().matches(StringConstant.Common.PHONE_REGRLAR_EXPRESSION)) {
            log.info("确认为手机号登录");
            userDO = userDAO.lambdaQuery().eq(UserDO::getPhone, userLoginVO.getUser()).one();
        }
        if (userLoginVO.getUser().matches(StringConstant.Common.USER_NAME_REGRLAR_EXPRESSION)) {
            log.info("确认为用户名登录");
            userDO = userDAO.lambdaQuery().eq(UserDO::getName, userLoginVO.getUser()).one();
        }
        if (userDO != null) {
            //检查密码是否正确
            if (!PasswordUtil.verify(userLoginVO.getPassword(), userDO.getPassword())) {
                throw new UserAuthenticationException(
                        UserAuthenticationException.ErrorType.LOGIN_WRONG, request);
            }
            userLoginDTO = loginReturn(userDO);
            return userLoginDTO;
        }
        log.debug("确认为是否为学号登录，检查是否初始化");
        StudentDO studentDO = studentDAO.getStudentById(userLoginVO.getUser());
        if (studentDO != null) {
            //校验是否初始化
            log.debug("确认为学号登录，检查是否初始化");
            if (studentDO.getUserUuid() != null) {
                log.debug("学号登录已经初始化过");
                UserDO userDoS = userDAO.getById(studentDO.getUserUuid());
                if (userDoS != null) {
                    if (PasswordUtil.verify(userLoginVO.getPassword(), userDoS.getPassword())) {
                        return loginReturn(userDoS);
                    }
                    throw new UserAuthenticationException(
                            UserAuthenticationException.ErrorType.LOGIN_WRONG, request);
                }
                throw new BusinessException("意料之外的错误", ErrorCode.OPERATION_ERROR);
            }
            log.info("检查密码是否符合初始化标准");
            if (userLoginVO.getPassword().equals("stu" + userLoginVO.getUser())) {
                //进行初始化操作
                log.info("确认为学号登录的初始化");
                userLoginDTO.setInitialization(true);
                StudentDTO studentDTO = new StudentDTO();
                BeanUtils.copyProperties(studentDO, studentDTO);
                userLoginDTO.setTeacher(null);
                userLoginDTO.setStudent(studentDTO);
                return userLoginDTO;
            } else {
                throw new UserAuthenticationException(
                        UserAuthenticationException.ErrorType.LOGIN_WRONG, request);
            }

        }
        TeacherDO teacherDO = teacherDAO.getTeacherById(userLoginVO.getUser());
        if (teacherDO != null) {
            log.debug("确认为工号登录，检查是否初始化");
            if (teacherDO.getUserUuid() != null) {
                log.debug("工号登录已经初始化过");
                UserDO userDoS = userDAO.getById(teacherDO.getUserUuid());
                if (userDoS != null) {
                    if (PasswordUtil.verify(userLoginVO.getPassword(), userDoS.getPassword())) {
                        //正常登录
                        return loginReturn(userDoS);
                    }
                    throw new UserAuthenticationException(
                            UserAuthenticationException.ErrorType.LOGIN_WRONG, request);
                }
                throw new BusinessException("意料之外的错误", ErrorCode.OPERATION_ERROR);
            }
            log.debug("检查密码是否符合初始化要求");
            if (userLoginVO.getPassword().equals("te" + userLoginVO.getUser())) {
                log.debug("确认为工号登录的初始化");
                userLoginDTO.setInitialization(true);
                TeacherDTO teacherDTO = new TeacherDTO();
                BeanUtils.copyProperties(teacherDO, teacherDTO);
                userLoginDTO.setStudent(null);
                userLoginDTO.setTeacher(teacherDTO);
                return userLoginDTO;
            }
            throw new UserAuthenticationException(
                    UserAuthenticationException.ErrorType.LOGIN_WRONG, request);
        }
        throw new UserAuthenticationException(
                UserAuthenticationException.ErrorType.LOGIN_WRONG, request);

    }

    @Override
    public void userRegistered(UserInitializationVO userInitializationVO) {
        //查明是否为学生或者老师
        if (userInitializationVO.getPassword().startsWith("stu")) {
            log.info("确认为学生初始化");
            StudentDO studentDO = studentDAO.getStudentById(userInitializationVO.getUser());
            if (studentDO == null) {
                throw new BusinessException("学生信息不存在", ErrorCode.BODY_ERROR);
            }
            if (studentDO.getUserUuid() != null) {
                throw new BusinessException("用户已注册", ErrorCode.BODY_ERROR);
            }
            UserDO userDO = new UserDO();
            //传递了用户名，密码，邮箱，手机号
            BeanUtils.copyProperties(userInitializationVO, userDO);
            userDO.setPassword(PasswordUtil.encrypt(userInitializationVO.getNewPassword()));
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
            TeacherDO teacherDO = teacherDAO.getTeacherById(userInitializationVO.getUser());
            if (teacherDO == null) {
                throw new BusinessException("教师信息不存在", ErrorCode.BODY_ERROR);
            }
            if (teacherDO.getUserUuid() != null) {
                throw new BusinessException("用户已注册", ErrorCode.BODY_ERROR);
            }
            UserDO userDO = new UserDO();
            //传递了用户名，密码，邮箱，手机号
            BeanUtils.copyProperties(userInitializationVO, userDO);
            userDO.setPassword(PasswordUtil.encrypt(userInitializationVO.getNewPassword()));
            checkUser(userDO);
            RoleDO roleDO = roleDAO.getRoleByName("老师");
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

    @Override
    public void checkPassword(UserInitializationVO userInitializationVO) {
        log.info("检查新密码是否为初始化密码");
        log.info("userInitializationVO: {}", userInitializationVO);
        if (userInitializationVO.getPassword().equals(userInitializationVO.getNewPassword())) {
            throw new BusinessException("密码不能为初始化密码", ErrorCode.BODY_ERROR);
        }
    }
}
