package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 教师DO
 * @author FLASHLACK
 */
@Data
@TableName("cs_teacher")
@Accessors(chain = true)
public class TeacherDO {
    /**
     * 教室主键，采用 UUID 自动生成
     */
    @TableId(value = "teacher_uuid", type = IdType.ASSIGN_UUID)
    private String teacherUuid;
    /**
     * 单位主键
     */
    @TableField(value = "unit_uuid")
    private String unitUuid;
    /**
     * 用户主键
     */
    @TableField(value = "user_uuid")
    private String userUuid;
    /**
     * 教师工号
     */
    @TableField(value = "id")
    private String id;
    /**
     * 教师姓名
     */
    @TableField(value = "name")
    private String name;
    /**
     * 教师英文名
     */
    @TableField(value = "english_name")
    private String englishName;
    /**
     * 教师名族
     */
    @TableField(value = "ethnic")
    private String ethnic;
    /**
     * 教师性别 0：女 1：男
     */
    @TableField(value = "sex")
    private Integer sex;
    /**
     * 教师电话
     */
    @TableField(value = "phone")
    private String phone;
    /**
     * 教师邮箱
     */
    @TableField(value = "email")
    private String email;
    /**
     * 教师职称
     */
    @TableField(value = "job_title")
    private String jobTitle;
    /**
     * 教师描述
     */
    @TableField(value = "`desc`")
    private String desc;
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
