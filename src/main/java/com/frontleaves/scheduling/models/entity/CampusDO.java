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

package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 校区表实体类
 * <p>
 * 对应数据库表：`cs_campus`
 * 该类用于表示校区的相关信息，包括校区的唯一标识、名称、编码、描述、状态、地址以及创建和更新时间。
 * </p>
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author xiao_lfeng
 */
@Data
@TableName("cs_campus")
@Accessors(chain = true)
public class CampusDO {

    /**
     * 校区主键
     */
    @TableId(value = "campus_uuid", type = IdType.INPUT)
    private String campusUuid;

    /**
     * 校区名称
     */
    @TableField("campus_name")
    private String campusName;

    /**
     * 校区编码
     */
    @TableField("campus_code")
    private String campusCode;

    /**
     * 校区描述
     */
    @TableField("campus_desc")
    private String campusDesc;

    /**
     * 校区状态 0:禁用 1:启用
     */
    @TableField("campus_status")
    private Integer campusStatus;

    /**
     * 校区地址
     */
    @TableField("campus_address")
    private String campusAddress;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
