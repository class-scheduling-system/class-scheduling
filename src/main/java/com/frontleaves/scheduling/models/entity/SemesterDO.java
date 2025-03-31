package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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

    @TableId(value = "semester_uuid", type = IdType.ASSIGN_UUID)
    private String semesterUuid;

    @TableField("name")
    private String name;

    @TableField("code")
    private String code;

    @TableField("start_date")
    private Timestamp startDate;

    @TableField("end_date")
    private Timestamp endDate;

    @TableField("is_enabled")
    private Boolean isEnabled;

    @TableField("created_at")
    private Timestamp createdAt;

    @TableField("updated_at")
    private Timestamp updatedAt;
}