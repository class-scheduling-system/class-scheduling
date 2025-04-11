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
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 排课分配实体类
 * <p>
 * 对应数据库表：`cs_class_assignment`
 * 本类用于封装排课分配的详细信息，主键为 class_assignment_uuid，采用 UUID 自动生成。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("cs_class_assignment")
@NoArgsConstructor
@Accessors(chain = true)
public class ClassAssignmentDO {

    /**
     * 排课主键，采用 UUID 自动生成
     */
    @TableId(value = "class_assignment_uuid", type = IdType.ASSIGN_UUID)
    private String classAssignmentUuid;

    /**
     * 学期主键
     */
    private String semesterUuid;

    /**
     * 课程主键
     */
    private String courseUuid;

    /**
     * 教师主键
     */
    private String teacherUuid;

    /**
     * 校区主键
     */
    private String campusUuid;

    /**
     * 教学楼主键
     */
    private String buildingUuid;

    /**
     * 教室主键
     */
    private String classroomUuid;

    /**
     * 教学班主键
     */
    private String teachingClassUuid;

    /**
     * 课程归属
     */
    private String courseOwnership;

    /**
     * 学时类型
     */
    private String creditHourType;

    /**
     * 教学学时（指教师实际授课的学时）
     */
    private BigDecimal teachingHours;

    /**
     * 排课学时（指课程安排的学时）
     */
    private BigDecimal scheduledHours;

    /**
     * 总需学时
     */
    private BigDecimal totalHours;

    /**
     * 排课优先级
     */
    private Integer schedulingPriority;
    /**
     * 教学校区
     */
    private String teachingCampus;

    /**
     * 上课时间，JSON格式
     */
    private String classTime;

    /**
     * 连堂节数
     */
    private Integer consecutiveSessions;

    /**
     * 教室类型
     */
    private String classroomType;

    /**
     * 指定时间，JSON格式
     */
    private String specifiedTime;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
