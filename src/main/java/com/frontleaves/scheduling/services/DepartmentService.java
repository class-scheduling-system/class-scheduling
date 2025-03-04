package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.DepartmentDTO;

import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
import org.springframework.stereotype.Service;

@Service
public interface DepartmentService {

    DepartmentDTO addDepartment(DepartmentVO departmentVOO);

    DepartmentDTO getDepartment(String departmentUuid);

    void deleteDepartment(String departmentUuid);

    DepartmentDTO updateDepartment(String departmentUuid, DepartmentVO departmentVO);

    PageDTO<DepartmentDTO> getDepartmentList(int page, int size, boolean isDesc, String name);
}
