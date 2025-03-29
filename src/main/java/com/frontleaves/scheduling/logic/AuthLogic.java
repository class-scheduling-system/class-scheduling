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
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.extra.mail.MailUtil;
import cn.hutool.json.JSONUtil;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.dto.base.RoleDTO;
import com.frontleaves.scheduling.models.dto.base.StudentDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherDTO;
import com.frontleaves.scheduling.models.dto.base.UserDTO;
import com.frontleaves.scheduling.models.dto.merge.UserLoginDTO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.frontleaves.scheduling.services.AuthService;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.ServerInternalErrorException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

/**
 * 用户认证逻辑处理类
 * <p>
 * 该类实现了 {@code AuthService} 接口，提供了用户登录验证、新用户注册、默认密码检查以及新用户登录信息检查等功能。
 * 通过与数据库交互，确保用户的合法性和安全性。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthLogic implements AuthService {
    private final UserDAO userDAO;
    private final StudentDAO studentDAO;
    private final TeacherDAO teacherDAO;
    private final RoleDAO roleDAO;
    private final TokenDAO tokenDAO;
    private final UserService userService;

    private final Environment env;

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
        StudentDO studentDO = studentDAO.getStudentByUserUuid(userDO.getUserUuid());
        if (studentDO != null) {
            userLoginDTO.setStudent(BeanUtil.toBean(studentDO, StudentDTO.class));
        }
        TeacherDO teacherDO = teacherDAO.getTeacherByUserUuid(userDO.getUserUuid());
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
    @NotNull
    private UserLoginDTO verifyLogin(UserDO user, UserLoginVO userLoginVO, HttpServletRequest request)
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
     * 检查邮箱是否在指定时间内重复发送
     *
     * @param userDO 用户数据对象
     * @throws BusinessException 如果在限制时间内重复发送则抛出异常
     */
    private void checkEmailSendFrequency(UserDO userDO) {
        // 从DAO层获取邮件验证令牌的创建时间
        long lastSendTime = tokenDAO.getEmailTokenCreatedAt(userDO);
        // 如果存在上次发送记录
        if (lastSendTime > 0) {
            long currentTime = System.currentTimeMillis();
            // 转换为毫秒
            long limitTimeMillis = (long) 3 * 60 * 1000;
            // 检查是否在限制时间内
            if (currentTime - lastSendTime < limitTimeMillis) {
                throw new BusinessException("邮件已发送，请勿重复操作", ErrorCode.OPERATION_ERROR);
            }
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
        userService.checkUserExist(
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
                    .setRoleUuid(SystemConstant.getRoleStudent());
        } else {
            TeacherDO teacherDO = teacherDAO.getTeacherById(userInitializationVO.getUser());
            if (teacherDO == null) {
                throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
            }
            if (teacherDO.getUserUuid() != null && !teacherDO.getUserUuid().isEmpty()) {
                throw new BusinessException("教师已注册", ErrorCode.BODY_ERROR);
            }
            userDO = BeanUtil.toBean(userInitializationVO, UserDO.class)
                    .setRoleUuid(SystemConstant.getRoleTeacher());
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
     * 忘记密码功能实现
     *
     * @param email 用户注册时使用的邮箱地址
     * @return 返回一个包含Token过期时间的响应DTO
     * @throws UserAuthenticationException 如果用户不存在，则抛出用户认证异常
     */
    @Override
    public ForgetPasswordResponseDTO forgetPassword(String email) {
        if (!email.matches(StringConstant.Regular.EMAIL_REGULAR_EXPRESSION)) {
            throw new BusinessException("邮箱格式错误", ErrorCode.BODY_ERROR);
        }
        // 检查用户是否存在
        UserDO userDO = userDAO.getUserByMail(email);
        if (userDO == null) {
            throw new BusinessException("此邮箱并未绑定用户", ErrorCode.BODY_ERROR);
        }
        // 检查邮件发送频率，防止频繁发送
        checkEmailSendFrequency(userDO);
        //生成邮箱Token
        EmailVerificationTokenDTO tokenDTO = tokenDAO.createEmailToken(userDO);
        // 构造重置密码的URL
        String url =env.getProperty("project.base-api") +
                env.getProperty("project.reset-password") + tokenDTO.getToken();
        // 异步发送邮件
        CompletableFuture.runAsync(() -> {
            try {
                // 使用Hutool的ResourceUtil读取邮件模板
                String templateContent = ResourceUtil.readUtf8Str("template/reset-password.html");
                // 替换模板中的占位符
                templateContent = templateContent.replace("${url}", url);
                templateContent = templateContent.replace("${name}", userDO.getName());
                // 发送邮件
                MailUtil.send(email, "重置密码", templateContent, true);
            } catch (IORuntimeException e) {
                // 记录邮件模板读取失败的异常
                log.error(LogConstant.SERVICE + "邮件模板读取失败", e);
                // 异步处理中只记录异常，不抛出
            } catch (Exception e) {
                // 记录邮件发送失败的异常
                log.error(LogConstant.SERVICE + "邮件发送失败", e);
                // 异步处理中只记录异常，不抛出
            }
        });
        // 不等待邮件发送完成，直接返回结果
        ForgetPasswordResponseDTO forgetPasswordResponseDTO = new ForgetPasswordResponseDTO();
        forgetPasswordResponseDTO.setTokenExpireTime(tokenDTO.getExpireTime());
        return forgetPasswordResponseDTO;
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
     * 重置密码前的验证方法
     * 本方法主要用于验证用户提交的重置密码令牌(token)及新密码的合法性与正确性
     * 它首先通过token验证用户的身份，然后检查新密码是否符合要求，以确保账户安全
     *
     * @param token       用户接收的重置密码邮件中的token，用于验证用户身份
     * @param newPassword 用户设置的新密码，必须与旧密码不同
     * @return 返回验证通过的用户信息对象UserDO，如果验证失败，则抛出异常
     * @throws BusinessException 当用户不存在或新密码与旧密码相同时，抛出此异常
     */
    @Override
    public UserDO checkResetPassword(String token, @NotNull String newPassword) {
        //校验Token
        String uuid = tokenDAO.verifyEmailToken(token);
        tokenDAO.deleteEmailToken(token);

        //根据UUID获取用户信息
        UserDO userDO = userDAO.getUserByUuid(uuid);
        if (userDO == null) {
            throw new BusinessException("用户不存在，意料之外的错误", ErrorCode.OPERATION_ERROR);
        }

        //验证新密码是否与旧密码相同
        if (PasswordUtil.verify(newPassword, userDO.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同", ErrorCode.BODY_ERROR);
        }

        //验证通过，返回用户信息
        return userDO;
    }

    /**
     * 重置用户密码
     *
     * @param userDO      用户实体对象，包含用户的相关信息
     * @param newPassword 新密码，用于替换用户的旧密码
     */
    @Override
    public void resetPassword(@NotNull UserDO userDO, String newPassword) {
        // 对新密码进行加密处理，以确保密码的安全性
        userDO.setPassword(PasswordUtil.encrypt(newPassword));
        // 更新数据库中的用户记录，以保存新的密码信息
        userDAO.updateUserPassword(userDO);
    }

    /**
     * 根据请求检查并更新用户资料
     * 此方法首先根据HTTP请求获取用户信息如果用户不存在，则抛出用户认证异常
     * 对于每个提供的参数（姓名、邮箱、电话），如果参数不为空，则验证其格式
     * 如果格式不正确，则抛出业务异常无论参数是否为空，都将更新用户信息
     *
     * @param name    用户名，可为空，但不能为空字符串，需要符合特定的正则表达式
     * @param email   邮箱地址，可为空，但不能为空字符串，需要符合邮箱的正则表达式
     * @param phone   电话号码，可为空，但不能为空字符串，需要符合电话号码的正则表达式
     * @param request HTTP请求，用于获取当前用户信息
     * @return 更新后的用户信息对象
     * @throws UserAuthenticationException 如果用户不存在
     * @throws BusinessException           如果用户名、邮箱或电话号码格式不正确
     */
    @Override
    public UserDO checkProfile(String name, String email, String phone, HttpServletRequest request) {
        // 根据请求获取用户信息
        UserDO userDO = userService.getUserByRequest(request);
        // 如果用户不存在，抛出用户认证异常
        if (userDO == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
        }
        //控制层已经判断，不为空不为空字符串
        //如果为空则为null，后面的update会判断空值更新
        userDO.setName(name)
                .setEmail(email)
                .setPhone(phone);
        // 返回更新后的用户信息
        return userDO;
    }

    /**
     * 更新并获取用户资料
     * 此方法首先更新用户的资料，然后根据用户UUID获取最新的用户信息，并将其转换为BackProfileDTO对象返回
     * 如果无法找到更新后的用户信息，抛出业务异常
     *
     * @param userDO 用户数据对象，包含用户的基本信息和更新内容
     * @return 返回更新后的用户资料DTO对象
     * @throws BusinessException 当系统错误时抛出此异常
     */
    @Override
    public BackProfileDTO profile(UserDO userDO) {
        UserDO oldUser = userDAO.getUserByUuid(userDO.getUserUuid());
        // 更新用户资料
        userDAO.updateUserProfile(userDO, oldUser);
        // 通过用户UUID获取最新的用户信息
        UserDO newUserDO = userDAO.getUserByUuid(userDO.getUserUuid());
        assert newUserDO != null;
        // 将获取的用户信息转换为DTO对象并返回
        return BeanUtil.toBean(newUserDO, BackProfileDTO.class);
    }

    @Override
    public void changePassword(
            @NotNull String currentPassword, @NotNull String newPassword,
            HttpServletRequest request) {
        // 获取当前用户信息
        UserDO userDO = userService.getUserByRequest(request);
        if (userDO == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
        }
        // 验证当前密码是否正确
        if (!PasswordUtil.verify(currentPassword, userDO.getPassword())) {
            throw new BusinessException("当前密码错误", ErrorCode.BODY_ERROR);
        }
        // 对新密码进行加密处理，以确保密码的安全性
        userDO.setPassword(PasswordUtil.encrypt(newPassword));
        userDAO.updateUserPassword(userDO);
    }
}
