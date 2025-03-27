package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.sql.Timestamp;

/**
 * 课程类型实体类
 * 对应数据库表：cs_course_type
 *
 * @author FLASHLACK
 */
@Data // 自动生成 getter、setter、toString 等方法
@TableName("cs_course_type") // 指定数据库表名
public class CourseTypeDO {

    /**
     * 课程类型主键
     */
    @TableId(value = "course_type_uuid", type = IdType.ASSIGN_UUID) // 主键，使用 UUID 策略
    private String courseTypeUuid;

    /**
     * 课程类型名称
     */
    @TableField("name")
    private String name;

    /**
     * 课程类型描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}