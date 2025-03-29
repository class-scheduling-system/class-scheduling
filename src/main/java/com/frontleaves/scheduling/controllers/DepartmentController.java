package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.base.DepartmentDTO;
import com.frontleaves.scheduling.models.dto.lite.DepartmentLiteDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
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

import java.util.List;

/**
 * 部门控制器
 * <p>
 * 该类提供了处理部门相关请求的 RESTful API，包括添加、查询、删除和更新部门信息等功能。
 *
 * @author qiyu
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/department")
public class DepartmentController {
    private final DepartmentService departmentService;

    /**
     * 添加新部门的接口
     * 该方法通过POST请求接收部门信息，并进行验证和处理，返回新创建的部门信息
     *
     * @param departmentVO 部门添加请求对象，包含需要验证的部门信息
     * @return 返回包含成功消息和新创建部门数据的响应实体
     */
    @PostMapping("")
    public ResponseEntity<BaseResponse<DepartmentDTO>> addDepartment(
            @RequestBody @Validated DepartmentVO departmentVO
    ) {
        // 调用服务层方法，处理部门添加逻辑
        DepartmentDTO departmentDTO = departmentService.addDepartment(departmentVO);
        // 使用ResultUtil工具类生成成功响应，包含成功消息和部门数据
        return ResultUtil.success("部门创建成功", departmentDTO);
    }

    /**
     * 根据部门UUID获取部门信息
     *
     * @param departmentUuid 部门的唯一标识符（UUID）
     * @return 返回包含部门信息的响应实体
     * <p>
     * 此方法首先会检查传入的部门UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 表示参数错误接着，调用部门服务的getDepartment方法获取部门信息最后，使用ResultUtil
     * 工具类生成包含“查询成功”消息和部门信息的响应实体返回
     */
    @GetMapping("/{department_uuid}")
    public ResponseEntity<BaseResponse<DepartmentDTO>> getDepartment(
            @PathVariable("department_uuid") String departmentUuid
    ) {
        // 检查部门UUID是否为空或空白，如果为空或空白，则抛出业务异常
        if (departmentUuid == null || departmentUuid.isBlank()) {
            throw new BusinessException(StringConstant.DEPARTMENT_UUID_NOT_EMPTY, ErrorCode.PARAMETER_ERROR);
        }
        // 调用部门服务的getDepartment方法获取部门信息
        DepartmentDTO departmentDTO = departmentService.getDepartment(departmentUuid);
        // 使用ResultUtil工具类生成包含“查询成功”消息和部门信息的响应实体返回
        return ResultUtil.success("查询成功", departmentDTO);
    }

    /**
     * 删除部门
     *
     * @param departmentUuid 部门的唯一标识符（UUID）
     * @return 返回包含成功消息的响应实体
     * <p>
     * 此方法首先会检查传入的部门UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 表示参数错误接着，调用部门服务的deleteDepartment方法删除部门最后，使用ResultUtil
     * 工具类生成包含“删除成功”消息的响应实体返回
     */

    @DeleteMapping("/{department_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteDepartment(
            @PathVariable("department_uuid") String departmentUuid
    ) {
        if (departmentUuid == null || departmentUuid.isBlank()) {
            throw new BusinessException(StringConstant.DEPARTMENT_UUID_NOT_EMPTY, ErrorCode.PARAMETER_ERROR);
        }
        departmentService.deleteDepartment(departmentUuid);
        return ResultUtil.success("删除成功");
    }

    /**
     * 更新部门信息
     *
     * @param departmentUuid 部门的唯一标识符（UUID）
     * @param departmentVO   部门添加请求对象，包含需要验证的部门信息
     * @return 返回包含成功消息和更新后的部门数据的响应实体
     * <p>
     * 此方法首先会检查传入的部门UUID是否为空或空白，如果为空或空白，则抛出业务异常，
     * 表示参数错误接着，调用部门服务的updateDepartment方法更新部门信息最后，使用ResultUtil
     * 工具类生成包含“部门修改成功”消息和更新后的部门数据的响应实体返回
     */
    @PutMapping("/{department_uuid}")
    public ResponseEntity<BaseResponse<DepartmentDTO>> updateDepartment(
            @PathVariable("department_uuid") String departmentUuid,
            @RequestBody DepartmentVO departmentVO
    ) {
        if (departmentUuid == null || departmentUuid.isBlank()) {
            throw new BusinessException(StringConstant.DEPARTMENT_UUID_NOT_EMPTY, ErrorCode.PARAMETER_ERROR);
        }
        DepartmentDTO departmentDTO = departmentService.updateDepartment(departmentUuid, departmentVO);
        return ResultUtil.success("部门修改成功", departmentDTO);
    }

    /**
     * 获取部门列表
     *
     * @param page   页码，默认为1
     * @param size   每页大小，默认为20
     * @param isDesc 是否降序排列，默认为true
     * @param name   部门名称，可选参数
     * @return 返回包含部门列表的响应实体
     * <p>
     * 此方法用于获取部门列表，支持分页、排序和名称过滤功能。
     * 通过调用部门服务的getDepartmentList方法获取部门列表，并使用ResultUtil工具类生成响应返回
     */

    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<DepartmentDTO>>> getDepartmentPage(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "keyword", required = false) String name
    ) {
        PageDTO<DepartmentDTO> departmentList = departmentService.getDepartmentPage(page, size, isDesc, name);
        return ResultUtil.success("部门列表获取成功", departmentList);
    }

    /**
     * 获取部门简洁列表
     *
     * @return 返回包含部门简洁列表的响应实体
     * <p>
     * 此方法用于获取部门简洁列表，不包含详细信息。
     * 通过调用部门服务的getDepartmentList方法获取部门简洁列表，并使用ResultUtil工具类生成响应返回
     */
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<DepartmentLiteDTO>>> getDepartmentList() {
        List<DepartmentLiteDTO> departmentList = departmentService.getDepartmentList();
        return ResultUtil.success("获取部门列表成功", departmentList);
    }
}






