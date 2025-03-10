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

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * Teacher 数据传输对象
 * <p>
 * 用于在不同层之间传输教师基本信息。该 DTO 包含教师主键、单位主键、用户主键、教师工号、
 * 教师姓名、教师英文名、教师民族、教师性别、教师电话、教师邮箱、教师职称、教师描述、
 * 创建时间及更新时间等字段。
 * </p>
 *
 * 注意：
 * - 由于 SQL 中字段名 `desc` 可能与部分关键字冲突，此处仍采用 desc 命名，但在使用时请注意避免歧义。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TeacherDTO {

    /**
     * 教师主键
     */
    private String teacherUuid;

    /**
     * 单位主键
     */
    private String unitUuid;

    /**
     * 用户主键
     */
    private String userUuid;

    /**
     * 教师工号
     */
    private String id;

    /**
     * 教师姓名
     */
    private String name;

    /**
     * 教师英文名
     */
    private String englishName;

    /**
     * 教师民族
     */
    private String ethnic;

    /**
     * 教师性别（0：女，1：男）
     */
    private Integer sex;

    /**
     * 教师电话
     */
    private String phone;

    /**
     * 教师邮箱
     */
    private String email;

    /**
     * 教师职称
     */
    private String jobTitle;

    /**
     * 教师描述
     */
    private String desc;

    /**
     * 教师状态（0：禁用，1：启用，2：未激活）
     */
    private Short status;

    /**
     * 创建时间（单位：毫秒时间戳）
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间（单位：毫秒时间戳）
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}
