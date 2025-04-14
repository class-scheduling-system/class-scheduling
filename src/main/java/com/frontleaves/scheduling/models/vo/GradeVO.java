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

package com.frontleaves.scheduling.models.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Date;

/**
 * 年级视图对象
 * <p>
 * 该类用于接收前端传递的年级信息，封装年级相关的请求参数。
 * 包含年级名称、入学年份、年级开始日期、年级结束日期和年级描述等字段。
 * </p>
 *
 * @author xiao_lfneg
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class GradeVO {
    
    /**
     * 年级名称（如：2020级、2021级）
     */
    @NotBlank(message = "年级名称不能为空")
    @Size(min = 2, max = 32, message = "年级名称长度必须在2-32个字符之间")
    private String name;

    /**
     * 入学年份
     */
    @NotNull(message = "入学年份不能为空")
    private Short year;

    /**
     * 年级开始日期
     */
    @NotNull(message = "年级开始日期不能为空")
    @PastOrPresent(message = "年级开始日期不能是未来日期")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date startDate;

    /**
     * 年级结束日期
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date endDate;

    /**
     * 年级描述
     */
    @Size(max = 255, message = "年级描述不能超过255个字符")
    private String description;
} 
