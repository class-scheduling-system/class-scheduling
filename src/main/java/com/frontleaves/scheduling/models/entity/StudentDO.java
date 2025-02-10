package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * @author FLASHLACK
 */
@Data
@TableName(value = "cs_student")
@Accessors(chain = true)
public class StudentDO {
    /**
     * 学生主键，采用 UUID 自动生成
     */
    @TableId(value = "student_uuid", type = IdType.ASSIGN_UUID)
    private String studentUuid;
    /**
     * 学号
     */
    @TableField(value = "id")
    private String id;
    /**
     * 姓名
     */
    @TableField(value = "name")
    private String name;
    /**
     * 性别 0:女 1:男
     */
    @TableField(value ="gender")
    private Integer gender;
    /**
     * 年级
     */
    @TableField(value = "grade")
    private String grade;
    /**
     * 学院
     */
    @TableField(value = "department")
    private String department;
    /**
     * 专业
     */
    @TableField(value = "major")
    private String major;
    /**
     * 班级
     */
    @TableField(value = "class")
    private String clazz;
    /**
     * 对应用户主键
     */
    @TableField(value = "user_uuid")
    private String userUuid;
    /**
     * 创建时间
     */
    @TableField(value = "created_at")
    private Timestamp createdAt;
    /**
     * 更新时间
     */
    @TableField(value = "updated_at")
    private Timestamp updatedAt;


}
