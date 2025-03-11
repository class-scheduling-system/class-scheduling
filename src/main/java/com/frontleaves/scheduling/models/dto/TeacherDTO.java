package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * Teacher 数据传输对象
 * <p>
 * 用于在不同层之间传输教师基本信息。该 DTO 包含教师主键、单位主键、用户主键、教师工号、
 * 教师姓名、教师英文名、教师民族、教师性别、教师电话、教师邮箱、教师职称、教师描述、
 * 创建时间及更新时间等字段。
 * </p>
 *
 * 注意：
 * - 由于 SQL 中字段名 `desc` 可能与部分关键字冲突，此处仍采用 desc 命名，但在使用时请注意避免歧义。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TeacherDTO {

    /**
     * 教师主键
     */
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
     * 教师性别（0：女，1：男）
     */
    private Boolean sex;

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
    private String desc;

    /**
     * 教师状态（0：禁用，1：启用）
     */
    private Boolean status;

    /**
     * 创建时间（单位：毫秒时间戳）
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间（单位：毫秒时间戳）
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}
