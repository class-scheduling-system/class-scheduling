package com.frontleaves.scheduling.models.entity.multiple;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

@Data
@TableName("cs_building")
@NoArgsConstructor
@Accessors(chain = true)
public class TeacherAndUserDO {
    private TeacherDO teacher;
    private UserDO user;
}

