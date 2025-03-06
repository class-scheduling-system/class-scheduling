package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherDTO;
import com.frontleaves.scheduling.models.dto.TeacherDisableDTO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import org.springframework.stereotype.Service;

@Service
public interface TeacherService {
    void addTeacher(TeacherVO teacherVO);

    TeacherDTO getTeacher(String teacherUuid);

    PageDTO<TeacherDTO> getTeacherList(Integer page, Integer size, Boolean isDesc, String department, String status, String name);

    TeacherDisableDTO disableTeacher(String teacherUuid, Boolean disable);

    void deleteTeacher(String teacherUuid);

    void updateTeacher(String teacherUuid, TeacherVO teacherVO);
}
