package com.frontleaves.scheduling.logic;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.StudentDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.dto.StudentDisableDTO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /**
     * 根据 studentUuid 获取学生信息
     */
    @Override
    public StudentDTO getStudentByUuid(String studentUuid) {
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }
        return convertToStudentDTO(studentDO);
    }

    /**
     * 获取学生列表信息
     *
     * @param page        页码，从1开始
     * @param size        每页记录数
     * @param isDesc      是否降序排列
     * @param clazz       可选参数，班级名称
     * @param isGraduated 可选参数，是否毕业
     * @param name        可选参数，学生姓名
     * @param id          可选参数，学生ID
     * @return 返回一个包含学生信息的PageDTO对象
     */
    @Override
    public PageDTO<StudentDTO> getStudentList(int page, int size, Boolean isDesc, @Nullable String clazz, @Nullable Boolean isGraduated, @Nullable String name, @Nullable String id) {
        // 调用DAO层方法获取分页学生数据
        Page<StudentDO> resultPage = studentDAO.listStudents(page, size, isDesc, clazz, isGraduated, name, id);

        // 将查询结果转换为StudentDTO列表
        List<StudentDTO> dtoList = resultPage.getRecords().stream().map(studentDO -> {
            StudentDTO studentDTO = new StudentDTO();
            // 使用Spring框架的BeanUtils工具类进行属性复制
            BeanUtils.copyProperties(studentDO, studentDTO);
            return studentDTO;
        }).toList();

        // 构造并返回PageDTO对象
        return new PageDTO<>(dtoList, resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
    }


    @Override
    public StudentDO addStudent(StudentDO studentVO) {
        // 校检数据完整性
        if (studentVO == null || studentVO.getId() == null || studentVO.getName() == null || studentVO.getGender() == null || studentVO.getGradeUuid() == null || studentVO.getClazz() == null) {
            throw new IllegalArgumentException("学生信息不完整");
        }
        if (!Pattern.matches(StringConstant.Regular.STUDENT_ID_REGULAR_EXPRESSION, studentVO.getStudentUuid())) {
            throw new BusinessException("学生UUID格式错误", ErrorCode.PARAMETER_INVALID);
        }
        if (!Pattern.matches(StringConstant.Regular.STUDENT_NAME_REGULAR_EXPRESSION, studentVO.getName())) {
            throw new BusinessException("学生姓名格式错误", ErrorCode.PARAMETER_INVALID);
        }

        StudentDO studentDO = new StudentDO();
        studentDO.setStudentUuid(studentVO.getStudentUuid())
                .setId(studentVO.getId())
                .setName(studentVO.getName())
                .setGender(studentVO.getGender())
                .setGradeUuid(studentVO.getGradeUuid())
                .setClazz(studentVO.getClazz())
                .setDepartment(studentVO.getDepartment())
                .setMajor(studentVO.getMajor());

        // 存入数据库
        studentDAO.save(studentDO);
        convertToStudentDTO(studentDO);
        return studentDO;
    }

    @Override
    public StudentDisableDTO disableStudent(String studentUuid, Boolean disable) {
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }

        // 检查学生是否已有账号
        boolean hasAccount = userDAO.exists(new LambdaQueryWrapper<UserDO>().eq(UserDO::getUserUuid, studentUuid));
        if (!hasAccount) {
            throw new BusinessException("该学生未创建系统账号", ErrorCode.NOT_EXIST);
        }

        boolean updated = studentDAO.updateStudentStatus(studentUuid, disable);
        if (!updated) {
            throw new BusinessException("更新学生状态失败", ErrorCode.OPERATION_ERROR);
        }

        return new StudentDisableDTO(studentUuid, disable, true);
    }


    @Override
    public void deleteStudent(String studentUuid) {

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
        // 根据学生UUID从数据库中获取学生实体
        StudentDO studentDO = studentDAO.getStudentByUuid(studentUuid);
        // 检查学生是否存在，如果不存在则抛出异常
        if (studentDO == null) {
            throw new BusinessException("学生不存在", ErrorCode.NOT_EXIST);
        }

        // 将视图对象中的属性复制到实体对象中，以准备更新数据库
        BeanUtils.copyProperties(studentVO, studentDO);
        boolean updated = studentDAO.updateById(studentDO);
        // 如果更新失败，则抛出异常
        if (!updated) {
            throw new BusinessException("学生信息更新失败", ErrorCode.OPERATION_FAILED);
        }

        StudentDTO studentDTO = new StudentDTO();
        BeanUtils.copyProperties(studentDO, studentDTO);
        return studentDTO;
    }

    private StudentDTO convertToStudentDTO(StudentDO studentDO) {
        return new StudentDTO()
                .setStudentUuid(studentDO.getStudentUuid())
                .setId(studentDO.getId())
                .setName(studentDO.getName())
                .setGender(studentDO.getGender())
                .setGradeUuid(studentDO.getGradeUuid())
                .setClazz(studentDO.getClazz());
    }
}


