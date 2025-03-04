package com.frontleaves.scheduling.services;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;

import com.frontleaves.scheduling.models.vo.DepartmentAddVO;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public interface DepartmentService {

    DepartmentDTO addDepartment(DepartmentAddVO departmentAddVOO);

    DepartmentDTO getDepartment(String departmentUuid);

    void deleteDepartment(String departmentUuid);

    DepartmentDTO updateDepartment(String departmentUuid, DepartmentAddVO departmentAddVO);

    PageDTO<DepartmentDTO> getDepartmentList(int page, int size, boolean isDesc,String name);
}
