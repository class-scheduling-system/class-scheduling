package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Date;
import java.sql.Timestamp;

/**
 * 学期表的 DO 对象
 * <p>
 * 该类是学期表的实体类，用于映射数据库中的学期表。
 * 包含了学期的基本信息，如学期名称、代码、开始和结束日期等。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("cs_semester")
public class SemesterDO {
    /**
     * 学期名称
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

    @TableField("created_at")
    private Timestamp createdAt;

    @TableField("updated_at")
    private Timestamp updatedAt;
}
