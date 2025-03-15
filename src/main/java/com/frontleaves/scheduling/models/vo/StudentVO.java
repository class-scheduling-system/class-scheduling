package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.Accessors;

/**
 * 学生视图对象
 * <p>
 * 用户仅需提提供学号、姓名、性别、班级和毕业状态
 * 用户通过班级信息即可定位到对应的年级、学院和专业
 * userUUid 由系统在登录或后续操作时自动生成,不由前端传递
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StudentVO {
    @NotBlank(message = "学号不能为空")
    private String id;

    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotNull(message = "性别不能为空")
    private Boolean gender;

    @NotBlank(message = "班级不能为空")
    private String clazz;

    @NotNull(message = "毕业状态不能为空")
    private Boolean graduated;
}
