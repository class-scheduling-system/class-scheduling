package com.frontleaves.scheduling.controllers;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;

import com.frontleaves.scheduling.models.vo.DepartmentAddVO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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

    /**
     * 根据部门UUID获取部门信息
     *
     * @param departmentUuid 部门的唯一标识符（UUID）
     * @return 返回包含部门信息的响应实体
     *
     * 此方法首先会检查传入的部门UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 表示参数错误接着，调用部门服务的getDepartment方法获取部门信息最后，使用ResultUtil
     * 工具类生成包含“查询成功”消息和部门信息的响应实体返回
     */
    @GetMapping("/{department_uuid}")
    public ResponseEntity<BaseResponse<DepartmentDTO>> getDepartment(
            @PathVariable("department_uuid") String departmentUuid) {
        // 检查部门UUID是否为空或空白，如果为空或空白，则抛出业务异常
        if (departmentUuid == null || departmentUuid.isBlank()) {
            throw new BusinessException("部门UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        // 调用部门服务的getDepartment方法获取部门信息
        DepartmentDTO departmentDTO = departmentService.getDepartment(departmentUuid);
        // 使用ResultUtil工具类生成包含“查询成功”消息和部门信息的响应实体返回
        return ResultUtil.success("查询成功", departmentDTO);
    }

    @DeleteMapping("/{department_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteDepartment(
            @PathVariable("department_uuid") String departmentUuid) {
        departmentService.deleteDepartment(departmentUuid);
        return ResultUtil.success("删除成功");
    }

    @PutMapping("/{department_uuid}")
    public ResponseEntity<BaseResponse<DepartmentDTO>> updateDepartment(
            @PathVariable("department_uuid") String departmentUuid,
            @RequestBody DepartmentAddVO departmentAddVO) {
        DepartmentDTO departmentDTO= departmentService.updateDepartment(departmentUuid, departmentAddVO);
        return ResultUtil.success("部门修改成功", departmentDTO);
    }

    @GetMapping("")
    public ResponseEntity<BaseResponse<PageDTO<DepartmentDTO>>> getDepartmentList(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "name", required = false) String name
    ) {
        PageDTO<DepartmentDTO> departmentList = departmentService.getDepartmentList(page, size, isDesc,name);
        return ResultUtil.success("部门列表获取成功", departmentList);
    }
}






