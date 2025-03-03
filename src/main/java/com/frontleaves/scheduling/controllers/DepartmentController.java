package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.models.dto.DepartmentDTO;

import com.frontleaves.scheduling.models.vo.DepartmentAddVO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部门控制器类
 * <p>
 * 该类提供了与部门相关的 RESTful API 接口，包括添加新部门的功能。
 * </p>
 *
 * @author FLASHLACK
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/departments")
public class DepartmentController {
    private final DepartmentService departmentService;
    /**
     * 添加新部门的接口
     * 该方法通过POST请求接收部门信息，并进行验证和处理，返回新创建的部门信息
     *
     * @param departmentAddVO 部门添加请求对象，包含需要验证的部门信息
     * @return 返回包含成功消息和新创建部门数据的响应实体
     */
    @PostMapping("")
    public ResponseEntity<BaseResponse<DepartmentDTO>> addDepartment(
            @RequestBody @Validated DepartmentAddVO departmentAddVO) {
        // 调用服务层方法，处理部门添加逻辑
        DepartmentDTO departmentDTO=departmentService.addDepartment(departmentAddVO);
        // 使用ResultUtil工具类生成成功响应，包含成功消息和部门数据
        return ResultUtil.success("部门创建成功",departmentDTO);
    }
}






