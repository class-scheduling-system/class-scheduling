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
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.Map;

/**
 * 排课冲突实体类
 * <p>
 * 对应数据库表：`cs_scheduling_conflict`
 * 本类用于封装排课冲突的详细信息，主键为 conflict_uuid，采用 UUID 自动生成。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName(value = "cs_scheduling_conflict", autoResultMap = true)
@NoArgsConstructor
@Accessors(chain = true)
public class SchedulingConflictDO {
    /**
     * 冲突主键，采用 UUID 自动生成
     */
    @TableId(value = "conflict_uuid", type = IdType.ASSIGN_UUID)
    private String conflictUuid;

    /**
     * 学期主键
     */
    private String semesterUuid;

    /**
     * 第一个排课主键
     */
    private String firstAssignmentUuid;

    /**
     * 第二个排课主键
     */
    private String secondAssignmentUuid;

    /**
     * 冲突类型: 1-教师冲突 2-教室冲突 3-班级冲突 4-其他冲突
     */
    private Integer conflictType;

    /**
     * 冲突时间
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> conflictTime;

    /**
     * 冲突描述
     */
    private String description;

    /**
     * 解决状态: 0-未解决 1-已解决 2-忽略
     */
    private Integer resolutionStatus;

    /**
     * 解决方法: 1-调整第一个课程 2-调整第二个课程 3-同时调整 4-其他
     */
    private Integer resolutionMethod;

    /**
     * 解决备注
     */
    private String resolutionNotes;

    /**
     * 解决人
     */
    private String resolvedBy;

    /**
     * 解决时间
     */
    private Timestamp resolvedAt;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
