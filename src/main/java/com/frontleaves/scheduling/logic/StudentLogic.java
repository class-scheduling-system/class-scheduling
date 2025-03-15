package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.StudentDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.ClassMappingDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.dto.StudentDisableDTO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.frontleaves.scheduling.utils.MappingUtil;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentLogic implements StudentService {

    private final StudentDAO studentDAO;
    private final UserDAO userDAO;
    private final MappingUtil mappingUtil;

    /**
     * 根据 studentUuid 获取学生信息
     */
    @Override
    public StudentDTO getStudentByUuid(String studentUuid) {
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }
        return BeanUtil.toBean(studentDO, StudentDTO.class);
    }

    /**
     * 获取学生列表信息
     *
     * @param page        页码,从1开始
     * @param size        每页记录数
     * @param isDesc      是否降序排列
     * @param clazz       可选参数,班级名称
     * @param isGraduated 可选参数,是否毕业
     * @param name        可选参数,学生姓名
     * @param id          可选参数,学生ID
     * @return 返回一个包含学生信息的PageDTO对象
     */
    @Override
    public PageDTO<StudentDTO> getStudentList(int page, int size, Boolean isDesc,
                                              @Nullable String clazz, @Nullable Boolean isGraduated,
                                              @Nullable String name, @Nullable String id
    ) {
        // 调用DAO层方法获取分页学生数据
        Page<StudentDO> resultPage = studentDAO.listStudents(page, size, isDesc, clazz, isGraduated, name, id);

        // 使用ProjectUtil 中的 convertPageToPageDTO 方法进行抓换
        return ProjectUtil.convertPageToPageDTO(resultPage, StudentDTO.class);
    }

    /**
     * 添加学生信息
     * <p>
     * 此方法负责接收一个学生对象,验证其信息的完整性,格式的正确性,
     * 然后将学生信息保存到数据库中,并转换为学生DTO对象
     * </p>
     *
     * @param studentDTO 学生对象,包含学生的基本信息
     * @return 保存后的学生对象
     */
    @Override
    public StudentDTO addStudent(StudentDTO studentDTO) {
        // 生成 UUID,避免由前端传递
        String studentUuid = UuidUtil.generateUuidNoDash();

        // 校验学生姓名
        if (!Pattern.matches(StringConstant.Regular.STUDENT_NAME_REGULAR_EXPRESSION, studentDTO.getName())) {
            throw new BusinessException("学生姓名格式错误", ErrorCode.PARAMETER_INVALID);
        }
        // 检查班级是否为空(进行外键映射)
        if (studentDTO.getClazz() == null || studentDTO.getClazz().isBlank()) {
            throw new BusinessException("班级不能为空", ErrorCode.PARAMETER_INVALID);
        }

        // 根据班级信息自动定位年级、学院和专业
        ClassMappingDTO classMapping = mappingUtil.getClassMappingByClazz(studentDTO.getClazz());
        // 将映射出来的信息设置到DTO中
        studentDTO.setGradeUuid(classMapping.getGradeUuid());
        studentDTO.setDepartment(classMapping.getDepartmentUuid());
        studentDTO.setMajor(classMapping.getMajorUuid());

        // DTO -> DO
        StudentDO studentDO = BeanUtil.toBean(studentDTO, StudentDO.class);
        studentDO.setStudentUuid(studentUuid);

        // 存入数据库
        try {
            studentDAO.save(studentDO);
        } catch (Exception e) {
            log.error("学生信息保存失败", e);
            throw new BusinessException("学生信息保存失败", ErrorCode.OPERATION_FAILED);
        }
        // 从数据库重新查询完整记录,确保自动填充的数据能获取到
        StudentDO saveStudentDO = studentDAO.getStudentByUuid(studentUuid);
        if (saveStudentDO == null) {
            throw new BusinessException("学生信息保存失败", ErrorCode.OPERATION_FAILED);
        }

        // DO -> DTO
        StudentDTO resultDTO = BeanUtil.toBean(studentDO, StudentDTO.class);
        resultDTO.setStatus(resultDTO.getUserUuid() != null && !resultDTO.getUserUuid().isBlank());
        return resultDTO;
    }

    /**
     * 根据学生UUID禁用或启用学生账户
     * <p>
     * 此方法首先根据学生UUID获取学生详细信息,然后检查该学生是否已创建系统账号
     * 如果学生存在且已创建系统账号,则根据传入的禁用标志更新用户状态
     * </p>
     *
     * @param studentUuid 学生的唯一UUID
     * @param disable 是否禁用学生账户的标志,true表示禁用,false表示启用
     * @return 返回一个包含学生UUID和禁用状态的DTO对象
     */
    @Override
    public StudentDisableDTO disableStudent(String studentUuid, Boolean disable) {
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }

        // 检查学生是否已有账号(是否注册)
        if (studentDO.getUserUuid() == null || studentDO.getUserUuid().isBlank()) {
            throw new BusinessException("该学生未创建系统账号", ErrorCode.NOT_EXIST);
        }

        // 获取用户对象
        UserDO oldUserDO = userDAO.getUserByUuid(studentDO.getUserUuid());
        if (oldUserDO == null) {
            throw new BusinessException(StringConstant.USER_NOT_EXIST, ErrorCode.NOT_EXIST);
        }
        UserDO newUserDO = BeanUtil.toBean(oldUserDO, UserDO.class);

        // 更新用户状态
        newUserDO.setStatus((byte) (disable ? 0 : 1));
        userDAO.updateUser(oldUserDO, newUserDO);

        return new StudentDisableDTO()
                .setStudentUuid(studentDO.getStudentUuid())
                .setStatus(disable);
    }


    /**
     * 删除学生
     * <p>
     * 此方法首先通过学生UUID检查学生是否存在如果学生存在,进一步检查学生是否已注册
     * 如果学生未注册,则执行删除操作；如果已注册或不存在,则抛出相应的异常
     * </p>
     *
     * @param studentUuid 学生的唯一标识符
     * @throws BusinessException 当学生不存在或学生已注册时抛出
     */
    @Override
    public void deleteStudent(String studentUuid) {
        // 检查学生是否存在
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }

        // 检查学生是否已注册
        boolean isRegistered = userDAO.existsByUserUuid(studentDO.getUserUuid());
        if (isRegistered) {
            throw new BusinessException("学生已注册,无法删除", ErrorCode.OPERATION_FAILED);
        }

        // 删除学生
        studentDAO.removeById(studentUuid);
    }

    /**
     * 编辑学生信息
     *
     * @param studentUuid 学生的唯一标识符
     * @param studentVO 包含学生新信息的视图对象
     * @return 更新后的学生数据传输对象
     * @throws BusinessException 如果学生不存在或更新操作失败
     */
    @Override
    public StudentDTO editStudent(String studentUuid, StudentVO studentVO) {
        StudentDO studentDO = studentDAO.editStudent(studentUuid, studentVO);
        StudentDTO studentDTO = new StudentDTO();
        BeanUtils.copyProperties(studentDO, studentDTO);
        return studentDTO;
    }
}
