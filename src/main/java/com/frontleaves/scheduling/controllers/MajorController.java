package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.MajorDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.vo.MajorVO;
import com.frontleaves.scheduling.services.MajorService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.frontleaves.scheduling.constants.StringConstant.Major.MAJOR_UUID_FORMAT_ERROR;

/**
 * 专业控制器
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
@RequestMapping("/api/v1/major")
public class MajorController {

    private final MajorService majorService;

    /**
     * 创建专业
     * <p>
     * 该方法通过POST请求接收一个MajorDTO对象,用于创建新的专业信息.
     * 主要功能包括验证传入的专业信息是否有效,以及将新专业信息保存到数据库中.
     * </p>
     *
     * @param majorVO 新专业的详细信息，包括专业名称、所属学院等
     * @return 返回一个包含新创建专业信息的ResponseEntity对象
     */
    @PostMapping("")
    public @NotNull ResponseEntity<BaseResponse<MajorDTO>> addMajor(@RequestBody MajorVO majorVO) {
        // VO -> DO
        MajorDTO majorDO = new MajorDTO();
        BeanUtils.copyProperties(majorVO, majorDO);

        // 调用 Service 层方法创建专业信息
        MajorDTO created = majorService.createMajor(majorDO);
        // DO -> DTO
        MajorDTO createdDTO = new MajorDTO();
        BeanUtils.copyProperties(created, createdDTO);

        // 返回DTO给前端
        return ResultUtil.success("专业创建成功", created);
    }

    /**
     * 修改专业
     * <p>
     * 该方法通过通过PUT请求编辑已有专业信息
     * 主要功能包括验证传入的专业信息是否有效,以及将修改后的专业信息保存到数据库中.
     * </p>
     *
     * @param majorUuid 专业的唯一标识符,用于定位要修改的专业
     * @param majorVO 修改后的专业详细信息,包括专业名称、所属学院等
     * @return 返回修改结果的响应实体
     */
    @PutMapping("/{major_uuid}")
    public @NotNull ResponseEntity<BaseResponse<MajorDO>> updateMajor(
            @PathVariable("major_uuid") String majorUuid,
            @RequestBody MajorVO majorVO
    ) {
        log.debug("===> 进入 updateMajor 方法, majorUuid: {}", majorUuid);

        // 对 majorUuid 进行正则判断
        if (!majorUuid.matches(StringConstant.Regular.UUID_REGULAR_EXPRESSION)) {
            throw new IllegalArgumentException(MAJOR_UUID_FORMAT_ERROR);
        }
        // VO -> DO
        MajorDO majorDO = new MajorDO();
        BeanUtils.copyProperties(majorVO, majorDO);

        MajorDO updated = majorService.updateMajor(majorUuid, majorDO);
        // DO -> DTO
        MajorDTO updatedDTO = new MajorDTO();
        BeanUtils.copyProperties(updated, updatedDTO);
        return ResultUtil.success("专业修改成功", updated);
    }

    /**
     * 删除专业
     * <p>
     * 该方法通过DELETE请求删除指定的专业信息,
     * 接收路径参数 major_uuid 作为专业的唯一标识符
     * </p>
     *
     * @param majorUuid 专业的唯一标识符,通过路径变量传递(必填)
     * @return 返回一个成功的响应实体,包含操作成功的消息;
     *         如果删除失败或专业不存在,则会抛出异常.
     */
    @DeleteMapping("/{major_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteMajor(
            @PathVariable("major_uuid")
            String majorUuid
    ) {
        // 业务流程，使用 info 级别
        log.info("删除专业: {}", majorUuid);

        if (!majorUuid.matches(StringConstant.Regular.UUID_REGULAR_EXPRESSION)) {
            throw new IllegalArgumentException(MAJOR_UUID_FORMAT_ERROR);
        }
        majorService.deleteMajor(majorUuid);
        return ResultUtil.success("专业删除成功");
    }

    /**
     * 查询专业
     * <p>
     * 根据专业的UUID查询专业信息,
     * 返回专业名称、所属学院、专业代码、专业状态、教育年限、培养层次等详细信息.
     * </p>
     *
     * @param majorUuid 专业的UUID
     * @return 查询成功的专业 DTO
     */
    @GetMapping("/{major_uuid}")
    public ResponseEntity<BaseResponse<MajorDTO>> getMajor(
            @PathVariable("major_uuid") String majorUuid
    ) {
        log.debug("查询专业信息: {}", majorUuid);

        if (!majorUuid.matches(StringConstant.Regular.UUID_REGULAR_EXPRESSION)) {
            throw new IllegalArgumentException(MAJOR_UUID_FORMAT_ERROR);
        }
        // 调用 Service 层方法获取 MajorDO
        MajorDTO majorDO = majorService.getMajor(majorUuid);
        MajorDTO majorDTO = new MajorDTO();
        BeanUtils.copyProperties(majorDO, majorDTO);
        return ResultUtil.success("查询成功", majorDTO);
    }

    /**
     * 获取专业列表(管理员)
     * <p>
     * 此接口允许管理员根据分页参数和可选的排序、部门和名称筛选条件来获取专业信息列表
     * </p>
     *
     * @param page 页码，指定从哪一页开始获取数据
     * @param size 每页的大小，即每页包含的专业数量
     * @param isDesc 是否降序排列，默认为true，表示降序排列
     * @param department 部门名称，用于筛选属于特定部门的专业，此参数为可选
     * @param name 专业名称，用于模糊匹配专业名称，此参数为可选
     * @return 返回一个ResponseEntity对象，其中包含BaseResponse对象，该对象中包含分页的专业信息
     */
    @GetMapping("/list/admin")
    public ResponseEntity<BaseResponse<PageDTO<MajorDTO>>> listMajorForAdmin(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "name", required = false) String name
    ) {
        log.debug("管理员查询专业列表, page: {}, size: {}, isDesc: {}, department: {}, name: {}",
                page, size, isDesc, department, name);
        // 调用服务层方法获取专业列表
        PageDTO<MajorDTO> result = majorService.listMajorsForAdmin(page, size, isDesc, department, name);
        // 使用ResultUtil工具类返回成功结果
        return ResultUtil.success("查询成功", result);
    }

    /**
     * 获取专业列表(教务)
     * <p>
     * 该接口用于分页查询专业信息,
     * 支持按部门名称和专业名称进行筛选,并可指定排序方式.
     * </p>
     *
     * @param page 当前页码，从1开始计数，表示分页查询的页数。
     * @param size 每页显示的记录数，表示每页返回的专业数量。
     * @param isDesc 是否降序排序，默认值为true。如果为true，则结果按降序排列；否则按升序排列。
     * @param department 部门名称（可选），用于筛选特定部门下的专业信息。如果为空，则不进行部门筛选。
     * @param name 专业名称（可选），用于模糊匹配专业名称。如果为空，则不进行名称筛选。
     * @return 返回一个ResponseEntity对象，包含分页查询结果和成功消息。
     */
    @GetMapping("/list/academic")
    public @NotNull ResponseEntity<BaseResponse<PageDTO<MajorDTO>>> listMajorForAcademic(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "name", required = false) String name
    ) {
        log.debug("教务查询专业列表, page: {}, size: {}, isDesc: {}, department: {}, name: {}",
                page, size, isDesc, department, name);
        PageDTO<MajorDTO> result = majorService.listMajorsForAcademic(page, size, isDesc, department, name);
        return ResultUtil.success("查询成功", result);
    }

    /**
     * 获取专业列表(学生)
     * <p>
     * 该接口用于学生用户获取专业列表,支持分页查询和排序,
     * 可以根据部门和专业名称进行筛选.
     * </p>
     *
     * @param page 页码，从1开始
     * @param size 每页大小，用于限制最大条数
     * @param isDesc 是否降序，true为降序，false为升序，默认为true
     * @param department 部门名称，用于筛选属于特定部门的专业，可选参数
     * @param name 专业名称，用于模糊匹配专业名称，可选参数
     * @return 返回包含专业列表的ResponseEntity对象
     */
    @GetMapping("/list/student")
    public @NotNull ResponseEntity<BaseResponse<PageDTO<MajorDTO>>> listMajorForStudent(
            @RequestParam("page") int page,
            @RequestParam("size") int size,
            @RequestParam(value = "is_desc", required = false, defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "name", required = false) String name
    ) {
        log.debug("学生查询专业列表, page: {}, size: {}, isDesc: {}, department: {}, name: {}",
                page, size, isDesc, department, name);
        PageDTO<MajorDTO> result = majorService.listMajorsForStudent(page, size, isDesc, department, name);
        return ResultUtil.success("查询成功", result);
    }
}


