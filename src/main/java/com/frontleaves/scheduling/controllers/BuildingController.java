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

import com.frontleaves.scheduling.constants.LogConstant;
import com.frontleaves.scheduling.models.dto.BuildingDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.vo.BuildingOperateVO;
import com.frontleaves.scheduling.services.BuildingService;
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
     * 该方法用于根据分页参数和可选的关键词获取教学楼教学楼列表。如果提供了关键词，则会根据关键词进行模糊搜索。
     *
     * @param page    当前页码，默认值为 1
     * @param size    每页显示的条目数，默认值为 20，最大值为 200
     * @param isDesc  是否按降序排列，默认值为 true
     * @param keyword 可选的搜索关键词
     * @return 包含分页数据的响应实体，其中包含 {@code BuildingDTO} 对象的列表
     */
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<PageDTO<BuildingDTO>>> getBuildingList(
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
        if (keyword == null || keyword.isBlank()) {
            PageDTO<BuildingDTO> buildingList = buildingService.getBuildingList(page, size, isDesc);
            return ResultUtil.success("教学楼建筑列表成功", buildingList);
        } else {
            PageDTO<BuildingDTO> buildingListHasKeyword = buildingService.getBuildingListHasKeyword(page, size, isDesc, keyword);
            return ResultUtil.success("教学楼建筑列表成功", buildingListHasKeyword);
        }
    }

    /**
     * 获取教学楼信息
     * <p>
     * 通过提供的教学楼 UUID 或名称获取教学楼的详细信息。如果传入的教学楼标识为空或无效，则会抛出异常。
     *
     * @param building 教学楼的 UUID 或名称
     * @return 包含教学楼信息的 {@code ResponseEntity} 对象，其中包含一个 {@code BaseResponse<BuildingDTO>} 对象
     */
    @GetMapping("")
    public ResponseEntity<BaseResponse<BuildingDTO>> getBuilding(
            @RequestParam String building
    ) {
        if (building == null || building.isBlank()) {
            throw new BusinessException("教学楼UUID/名称不能为空", ErrorCode.PARAMETER_INVALID);
        }
        BuildingDTO getBuildingDTO = buildingService.getBuilding(building);
        return ResultUtil.success("获取教学楼信息成功", getBuildingDTO);
    }

    /**
     * 根据校区获取教学楼列表
     * <p>
     * 该方法用于根据指定的校区UUID来获取其下的教学楼信息。支持分页查询，同时允许设置是否降序排列结果。
     *
     * @param campusUuid 校区的唯一标识符，不能为空或空白字符串
     * @param page       请求的页码，默认为1
     * @param size       每页显示的教学楼数量，默认为20
     * @param isDesc     是否按照降序排列，默认为true表示降序
     * @return 包含请求状态和数据响应体的ResponseEntity对象，其中数据部分是包含分页信息的教学楼列表
     */
    @GetMapping("/campus/{campus_uuid}")
    public ResponseEntity<BaseResponse<PageDTO<BuildingDTO>>> getBuildingByCampus(
            @PathVariable("campus_uuid") String campusUuid,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc
    ) {
        if (campusUuid == null || campusUuid.isBlank()) {
            throw new BusinessException("校区不能为空", ErrorCode.PARAMETER_INVALID);
        }
        PageDTO<BuildingDTO> buildingList = buildingService.getBuildingByCampus(campusUuid, page, size, isDesc);
        return ResultUtil.success("获取教学楼列表成功", buildingList);
    }

    /**
     * 添加教学楼
     * <p>
     * 该方法用于向系统中添加一个新的教学楼信息。通过传入的 {@code BuildingOperateVO} 对象，可以指定教学楼所属的校区UUID、教学楼名称以及其状态。
     * 成功添加后，返回一个包含成功消息的响应实体。
     *
     * @param buildingVO 包含新增教学楼信息的对象，包括校区UUID、教学楼名称和状态
     * @return 返回一个表示操作结果的响应实体，如果添加成功，则携带相应的成功消息
     */
    @PostMapping("")
    public ResponseEntity<BaseResponse<Void>> addBuilding(
            @RequestBody @Validated BuildingOperateVO buildingVO
    ) {
        buildingService.addBuilding(buildingVO.getCampusUuid(), buildingVO.getBuildingName(), buildingVO.getStatus());
        return ResultUtil.success("添加教学楼成功");
    }

    /**
     * 更新教学楼信息
     * <p>
     * 该方法用于根据提供的教学楼 UUID 更新教学楼的相关信息。更新的信息包括所属校区 UUID、教学楼名称以及状态。
     * 方法接收两个参数：一个是表示教学楼的唯一标识符 {@code buildingUuid}，另一个是包含待更新数据的对象 {@code BuildingOperateVO}。
     * 成功执行后，将返回一个成功响应。
     *
     * @param buildingUuid 教学楼的唯一标识符
     * @param buildingVO 包含更新所需数据的对象，具体包括校区 UUID、教学楼名称和状态
     * @return 包含操作结果的消息，如果更新成功，则返回成功提示
     */
    @PutMapping("/{building_uuid}")
    public ResponseEntity<BaseResponse<Void>> updateBuilding(
            @PathVariable("building_uuid") String buildingUuid,
            @RequestBody @Validated BuildingOperateVO buildingVO
    ) {
        buildingService.updateBuilding(
                buildingUuid,
                buildingVO.getCampusUuid(),
                buildingVO.getBuildingName(),
                buildingVO.getStatus()
        );
        return ResultUtil.success("更新教学楼成功");
    }
}
