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
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 教学班表实体类
 * <p>
 * 对应数据库表：`cs_teaching_class`
 * 主键为 teaching_class_uuid，类型为 UUID 自动生成。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@TableName(value = "cs_teaching_class")
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class TeachingClassDO {
    /**
     * 教学班主键
     */
    @TableId(value = "teaching_class_uuid", type = IdType.ASSIGN_UUID)
    private String teachingClassUuid;

    /**
     * 学期主键
     */
    private String semesterUuid;

    /**
     * 课程主键
     */
    private String courseUuid;

    /**
     * 教学班编号
     */
    private String teachingClassCode;

    /**
     * 教学班名称
     */
    private String teachingClassName;

    /**
     * 包含的行政班级(包含班级UUID和课程性质[必修/选修])，JSON格式
     */
    private String administrativeClasses;

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
