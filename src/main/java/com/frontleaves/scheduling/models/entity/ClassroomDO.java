package com.frontleaves.scheduling.models.entity;

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
