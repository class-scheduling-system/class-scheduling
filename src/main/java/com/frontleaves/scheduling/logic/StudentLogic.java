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
import java.util.ArrayList;
import java.util.List;

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
        List<List<String>> rows = new ArrayList<>();
        // 表头
        List<String> header = new ArrayList<>();
        header.add("学院");
        header.add("专业");
        header.add("年级");
        header.add("班级");
        header.add("学号");
        header.add("姓名");
        header.add("性别");
        rows.add(header);
        // 示例数据
        ExcelWriter writer = ExcelUtil.getWriter(true);
        writer.write(rows, true);
        // 4. 将 Excel 内容写入 ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        writer.flush(outputStream, true);
        // 5. 关闭 writer，释放资源
        writer.close();
        // 6. 返回字节数组
        return outputStream.toByteArray();
    }
}
