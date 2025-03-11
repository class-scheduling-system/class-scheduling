package com.frontleaves.scheduling.models.entity.multiple;

import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class UserAndTeacherDO {
    private TeacherDO teacher;
    private UserDO user;
}

