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

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;

/**
 * 教学班数据传输对象
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Accessors(chain = true)
public class TeachingClassDTO {

    /**
     * 教学班主键
     */
    private String teachingClassUuid;

    /**
     * 学期主键
     */
    private String semesterUuid;

    /**
     * 学期名称（非数据库字段，用于前端展示）
     */
    private String semesterName;

    /**
     * 课程主键
     */
    private String courseUuid;

    /**
     * 课程名称（非数据库字段，用于前端展示）
     */
    private String courseName;

    /**
     * 教学班编号
     */
    private String teachingClassCode;

    /**
     * 教学班名称
     */
    private String teachingClassName;

    /**
     * 包含的行政班级(包含班级UUID和课程性质[必修/选修])
     * 用于JSON序列化/反序列化
     */
    private String administrativeClasses;

    /**
     * 行政班级列表（非数据库字段，用于前端展示）
     * 包含行政班UUID和课程性质
     */
    private List<Map<String, Object>> administrativeClassList;

    /**
     * 行政班级名称列表（非数据库字段，用于前端展示）
     */
    private List<String> administrativeClassNames;

    /**
     * 如果是必修课则该字段为true，否则为false
     */
    private Boolean isAdministrative;

    /**
     * 班级规模
     */
    private Integer classSize;

    /**
     * 实际学生人数
     */
    private Integer actualStudentCount;

    /**
     * 开课院系
     */
    private String courseDepartmentUuid;

    /**
     * 开课院系名称（非数据库字段，用于前端展示）
     */
    private String courseDepartmentName;

    /**
     * 教学班描述
     */
    private String description;

    /**
     * 是否启用(0:禁用,1:启用)
     */
    private Boolean isEnabled;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
