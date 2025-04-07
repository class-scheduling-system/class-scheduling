package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.vo.AdministrativeClassVO;
import com.frontleaves.scheduling.services.AdministrativeClassService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.frontleaves.scheduling.constants.StringConstant.AdministrativeClass.ADMINISTRATIVE_CLASS_UUID_FORMAT_ERROR;

/**
 * 行政班级控制器
 * <p>
 * 提供创建、修改、删除、查询及分页列表查询等接口
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/administrative-class")
public class AdministrativeClassController {

    private final AdministrativeClassService administrativeClassService;

    /**
     * 创建行政班级
     * <p>
     * 该方法通过POST请求接收一个AdministrativeClassVO对象,用于创建新的行政班级信息.
     * 主要功能包括验证传入的行政班级信息是否有效,以及将新行政班级信息保存到数据库中.
     * </p>
     *
     * @param administrativeClassVO 新行政班级的详细信息，包括班级名称、所属院系、专业等
     * @return 返回一个包含新创建行政班级信息的ResponseEntity对象
     */
    @PostMapping("")
    public @NotNull ResponseEntity<BaseResponse<AdministrativeClassDTO>> addAdministrativeClass(
            @RequestBody AdministrativeClassVO administrativeClassVO
    ) {
        log.debug("创建行政班级, administrativeClassVO: {}", administrativeClassVO);
        
        // VO -> DTO
        AdministrativeClassDTO administrativeClassDTO = new AdministrativeClassDTO();
        BeanUtils.copyProperties(administrativeClassVO, administrativeClassDTO);

        // 调用 Service 层方法创建行政班级信息
        AdministrativeClassDTO created = administrativeClassService.createAdministrativeClass(administrativeClassDTO);

        // 返回DTO给前端
        return ResultUtil.success("行政班级创建成功", created);
    }

    /**
     * 修改行政班级
     * <p>
     * 该方法通过通过PUT请求编辑已有行政班级信息
     * 主要功能包括验证传入的行政班级信息是否有效,以及将修改后的行政班级信息保存到数据库中.
     * </p>
     *
     * @param administrativeClassUuid 行政班级的唯一标识符,用于定位要修改的行政班级
     * @param administrativeClassVO 修改后的行政班级详细信息,包括班级名称、所属院系、专业等
     * @return 返回修改结果的响应实体
     */
    @PutMapping("/{administrative_class_uuid}")
    public @NotNull ResponseEntity<BaseResponse<AdministrativeClassDTO>> updateAdministrativeClass(
            @PathVariable("administrative_class_uuid") String administrativeClassUuid,
            @RequestBody AdministrativeClassVO administrativeClassVO
    ) {
        log.debug("===> 进入 updateAdministrativeClass 方法, administrativeClassUuid: {}", administrativeClassUuid);

        // 对 administrativeClassUuid 进行正则判断
        if (!administrativeClassUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new IllegalArgumentException(ADMINISTRATIVE_CLASS_UUID_FORMAT_ERROR);
        }
        
        // VO -> DTO
        AdministrativeClassDTO administrativeClassDTO = new AdministrativeClassDTO();
        BeanUtils.copyProperties(administrativeClassVO, administrativeClassDTO);

        AdministrativeClassDTO updated = administrativeClassService.updateAdministrativeClass(
                administrativeClassUuid, administrativeClassDTO);
                
        return ResultUtil.success("行政班级修改成功", updated);
    }

    /**
     * 删除行政班级
     * <p>
     * 该方法通过DELETE请求删除指定的行政班级信息,
     * 接收路径参数 administrative_class_uuid 作为行政班级的唯一标识符
     * </p>
     *
     * @param administrativeClassUuid 行政班级的唯一标识符,通过路径变量传递(必填)
     * @return 返回一个成功的响应实体,包含操作成功的消息;
     *         如果删除失败或行政班级不存在,则会抛出异常.
     */
    @DeleteMapping("/{administrative_class_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteAdministrativeClass(
            @PathVariable("administrative_class_uuid") String administrativeClassUuid
    ) {
        log.debug("删除行政班级: {}", administrativeClassUuid);

        if (!administrativeClassUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new IllegalArgumentException(ADMINISTRATIVE_CLASS_UUID_FORMAT_ERROR);
        }
        
        administrativeClassService.deleteAdministrativeClass(administrativeClassUuid);
        return ResultUtil.success("行政班级删除成功");
    }

    /**
     * 查询行政班级
     * <p>
     * 根据行政班级的UUID查询行政班级信息,
     * 返回行政班级名称、所属院系、专业、年级等详细信息.
     * </p>
     *
     * @param administrativeClassUuid 行政班级的UUID
     * @return 查询成功的行政班级 DTO
     */
    @GetMapping("/{administrative_class_uuid}")
    public ResponseEntity<BaseResponse<AdministrativeClassDTO>> getAdministrativeClass(
            @PathVariable("administrative_class_uuid") String administrativeClassUuid
    ) {
        log.debug("查询行政班级信息: {}", administrativeClassUuid);

        if (!administrativeClassUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new IllegalArgumentException(ADMINISTRATIVE_CLASS_UUID_FORMAT_ERROR);
        }
        
        // 调用 Service 层方法获取 AdministrativeClassDTO
        AdministrativeClassDTO administrativeClassDTO = administrativeClassService.getAdministrativeClass(administrativeClassUuid);
        return ResultUtil.success("查询成功", administrativeClassDTO);
    }

    /**
     * 获取行政班级列表(管理员)
     * <p>
     * 此接口允许管理员根据分页参数和可选的排序、部门、专业和名称筛选条件来获取行政班级信息列表
     * </p>
     *
     * @param page 页码，指定从哪一页开始获取数据
     * @param size 每页的大小，即每页包含的行政班级数量
     * @param isDesc 是否降序排列，默认为true，表示降序排列
     * @param departmentUuid 部门UUID，用于筛选属于特定部门的行政班级，此参数为可选
     * @param majorUuid 专业UUID，用于筛选属于特定专业的行政班级，此参数为可选
     * @param name 行政班级名称，用于模糊匹配行政班级名称，此参数为可选
     * @return 返回一个ResponseEntity对象，其中包含BaseResponse对象，该对象中包含分页的行政班级信息
     */
    @GetMapping("/list/admin")
    public ResponseEntity<BaseResponse<PageDTO<AdministrativeClassDTO>>> listAdministrativeClassForAdmin(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "department_uuid", required = false) String departmentUuid,
            @RequestParam(value = "major_uuid", required = false) String majorUuid,
            @RequestParam(value = "name", required = false) String name
    ) {
        log.debug("管理员查询行政班级列表, page: {}, size: {}, isDesc: {}, departmentUuid: {}, majorUuid: {}, name: {}",
                page, size, isDesc, departmentUuid, majorUuid, name);
                
        // 调用服务层方法获取行政班级列表
        PageDTO<AdministrativeClassDTO> result = administrativeClassService.listAdministrativeClassForAdmin(
                page, size, isDesc, departmentUuid, majorUuid, name);
                
        // 使用ResultUtil工具类返回成功结果
        return ResultUtil.success("查询成功", result);
    }

    /**
     * 获取行政班级列表(教务)
     * <p>
     * 该接口用于分页查询行政班级信息,
     * 支持按部门UUID、专业UUID和班级名称进行筛选,并可指定排序方式.
     * </p>
     *
     * @param page 当前页码，从1开始计数，表示分页查询的页数。
     * @param size 每页显示的记录数，表示每页返回的行政班级数量。
     * @param isDesc 是否降序排序，默认值为true。如果为true，则结果按降序排列；否则按升序排列。
     * @param departmentUuid 部门UUID（可选），用于筛选特定部门下的行政班级信息。如果为空，则不进行部门筛选。
     * @param majorUuid 专业UUID（可选），用于筛选特定专业下的行政班级信息。如果为空，则不进行专业筛选。
     * @param name 行政班级名称（可选），用于模糊匹配行政班级名称。如果为空，则不进行名称筛选。
     * @return 返回一个ResponseEntity对象，包含分页查询结果和成功消息。
     */
    @GetMapping("/list/academic")
    public @NotNull ResponseEntity<BaseResponse<PageDTO<AdministrativeClassDTO>>> listAdministrativeClassForAcademic(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "department_uuid", required = false) String departmentUuid,
            @RequestParam(value = "major_uuid", required = false) String majorUuid,
            @RequestParam(value = "name", required = false) String name
    ) {
        log.debug("教务查询行政班级列表, page: {}, size: {}, isDesc: {}, departmentUuid: {}, majorUuid: {}, name: {}",
                page, size, isDesc, departmentUuid, majorUuid, name);
                
        PageDTO<AdministrativeClassDTO> result = administrativeClassService.listAdministrativeClassForAcademic(
                page, size, isDesc, departmentUuid, majorUuid, name);
                
        return ResultUtil.success("查询成功", result);
    }

    /**
     * 获取行政班级列表(学生)
     * <p>
     * 该接口用于学生用户获取行政班级列表,支持分页查询和排序,
     * 可以根据部门UUID、专业UUID和班级名称进行筛选.
     * </p>
     *
     * @param page 页码，从1开始
     * @param size 每页大小，用于限制最大条数
     * @param isDesc 是否降序，true为降序，false为升序，默认为true
     * @param departmentUuid 部门UUID，用于筛选属于特定部门的行政班级，可选参数
     * @param majorUuid 专业UUID，用于筛选属于特定专业的行政班级，可选参数
     * @param name 行政班级名称，用于模糊匹配行政班级名称，可选参数
     * @return 返回包含行政班级列表的ResponseEntity对象
     */
    @GetMapping("/list/student")
    public @NotNull ResponseEntity<BaseResponse<PageDTO<AdministrativeClassDTO>>> listAdministrativeClassForStudent(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "department_uuid", required = false) String departmentUuid,
            @RequestParam(value = "major_uuid", required = false) String majorUuid,
            @RequestParam(value = "name", required = false) String name
    ) {
        log.debug("学生查询行政班级列表, page: {}, size: {}, isDesc: {}, departmentUuid: {}, majorUuid: {}, name: {}",
                page, size, isDesc, departmentUuid, majorUuid, name);
                
        PageDTO<AdministrativeClassDTO> result = administrativeClassService.listAdministrativeClassForStudent(
                page, size, isDesc, departmentUuid, majorUuid, name);
                
        return ResultUtil.success("查询成功", result);
    }
    
    /**
     * 获取所有行政班级列表(不分页)
     * <p>
     * 该接口用于获取所有行政班级的简单列表，常用于下拉选择框等场景
     * </p>
     * 
     * @return 所有行政班级的列表
     */
    @GetMapping("/list/all")
    public @NotNull ResponseEntity<BaseResponse<List<AdministrativeClassDTO>>> listAllAdministrativeClass() {
        log.debug("获取所有行政班级列表");
        
        List<AdministrativeClassDTO> result = administrativeClassService.listAllAdministrativeClass();
        
        return ResultUtil.success("查询成功", result);
    }
}