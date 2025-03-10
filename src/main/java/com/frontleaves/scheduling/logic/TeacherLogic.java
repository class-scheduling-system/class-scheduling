package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherDTO;
import com.frontleaves.scheduling.models.dto.TeacherDisableDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.entity.multiple.UserAndTeacherDO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import com.frontleaves.scheduling.services.TeacherService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherLogic implements TeacherService {
    private final DepartmentDAO departmentDAO;
    private final UserDAO userDAO;
    private final TeacherDAO teacherDAO;

    /**
     * 添加教师信息
     * <p>
     * 本方法首先根据传入的部门UUID和用户UUID验证部门和用户的存在性，
     * 如果不存在则抛出业务异常接着将TeacherVO对象的属性复制到TeacherDO对象中，
     * 生成一个新的UUID作为教师UUID，并保存到数据库中
     *
     * @param teacherVO 教师视图对象，包含要添加的教师的相关信息
     * @throws BusinessException 如果部门或用户不存在，则抛出此异常
     */
    @Override
    public void addTeacher(@NotNull TeacherVO teacherVO) {
        // 根据部门UUID获取部门信息
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(teacherVO.getUnitUuid());
        // 如果部门不存在，抛出业务异常
        if (departmentDO == null) {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }

        // 根据用户UUID获取用户信息
        UserDO userDO = userDAO.getUserByUuid(teacherVO.getUserUuid());
        // 如果用户不存在，抛出业务异常
        if (userDO == null) {
            throw new BusinessException(StringConstant.USER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }

        // 创建一个新的TeacherDO对象
        TeacherDO teacherDO = new TeacherDO();
        // 将TeacherVO对象的属性复制到TeacherDO对象中
        BeanUtils.copyProperties(teacherVO, teacherDO);
        // 生成一个新的UUID作为教师UUID，并设置到TeacherDO对象中
        teacherDO.setTeacherUuid(UuidUtil.generateUuidNoDash());

        // 保存TeacherDO对象到数据库中
        teacherDAO.save(teacherDO);
    }

    /**
     * 根据教师UUID获取教师详情
     * <p>
     * 此方法首先验证给定的教师UUID是否符合无连字符的UUID正则表达式，
     * 如果符合，则调用数据访问对象方法获取对应的教师详细信息，
     * 如果教师信息不存在，则抛出业务异常，表示教师不存在，
     * 最后，将获取到的教师信息转换为目标DTO类并返回
     *
     * @param teacherUuid 教师的UUID，用于唯一标识教师
     * @return TeacherDTO 教师详情的数据传输对象
     * @throws BusinessException 当教师不存在时抛出的业务异常
     */
    @Override
    public TeacherDTO getTeacher(String teacherUuid) {
        TeacherDO teacherDTO = null;
        // 验证教师UUID是否符合无连字符的UUID正则表达式
        if (teacherUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            // 根据UUID获取教师详细信息
            teacherDTO = teacherDAO.getTeacherByUuid(teacherUuid);
        }
        // 如果教师信息不存在，抛出业务异常
        if (teacherDTO == null) {
            throw new BusinessException(StringConstant.TEACHER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        // 将教师信息转换为目标DTO类并返回
        return BeanUtil.toBean(teacherDTO, TeacherDTO.class);
    }

    /**
     * 获取教师列表方法
     * <p>
     * 该方法用于根据指定的分页参数和筛选条件获取教师信息列表它首先调用DAO层方法获取原始数据，
     * 然后根据获取的数据情况决定返回一个空的PageDTO还是转换后的TeacherDTO页面对象
     *
     * @param page       页码，表示请求的页面编号
     * @param size       页面大小，表示每页包含的记录数
     * @param isDesc     是否降序，用于指定结果的排序方式
     * @param department 部门名称，用于筛选属于特定部门的教师
     * @param status     状态，用于筛选具有特定状态的教师
     * @param name       教师姓名，用于筛选姓名中包含特定字符串的教师
     * @return 返回一个PageDTO<TeacherDTO>对象，其中包含根据筛选条件查询到的教师信息
     */
    @Override
    public PageDTO<TeacherDTO> getTeacherList(Integer page, Integer size, Boolean isDesc, String department, String status, String name) {
        // 调用DAO层方法获取教师列表
        List<UserAndTeacherDO> teacherList;
        if (status == null || status.isBlank()) {
            teacherList = teacherDAO.getTeacherList(page, size, isDesc, department, null, name);
        } else {
            if (Boolean.parseBoolean(status)) {
                teacherList = teacherDAO.getTeacherList(page, size, isDesc, department, 1, name);
            } else {
                teacherList = teacherDAO.getTeacherList(page, size, isDesc, department, 0, name);
            }
        }
        
        // 如果查询结果为null，应该返回一个空的PageDTO对象
        if (teacherList == null || teacherList.isEmpty()) {
            return new PageDTO<TeacherDTO>()
                    .setTotal(0L)
                    .setSize(Long.valueOf(size))
                    .setRecords(new ArrayList<>())
                    .setCurrent(Long.valueOf(page));
        } else {
            // 将获取的教师列表转换为TeacherDTO对象列表
            List<TeacherDTO> teacherDTOList = teacherList.stream()
                    .map(teacher -> BeanUtil
                            .toBean(teacher.getTeacher(), TeacherDTO.class)
                            .setStatus(teacher.getUser() != null ? teacher.getUser().getStatus() : 2)
                    )
                    .toList();
            return new PageDTO<TeacherDTO>()
                    .setTotal((long) teacherList.size())
                    .setSize(Long.valueOf(size))
                    .setRecords(teacherDTOList)
                    .setCurrent(Long.valueOf(page));
        }
    }

    /**
     * 禁用教师接口的实现方法
     *
     * @param teacherUuid 教师的唯一标识符
     * @param disable     是否禁用教师，true代表禁用，false代表不禁用
     * @return 返回一个包含教师禁用信息的DTO对象
     * <p>
     * 此方法首先根据教师的UUID从数据库中获取教师对象，然后检查教师是否存在以及是否已注册
     * 如果教师存在且已注册，则根据提供的禁用状态更新其用户账户状态
     */
    @Override
    public TeacherDisableDTO disableTeacher(String teacherUuid, Boolean disable) {
        // 根据教师UUID获取教师对象
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(teacherUuid);
        // 如果教师不存在，抛出异常
        if (teacherDO == null) {
            throw new BusinessException(StringConstant.TEACHER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        // 如果教师未注册，抛出异常
        if (teacherDO.getUserUuid() == null || teacherDO.getUserUuid().isBlank()) {
            throw new BusinessException("教师未注册", ErrorCode.NOT_EXIST);
        }
        // 根据教师关联的用户UUID获取用户对象
        UserDO oldUserDO = userDAO.getUserByUuid(teacherDO.getUserUuid());
        // 创建一个新的用户对象，用于更新用户状态
        UserDO newUserDO = BeanUtil.toBean(oldUserDO, UserDO.class);
        // 如果用户不存在，抛出异常
        if (oldUserDO == null) {
            throw new BusinessException(StringConstant.USER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        // 更新用户状态为禁用或不禁用
        newUserDO.setStatus((short) (Boolean.compare(disable, true) == 0 ? 0 : 1));
        // 更新数据库中的用户对象
        userDAO.updateUser(oldUserDO, newUserDO);
        // 创建并返回一个包含教师禁用信息的DTO对象
        return new TeacherDisableDTO()
                .setTeacherUuid(teacherDO.getTeacherUuid())
                .setStatus(disable);
    }

    /**
     * 删除教师信息
     *
     * @param teacherUuid 教师的唯一标识符UUID
     * @throws BusinessException 如果教师不存在或已注册，将抛出此异常
     */
    @Override
    public void deleteTeacher(String teacherUuid) {
        // 根据UUID获取教师对象
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(teacherUuid);

        // 检查教师是否存在，如果不存在则抛出异常
        if (teacherDO == null) {
            throw new BusinessException(StringConstant.TEACHER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }

        // 检查教师是否已注册，如果已注册则抛出异常
        if (teacherDO.getUserUuid() != null && !teacherDO.getUserUuid().isBlank()) {
            throw new BusinessException("教师已注册，无法删除", ErrorCode.EXISTED);
        }

        // 调用DAO方法删除教师信息
        teacherDAO.deleteTeacher(teacherDO);
    }

    /**
     * 更新教师信息
     *
     * @param teacherUuid 教师的唯一标识符
     * @param teacherVO 包含教师更新信息的视图对象
     *
     * 此方法首先根据教师的UUID从数据库中获取教师对象如果教师对象存在，则根据提供的教师视图对象中的信息更新教师对象
     * 如果视图对象中包含了单位UUID，则验证该单位是否存在如果单位不存在，则抛出业务异常
     * 同样，如果视图对象中包含了用户UUID，则验证该用户是否存在如果用户不存在，则抛出业务异常
     * 最后，将视图对象中的属性复制到教师对象中，并更新数据库中的教师信息
     */
    @Override
    public void updateTeacher(String teacherUuid, TeacherVO teacherVO) {
        // 根据UUID获取教师对象
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(teacherUuid);
        if (teacherDO != null) {
            // 如果教师视图对象中包含单位UUID，则检查单位是否存在
            if (teacherVO.getUnitUuid() != null) {
                DepartmentDO getDepartment = departmentDAO.getDepartmentByUuid(teacherVO.getUnitUuid());
                if (getDepartment == null) {
                    throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
                }
            }
            // 如果教师视图对象中包含用户UUID，则检查用户是否存在
            if (teacherVO.getUserUuid() != null) {
                UserDO getUser = userDAO.getUserByUuid(teacherVO.getUserUuid());
                if (getUser == null) {
                    throw new BusinessException(StringConstant.USER_NOT_EXIST, ErrorCode.NOT_EXIST);
                }
            }
            // 将视图对象中的属性复制到教师对象中，并更新数据库中的教师信息
            BeanUtils.copyProperties(teacherVO, teacherDO);
            teacherDAO.updateTeacher(teacherDO);
        }
    }
}