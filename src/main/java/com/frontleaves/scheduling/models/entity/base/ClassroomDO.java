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
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 教室实体类
 * <p>
 * 该类用于表示教室信息，对应数据库表：`cs_classroom`。通过 MyBatis-Plus 注解映射到数据库表中的字段。
 * 包含了教室的基本属性如编号、名称、容量等，以及一些附加属性如是否是考场、是否有空调等。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("cs_classroom")
@Accessors(chain = true)
public class ClassroomDO {

    /**
     * 教室主键
     */
    @TableId(value = "classroom_uuid", type = IdType.ASSIGN_UUID)
    private String classroomUuid;

    /**
     * 教室编号
     */
    @TableField("number")
    private String number;

    /**
     * 教室名称
     */
    @TableField("name")
    private String name;

    /**
     * 校区主键
     */
    @TableField("campus_uuid")
    private String campusUuid;

    /**
     * 楼栋主键
     */
    @TableField("building_uuid")
    private String buildingUuid;

    /**
     * 楼层
     */
    @TableField("floor")
    private String floor;

    /**
     * 教室类型
     */
    @TableField("type")
    private String type;

    /**
     * 教室标签，json格式存储
     */
    @TableField("tag")
    private String tag;

    /**
     * 教室容量
     */
    @TableField("capacity")
    private Integer capacity;

    /**
     * 是否是考场
     */
    @TableField("examination_room")
    private Boolean examinationRoom;

    /**
     * 考场容量
     */
    @TableField("examination_room_capacity")
    private Integer examinationRoomCapacity;

    /**
     * 是否是多媒体教室
     */
    @TableField("is_multimedia")
    private Boolean isMultimedia;

    /**
     * 是否有空调
     */
    @TableField("is_air_conditioned")
    private Boolean isAirConditioned;

    /**
     * 教室状态 0:禁用 1:启用
     */
    @TableField("status")
    private Boolean status;

    /**
     * 教室描述
     */
    @TableField("description")
    private String description;

    /**
     * 管理部门
     */
    @TableField("management_department")
    private String managementDepartment;

    /**
     * 教室面积
     */
    @TableField("area")
    private BigDecimal area;

    /**
     * 桌椅类型
     */
    @TableField("tables_chairs_type")
    private String tablesChairsType;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private Timestamp updatedAt;
}
