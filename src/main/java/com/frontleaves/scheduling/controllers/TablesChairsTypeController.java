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
import com.frontleaves.scheduling.models.dto.TablesChairsTypeLiteDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TablesChairsTypeDTO;
import com.frontleaves.scheduling.models.vo.TablesChairsTypeVO;
import com.frontleaves.scheduling.services.TablesChairsTypeService;
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
 * 桌椅类型控制器
 *
 * @author xiao_lfeng
 * @version v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tables-chairs")
public class TablesChairsTypeController {

    private final TablesChairsTypeService tablesChairsTypeService;

    /**
     * 添加桌椅类型
     *
     * @param tablesChairsTypeVO 包含要添加的桌椅类型信息的请求体
     * @return 返回一个包含成功消息和新添加桌椅类型信息的响应实体
     */
    @RequestRole({"管理员"})
    @PostMapping("")
    public ResponseEntity<BaseResponse<TablesChairsTypeDTO>> addTablesChairsType(
            @RequestBody @Validated TablesChairsTypeVO tablesChairsTypeVO
    ) {
        TablesChairsTypeDTO tablesChairsTypeDTO = tablesChairsTypeService.addTablesChairsType(
                tablesChairsTypeVO.getName(),
                tablesChairsTypeVO.getDescription(),
                tablesChairsTypeVO.getBase64Img()
        );
        return ResultUtil.success("添加桌椅类型成功", tablesChairsTypeDTO);
    }

    /**
     * 更新桌椅类型
     *
     * @param uuid              桌椅类型的唯一标识符
     * @param tablesChairsTypeVO 包含要更新的桌椅类型信息的请求体
     * @return 返回一个包含成功消息和更新后的桌椅类型信息的响应实体
     */
    @RequestRole({"管理员"})
    @PutMapping("/{uuid}")
    public ResponseEntity<BaseResponse<TablesChairsTypeDTO>> updateTablesChairsType(
            @PathVariable("uuid") String uuid,
            @RequestBody @Validated TablesChairsTypeVO tablesChairsTypeVO
    ) {
        TablesChairsTypeDTO tablesChairsTypeDTO = tablesChairsTypeService.updateTablesChairsType(
                uuid,
                tablesChairsTypeVO.getName(),
                tablesChairsTypeVO.getDescription(),
                tablesChairsTypeVO.getBase64Img()
        );
        return ResultUtil.success("更新桌椅类型成功", tablesChairsTypeDTO);
    }

    /**
     * 删除桌椅类型
     *
     * @param uuid 桌椅类型的唯一标识符
     * @return 返回一个包含成功消息的响应实体
     */
    @RequestRole({"管理员"})
    @DeleteMapping("/{uuid}")
    public ResponseEntity<BaseResponse<String>> deleteTablesChairsType(
            @PathVariable("uuid") String uuid
    ) {
        tablesChairsTypeService.deleteTablesChairsType(uuid);
        return ResultUtil.success("删除桌椅类型成功", uuid);
    }

    /**
     * 获取桌椅类型信息
     *
     * @param uuid 桌椅类型的唯一标识符
     * @return 返回一个包含成功消息和桌椅类型信息的响应实体
     */
    @RequestLogin
    @GetMapping("/{uuid}")
    public ResponseEntity<BaseResponse<TablesChairsTypeDTO>> getTablesChairsType(
            @PathVariable("uuid") String uuid
    ) {
        TablesChairsTypeDTO tablesChairsTypeDTO = tablesChairsTypeService.getTablesChairsTypeByUuid(uuid);
        return ResultUtil.success("获取桌椅类型信息成功", tablesChairsTypeDTO);
    }

    /**
     * 获取桌椅类型分页数据
     *
     * @param page    请求的数据页码，默认值为 {@code 1}
     * @param size    每页显示的记录数，默认值为 {@code 20}
     * @param keyword 用于过滤的关键词，可选参数
     * @param isDesc  结果排序方式，如果设置为 {@code true} 则表示按降序排列，默认值为 {@code true}
     * @return 包含了请求状态和桌椅类型数据列表的响应实体
     */
    @RequestRole({"管理员"})
    @GetMapping("/page")
    public ResponseEntity<BaseResponse<PageDTO<TablesChairsTypeDTO>>> getTablesChairsTypePage(
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
        PageDTO<TablesChairsTypeDTO> pageDTO = tablesChairsTypeService.getTablesChairsTypePage(page, size, isDesc, keyword);
        return ResultUtil.success("获取桌椅类型分页数据成功", pageDTO);
    }

    /**
     * 获取桌椅类型列表
     *
     * @return 包含桌椅类型列表的响应实体
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<TablesChairsTypeLiteDTO>>> getTablesChairsTypeList() {
        List<TablesChairsTypeLiteDTO> tablesChairsTypeList = tablesChairsTypeService.getTablesChairsTypeList();
        return ResultUtil.success("获取桌椅类型列表成功", tablesChairsTypeList);
    }
}
