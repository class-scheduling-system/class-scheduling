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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 教师课程偏好视图对象
 * <p>
 * 该类用于接收前端传递的教师课程偏好信息，包含了教师对特定时间段的课程偏好设置。
 * 所有字段都经过了适当的验证注解，以确保数据的合法性。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class TeacherPreferencesVO {
    /**
     * 教师主键
     */
    @NotNull(message = "教师主键不能为空")
    @Pattern(regexp = "^[0-9a-f]{32}$", message = "教师主键格式不正确")
    private String teacherUuid;

    /**
     * 学期主键
     */
    @NotNull(message = "学期主键不能为空")
    @Pattern(regexp = "^[0-9a-f]{32}$", message = "学期主键格式不正确")
    private String semesterUuid;

    /**
     * 星期几（1-7）
     */
    @NotNull(message = "星期几不能为空")
    @Min(value = 1, message = "星期几必须在1-7之间")
    @Max(value = 7, message = "星期几必须在1-7之间")
    private Integer dayOfWeek;

    /**
     * 第几节课（1-12）
     */
    @NotNull(message = "第几节课不能为空")
    @Min(value = 1, message = "第几节课必须在1-12之间")
    @Max(value = 12, message = "第几节课必须在1-12之间")
    private Integer timeSlot;

    /**
     * 偏好程度（1：最不期望，2：尽量避免，3：可接受，4：较期望，5：非常期望）
     */
    @NotNull(message = "偏好程度不能为空")
    @Min(value = 1, message = "偏好程度必须在1-5之间")
    @Max(value = 5, message = "偏好程度必须在1-5之间")
    private Integer preferenceLevel;

    /**
     * 偏好原因
     */
    private String reason;
}