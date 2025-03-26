package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 学期DO
 *
 * @author FLASHLACK
 */
@Data
@TableName("cs_semester")
public class SemesterDO {
    /**
     * 学期主键
     */
    @TableId(value = "semester_uuid", type = IdType.ASSIGN_UUID)
    private String semesterUuid;

    /**
     * 学期名称
     */
    @TableField("name")
    private String name;

    /**
     * 学期描述
     */
    @TableField("description")
    private String description;

    /**
     * 学期开始日期
     */
    @TableField("start_date")
    private Date startDate;

    /**
     * 学期结束日期
     */
    @TableField("end_date")
    private Date endDate;

    /**
     * 是否当前学期
     */
    @TableField("is_current")
    private Boolean isCurrent;

    /**
     * 是否启用
     */
    @TableField("is_enabled")
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