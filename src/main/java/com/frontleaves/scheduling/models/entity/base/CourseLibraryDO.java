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

package com.frontleaves.scheduling.models.entity.base;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 课程库表的 DO 对象
 * @author qiyu
 */
@Data
@TableName("cs_course_library")
public class CourseLibraryDO {

    @TableId(value = "course_library_uuid", type = IdType.ASSIGN_UUID)
    private String courseLibraryUuid;

    @TableField("id")
    private String id;

    @TableField("name")
    private String name;

    @TableField("english_name")
    private String englishName;

    @TableField("category")
    private String category;

    @TableField("property")
    private String property;

    @TableField("type")
    private String type;

    @TableField("nature")
    private String nature;

    @TableField("department")
    private String department;

    @TableField("is_enabled")
    private Boolean isEnabled;

    @TableField("total_hours")
    private BigDecimal totalHours;

    @TableField("week_hours")
    private BigDecimal weekHours;

    @TableField("theory_hours")
    private BigDecimal theoryHours;

    @TableField("experiment_hours")
    private BigDecimal experimentHours;

    @TableField("practice_hours")
    private BigDecimal practiceHours;

    @TableField("computer_hours")
    private BigDecimal computerHours;

    @TableField("other_hours")
    private BigDecimal otherHours;

    @TableField("credit")
    private BigDecimal credit;

    @TableField("description")
    private String description;

    @TableField("edit_user")
    private String editUser;

    @TableField("created_at")
    private Timestamp createdAt;

    @TableField("updated_at")
    private Timestamp updatedAt;
}
