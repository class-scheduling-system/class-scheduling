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
import com.frontleaves.scheduling.models.dto.scheduling.CreditHourTypeEnuDTO;
import com.frontleaves.scheduling.services.CreditHourTypeService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学时类型控制器
 * <p>
 * 提供学时类型相关接口，包括获取学时类型列表等功能
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/credit-hour-type")
public class CreditHourTypeController {

    private final CreditHourTypeService creditHourTypeService;

    /**
     * 获取学时类型列表
     * <p>
     * 获取系统中所有学时类型的列表，不需要分页。
     * </p>
     *
     * @return 学时类型列表
     */
    @RequestLogin
    @GetMapping("/list")
    public ResponseEntity<BaseResponse<List<CreditHourTypeEnuDTO>>> getCreditHourTypeList() {
        List<CreditHourTypeEnuDTO> creditHourTypeList = creditHourTypeService.getList();
        return ResultUtil.success("获取学时类型列表成功", creditHourTypeList);
    }
} 