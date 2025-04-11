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

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 表实体
 * <p>
 * 该类用于定义表实体，对应 `information_schema.tables` 视图。
 * 字段映射规则：数据库字段使用大写蛇形命名，Java 中使用驼峰命名。
 * 通过 {@link TableField} 注解实现字段映射。
 * </p>
 *
 * @author 锋楪技术团队
 * @version v1.0.0
 * @since 2025-01-24
 */
@Data
@TableName(value = "TABLES", schema = "INFORMATION_SCHEMA")
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class TableDO {

    /**
     * 表目录。
     * 对应数据库字段：TABLE_CATALOG
     */
    @TableField("TABLE_CATALOG")
    private String tableCatalog;

    /**
     * 数据库名（模式）。
     * 对应数据库字段：TABLE_SCHEMA
     */
    @TableField("TABLE_SCHEMA")
    private String tableSchema;

    /**
     * 表名。
     * 对应数据库字段：TABLE_NAME
     */
    @TableField("TABLE_NAME")
    private String tableName;

    /**
     * 表类型（如：BASE TABLE 或 VIEW）。
     * 对应数据库字段：TABLE_TYPE
     */
    @TableField("TABLE_TYPE")
    private String tableType;
}
