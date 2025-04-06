package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 教学班实体
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
@TableName("cs_teaching_class")
public class TeachingClassDO {
    /**
     * 教学班主键
     */
    @TableId(value = "teaching_class_uuid", type = IdType.ASSIGN_UUID)
    private String teachingClassUuid;

    /**
     * 学期主键
     */
    private String semesterUuid;

    /**
     * 课程主键
     */
    private String courseUuid;

    /**
     * 教学班编号
     */
    private String teachingClassCode;

    /**
     * 教学班名称
     */
    private String teachingClassName;

    /**
     * 包含的行政班级(包含班级UUID)
     */
    private String administrativeClasses;

    /**
     * 如果是必修课（区分必修和选修，选修不包含行政班）则该字段为true，否则为false
     */
    private Boolean isAdministrative;

    /**
     * 班级规模
     */
    private Integer classSize;

    /**
     * 实际学生人数
     */
    private Integer actualStudentCount;

    /**
     * 开课院系
     */
    private String courseDepartmentUuid;

    /**
     * 教学班描述
     */
    private String description;

    /**
     * 是否启用(0:禁用,1:启用)
     */
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