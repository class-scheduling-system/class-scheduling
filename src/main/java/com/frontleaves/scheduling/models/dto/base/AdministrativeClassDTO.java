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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 行政班级DTO
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class AdministrativeClassDTO {
    /**
     * 行政班主键
     */
    private String administrativeClassUuid;
    /**
     * 所属部门/院系
     */
    private String departmentUuid;
    /**
     * 所属专业
     */
    private String majorUuid;
    /**
     * 班级编号
     */
    private String classCode;
    /**
     * 班级名称
     */
    private String className;
    /**
     * 年级UUID
     */
    private String gradeUuid;
    /**
     * 学生人数
     */
    private Integer studentCount;
    /**
     * 辅导员UUID
     */
    private String counselorUuid;
    /**
     * 班长UUID
     */
    private String monitorUuid;
    /**
     * 是否启用(0:禁用,1:启用)
     */
    private Boolean isEnabled;
    /**
     * 班级描述
     */
    private String description;
    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;
    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}
