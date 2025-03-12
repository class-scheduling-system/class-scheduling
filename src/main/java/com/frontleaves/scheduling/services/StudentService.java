package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.dto.ImportBatchStudentCheckResultDTO;
import com.frontleaves.scheduling.models.vo.BatchAddStudentVO;

/**
 * 学生服务接口
 * @author FLASHLACK
 */
public interface StudentService {
    /**
     * 检查导入
     *
     * @param batchAddStudentVO 批量添加学生信息的值对象
     * @return 导入检查结果DTO
     */
    ImportBatchStudentCheckResultDTO checkImport(
            BatchAddStudentVO batchAddStudentVO);

    BackAddStudentDTO batchImport(
            ImportBatchStudentCheckResultDTO importBatchStudentCheckResultDTO,
            BatchAddStudentVO batchAddStudentVO);

    /**
     * 获取学生导入信息模板
     * @return 学生导入信息模板
     */
    byte[] getExample();
}
