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

package com.frontleaves.scheduling.models.vo;

import com.frontleaves.scheduling.constants.StringConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 教学楼操作视图对象
 * <p>
 * 该类用于表示教学楼操作相关的数据，主要用于前端与后端的数据交互。它包含了教学楼名称、校区主键以及教学楼状态等信息。
 * <p>
 * 通过使用 {@code @Getter} 注解，自动为所有字段生成 getter 方法；通过使用 {@code @NoArgsConstructor} 和 {@code @AllArgsConstructor} 注解，
 * 分别提供了无参构造函数和全参数构造函数，方便不同场景下的对象创建。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class BuildingOperateVO {

    /**
     * 教学楼名称
     */
    @NotBlank(message = "教学楼名称不能为空")
    private String buildingName;

    /**
     * 校区主键
     */
    @NotBlank(message = "校区不能为空")
    @Pattern(regexp = StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION, message = "校区输入有误")
    private String campusUuid;

    /**
     * 教学楼状态（0:禁用，1:启用）
     */
    @NotNull(message = "教学楼状态不能为空")
    private Boolean status;
}
