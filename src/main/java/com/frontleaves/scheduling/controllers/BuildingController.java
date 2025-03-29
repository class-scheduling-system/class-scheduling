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
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
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
import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.models.dto.*;
import com.frontleaves.scheduling.models.dto.base.BuildingDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.lite.BuildingLiteDTO;
import com.frontleaves.scheduling.models.dto.merge.PrepareBuildingDTO;
import com.frontleaves.scheduling.models.vo.BatchAddBuildingVO;
import com.frontleaves.scheduling.models.vo.BuildingOperateVO;
import com.frontleaves.scheduling.services.BuildingService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * 教学楼控制器
 * <p>
 * 该类提供了处理教学楼相关请求的 RESTful API，包括获取教学楼列表等功能。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/building")
public class BuildingController {

    private final BuildingService buildingService;

    /**
     * 获取教学楼列表
     * <p>
     * 该方法用于获取教学楼的分页列表。支持按关键词搜索，并且可以指定是否按降序排列。
     * 如果提供了关键词 {@code keyword}，则会根据关键词进行搜索；否则返回所有教学楼的列表。
     * 单页查询的最大条目数为 200，超过此限制将抛出异常。
     *
     * @param page    当前页码，默认值为 1
     * @param size    每页显示的条目数，默认值为 20
     * @param isDesc  是否按降序排列，默认值为 true
     * @param keyword 搜索关键词，可选参数
     * @return 包含教学楼列表的分页数据和响应状态的 {@code ResponseEntity}
     */
    @RequestRole({"管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<BuildingDTO>>> getBuildingPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc,
            @RequestParam(value = "keyword", required = false) String keyword
    ) {
        // 单页查询不允许超过 200
        if (size > 200) {
            throw new BusinessException("单页查询不允许超过 200", ErrorCode.PARAMETER_INVALID);
        }
        log.debug(LogConstant.CONTROLLER + "获取教学楼列表，page: {}, size: {}, keyword: {}", page, size, keyword);
        PageDTO<BuildingDTO> buildingList;
        if (keyword == null || keyword.isBlank()) {
            buildingList = buildingService.getBuildingPage(page, size, isDesc, null);
        } else {
            buildingList = buildingService.getBuildingPage(page, size, isDesc, keyword);
        }
        return ResultUtil.success("教学楼建筑列表成功", buildingList);
    }

    /**
     * 获取教学楼信息
     * <p>
     * 该方法用于根据教学楼的 UUID 或名称获取教学楼的详细信息。如果提供的 {@code building} 参数为空或空白，
     * 则会抛出一个业务异常，提示“教学楼UUID/名称不能为空”。成功获取后，将以 JSON 格式返回教学楼的信息。
     *
     * @param building 教学楼的 UUID 或名称
     * @return 包含教学楼信息的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("")
    public ResponseEntity<BaseResponse<BuildingDTO>> getBuilding(
            @RequestParam String building
    ) {
        String verifyBuilding = Optional.ofNullable(building)
                .filter(buildingName -> !buildingName.isBlank())
                .filter(buildingUuid -> buildingUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException("教学楼主键格式有误", ErrorCode.PARAMETER_INVALID));
        BuildingDTO buildingDTO = Optional.ofNullable(buildingService.getBuildingByUuidOrName(verifyBuilding))
                .orElseThrow(() -> new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST));
        return ResultUtil.success("获取教学楼成功", buildingDTO);
    }

    /**
     * 根据校区获取教学楼列表
     * <p>
     * 该方法用于根据指定的校区UUID获取该校区下的教学楼列表。支持分页查询，并可以设置查询结果是否按降序排列。
     *
     * @param campusUuid 校区的唯一标识符，不能为空或空白字符串
     * @param page       请求的页码，默认值为1
     * @param size       每页显示的教学楼数量，默认值为20
     * @param isDesc     查询结果是否按降序排列，默认值为true
     * @return 包含状态信息和数据的响应实体，其中数据部分是{@code PageDTO<BuildingDTO>}类型，代表了当前页的教学楼列表
     */
    @RequestRole({"管理员"})
    @GetMapping("/campus/{campus_uuid}")
    public ResponseEntity<BaseResponse<PageDTO<BuildingDTO>>> getBuildingByCampus(
            @PathVariable("campus_uuid") String campusUuid,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc
    ) {
        if (!campusUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("校区主键不合法", ErrorCode.PARAMETER_INVALID);
        }
        PageDTO<BuildingDTO> buildingList = buildingService.getBuildingByCampus(campusUuid, page, size, isDesc);
        return ResultUtil.success("获取教学楼列表成功", buildingList);
    }

    /**
     * 添加教学楼
     * <p>
     * 该方法用于向系统中添加一个新的教学楼。通过传入包含校园 UUID、教学楼名称和状态的 {@code BuildingOperateVO} 对象，
     * 系统将调用相应的服务层方法来完成教学楼的添加操作。
     * <p>
     * 请求需要管理员权限，并且请求体中的数据需要经过验证。
     *
     * @param buildingVO 包含新教学楼信息的对象，其中包括：{@code campusUuid} 校园唯一标识符,
     *                   {@code buildingName} 教学楼名称, 和 {@code status} 教学楼状态
     * @return ResponseEntity<BaseResponse < Void>> 返回一个表示操作结果的响应实体，其中包含了成功或失败的信息。
     */
    @RequestRole({"管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<Void>> addBuilding(
            @RequestBody @Validated BuildingOperateVO buildingVO
    ) {
        buildingService.addBuilding(
                buildingVO.getCampusUuid(),
                buildingVO.getBuildingName(),
                buildingVO.getStatus()
        );
        return ResultUtil.success("添加教学楼成功");
    }

    /**
     * 更新教学楼信息
     * <p>
     * 该方法用于更新指定 UUID 的教学楼信息。通过提供的 {@code BuildingOperateVO} 对象，可以更新教学楼的名称、状态以及所属校区。
     * 只有具备管理员权限的用户才能调用此接口。
     *
     * @param buildingUuid 教学楼的唯一标识符
     * @param buildingVO   包含更新后的教学楼信息的对象
     * @return 返回一个包含成功信息的响应实体
     */
    @RequestRole({"管理员"})
    @PutMapping("/{building_uuid}")
    public ResponseEntity<BaseResponse<Void>> updateBuilding(
            @PathVariable("building_uuid") String buildingUuid,
            @RequestBody @Validated BuildingOperateVO buildingVO
    ) {
        log.debug(LogConstant.CONTROLLER + "更新教学楼信息，buildingUuid: {}", buildingUuid);
        if (!buildingUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("教学楼主键不合法", ErrorCode.PARAMETER_INVALID);
        }
        buildingService.updateBuilding(
                buildingUuid,
                buildingVO.getCampusUuid(),
                buildingVO.getBuildingName(),
                buildingVO.getStatus()
        );
        return ResultUtil.success("更新教学楼成功");
    }

    /**
     * 删除教学楼
     * <p>
     * 该方法用于根据提供的教学楼唯一标识 {@code buildingUuid} 删除指定的教学楼。
     * 只有具有管理员权限的用户才能调用此接口。
     * <p>
     * 成功删除后，返回一个包含成功信息的响应实体。
     *
     * @param buildingUuid 教学楼的唯一标识符
     * @return 包含删除成功信息的响应实体
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{building_uuid}")
    public ResponseEntity<BaseResponse<Void>> deleteBuilding(
            @PathVariable("building_uuid") String buildingUuid
    ) {
        if (!buildingUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("教学楼主键不合法", ErrorCode.PARAMETER_INVALID);
        }
        buildingService.deleteBuilding(buildingUuid);
        return ResultUtil.success("删除教学楼成功");
    }


    /**
     * 获取教学楼页面信息
     * <p>
     * 该方法通过GET请求处理获取教学楼列表的请求它接受一个keyword参数，
     * 用于搜索或过滤教学楼信息返回的是一个包含BuildingLiteDTO对象列表的响应，
     * 表示筛选后的教学楼信息列表
     *
     * @param keyword 搜索关键字，用于匹配教学楼信息
     * @return 包含教学楼列表的响应实体，使用HTTP状态码和自定义的BaseResponse封装
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<BuildingLiteDTO>>> getBuildingList(
            @RequestParam(required = false) String keyword
    ) {
        List<BuildingLiteDTO> buildingList = buildingService.getBuildingList(keyword);
        return ResultUtil.success("获取教学楼列表成功", buildingList);
    }

    /**
     * 获取教学楼导入模板
     * 该方法用于提供用户下载建筑物信息导入的Excel模板，特别适用于管理员角色
     * 使用了RequestRole注解来限制只有具有"管理员"角色的用户才能访问此端点
     *
     * @return ResponseEntity<byte[]> 返回包含模板文件的响应实体，设置为附件形式，文件名为"教学楼导入模板.xlsx"
     */
    @RequestRole({"管理员"})
    @GetMapping("/get-template")
    public ResponseEntity<byte[]> getBuildingImportTemplate() {
        // 准备建筑物示例数据，用于生成导入模板
        PrepareBuildingDTO prepareBuildingExampleDTO = buildingService.prepareCampusData();
        // 获取建筑物导入模板的字节数组
        byte[] bytes = buildingService.getBuildingImportTemplate(prepareBuildingExampleDTO);

        // 初始化HTTP响应头，用于设置模板文件的下载信息
        HttpHeaders headers = Optional.of(new HttpHeaders())
                .map(header -> {
                    // 设置内容类型为二进制流
                    header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
                    // 设置内容长度
                    header.setContentLength(bytes.length);
                    // 对文件名进行URL编码，确保文件名在不同浏览器中正确显示
                    String fileName = URLEncoder.encode("教学楼导入模板.xlsx", StandardCharsets.UTF_8)
                            .replace("+", "%20");
                    // 设置内容处置，以附件形式下载文件，并处理文件名的UTF-8编码
                    header.add(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileName + "\"; filename*=UTF-8''" + fileName);
                    return header;
                })
                // 如果生成响应头失败，则抛出业务异常
                .orElseThrow(() -> new BusinessException("获取响应头失败", ErrorCode.SERVER_INTERNAL_ERROR));

        // 返回包含模板文件的响应实体，状态码为HTTP 200 OK
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * 批量导入教学楼信息
     * <p>
     * 该方法用于批量导入教学楼信息，支持忽略错误的选项。
     * 只有具有"管理员"角色的用户才能访问此方法。
     *
     * @param batchAddBuildingVO 批量添加教学楼的信息对象
     * @return 返回一个包含操作结果的ResponseEntity对象
     */
    @RequestRole({"管理员"})
    @PostMapping("/batch-import")
    // 定义方法批量导入教学楼信息，返回一个包含操作结果的ResponseEntity对象
    public ResponseEntity<BaseResponse<BackAddBuildingDTO>> batchImportBuildings(
            @RequestBody @Validated BatchAddBuildingVO batchAddBuildingVO
    ) {
        // 调用buildingService的verifyBuildingBatchAndBackFile方法验证批量导入的数据并获取处理后的文件
        byte[] file = buildingService.verifyBuildingBatchAndBackFile(batchAddBuildingVO);

        // 根据batchAddBuildingVO中的ignoreError标志决定是否忽略错误并继续执行批量导入
        // 如果ignoreError为true，则调用batchImportIgnoreError方法，否则调用batchImportNoIgnoreError方法
        // 执行批量导入教学楼的操作
        BackAddBuildingDTO backAddBuildingDTO = Optional.ofNullable(batchAddBuildingVO.getIgnoreError())
                .filter(Boolean.TRUE::equals)
                .map(ignoreError -> buildingService.batchImportIgnoreError(file))
                .orElseGet(() -> buildingService.batchImportNoIgnoreError(file));

        // 检查是否有教学楼导入失败
        if (backAddBuildingDTO.getFailedCount() > 0) {
            // 如果有教学楼导入失败，返回带有错误信息的响应
            return ResultUtil.success("存在添加失败的教学楼", backAddBuildingDTO);
        }

        // 如果所有教学楼都成功导入，返回批量添加教学楼成功的响应
        return ResultUtil.success("批量添加教学楼成功", backAddBuildingDTO);
    }
}
