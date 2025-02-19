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
import com.frontleaves.scheduling.services.BuildingService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教学楼控制器
 * <p>
 * 该类提供了处理建筑相关请求的 RESTful API，包括获取建筑列表等功能。
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
     * 获取建筑列表
     * <p>
     * 该方法用于根据分页参数和可选的关键词获取教学楼建筑列表。如果提供了关键词，则会根据关键词进行模糊搜索。
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
        log.debug(LogConstant.CONTROLLER + "获取建筑列表，page: {}, size: {}, keyword: {}", page, size, keyword);
        if (keyword == null || keyword.isBlank()) {
            PageDTO<BuildingDTO> buildingList = buildingService.getBuildingList(page, size, isDesc);
            return ResultUtil.success("教学楼建筑列表成功", buildingList);
        } else {
            PageDTO<BuildingDTO> buildingListHasKeyword = buildingService.getBuildingListHasKeyword(page, size, isDesc, keyword);
            return ResultUtil.success("教学楼建筑列表成功", buildingListHasKeyword);
        }
    }
}
