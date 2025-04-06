package com.frontleaves.scheduling.models.entity.multiple;

import com.frontleaves.scheduling.models.entity.base.StudentDO;
import com.frontleaves.scheduling.models.entity.base.UserDO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 封装学生和对应用户信息的 DO 对象
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class UserAndStudentDO {
    /**
     * 学生信息
     */
    private StudentDO student;

    /**
     * 对应用户信息,可能为null(未注册)
     */
    private UserDO user;

}
