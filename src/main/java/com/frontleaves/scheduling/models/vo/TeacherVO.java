package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TeacherVO {
    /**
     * 单位主键
     */
    @NotBlank(message = "单位主键不能为空")
    private String unitUuid;
    /**
     * 用户主键
     */
    @NotBlank(message = "用户主键不能为空")
    private String userUuid;
    /**
     * 教师工号
     */
    @NotBlank(message = "教师工号不能为空")
    private String id;
    /**
     * 教师姓名
     */
    @NotBlank(message = "教师姓名不能为空")
    private String name;
    /**
     * 教师英文名
     */
    @NotBlank(message = "教师英文名不能为空")
    private String englishName;
    /**
     * 教师名族
     */
    @NotBlank(message = "教师民族不能为空")
    private String ethnic;
    /**
     * 教师性别 0：女 1：男
     */
    @NotNull(message = "教师性别不能为空")
    private Integer sex;
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

}
