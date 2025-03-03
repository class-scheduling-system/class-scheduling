package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.DepartmentDTO;

import com.frontleaves.scheduling.models.vo.DepartmentAddVO;
import org.springframework.stereotype.Service;

@Service
public interface DepartmentService {

    DepartmentDTO addDepartment(DepartmentAddVO departmentAddVOO);
}
