package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherDTO;
import com.frontleaves.scheduling.models.dto.TeacherDisableDTO;
import com.frontleaves.scheduling.models.dto.TeacherLiteDTO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Service
public interface TeacherService {
    void addTeacher(TeacherVO teacherVO);


    @NotNull
    TeacherDTO getTeacher(String teacherUuid);

    PageDTO<TeacherDTO> getTeacherList(Integer page, Integer size, Boolean isDesc, String department, String status, String name);

    TeacherDisableDTO disableTeacher(String teacherUuid, Boolean disable);

    void deleteTeacher(String teacherUuid);

    void updateTeacher(String teacherUuid, TeacherVO teacherVO);

    /**
     * 获取教师简单列表
     * <p>
     * 该方法用于获取教师的基本信息列表，包括UUID、姓名、部门和类型。
     * 支持按部门和教师类型进行筛选。
     * </p>
     *
     * @param departmentUuid  部门UUID，可选参数
     * @param teacherTypeUuid 教师类型UUID，可选参数
     * @return 返回教师简单信息列表
     */
    List<TeacherLiteDTO> getTeacherLiteList(String departmentUuid, String teacherTypeUuid);

    /**
     * 获取教师导入信息模板
     *
     * @param prepareTeacherExampleDTO 包含生成模板所需的配置信息，如部门列表、教师类型等
     * @return 返回生成的Excel模板文件的字节数组
     */
    byte[] getExample(PrepareTeacherExampleDTO prepareTeacherExampleDTO);

    /**
     * 验证教师批量导入数据并返回校验结果
     * <p>
     * 该方法对上传的教师批量导入数据进行验证，
     * 检查数据格式、必填字段等是否符合要求。
     * </p>
     *
     * @param teacherBatchImportVO 教师批量导入的请求数据对象
     * @return 返回验证后的数据文件字节数组
     */
    byte[] verifyTeacherBatchAndBackFile(TeacherBatchImportVO teacherBatchImportVO);

    /**
     * 获取部门UUID
     * <p>
     * 从请求中解析并返回当前用户所属的部门UUID。
     * 用于确保用户只能在其所属部门范围内进行操作。
     * </p>
     *
     * @param request HTTP请求对象，包含用户的部门信息
     * @return 返回用户所属的部门UUID
     */
    String getDepartmentUuid(HttpServletRequest request);

    /**
     * 批量导入教师信息，忽略警告
     * <p>
     * 该方法在导入过程中会忽略非致命性的警告，继续完成导入操作。
     * 适用于数据量较大且允许存在少量非关键错误的批量导入场景。
     * </p>
     *
     * @param file           教师信息的Excel文件字节数组
     * @param departmentUuid 部门的唯一标识符
     * @return 返回批量导入的结果，包含成功和失败的统计信息
     */
    BackAddTeacherDTO batchImportIgnoreError(byte[] file, String departmentUuid);

    /**
     * 批量导入教师信息，不忽略警告
     * <p>
     * 该方法会在导入过程中严格验证数据的合法性，如遇到任何警告或错误都会终止导入操作。
     * 适用于需要确保数据完全准确的场景。
     * </p>
     *
     * @param file           教师信息的Excel文件字节数组
     * @param departmentUuid 部门的唯一标识符
     * @return 返回批量导入的结果，包含成功和失败的统计信息
     */
    BackAddTeacherDTO batchImportNoIgnoreError(byte[] file, String departmentUuid);

    /**
     * 根据请求获取用户详细信息
     * <p>
     * 此方法主要用于从请求中提取用户信息，并进一步检查该用户是否具有教务事务权限
     * 如果用户存在并且具有相应的权限，则返回权限的唯一标识符
     * </p>
     *
     * @param request HTTP请求对象，包含用户请求的相关信息
     * @return 返回教务事务权限的唯一标识符
     * @throws BusinessException 如果用户没有学术事务权限，则抛出业务异常
     */
    PrepareTeacherExampleDTO prepareDepartmentData(HttpServletRequest request);
}
