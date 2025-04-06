package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherDTO;
import com.frontleaves.scheduling.models.dto.lite.TeacherDisableDTO;
import com.frontleaves.scheduling.models.dto.lite.TeacherLiteDTO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import jakarta.validation.constraints.NotNull;

import java.util.List;


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
}
