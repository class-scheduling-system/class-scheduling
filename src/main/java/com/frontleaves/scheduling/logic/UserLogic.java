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
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.UserAddVO;
import com.frontleaves.scheduling.models.vo.UserEditVO;
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

import java.util.List;
import java.util.Optional;

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
    private final PermissionDAO permissionDAO;
    private final AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    private final DepartmentDAO departmentDAO;

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
     * 根据用户角色聚合三方信息(学生、教师或普通用户)
     *
     * @param userByRequest 当前登录用户实体
     * @return UserInfoDTO聚合后的用户信息
     */
    @Override
    public UserInfoDTO getUserInfoWithRole(@NotNull UserDO userByRequest) {
        // 校验 roleUuid 是否为空或无效
        // UserDO
        String roleUuid = userByRequest.getRoleUuid();
        if (roleUuid == null || roleUuid.trim().isEmpty()) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }

        // 获取用户的角色信息
        RoleDTO role = roleDAO.getRoleByUuid(roleUuid);
        if (role == null) {
            throw new ServerInternalErrorException(StringConstant.DATABASE_OPERATION_FAILED);
        }

        // 创建 UserInfoDTO，返回基础的用户信息
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        UserDTO userDTO = ProjectUtil.convertUserDoToUserDTO(userByRequest)
                .setRole(role);
        userInfoDTO.setUser(userDTO);

        // 判断角色类型并填充对应的角色信息
        if (role.getRoleUuid().equals(SystemConstant.getRoleStudent())) {
            StudentDO studentDO = studentDAO.getStudentByUserUuid(userByRequest.getUserUuid());
            assert studentDO != null;
            userInfoDTO.setStudent(BeanUtil.toBean(studentDO, StudentDTO.class));
        } else if ("教师".equals(role.getRoleName())) {
            TeacherDO teacherDO = teacherDAO.getTeacherByUserUuid(userByRequest.getUserUuid());
            assert teacherDO != null;
            userInfoDTO.setTeacher(BeanUtil.toBean(teacherDO, TeacherDTO.class));
        }
        return userInfoDTO;
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
        UserDO userDO = userDAO.getUserByUuid(userUuid);
        if (userDO == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
        }
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userDO.getRoleUuid());
        if (roleDTO == null) {
            throw new ServerInternalErrorException("角色不存在");
        }
        //检查是否为学生或者老师
        UserInfoDTO userInfoDTO = new UserInfoDTO();
        if (roleDTO.getRoleUuid().equals(SystemConstant.getRoleStudent())) {
            StudentDO studentDO = studentDAO.lambdaQuery().eq(StudentDO::getUserUuid, userUuid).one();
            assert studentDO != null;
            userInfoDTO.setStudent(BeanUtil.toBean(studentDO, StudentDTO.class));
        }
        if (roleDTO.getRoleUuid().equals(SystemConstant.getRoleTeacher())) {
            TeacherDO teacherDO = teacherDAO.lambdaQuery().eq(TeacherDO::getUserUuid, userUuid).one();
            assert teacherDO != null;
            userInfoDTO.setTeacher(BeanUtil.toBean(teacherDO, TeacherDTO.class));
        }
        UserDTO userDTO = ProjectUtil.convertUserDoToUserDTO(userDO)
                .setRole(BeanUtil.toBean(roleDTO, RoleDTO.class));
        log.debug("UserDTO: {}", userDTO);
        userInfoDTO.setUser(userDTO);
        return userInfoDTO;
    }

    /**
     * 检查用户添加数据的合法性。
     * <p>
     * 该方法用于验证用户在注册或添加时提交的数据是否符合系统规则，包括但不限于：
     * - 角色信息的有效性检查：确保提供的角色UUID对应的角色存在，并且不允许添加学生或教师类型用户。
     * - 用户唯一性检查：通过用户名、邮箱和手机号确保用户信息的唯一性。
     * 如果检测到任何违反规则的情况，则抛出相应的异常。
     *
     * @param userAddVO 用户添加数据对象，包含用户的基本信息和角色信息
     * @return boolean 返回是否否为教务，目前逻辑中固定返回 {@code SystemConstant.getRoleAcademic().equals(roleDTO.getRoleUuid())}
     * @throws BusinessException 当用户数据不符合添加规则时抛出此异常
     */
    @Override
    public boolean checkAddUser(@NotNull UserAddVO userAddVO) {
        // 获取用户角色信息
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userAddVO.getRoleUuid());
        if (roleDTO == null) {
            throw new BusinessException("无此类用户角色", ErrorCode.BODY_ERROR);
        }
        // 禁止添加学生或教师类型用户
        if (SystemConstant.getRoleStudent().equals(roleDTO.getRoleUuid())
                || SystemConstant.getRoleTeacher().equals(roleDTO.getRoleUuid())) {
            throw new BusinessException("此类用户数据不允许添加", ErrorCode.BODY_ERROR);
        }
        // 检查用户信息是否存在重复
        log.debug("检查用户是否存在开始前");
        this.checkUserExist(userAddVO.getName(), userAddVO.getEmail(), userAddVO.getPhone());
        log.debug("检查用户是否存在结束");
        // 返回是否允许添加用户，当前逻辑中固定返回学术角色的判断结果
        if (SystemConstant.getRoleAcademic().equals(roleDTO.getRoleUuid())) {
            if (userAddVO.getDepartment() != null && userAddVO.getType() != null) {
                return true;
            }
            throw new BusinessException("教务部门不能为空", ErrorCode.BODY_ERROR);
        }
        if (userAddVO.getDepartment() != null && !userAddVO.getDepartment().isEmpty()){
            throw new BusinessException("不添加教务的情况下，此处不应有部门数据", ErrorCode.BODY_ERROR);
        }
        if (userAddVO.getType() != null){
            throw new BusinessException("不添加教务的情况下，此处不应有权限类型数据", ErrorCode.BODY_ERROR);
        }
        return false;
    }

    @Override
    public UserAddInfoDTO addUser(UserAddVO userAddVO, Boolean isAcademic) {
        RoleDTO roleDTO = roleDAO.getRoleByUuid(userAddVO.getRoleUuid());
        if (roleDTO == null) {
            throw new BusinessException(StringConstant.USER_DATA_NOT_EXIST, ErrorCode.BODY_ERROR);
        }
        UserAddInfoDTO userInfoDTO = new UserAddInfoDTO();
        //交互用户信息
        UserDO userDO = BeanUtil.toBean(userAddVO, UserDO.class)
                .setUserUuid(UuidUtil.generateUuidNoDash());
        if (userDO.getPassword() == null || userDO.getPassword().isEmpty()) {
            userInfoDTO.setNewPassword(RandomUtil.randomString(8));
            userDO.setPassword(PasswordUtil.encrypt(userInfoDTO.getNewPassword()));
        } else {
            userDO.setPassword(PasswordUtil.encrypt(userDO.getPassword()));
        }
        userDO.setRoleUuid(roleDTO.getRoleUuid());
        if (userAddVO.getPermission() != null) {
            checkPermission(userDO, userAddVO.getPermission());
        }
        log.debug("添加用户UserDO: {}", userDO);
        userDAO.save(userDO);
        if (Boolean.TRUE.equals(isAcademic)) {
            if (userAddVO.getDepartment().isEmpty() && userAddVO.getType() == null) {
                throw new BusinessException("添加部门数据缺少部门Uuid或权限类型 ", ErrorCode.BODY_ERROR);
            }
            DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(userAddVO.getDepartment());
            if (departmentDO == null) {
                throw new BusinessException("部门不存在", ErrorCode.BODY_ERROR);
            }
            AcademicAffairsPermissionDO academicAffairsPermissionDO = new AcademicAffairsPermissionDO();
            academicAffairsPermissionDO.setAuthorizedUser(userDO.getUserUuid())
                    .setDepartment(departmentDO.getDepartmentUuid())
                    .setType(userAddVO.getType());
            academicAffairsPermissionDAO.save(academicAffairsPermissionDO);
        }
        // 构造信息
        UserDO newUserDO = userDAO.getUserByUuid(userDO.getUserUuid());
        if (newUserDO == null) {
            throw new BusinessException("添加用户失败", ErrorCode.OPERATION_ERROR);
        }
        UserDTO userDTO = ProjectUtil.convertUserDoToUserDTO(newUserDO)
                .setRole(BeanUtil.toBean(roleDTO, RoleDTO.class));
        log.debug("添加用户最后的UserDTO: {}", userDTO);
        userInfoDTO.setUser(userDTO);
        return userInfoDTO;
    }


    /**
     * 检查权限
     *
     * @param userDO        用户数据对象
     * @param permissionKey 权限键列表
     */
    private void checkPermission(UserDO userDO, List<String> permissionKey) {
        if (!permissionKey.isEmpty()) {
            for (String permission : permissionKey) {
                if (permissionDAO.getPermissionKey(permission) == null) {
                    throw new BusinessException("权限不存在", ErrorCode.BODY_ERROR);
                }
            }
            String jsonPermission = JSONUtil.toJsonStr(permissionKey);
            // 保持 JSON 格式存储
            userDO.setPermission(jsonPermission);
        }
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
            throw new ServerInternalErrorException("角色不存在，意料之外的错误");
        }
        if (roleDTO.getRoleUuid().equals(SystemConstant.getRoleStudent())) {
            StudentDO studentDO = studentDAO.getStudentByUserUuid(userUuid);
            log.debug("删除学生信息");
            if (studentDO != null) {
                studentDAO.deleteStudent(studentDO);
            }
            userDAO.deleteUser(userDO);
        } else if (roleDTO.getRoleUuid().equals(SystemConstant.getRoleTeacher())) {
            TeacherDO teacherDO = teacherDAO.getTeacherByUserUuid(userUuid);
            log.debug("删除教师信息");
            if (teacherDO != null) {
                teacherDAO.deleteTeacher(teacherDO);
            }
            userDAO.deleteUser(userDO);
        } else {
            log.debug("删除用户信息");
            userDAO.deleteUser(userDO);
        }
    }

    /**
     * 检查用户更新数据的合法性并返回用户旧的角色信息。
     * <p>
     * 该方法主要用于验证用户在编辑时提交的数据是否符合系统规则，包括但不限于用户名、邮箱、手机号的唯一性检查，
     * 以及角色变更的限制。如果检测到任何违反规则的情况，则抛出相应的异常。
     *
     * @param userOldDO  原始用户数据对象，包含用户的基本信息和角色信息
     * @param userEditVO 用户编辑数据对象，包含用户希望修改的信息
     * @param request    HTTP请求对象，用于上下文信息传递（如异常处理）
     * @return RoleDTO 返回用户原始的角色信息
     * @throws UserAuthenticationException  当用户不存在时抛出此异常
     * @throws BusinessException            当用户数据不符合更新规则时抛出此异常
     * @throws ServerInternalErrorException 当角色信息缺失或无效时抛出此异常
     */
    @Contract("null, _, _ -> fail")
    private @NotNull RoleDTO checkUpdateDate(UserDO userOldDO, UserEditVO userEditVO, HttpServletRequest request) {
        // 验证用户是否存在
        if (userOldDO == null) {
            throw new UserAuthenticationException(UserAuthenticationException.ErrorType.USER_NOT_EXIST, request);
        }

        // 验证用户名唯一性
        if (!userEditVO.getName().isEmpty()
                && !userEditVO.getName().equals(userOldDO.getName())
                && userDAO.getUserByName(userEditVO.getName()) != null) {
            throw new BusinessException("用户名已存在", ErrorCode.BODY_ERROR);
        }

        // 验证邮箱唯一性
        if (!userEditVO.getEmail().isEmpty()
                && !userEditVO.getEmail().equals(userOldDO.getEmail())
                && userDAO.getUserByMail(userEditVO.getEmail()) != null) {
            throw new BusinessException("邮箱已存在", ErrorCode.BODY_ERROR);
        }

        // 验证手机号唯一性
        if (!userEditVO.getPhone().isEmpty()
                && !userEditVO.getPhone().equals(userOldDO.getPhone())
                && userDAO.getUserByTel(userEditVO.getPhone()) != null) {
            throw new BusinessException("手机号已存在", ErrorCode.BODY_ERROR);
        }

        // 获取用户原始角色信息
        RoleDTO roleOldDTO = roleDAO.getRoleByUuid(userOldDO.getRoleUuid());
        if (roleOldDTO == null) {
            throw new ServerInternalErrorException("角色不存在意料之外的错误");
        }

        // 禁止编辑学生或教师类型用户
        if (roleOldDTO.getRoleUuid().equals(SystemConstant.getRoleStudent())
                || roleOldDTO.getRoleUuid().equals(SystemConstant.getRoleTeacher())) {
            throw new BusinessException("此类用户数据不允许编辑", ErrorCode.BODY_ERROR);
        }

        // 禁止将用户角色更改为学生或教师
        if (!userEditVO.getRoleUuid().isEmpty()
                && (userEditVO.getRoleUuid().equals(SystemConstant.getRoleStudent())
                || userEditVO.getRoleUuid().equals(SystemConstant.getRoleTeacher()))) {
            throw new BusinessException("不允许将角色编辑为学生或者老师", ErrorCode.BODY_ERROR);
        }

        // 返回用户原始角色信息
        return roleOldDTO;
    }


    @Override
    @Transactional
    public UserInfoDTO updateUser(@NotNull String userUuid, UserEditVO userEditVO, HttpServletRequest request) {
        UserDO userOldDO = userDAO.getUserByUuid(userUuid);
        RoleDTO roleOldDTO = checkUpdateDate(userOldDO, userEditVO, request);
        log.debug("更新用户信息开始");
        UserDO userNewDO = this.exchangeOfUserData(userEditVO, userOldDO);
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
    public PageDTO<UserInfoDTO> getUserList(
            int page, int size, String keyWord, boolean isDesc,
            HttpServletRequest request
    ) {
        Page<UserDO> userDoPage = userDAO.getUserDoPage(page, size, keyWord, isDesc);
        List<UserInfoDTO> userInfoDTOList = userDoPage
                .getRecords().stream()
                .map(userDO -> {
                    //检查是否为学生或者老师
                    RoleDTO roleDTO = roleDAO.getRoleByUuid(userDO.getRoleUuid());
                    assert roleDTO != null;
                    UserInfoDTO userInfoDTO = new UserInfoDTO();
                    UserDTO userDTO = ProjectUtil.convertUserDoToUserDTO(userDO)
                            .setRole(BeanUtil.toBean(roleDTO, RoleDTO.class));
                    userInfoDTO.setUser(userDTO);
                    if (roleDTO.getRoleUuid().equals(SystemConstant.getRoleStudent())) {
                        StudentDO studentDO = studentDAO.getStudentByUserUuid(userDO.getUserUuid());
                        StudentDTO studentDTO = BeanUtil.toBean(studentDO, StudentDTO.class);
                        userInfoDTO.setStudent(studentDTO);
                    }
                    if (roleDTO.getRoleUuid().equals(SystemConstant.getRoleTeacher())) {
                        TeacherDO teacherDO = teacherDAO.getTeacherByUserUuid(userDO.getUserUuid());
                        TeacherDTO teacherDTO = BeanUtil.toBean(teacherDO, TeacherDTO.class);
                        userInfoDTO.setTeacher(teacherDTO);
                    }
                    return userInfoDTO;
                }).toList();
        return ProjectUtil.convertPageToPageDTO(userDoPage, UserInfoDTO.class)
                .setRecords(userInfoDTOList);
    }

    /**
     * 检查页数和每页大小
     *
     * @param page 页数
     * @param size 每页大小
     */
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
    @Contract("_, _ -> param2")
    private UserDO exchangeOfUserData(@NotNull UserEditVO userEditVO, UserDO userDO) {
        //检查是否为空并且赋值
        Optional.ofNullable(userEditVO.getName())
                .filter(name -> !name.isEmpty()).ifPresent(userDO::setName);
        Optional.ofNullable(userEditVO.getPassword())
                .filter(password -> !password.isEmpty())
                .ifPresent(password -> userDO.setPassword(PasswordUtil.encrypt(password)));
        Optional.ofNullable(userEditVO.getEmail())
                .filter(email -> !email.isEmpty()).ifPresent(userDO::setEmail);
        Optional.ofNullable(userEditVO.getPhone())
                .filter(phone -> !phone.isEmpty()).ifPresent(userDO::setPhone);
        Optional.ofNullable(userEditVO.getStatus())
                .ifPresent(userDO::setStatus);
        Optional.ofNullable(userEditVO.getBan())
                .ifPresent(userDO::setBan);
        if (!userEditVO.getRoleUuid().isEmpty()) {
            log.debug("用户角色：{}", userEditVO.getRoleUuid());
            log.debug("改变用户角色");
            RoleDTO roleNewDTO = roleDAO.getRoleByUuid(userEditVO.getRoleUuid());
            if (roleNewDTO == null) {
                throw new BusinessException(StringConstant.USER_DATA_NOT_EXIST, ErrorCode.BODY_ERROR);
            }
            if (roleNewDTO.getRoleUuid().equals(SystemConstant.getRoleStudent())
                    || roleNewDTO.getRoleUuid().equals(SystemConstant.getRoleTeacher())) {
                throw new BusinessException("不允许将角色编辑为学生或者老师", ErrorCode.BODY_ERROR);
            }
            userDO.setRoleUuid(userEditVO.getRoleUuid());
        }
        this.checkPermission(userDO, userEditVO.getPermission());

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
