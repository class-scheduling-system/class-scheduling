package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.StudentDTO;
import com.frontleaves.scheduling.models.dto.StudentDisableDTO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import jakarta.annotation.Nullable;

/**
 * 学生服务接口
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
public interface StudentService {

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
                                       @Nullable String clazz,@Nullable Boolean isGraduated,
                                       @Nullable String name, @Nullable String id);


    /**
     * 添加学生
     *
     */
    StudentDO addStudent(StudentDO studentVO);

    /**
     * 停用学生
     */
//    StudentDisableDTO disableStudent(String studentUuid, Boolean disable);

    /**
     * 删除学生
     */
    void deleteStudent(String studentUuid);

    /**
     * 编辑学生
     */
    StudentDTO editStudent(String studentUuid, StudentVO studentVO);

}
