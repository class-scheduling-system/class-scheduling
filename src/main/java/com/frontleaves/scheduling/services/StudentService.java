package com.frontleaves.scheduling.services;


import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.dto.StudentDisableDTO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 学生服务接口
 *
 * @author fanfan187 | FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
public interface StudentService {
    /**
     * 批量导入学生信息 忽略警告
     * @param file 文件
     * @param departmentUuid    部门uuid
     * @return 返回导入结果
     */
    BackAddStudentDTO batchImportNoIgnoreError(
            byte[] file,
            String departmentUuid
    );

    /**
     * 获取学生导入信息模板
     *
     * @return 学生导入信息模板
     */
    byte[] getExample(
            PrepareStudentExampleDTO prepareStudentExampleDTO
    );

    /**
     * 根据请求获取用户详细信息
     * 此方法主要用于从请求中提取用户信息，并进一步检查该用户是否具有学术事务权限
     * 如果用户存在并且具有相应的权限，则返回权限的唯一标识符
     *
     * @param request HTTP请求对象，包含用户请求的相关信息
     * @return 返回学术事务权限的唯一标识符
     * @throws BusinessException 如果用户没有学术事务权限，则抛出业务异常
     */
    PrepareStudentExampleDTO prepareDepartmentData(
            HttpServletRequest request);

    /**
     * 验证学生导入模板
     * @param batchAddStudentVO 批量添加学生信息
     * @return 返回学生导入模板
     */
    byte[] verifyStudentBatchAndBackFile(
            BatchAddStudentVO batchAddStudentVO);

    /**
     * 批量导入学生信息 忽略警告
     * @param file 文件
     * @param departmentUuid   部门uuid
     * @return 返回导入结果
     */
    BackAddStudentDTO batchImportIgnoreError(
            byte[] file,
            String departmentUuid
    );

    /**
     * 获取部门UUID
     *
     * @param request 请求对象
     * @return 部门UUID
     */
    String getDepartmentUuid(
            HttpServletRequest request);


    /**
     * 获取学生信息
     *
     * @param studentUuid 学生 UUID
     */
    StudentDTO getStudentByUuid(String studentUuid);

    /**
     * 获取学生分页列表
     */
    PageDTO<StudentDTO> getStudentList(int page, int size, Boolean isDesc,
                                       @Nullable String clazz, @Nullable Boolean isGraduated,
                                       @Nullable String name, @Nullable String id);

    /**
     * 添加学生
     *
     */
    StudentDTO addStudent(StudentDTO studentDTO);

    /**
     * 停用学生
     */
    StudentDisableDTO disableStudent(String studentUuid, Boolean disable);

    /**
     * 删除学生
     */
    void deleteStudent(String studentUuid);

    /**
     * 编辑学生
     */
    StudentDTO editStudent(String studentUuid, StudentVO studentVO);
}
