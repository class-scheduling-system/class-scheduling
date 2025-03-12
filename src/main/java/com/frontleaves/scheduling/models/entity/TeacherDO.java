package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 教师信息实体类
 * <p>
 * 对应数据库表：`cs_teacher`
 * 本类用于封装教师的详细信息，主键为 teacher_uuid，采用 UUID 自动生成。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@TableName("cs_teacher")
@NoArgsConstructor
@Accessors(chain = true)
public class TeacherDO {
    /**
     * 教师主键，采用 UUID 自动生成
     */
    @TableId(value = "teacher_uuid", type = IdType.ASSIGN_UUID)
    private String teacherUuid;

    /**
     * 单位主键
     */
    private String unitUuid;

    /**
     * 用户主键
     */
    private String userUuid;

    /**
     * 教师工号
     */
    private String id;

    /**
     * 教师姓名
     */
    private String name;

    /**
     * 教师英文名
     */
    private String englishName;

    /**
     * 教师民族
     */
    private String ethnic;

    /**
     * 教师性别 0：女 1：男
     */
    private Boolean sex;

    /**
     * 教师类型
     */
    private String type;

    /**
     * 教师电话
     */
    private String phone;

    /**
     * 教师邮箱
     */
    private String email;

    /**
     * 教师职称
     */
    private String jobTitle;

    /**
     * 教师描述
     */
    @TableField(value = "`desc`")
    private String desc;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
