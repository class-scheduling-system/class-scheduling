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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserAddVO;
import com.frontleaves.scheduling.models.vo.UserEditVO;
import com.frontleaves.scheduling.services.UserService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.HeaderUtil;
import com.xlf.utility.util.PasswordUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    private final UserDAO userDAO;
    private final StudentDAO studentDAO;
    private final RoleDAO roleDAO;
    private final TeacherDAO teacherDAO;

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
                throw new BusinessException("", ErrorCode.OPERATION_ERROR);
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
        this.checkUserExist(userAddVO.getName(), userAddVO.getEmail(), userAddVO.getPhone());
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
            StudentDO studentDO = studentDAO.lambdaQuery().eq(StudentDO::getUserUuid, userUuid).one();
            if (studentDO == null) {
                throw new BusinessException("学生信息不存在", ErrorCode.OPERATION_ERROR);
            }
            log.info("删除学生信息");
            studentDAO.deleteStudent(studentDO);
            userDAO.deleteUser(userDO);
        } else if ("老师".equals(roleDTO.getRoleName())) {
            TeacherDO teacherDO = teacherDAO.lambdaQuery().eq(TeacherDO::getUserUuid, userUuid).one();
            if (teacherDO == null) {
                throw new BusinessException("教师信息不存在", ErrorCode.OPERATION_ERROR);
            }
            log.info("删除教师信息");
            teacherDAO.deleteTeacher(teacherDO);
            userDAO.deleteUser(userDO);
        } else {
            log.info("删除用户信息");
            userDAO.deleteUser(userDO);
        }
    }

    @Override
    @Transactional
    public UserInfoDTO updateUser(@NotNull String userUuid, UserEditVO userEditVO, HttpServletRequest request) {
        UserDO userOldDO = userDAO.getUserByUuid(userUuid);
        if (userEditVO == null) {
            throw new BusinessException("用户编辑数据为空", ErrorCode.BODY_ERROR);
        }
        if (userOldDO == null) {
            throw new UserAuthenticationException(
                    UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
        }
        if (!userEditVO.getName().isEmpty()
                && !userEditVO.getName().equals(userOldDO.getName())
                && userDAO.getUserByName(userEditVO.getName()) != null) {
            throw new BusinessException("用户名已存在", ErrorCode.BODY_ERROR);
        }
        if (!userEditVO.getEmail().isEmpty()
                && !userEditVO.getEmail().equals(userOldDO.getEmail())
                && userDAO.getUserByMail(userEditVO.getEmail()) != null) {

            throw new BusinessException("邮箱已存在", ErrorCode.BODY_ERROR);
        }
        if (!userEditVO.getPhone().isEmpty()
                && !userEditVO.getPhone().equals(userOldDO.getPhone())
                && userDAO.getUserByTel(userEditVO.getPhone()) != null) {
            throw new BusinessException("手机号已存在", ErrorCode.BODY_ERROR);
        }
        //检查原先是否为学生或者老师
        RoleDTO roleOldDTO = roleDAO.getRoleByUuid(userOldDO.getRoleUuid());
        if (roleOldDTO == null) {
            throw new BusinessException("角色不存在意料之外的错误", ErrorCode.OPERATION_ERROR);
        }
        if ("学生".equals(roleOldDTO.getRoleName()) || "老师".equals(roleOldDTO.getRoleName())) {
            throw new BusinessException("此类用户数据不允许编辑", ErrorCode.BODY_ERROR);
        }
        log.debug("更新用户信息开始");
        UserDO userNewDO = exchangeOfUserData(userEditVO, userOldDO);
        userDAO.updateUser(userOldDO, userNewDO);
        log.debug("更新用户信息结束");
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        UserDTO userDTO = ProjectUtil.convertUserDoToUserDTO(userNewDO);
        if (userEditVO.getRoleUuid() != null && !userEditVO.getRoleUuid().isEmpty()) {
            RoleDTO roleNewDTO = roleDAO.getRoleByUuid(userEditVO.getRoleUuid());
            userDTO.setRole(BeanUtil.toBean(roleNewDTO, RoleDTO.class));
        } else {
            userDTO.setRole(BeanUtil.toBean(roleOldDTO, RoleDTO.class));
        }
        userInfoDTO.setUser(userDTO);
        return userInfoDTO;
    }

    @Override
    public PageDTO<UserInfoDTO> getUserList(@NotNull Integer page, @NotNull Integer size, String keyWord,
                                            Boolean isDesc, HttpServletRequest request) {
        Page<UserDO> userDOPage = userDAO.getUserList(page, size, keyWord, isDesc);
        List<UserInfoDTO> userInfoDTOList = userDOPage.getRecords().stream()
                .map(userDO -> {
                    //检查是否为学生或者老师
                    RoleDTO roleDTO = roleDAO.getRoleByUuid(userDO.getRoleUuid());
                    if (roleDTO == null) {
                        throw new BusinessException("角色不存在", ErrorCode.OPERATION_ERROR);
                    }
                    UserDTO userDTO;
                    UserInfoDTO userInfoDTO = new UserInfoDTO();
                    userDTO = ProjectUtil.convertUserDoToUserDTO(userDO)
                            .setRole(BeanUtil.toBean(roleDTO, RoleDTO.class));
                    userInfoDTO.setUser(userDTO);
                    if (roleDTO.getRoleUuid().equals(SystemConstant.getRoleStudent())) {
                        StudentDO studentDO = studentDAO.getStudentByUserUuid(userDO.getUserUuid());
                        StudentDTO studentDTO = BeanUtil.toBean(studentDO, StudentDTO.class);
                        userInfoDTO.setStudent(studentDTO);
                    }
                    if ("老师".equals(roleDTO.getRoleName())) {
                        TeacherDO teacherDO = teacherDAO.getTeacherByUserUuid(userDO.getUserUuid());
                        TeacherDTO teacherDTO = BeanUtil.toBean(teacherDO, TeacherDTO.class);
                        userInfoDTO.setTeacher(teacherDTO);
                    }
                    return userInfoDTO;
                }).toList();
        if (userInfoDTOList.isEmpty()) {
            throw new BusinessException("用户数据为空", ErrorCode.OPERATION_ERROR);
        }
        return ProjectUtil.convertPageToPageDTO(userDOPage, UserInfoDTO.class)
                .setRecords(userInfoDTOList);
    }

    @Override
    public void checkPageAndSize(Integer page, Integer size) {
        if (page == null || page < 1) {
            throw new BusinessException("页数错误", ErrorCode.PARAMETER_ERROR);
        }
        if (size == null || size < 1) {
            throw new BusinessException("每页大小错误", ErrorCode.PARAMETER_ERROR);
        }
    }

    /**
     * 交换用户数据
     *
     * @param userEditVO 用户编辑数据
     * @param userDO     用户数据对象
     * @return 用户数据对象
     */
    private UserDO exchangeOfUserData(UserEditVO userEditVO, UserDO userDO) {
        if (!userEditVO.getName().isEmpty()) {
            userDO.setName(userEditVO.getName());
        }
        if (!userEditVO.getPassword().isEmpty()) {
            userDO.setPassword(PasswordUtil.encrypt(userEditVO.getPassword()));
        }
        if (!userEditVO.getEmail().isEmpty()) {
            userDO.setEmail(userEditVO.getEmail());
        }
        if (!userEditVO.getPhone().isEmpty()) {
            userDO.setPhone(userEditVO.getPhone());
        }
        if (!userEditVO.getRoleUuid().isEmpty()) {
            log.debug("用户角色：{}", userEditVO.getRoleUuid());
            log.debug("改变用户角色");
            RoleDTO roleNewDTO = roleDAO.getRoleByUuid(userEditVO.getRoleUuid());
            if (roleNewDTO == null) {
                throw new BusinessException("此类用户数据不存在", ErrorCode.BODY_ERROR);
            }
            if ("学生".equals(roleNewDTO.getRoleName()) || "老师".equals(roleNewDTO.getRoleName())) {
                throw new BusinessException("不允许将角色编辑为学生或者老师", ErrorCode.BODY_ERROR);
            }
            userDO.setRoleUuid(userEditVO.getRoleUuid());
        }
        if (!userEditVO.getPermission().isEmpty()) {
            userDO.setPermission(JSONUtil.toJsonStr(userEditVO.getPermission()));
        }
        log.debug("更改对象：{}", userEditVO.getName());
        log.debug("用户数据对象：{}", userDO);
        return userDO;
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
    @Override
    public void checkUserExist(String username, String email, String phone) throws BusinessException {
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
}
