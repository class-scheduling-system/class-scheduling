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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserAddVO;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.HeaderUtil;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

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
    private final UserDAO userDAO;
    private final StudentDAO studentDAO;
    private final TeacherDAO teacherDAO;
    private final RoleDAO roleDAO;
    private final TokenDAO tokenDAO;


    /**
     * 检查新用户通过学生信息登录
     * <p>
     * 该方法用于检查新用户是否可以通过提供的学生信息进行登录。如果学生已关联用户，则使用关联的用户信息验证登录。
     * 如果学生未关联用户，则检查密码是否符合特定格式，如果符合则初始化一个新的用户登录信息。
     *
     * @param studentDO   学生数据对象 {@code StudentDO}，包含学生的详细信息
     * @param userLoginVO 用户登录视图对象 {@code UserLoginVO}，包含用户的登录信息
     * @param request     HTTP 请求对象 {@code HttpServletRequest}，包含请求的相关信息
     * @return 返回一个包含登录结果的 {@code UserLoginDTO} 对象
     * @throws UserAuthenticationException 如果密码不正确或验证失败，抛出用户认证异常
     */
    @NotNull
    private UserLoginDTO checkLoginForNewUserByStudent(@NotNull StudentDO studentDO, UserLoginVO userLoginVO, HttpServletRequest request) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        if (studentDO.getUserUuid() != null && !studentDO.getUserUuid().isEmpty()) {
            UserDO userDO = userDAO.getUserByUuid(studentDO.getUserUuid());
            return this.verifyLogin(userDO, userLoginVO, request);
        }
        if (!userLoginVO.getPassword().equals("stu" + userLoginVO.getUser())) {
            throw new UserAuthenticationException(
                    UserAuthenticationException.ErrorType.WRONG_PASSWORD,
                    request
            );
        }
        userLoginDTO
                .setStudent(BeanUtil.toBean(studentDO, StudentDTO.class))
                .setInitialization(true);
        return userLoginDTO;
    }

    /**
     * 检查新用户登录并初始化用户信息
     * <p>
     * 该方法用于教师角色下的新用户登录验证。首先检查教师对象是否关联了有效的用户UUID，如果存在则直接调用验证登录方法。
     * 如果不存在，则通过比对 {@code userLoginVO} 中的密码与特定规则（"te" + 用户名）来验证登录信息。如果密码验证失败，
     * 则抛出 {@code UserAuthenticationException} 异常。若验证成功，则创建一个新的 {@code UserLoginDTO} 对象，并设置其为初始化状态。
     *
     * @param teacherDO   教师数据对象，包含教师相关信息
     * @param userLoginVO 用户登录视图对象，包含用户名和密码等登录信息
     * @param request     HTTP请求对象，用于传递请求上下文信息
     * @return 返回一个包含教师信息和初始化标志的 {@code UserLoginDTO} 对象
     * @throws UserAuthenticationException 当密码验证失败时抛出此异常
     */
    @NotNull
    private UserLoginDTO checkLoginForNewUserByTeacher(@NotNull TeacherDO teacherDO, UserLoginVO userLoginVO, HttpServletRequest request) {
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        if (teacherDO.getUserUuid() != null && !teacherDO.getUserUuid().isEmpty()) {
            UserDO userDO = userDAO.getUserByUuid(teacherDO.getUserUuid());
            return this.verifyLogin(userDO, userLoginVO, request);
        }
        if (!userLoginVO.getPassword().equals("te" + userLoginVO.getUser())) {
            throw new UserAuthenticationException(
                    UserAuthenticationException.ErrorType.WRONG_PASSWORD,
                    request
            );
        }
        userLoginDTO
                .setTeacher(BeanUtil.toBean(teacherDO, TeacherDTO.class))
                .setInitialization(true);
        return userLoginDTO;
    }

    /**
     * 根据用户数据对象（UserDO）生成一个包含登录信息的 DTO 对象。
     * <p>
     * 该方法首先将传入的 UserDO 对象转换为 UserDTO 对象，并设置其角色信息。然后，它会检查用户是否关联有学生或教师的身份，
     * 如果存在，则相应的 StudentDO 或 TeacherDO 对象会被转换成对应的 DTO 并设置到返回的 UserLoginDTO 中。最后，为用户创建一个新的 Token，
     * 并将其添加到最终返回的 UserLoginDTO 中。此过程有助于构建完整的用户登录响应体。
     *
     * @param userDO 用户的数据传输对象，包含了用户的详细信息如用户名、密码等基本信息以及角色 UUID 等。
     * @return 包含了登录后的用户信息、可能的学生或教师身份信息及新生成的访问令牌的 UserLoginDTO 对象。
     */
    private @NotNull UserLoginDTO buildLoginData(@NotNull UserDO userDO) {
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userDO.getRoleUuid());
        UserDTO userDTO = BeanUtil.toBean(userDO, UserDTO.class)
                .setRole(BeanUtil.toBean(roleDTO, RoleDTO.class));
        if (userDO.getPermission() != null && !userDO.getPermission().isEmpty()) {
            userDTO.setPermission(JSONUtil.parseArray(userDO.getPermission()).toList(String.class));
        } else {
            userDTO.setPermission(new ArrayList<>());
        }
        UserLoginDTO userLoginDTO = new UserLoginDTO();
        // 学生教师验证「确认信息后将信息填入」
        StudentDO studentDO = studentDAO.getStudentByUuid(userDO.getUserUuid());
        if (studentDO != null) {
            userLoginDTO.setStudent(BeanUtil.toBean(studentDO, StudentDTO.class));
        }
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(userDO.getUserUuid());
        if (teacherDO != null) {
            userLoginDTO.setTeacher(BeanUtil.toBean(teacherDO, TeacherDTO.class));
        }
        userLoginDTO
                .setUser(userDTO)
                .setToken(tokenDAO.createToken(userDO))
                .setInitialization(false);
        return userLoginDTO;
    }

    /**
     * 验证用户登录信息。
     * <p>
     * 此方法用于验证传入的用户登录信息是否正确。首先检查用户对象是否存在，如果存在，则使用
     * {@code PasswordUtil.verify} 方法对比传入的密码和数据库中存储的密码哈希值是否匹配。如果密码验证成功，
     * 则调用 {@code buildLoginData} 方法生成并返回一个包含登录信息的 {@code UserLoginDTO} 对象。若用户不存在或密码不匹配，
     * 则抛出相应的异常。
     *
     * @param user        从数据库获取到的用户数据对象
     * @param userLoginVO 包含用户登录时提供的用户名和密码的视图对象
     * @param request     当前HTTP请求的对象，主要用于在抛出异常时记录请求上下文信息
     * @return 如果登录验证成功，返回一个包含用户登录后所需信息的数据传输对象
     * @throws ServerInternalErrorException 当遇到未预料的错误情况（如用户对象为null但按逻辑不应为空）时抛出
     * @throws UserAuthenticationException  当提供的密码与数据库中存储的密码不匹配时抛出
     */
    @Contract("null, _, _ -> fail")
    private @NotNull UserLoginDTO verifyLogin(UserDO user, UserLoginVO userLoginVO, HttpServletRequest request)
            throws ServerInternalErrorException, UserAuthenticationException {
        if (user != null) {
            if (PasswordUtil.verify(userLoginVO.getPassword(), user.getPassword())) {
                return this.buildLoginData(user);
            }
            throw new UserAuthenticationException(
                    UserAuthenticationException.ErrorType.WRONG_PASSWORD, request);
        }
        throw new ServerInternalErrorException("意料之外的错误");
    }

    /**
     * 检查用户信息是否已经存在于系统中。
     * <p>
     * 该方法通过用户名、邮箱和手机号来检测用户是否存在。如果任意一项已存在，则抛出 {@link BusinessException} 异常。
     *
     * @param username 用户名
     * @param email    邮箱地址
     * @param phone    手机号码
     * @throws BusinessException 如果用户名、邮箱或手机号已存在，则抛出此异常
     */
    private void checkUserExist(String username, String email, String phone) throws BusinessException {
        log.debug(LogConstant.SERVICE + "检测用户信息是否重复");
        if (userDAO.getUserByName(username) != null) {
            throw new BusinessException("用户名已存在", ErrorCode.BODY_ERROR);
        }
        if (userDAO.getUserByMail(email) != null) {
            throw new BusinessException("邮箱已存在", ErrorCode.BODY_ERROR);
        }
        if (userDAO.getUserByTel(phone) != null) {
            throw new BusinessException("手机号已存在", ErrorCode.BODY_ERROR);
        }
    }

    @Override
    public UserLoginDTO checkLoginForUser(@NotNull UserLoginVO userLoginVO, HttpServletRequest request) {
        UserDO userDO = null;
        //检查是否为邮箱登录
        if (userLoginVO.getUser().matches(StringConstant.Regular.EMAIL_REGULAR_EXPRESSION)) {
            log.debug(LogConstant.SERVICE + "确认为邮箱登录");
            userDO = userDAO.getUserByMail(userLoginVO.getUser());
        }
        if (userLoginVO.getUser().matches(StringConstant.Regular.PHONE_REGULAR_EXPRESSION)
                && userDO == null) {
            log.debug(LogConstant.SERVICE + "确认为手机号登录");
            userDO = userDAO.getUserByTel(userLoginVO.getUser());
        }
        if (userLoginVO.getUser().matches(StringConstant.Regular.USER_NAME_REGULAR_EXPRESSION)
                && userDO == null) {
            log.debug(LogConstant.SERVICE + "确认为用户名登录");
            userDO = userDAO.getUserByName(userLoginVO.getUser());
        }
        if (userDO != null) {
            return this.verifyLogin(userDO, userLoginVO, request);
        }
        log.debug(LogConstant.SERVICE + "确认为学号或工号登录");
        return null;
    }

    @Override
    @Transactional
    public void userRegistered(@NotNull UserInitializationVO userInitializationVO, HttpServletRequest request)
            throws BusinessException {
        // 检查用户是否存在
        this.checkUserExist(
                userInitializationVO.getName(),
                userInitializationVO.getEmail(),
                userInitializationVO.getPhone()
        );
        // 尝试获取教师或学生「TRUE 为学生/FALSE 为教师」
        UserDO userDO;
        if (userInitializationVO.isType()) {
            StudentDO studentDO = studentDAO.getStudentById(userInitializationVO.getUser());
            if (studentDO == null) {
                throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
            }
            if (studentDO.getUserUuid() != null && !studentDO.getUserUuid().isEmpty()) {
                throw new BusinessException("学生已注册", ErrorCode.BODY_ERROR);
            }
            userDO = BeanUtil.toBean(userInitializationVO, UserDO.class)
                    .setRoleUuid(roleDAO.getRoleByName("学生").getRoleUuid());
        } else {
            TeacherDO teacherDO = teacherDAO.getTeacherById(userInitializationVO.getUser());
            if (teacherDO == null) {
                throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
            }
            if (teacherDO.getUserUuid() != null && !teacherDO.getUserUuid().isEmpty()) {
                throw new BusinessException("教师已注册", ErrorCode.BODY_ERROR);
            }
            userDO = BeanUtil.toBean(userInitializationVO, UserDO.class)
                    .setRoleUuid(roleDAO.getRoleByName("教师").getRoleUuid());
        }
        // 构造信息
        String userUuid = UuidUtil.generateUuidNoDash();
        userDO
                .setUserUuid(userUuid)
                .setPassword(PasswordUtil.encrypt(userInitializationVO.getNewPassword()));
        userDAO.save(userDO);
        if (userInitializationVO.isType()) {
            studentDAO.updateUserUuid(userUuid, userInitializationVO.getUser());
        } else {
            teacherDAO.updateUserUuid(userUuid, userInitializationVO.getUser());
        }
    }

    /**
     * 检查用户是否使用了默认密码。
     * <p>
     * 该方法用于验证用户设置的新密码是否为系统生成的默认密码。如果新密码与默认密码相同，则抛出 {@code BusinessException} 异常。
     *
     * @param stuOrTeId   学生或教师的唯一标识符，用于生成默认密码
     * @param newPassword 用户尝试设置的新密码
     * @throws BusinessException 如果新密码是默认密码，则抛出此异常
     */
    @Override
    public void checkUserNotUseDefaultPassword(@NotNull String stuOrTeId, @NotNull String newPassword)
            throws BusinessException {
        String studentPassword = "stu" + stuOrTeId;
        String teacherPassword = "te" + stuOrTeId;
        if (newPassword.equals(studentPassword) || newPassword.equals(teacherPassword)) {
            throw new BusinessException("新密码不能为初始密码", ErrorCode.BODY_ERROR);
        }
    }

    /**
     * 检查新用户的登录信息。
     * <p>
     * 该方法首先根据提供的 {@code userLoginVO} 中的用户ID查找学生或教师信息。如果找到的学生或教师已经关联了用户UUID，
     * 则调用 {@code verifyLogin} 方法进行进一步验证。如果未关联用户UUID，则直接检查密码是否符合默认格式。
     * 如果密码不匹配，抛出 {@code UserAuthenticationException} 异常。如果所有检查都通过，则返回包含初始化状态的
     * {@code UserLoginDTO} 对象。
     *
     * @param userLoginVO 包含用户登录信息的对象，包括用户ID和密码
     * @param request     当前HTTP请求对象，用于异常处理时记录请求上下文
     * @return 包含学生或教师信息以及初始化状态的 {@code UserLoginDTO} 对象
     * @throws UserAuthenticationException 如果用户不存在、密码错误或其他认证问题发生时抛出
     */
    @Override
    public UserLoginDTO checkLoginForNewUser(@NotNull UserLoginVO userLoginVO, HttpServletRequest request)
            throws UserAuthenticationException {
        StudentDO studentDO = studentDAO.getStudentById(userLoginVO.getUser());
        if (studentDO != null) {
            return this.checkLoginForNewUserByStudent(studentDO, userLoginVO, request);
        } else {
            TeacherDO teacherDO = teacherDAO.getTeacherById(userLoginVO.getUser());
            if (teacherDO != null) {
                return this.checkLoginForNewUserByTeacher(teacherDO, userLoginVO, request);
            } else {
                throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
            }
        }
    }


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

    /**
     * 获取用户信息
     *
     * @param userUuid 用户唯一标识符
     * @param request  HTTP请求对象
     * @return 用户信息数据传输对象
     */
    @Override
    public UserInfoDTO getUserInfo(String userUuid, HttpServletRequest request) {
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userUuid).one();
        if (userDO == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
        }
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userDO.getRoleUuid());
        if (roleDTO == null) {
            throw new BusinessException("角色不存在", ErrorCode.OPERATION_ERROR);
        }
        //检查是否为学生或者老师
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        if ("学生".equals(roleDTO.getRoleName())) {
            StudentDO studentDO = studentDAO.lambdaQuery().eq(StudentDO::getUserUuid, userUuid).one();
            if (studentDO == null) {
                throw new BusinessException("学生信息不存在", ErrorCode.OPERATION_ERROR);
            }
            userInfoDTO.setStudent(BeanUtil.toBean(studentDO, StudentDTO.class));
        }
        if ("老师".equals(roleDTO.getRoleName())) {
            TeacherDO teacherDO = teacherDAO.lambdaQuery().eq(TeacherDO::getUserUuid, userUuid).one();
            if (teacherDO == null) {
                throw new BusinessException("教师信息不存在", ErrorCode.OPERATION_ERROR);
            }
            userInfoDTO.setTeacher(BeanUtil.toBean(teacherDO, TeacherDTO.class));
        }
        UserDTO userDTO = BeanUtil.toBean(userDO, UserDTO.class)
                .setPermission(ProjectUtil.convertUserDoToUserDTO(userDO).getPermission())
                .setRole(BeanUtil.toBean(roleDTO, RoleDTO.class));
        log.debug("UserDTO: {}", userDTO);
        userInfoDTO.setUser(userDTO);
        return userInfoDTO;
    }

    /**
     * 检查用户登录数据
     *
     * @param userAddVO 用户添加数据
     */
    @Override
    public void checkAddUser(UserAddVO userAddVO) {
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userAddVO.getRoleUuid());
        if (roleDTO == null) {
            throw new BusinessException("此类用户数据不存在", ErrorCode.BODY_ERROR);
        }
        if ("学生".equals(roleDTO.getRoleName()) || "老师".equals(roleDTO.getRoleName())) {
            throw new BusinessException("此类用户数据不允许添加", ErrorCode.BODY_ERROR);
        }
        log.debug("检查用户是否存在开始前");
        checkUserExist(userAddVO.getName(), userAddVO.getEmail(), userAddVO.getPhone());
        log.debug("检查用户是否存在结束");
    }

    /**
     * 添加用户
     *
     * @param userAddVO 用户添加数据
     * @return 用户信息数据传输对象
     */
    @Override
    public UserInfoDTO addUser(UserAddVO userAddVO) {
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userAddVO.getRoleUuid());
        UserDO userDO = BeanUtil.toBean(userAddVO, UserDO.class);
        if (userDO.getPassword() == null || userDO.getPassword().isEmpty()) {
            userDO.setPassword(PasswordUtil.encrypt(RandomUtil.randomString(8)));
        } else {
            userDO.setPassword(PasswordUtil.encrypt(userDO.getPassword()));
        }
        userDO.setRoleUuid(roleDTO.getRoleUuid())
                .setPermission(JSONUtil.toJsonStr(userAddVO.getPermission()));
        log.debug("添加用户UserDO: {}", userDO);
        userDAO.save(userDO);
        //构造信息
        UserDO newUserDO = userDAO.lambdaQuery().eq(UserDO::getPhone, userDO.getPhone()).one();
        if (newUserDO == null) {
            throw new BusinessException("添加用户失败", ErrorCode.OPERATION_ERROR);
        }
        UserDTO userDTO = ProjectUtil.convertUserDoToUserDTO(newUserDO)
                .setRole(BeanUtil.toBean(roleDTO, RoleDTO.class));
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        log.debug("添加用户最后的UserDTO: {}", userDTO);
        userInfoDTO.setUser(userDTO);
        return userInfoDTO;
    }

    /**
     * 检查用户唯一标识符
     *
     * @param userUuid 用户唯一标识符
     */
    @Override
    public void checkUuid(String userUuid) {
        if (userUuid == null || userUuid.isEmpty()) {
            throw new BusinessException("丢失用户主键", ErrorCode.PARAMETER_ERROR);
        }
    }

    /**
     * 删除用户
     *
     * @param userUuid 用户唯一标识符
     * @param request  HTTP请求对象
     */
    @Override
    @Transactional
    public void deleteUser(String userUuid, HttpServletRequest request) {
        UserDO userDO = userDAO.getUserByUuid(userUuid);
        if (userDO == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
        }
        //检查是否为学生还是老师
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userDO.getRoleUuid());
        if (roleDTO == null) {
            throw new BusinessException("角色不存在，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }
        if ("学生".equals(roleDTO.getRoleName())) {
            StudentDO studentDO = studentDAO.getStudentByUuid(userUuid);
            if (studentDO == null) {
                throw new BusinessException("学生信息不存在", ErrorCode.OPERATION_ERROR);
            }
            log.info("删除学生信息");
            studentDAO.lambdaUpdate().eq(StudentDO::getUserUuid, userUuid).remove();
            userDAO.deleteUser(userDO);
        } else if ("老师".equals(roleDTO.getRoleName())) {
            TeacherDO teacherDO = teacherDAO.getTeacherByUuid(userUuid);
            if (teacherDO == null) {
                throw new BusinessException("教师信息不存在", ErrorCode.OPERATION_ERROR);
            }
            log.info("删除教师信息");
            teacherDAO.lambdaUpdate().eq(TeacherDO::getUserUuid, userUuid).remove();
            userDAO.deleteUser(userDO);
        } else {
            log.info("删除用户信息");
            userDAO.deleteUser(userDO);
        }
    }
}
