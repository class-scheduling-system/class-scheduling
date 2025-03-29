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

import com.frontleaves.scheduling.annotations.RequestRole;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.UnitCategoryDTO;
import com.frontleaves.scheduling.models.dto.UnitCategoryLiteDTO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.vo.UnitCategoryVO;
import com.frontleaves.scheduling.services.UnitCategoryService;
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
import java.util.Optional;

/**
 * 单位类别控制器
 *
 * @author xiao_lfeng
 * @version v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/unit/category")
public class UnitCategoryController {

    private final UnitCategoryService unitCategoryService;

    /**
     * 添加单位类别
     *
     * @param unitCategoryVO 包含要添加的单位类别信息的请求体
     * @return 返回一个包含成功消息和新添加单位类别信息的响应实体
     */
    @RequestRole({"管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<UnitCategoryDTO>> addUnitCategory(
            @RequestBody @Validated UnitCategoryVO unitCategoryVO
    ) {
        unitCategoryService.checkAddUnitCategoryVO(unitCategoryVO);
        UnitCategoryDTO unitCategoryDTO = unitCategoryService.addUnitCategory(unitCategoryVO);
        return ResultUtil.success("添加单位类别成功", unitCategoryDTO);
    }

    /**
     * 更新单位类别
     *
     * @param unitCategoryUuid 单位类别的唯一标识符
     * @param unitCategoryVO   包含要更新的单位类别信息的请求体
     * @return 返回一个包含成功消息和更新后的单位类别信息的响应实体
     */
    @RequestRole({"管理员"})
    @PutMapping("/{unit_category_uuid}")
    public ResponseEntity<BaseResponse<UnitCategoryDTO>> updateUnitCategory(
            @PathVariable("unit_category_uuid") String unitCategoryUuid,
            @RequestBody @Validated UnitCategoryVO unitCategoryVO
    ) {
        UnitCategoryDO unitCategoryDO = unitCategoryService.checkUpdateUnitCategoryVO(unitCategoryUuid, unitCategoryVO);
        UnitCategoryDTO unitCategoryDTO = unitCategoryService.updateUnitCategory(unitCategoryVO, unitCategoryDO);
        return ResultUtil.success("更新单位类别成功", unitCategoryDTO);
    }

    /**
     * 删除单位类别
     *
     * @param unitCategoryUuid 单位类别的唯一标识符
     * @return 返回一个包含成功消息和被删除单位类别唯一标识符的响应实体
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{unit_category_uuid}")
    public ResponseEntity<BaseResponse<String>> deleteUnitCategory(
            @PathVariable("unit_category_uuid") String unitCategoryUuid
    ) {
        UnitCategoryDO unitCategoryDO = unitCategoryService.checkDeleteUnitCategory(unitCategoryUuid);
        unitCategoryService.deleteUnitCategory(unitCategoryDO);
        return ResultUtil.success("删除单位类别成功", unitCategoryUuid);
    }

    /**
     * 获取单位类别信息
     *
     * @param unitCategoryUuid 单位类别的唯一标识符
     * @return 返回一个包含成功消息和单位类别信息的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/{unit_category_uuid}")
    public ResponseEntity<BaseResponse<UnitCategoryDTO>> getUnitCategory(
            @PathVariable("unit_category_uuid") String unitCategoryUuid
    ) {
        UnitCategoryDTO unitCategoryDTO = unitCategoryService.getUnitCategoryDetail(unitCategoryUuid);
        return ResultUtil.success("获取单位类别信息成功", unitCategoryDTO);
    }

    /**
     * 获取单位类别分页数据
     *
     * @param page    请求的数据页码，默认值为 {@code 1}
     * @param size    每页显示的记录数，默认值为 {@code 20}
     * @param keyword 用于过滤的关键词，可选参数
     * @param isDesc  结果排序方式，如果设置为 {@code true} 则表示按降序排列，默认值为 {@code true}
     * @return 包含了请求状态和单位类别数据列表的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<UnitCategoryDTO>>> getUnitCategoryPage(
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "20") Integer size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "is_desc", defaultValue = "true") Boolean isDesc
    ) {
        Optional.ofNullable(size)
                .filter(s -> s > 0)
                .orElseThrow(() -> new BusinessException("每页显示数量错误", ErrorCode.PARAMETER_INVALID));
        Optional.of(size)
                .filter(s -> s <= 200)
                .orElseThrow(() -> new BusinessException("单页查询不允许超过 200", ErrorCode.PARAMETER_INVALID));
        Optional.ofNullable(page)
                .filter(p -> p > 0)
                .orElseThrow(() -> new BusinessException("页码参数错误", ErrorCode.PARAMETER_INVALID));
        keyword = Optional.ofNullable(keyword)
                .filter(key -> !key.isBlank())
                .orElse(null);
        PageDTO<UnitCategoryDTO> pageOfUnitCategory = unitCategoryService.getPageOfUnitCategory(page, size, isDesc, keyword);
        return ResultUtil.success("获取单位类别分页数据成功", pageOfUnitCategory);
    }

    /**
     * 获取单位类别列表
     *
     * @return 包含单位类别列表的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<UnitCategoryLiteDTO>>> getUnitCategoryList() {
        List<UnitCategoryLiteDTO> unitCategoryList = unitCategoryService.getUnitCategoryList();
        return ResultUtil.success("获取单位类别列表成功", unitCategoryList);
    }
}
