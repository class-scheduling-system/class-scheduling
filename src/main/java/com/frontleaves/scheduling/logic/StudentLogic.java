package com.frontleaves.scheduling.logic;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.GradeDAO;
import com.frontleaves.scheduling.daos.MajorDAO;
import com.frontleaves.scheduling.models.dto.BackAddStudentDTO;
import com.frontleaves.scheduling.models.dto.ImportBatchStudentCheckResultDTO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.GradeDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.vo.BatchAddStudentVO;
import com.frontleaves.scheduling.services.StudentService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 学生逻辑
 *
 * @author FALSHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentLogic implements StudentService {
    private final DepartmentDAO departmentDAO;
    private final MajorDAO majorDAO;
    private final AdministrativeClassDAO administrativeClassDAO;
    private final GradeDAO gradeDAO;

    @Override
    public ImportBatchStudentCheckResultDTO checkImport(BatchAddStudentVO batchAddStudentVO) {
        log.debug("检查导入");
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(batchAddStudentVO.getDepartmentUuid());
        if (departmentDO == null) {
            throw new BusinessException("检查导入数据失败，部门不存在", ErrorCode.BODY_ERROR);
        }
        MajorDO majorDO = majorDAO.getMajorByUuid(batchAddStudentVO.getMajorUuid());
        if (majorDO == null) {
            throw new BusinessException("检查导入数据失败，专业不存在", ErrorCode.BODY_ERROR);
        }
        AdministrativeClassDO administrativeClassDO = administrativeClassDAO
                .getAdministrativeClassByUuid(batchAddStudentVO.getAdministrativeClassUuid());
        if (administrativeClassDO == null) {
            throw new BusinessException("检查导入数据失败，行政班不存在", ErrorCode.BODY_ERROR);
        }
        GradeDO gradeDO = gradeDAO.getGradeByUuid(batchAddStudentVO.getGrade());
        if (gradeDO == null) {
            throw new BusinessException("检查导入数据失败，年级不存在", ErrorCode.BODY_ERROR);
        }
        ImportBatchStudentCheckResultDTO importBatchStudentCheckResultDTO = new ImportBatchStudentCheckResultDTO();
        importBatchStudentCheckResultDTO.setDepartmentUuid(departmentDO.getDepartmentUuid())
                .setMajorUuid(majorDO.getMajorUuid())
                .setAdministrativeClassUuid(administrativeClassDO.getAdministrativeClassUuid())
                .setGradeUuid(gradeDO.getGradeUuid());
        return importBatchStudentCheckResultDTO;
    }

    @Override
    public BackAddStudentDTO batchImport(ImportBatchStudentCheckResultDTO importBatchStudentCheckResultDTO,
                                         BatchAddStudentVO batchAddStudentVO) {
        return null;
    }

    @Override
    public byte[] getExample() {
        ExcelWriter writer = ExcelUtil.getWriter(true);


        // 写入表头（第0行）
        writer.writeCellValue(0, 0, "学院");
        writer.writeCellValue(1, 0, "专业");
        writer.writeCellValue(2, 0, "年级");
        writer.writeCellValue(3, 0, "班级");
        writer.writeCellValue(4, 0, "学号");
        writer.writeCellValue(5, 0, "姓名");
        writer.writeCellValue(6, 0, "性别");
        writer.writeCellValue(8, 0, "学院示例名称");
        writer.writeCellValue(9, 0, "专业示例名称");
        writer.writeCellValue(10, 0, "年级示例名称");
        writer.writeCellValue(11, 0, "班级示例名称");

        List<DepartmentDO> departmentNameList = departmentDAO.getDepartmentList();
        if (!departmentNameList.isEmpty()) {
            IntStream.range(0, departmentNameList.size())
                    .forEach(i -> writer.writeCellValue(8, i + 1, departmentNameList.get(i).getDepartmentName()));
        }
        List<MajorDO> majorNameList = majorDAO.getMajorList();
        if (!majorNameList.isEmpty()) {
            IntStream.range(0, majorNameList.size())
                    .forEach(i -> writer.writeCellValue(9, i + 1, majorNameList.get(i).getMajorName()));
        }
        List<GradeDO> gradeNameList = gradeDAO.getGradeList();
        if (!gradeNameList.isEmpty()) {
            IntStream.range(0, gradeNameList.size())
                    .forEach(i -> writer.writeCellValue(10, i + 1, gradeNameList.get(i).getName()));
        }
        List<AdministrativeClassDO> administrativeClassNameList = administrativeClassDAO.getAdministrativeClassList();
        if (!administrativeClassNameList.isEmpty()) {
            IntStream.range(0, administrativeClassNameList.size())
                    .forEach(i -> writer.writeCellValue(11, i + 1, administrativeClassNameList.get(i).getClassName()));
        }
        // 输出到字节流
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        // 关闭writer，释放资源
        writer.close();
        // 返回字节数组
        return outputStream.toByteArray();
    }
}
