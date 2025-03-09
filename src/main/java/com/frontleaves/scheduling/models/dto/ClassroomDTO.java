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
import com.frontleaves.scheduling.models.entity.ClassroomDO;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 教室数据传输对象
 * <p>
 * 该类是 {@code ClassroomDO} 实体类的 DTO，用于在数据传输过程中传递教室信息。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see ClassroomDO
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ClassroomDTO {

    /**
     * 教室主键
     */
    private String classroomUuid;

    /**
     * 教室编号
     */
    private String number;

    /**
     * 教室名称
     */
    private String name;

    /**
     * 校区主键
     */
    private String campusUuid;

    /**
     * 楼栋主键
     */
    private String buildingUuid;

    /**
     * 楼层
     */
    private String floor;

    /**
     * 教室容量
     */
    private Integer capacity;

    /**
     * 是否是考场
     */
    private Boolean examinationRoom;

    /**
     * 考场容量
     */
    @Nullable
    private Integer examinationRoomCapacity;

    /**
     * 是否是多媒体教室
     */
    private Boolean isMultimedia;

    /**
     * 是否有空调
     */
    private Boolean isAirConditioned;

    /**
     * 教室状态 0:禁用 1:启用
     */
    private Boolean status;

    /**
     * 教室描述
     */
    @Nullable
    private String description;

    /**
     * 管理部门
     */
    @Nullable
    private String managementDepartment;

    /**
     * 教室面积
     */
    private BigDecimal area;

    /**
     * 桌椅类型
     */
    @Nullable
    private String tablesChairsType;

    /**
     * 创建时间，时间戳以数字格式返回
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间，时间戳以数字格式返回
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}
