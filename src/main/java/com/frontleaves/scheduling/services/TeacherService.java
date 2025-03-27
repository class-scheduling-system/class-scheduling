package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherDTO;
import com.frontleaves.scheduling.models.dto.TeacherDisableDTO;
import com.frontleaves.scheduling.models.dto.TeacherLiteDTO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface TeacherService {
    void addTeacher(TeacherVO teacherVO);

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
}
