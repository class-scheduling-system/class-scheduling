package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.dto.PrepareStudentExampleDTO;
import com.frontleaves.scheduling.models.vo.BatchAddStudentVO;
import com.xlf.utility.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 学生服务接口
 * @author FLASHLACK
 */
public interface StudentService {


    BackAddStudentDTO batchImport(
            BatchAddStudentVO batchAddStudentVO);

    /**
     * 获取学生导入信息模板
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
}
