package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.vo.TeacherVO;
import org.springframework.stereotype.Service;

@Service
public interface TeacherService {
    void addTeacher(TeacherVO teacherVO);
}
