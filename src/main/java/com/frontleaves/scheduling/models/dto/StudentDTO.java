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

package com.frontleaves.scheduling.models.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Student 数据传输对象
 * <p>
 * 用于在不同层之间传输学生基本信息。
 * 该 DTO 包含学生主键、学号、学生姓名、性别、年级、学院、专业、班级、对应用户主键、
 * 创建时间及更新时间等字段。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StudentDTO {

    /**
     * 学生主键
     */
    private String studentUuid;

    /**
     * 学号
     */
    private String id;

    /**
     * 学生姓名
     */
    private String name;

    /**
     * 性别（0：女，1：男）
     */
    private Integer gender;

    /**
     * 年级
     */
    private String grade;

    /**
     * 学院
     */
    private String department;

    /**
     * 专业
     */
    private String major;

    /**
     * 班级
     * <p>
     * 注意：由于 class 为 Java 保留关键字，此处使用 clazz 表示班级。
     * </p>
     */
    @TableField(value = "class")
    private String clazz;

    /**
     * 对应用户主键
     */
    private String userUuid;

    /**
     * 创建时间（单位：毫秒时间戳）
     */
    private Long createdAt;

    /**
     * 更新时间（单位：毫秒时间戳）
     */
    private Long updatedAt;
}
