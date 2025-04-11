/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.annotations.RequestLogin;
import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.base.ClassroomTagDTO;
import com.frontleaves.scheduling.models.dto.base.ClassroomTypeDTO;
import com.frontleaves.scheduling.models.dto.base.FileDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.excel.BackAddClassroomDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomInfoDTO;
import com.frontleaves.scheduling.models.dto.merge.ClassroomLiteDTO;
import com.frontleaves.scheduling.models.vo.BatchAddClassroomVO;
import com.frontleaves.scheduling.models.vo.ClassroomVO;
import com.frontleaves.scheduling.services.*;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 教室控制器
 * <p>
 * 该控制器提供了与教室相关的 RESTful API 接口。通过这些接口，用户可以进行教室的管理操作，
 * 包括但不限于创建、查询、更新和删除教室信息。所有请求都应通过 {@code /api/v1/classroom} 路径进行。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/classroom")
public class ClassroomController {
    private final ClassroomService classroomService;
    private final BuildingService buildingService;
    private final CampusService campusService;
    private final DepartmentService departmentService;
    private final TablesChairsTypeService tablesChairsTypeService;

    /**
     * 获取教室标签列表
     * <p>
     * 该方法用于获取所有教室标签的列表。通过调用 {@code ClassroomService} 的 {@code listClassroomTags} 方法，
     * 从数据库中获取所有教室标签信息，并将其封装在 {@code BaseResponse} 对象中返回。
     * </p>
     *
     * @return 包含教室标签列表的 {@code ResponseEntity} 对象，其中 {@code BaseResponse} 中的数据部分为 {@code List<ClassroomTagDTO>} 类型
     */
    @RequestLogin
    @GetMapping("/tags")
    public ResponseEntity<BaseResponse<List<ClassroomTagDTO>>> getClassroomTagList() {
        List<ClassroomTagDTO> getTags = classroomService.listClassroomTags();
        return ResultUtil.success("成功", getTags);
    }

    /**
     * 获取教室类型列表
     * <p>
     * 该方法用于获取所有教室类型的列表。通过调用 {@code ClassroomService} 的 {@code listClassroomTypes} 方法，
     * 从数据库中获取所有教室类型信息，并将其封装在 {@code BaseResponse} 对象中返回。
     * </p>
     *
     * @return 包含教室类型列表的 {@code ResponseEntity} 对象，其中 {@code BaseResponse} 中的数据部分为 {@code List<ClassroomTypeDTO>} 类型
     */
    @RequestLogin
    @GetMapping("/types")
    public ResponseEntity<BaseResponse<List<ClassroomTypeDTO>>> getClassroomTypeList() {
        List<ClassroomTypeDTO> getTypes = classroomService.listClassroomTypes();
        return ResultUtil.success("成功", getTypes);
    }

    /**
     * 获取教室列表分页数据
     * <p>
     * 该方法用于根据给定的参数获取教室信息的分页数据。支持通过关键字、标签和类型进行过滤，并允许指定分页参数。
     *
     * @param page    分页的页码，从1开始，默认值为1
     * @param size    每页显示的数据条数，默认值为20，最大值为200
     * @param isDesc  是否按降序排序，默认值为true
     * @param keyword 查询的关键字，可选参数
     * @param tag     查询的标签，可选参数
     * @param type    查询的类型，可选参数
     * @return 包含教室列表分页数据的响应实体
     */
    @RequestLogin
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<ClassroomInfoDTO>>> getClassroomPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "type", required = false) String type
    ) {
        if (size > 200) {
            throw new BusinessException("单页查询不允许超过 200", ErrorCode.PARAMETER_INVALID);
        }
        PageDTO<ClassroomInfoDTO> classroomList = classroomService.getClassroomPage(page, size, isDesc, keyword, tag, type);
        return ResultUtil.success("教室列表成功", classroomList);
    }

    /**
     * 获取教室简单列表
     * <p>
     * 该方法用于获取教室的简化信息列表，主要用于下拉框等简单展示场景。
     * 返回的数据仅包含教室的基本信息，如UUID、编号、名称、容量和状态。
     * </p>
     *
     * @return 包含教室简单信息列表的响应实体
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<ClassroomLiteDTO>>> getClassroomLiteList(
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        List<ClassroomLiteDTO> classroomList = classroomService.listClassroomLite(keyword);
        return ResultUtil.success("获取教室列表成功", classroomList);
    }

    /**
     * 获取单个教室信息
     * <p>
     * 该方法用于根据教室的 UUID 获取教室的详细信息。如果提供的教室 UUID 不符合 UUID 格式，
     * 将抛出 {@code BusinessException} 异常，并返回相应的错误信息。如果教室不存在，
     * 也将抛出 {@code BusinessException} 异常。成功获取后，将返回包含教室详细信息的响应。
     * </p>
     *
     * @param classroomUuid 教室的唯一标识符，必须符合 UUID 格式（不含中划线）
     * @return 包含教室详细信息的 ResponseEntity 对象
     */
    @RequestLogin
    @GetMapping("/{classroom_uuid}")
    public ResponseEntity<BaseResponse<ClassroomInfoDTO>> getClassroom(
            @PathVariable("classroom_uuid") String classroomUuid
    ) {
        if (!classroomUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("教室主键不合法", ErrorCode.PARAMETER_INVALID);
        }
        ClassroomInfoDTO classroomInfoDTO = Optional.ofNullable(classroomService.getClassroomByUuid(classroomUuid))
                .orElseThrow(() -> new BusinessException("教室不存在", ErrorCode.NOT_EXIST));
        return ResultUtil.success("获取教室信息成功", classroomInfoDTO);
    }

    /**
     * 添加教室
     * <p>
     * 该方法用于根据传入的 {@code ClassroomVO} 对象添加一个新的教室。在添加过程中，会进行一系列数据可用性检查，确保关联的教学楼、校区、教室类型、管理部门以及桌椅类型均存在。
     * 如果任何一项数据不存在，则抛出 {@code BusinessException} 异常，并附带相应的错误码。如果所有数据验证通过，则调用服务层的方法将新的教室信息保存到数据库中，并返回包含成功信息及新教室详情的响应。
     *
     * @param classroomVO 包含待添加教室详细信息的视图对象
     * @return 响应实体，包含操作结果和新创建的教室信息
     */
    @RequestRole({"管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<ClassroomInfoDTO>> addClassroom(
            @RequestBody @Validated ClassroomVO classroomVO
    ) {
        // 数据是否存在
        Optional.ofNullable(classroomService.getClassroomByNumber(classroomVO.getNumber()))
                .ifPresent(data -> {
                    throw new BusinessException("教室已存在", ErrorCode.EXISTED);
                });
        // 数据可用性检查
        this.verifyClassroomDataHasExist(classroomVO);
        // 数据检查完成添加教室
        ClassroomInfoDTO classroomInfoDTO = classroomService.addClassroom(classroomVO);
        return ResultUtil.success("添加教室成功", classroomInfoDTO);
    }

    /**
     * 编辑教室
     * <p>
     * 该方法用于根据传入的 {@code ClassroomVO} 对象编辑指定的教室。在编辑过程中，会进行一系列数据可用性检查，确保关联的教学楼、校区、教室类型、管理部门以及桌椅类型均存在。
     * 如果任何一项数据不存在，则抛出 {@code BusinessException} 异常，并附带相应的错误码。如果所有数据验证通过，则调用服务层的方法将新的教室信息保存到数据库中，并返回包含成功信息及新教室详情的响应。
     *
     * @param classroomVO 包含待编辑教室详细信息的视图对象
     * @return 响应实体，包含操作结果和新创建的教室信息
     */
    @RequestRole({"管理员"})
    @PutMapping("/{classroom_uuid}")
    public ResponseEntity<BaseResponse<ClassroomInfoDTO>> editClassroom(
            @PathVariable("classroom_uuid") String classroomUuid,
            @RequestBody @Validated ClassroomVO classroomVO
    ) {
        if (!classroomUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("教室主键不合法", ErrorCode.PARAMETER_INVALID);
        }
        // 数据是否存在
        Optional.ofNullable(classroomService.getClassroomByUuid(classroomUuid))
                .orElseThrow(() -> new BusinessException("教室不存在", ErrorCode.NOT_EXIST));
        // 数据可用性检查
        this.verifyClassroomDataHasExist(classroomVO);
        // 数据检查完成编辑教室
        ClassroomInfoDTO classroomInfoDTO = classroomService.editClassroom(classroomUuid, classroomVO);
        return ResultUtil.success("编辑教室成功", classroomInfoDTO);
    }

    /**
     * 删除教室
     * <p>
     * 该方法通过给定的教室 UUID 删除指定的教室。如果提供的教室 UUID 不符合 UUID 格式，
     * 将抛出 {@code BusinessException} 异常，并返回相应的错误信息。
     * 成功删除后，将返回一个成功的响应消息。
     *
     * @param classroomUuid 教室的唯一标识符，必须符合 UUID 格式（不含中划线）
     * @return 包含成功或失败信息的 ResponseEntity 对象
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{classroom_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteClassroom(
            @PathVariable("classroom_uuid") String classroomUuid
    ) {
        if (!classroomUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("教室主键不合法", ErrorCode.PARAMETER_INVALID);
        }
        classroomService.deleteClassroom(classroomUuid);
        return ResultUtil.success("删除教室成功");
    }

    /**
     * 验证教室数据是否存在
     * <p>此方法用于验证传入的 {@code ClassroomVO} 对象中关联的数据实体是否在系统中存在。如果任何关联的数据实体不存在，则抛出相应的业务异常。</p>
     *
     * @param classroomVO 包含教室信息的对象，不能为空
     * @throws BusinessException 如果以下任一情况发生时抛出：<ul>
     *                           <li>教学楼不存在</li>
     *                           <li>校区不存在</li>
     *                           <li>教室类型不存在</li>
     *                           <li>管理部门不存在</li>
     *                           <li>桌椅类型不存在</li>
     *                           <li>教室标签不存在</li>
     *                           </ul>
     */
    private void verifyClassroomDataHasExist(@NotNull ClassroomVO classroomVO) {
        Optional.ofNullable(buildingService.getBuildingByUuidOrName(classroomVO.getBuildingUuid()))
                .orElseThrow(() -> new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST));
        Optional.ofNullable(campusService.getCampusByUuid(classroomVO.getCampusUuid()))
                .orElseThrow(() -> new BusinessException("校区不存在", ErrorCode.NOT_EXIST));
        Optional.ofNullable(classroomService.getClassroomTypeByUuid(classroomVO.getType()))
                .orElseThrow(() -> new BusinessException("教室类型不存在", ErrorCode.NOT_EXIST));
        Optional.ofNullable(classroomVO.getManagementDepartment())
                .filter(value -> !value.isBlank())
                .ifPresent(data -> Optional.ofNullable(departmentService.getDepartmentByUuid(data))
                        .orElseThrow(() -> new BusinessException("管理部门不存在", ErrorCode.NOT_EXIST))
                );
        Optional.ofNullable(classroomVO.getTablesChairsType())
                .filter(value -> !value.isBlank())
                .ifPresent(data -> Optional.ofNullable(tablesChairsTypeService.getTablesChairsTypeByUuid(data))
                        .orElseThrow(() -> new BusinessException("桌椅类型不存在", ErrorCode.NOT_EXIST))
                );
        Optional.ofNullable(classroomVO.getTag())
                .ifPresent(list -> list
                        .forEach(tag -> Optional.ofNullable(classroomService.getClassroomTagByUuid(tag))
                                .orElseThrow(() -> new BusinessException("教室标签不存在", ErrorCode.NOT_EXIST))
                        ));
    }

    /**
     * 获取教室导入模板
     * <p>
     * 该方法用于获取教室批量导入的Excel模板文件。模板包含必填字段、示例数据以及注意事项，
     * 用户可以根据模板格式正确填写数据，然后通过批量导入接口上传。
     * </p>
     *
     * @return 包含Base64编码的模板文件数据和文件信息的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/get-template")
    public ResponseEntity<BaseResponse<FileDTO>> getClassroomImportTemplate() {
        byte[] templateBytes = classroomService.getClassroomImportTemplate();

        // 将字节数组转换为Base64编码字符串
        FileDTO fileDTO = new FileDTO(
                "教室导入模板.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64," +
                        java.util.Base64.getEncoder().encodeToString(templateBytes)
        );

        return ResultUtil.success("获取教室导入模板成功", fileDTO);
    }

    /**
     * 批量导入教室信息
     * <p>
     * 该方法用于批量导入教室信息，支持忽略错误的选项。
     * 当ignoreError为true时，遇到错误数据会跳过继续导入其他数据，并记录错误信息。
     * 当ignoreError为false时，遇到任何错误都会立即终止导入过程。
     * </p>
     *
     * @param batchAddClassroomVO 包含Excel文件Base64编码和导入选项的请求对象
     * @return 包含导入结果统计信息的响应实体
     */
    @RequestRole({"管理员"})
    @PostMapping("/batch-import")
    public ResponseEntity<BaseResponse<BackAddClassroomDTO>> batchImportClassrooms(
            @RequestBody @Validated BatchAddClassroomVO batchAddClassroomVO
    ) {
        // 验证并获取处理后的文件
        byte[] file = classroomService.verifyClassroomBatchAndBackFile(batchAddClassroomVO);

        // 根据ignoreError参数决定导入方式
        BackAddClassroomDTO result = Optional.ofNullable(batchAddClassroomVO.getIgnoreError())
                .filter(Boolean.TRUE::equals)
                .map(ignoreError -> classroomService.batchImportIgnoreError(file))
                .orElseGet(() -> classroomService.batchImportNoIgnoreError(file));

        // 检查是否有教室导入失败
        if (result.getFailedCount() > 0) {
            return ResultUtil.success("存在添加失败的教室", result);
        }

        return ResultUtil.success("批量添加教室成功", result);
    }
}
