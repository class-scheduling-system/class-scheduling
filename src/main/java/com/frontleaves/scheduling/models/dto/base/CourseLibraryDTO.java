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

package com.frontleaves.scheduling.models.dto.base;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 课程库DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseLibraryDTO {
    /**
     * 课程库主键
     */
    private String courseLibraryUuid;

    /**
     * 课程编号
     */
    private String id;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程英文名称
     */
    private String englishName;

    /**
     * 课程类别
     */
    private String category;

    /**
     * 课程属性
     */
    private String property;

    /**
     * 课程类型
     */
    private String type;

    /**
     * 课程性质
     */
    private String nature;

    /**
     * 开课院系
     */
    private String department;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 总学时
     */
    private BigDecimal totalHours;

    /**
     * 周学时
     */
    private BigDecimal weekHours;

    /**
     * 理论学时
     */
    private BigDecimal theoryHours;

    /**
     * 实验学时
     */
    private BigDecimal experimentHours;

    /**
     * 实践学时
     */
    private BigDecimal practiceHours;

    /**
     * 上机学时
     */
    private BigDecimal computerHours;

    /**
     * 其他学时
     */
    private BigDecimal otherHours;

    /**
     * 学分
     */
    private BigDecimal credit;
    /**
     * 理论课教室类型
     */
    private String theoryClassroomType;

    /**
     * 实验课教室类型
     */
    private String experimentClassroomType;

    /**
     * 实践课教室类型
     */
    private String practiceClassroomType;

    /**
     * 上机课教室类型
     */
    private String computerClassroomType;

    /**
     * 课程描述
     */
    private String description;

    /**
     * 编辑用户
     */
    private String editUser;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
