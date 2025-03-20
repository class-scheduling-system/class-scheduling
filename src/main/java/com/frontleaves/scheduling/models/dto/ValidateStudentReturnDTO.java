package com.frontleaves.scheduling.models.dto;

import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.frontleaves.scheduling.models.entity.GradeDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 验证学生返回数据传输对象
 *
 * @author FLASHLAKC
 */
@Data
@Accessors(chain = true)
public class ValidateStudentReturnDTO {
    private GradeDO gradeDO;
    private MajorDO majorDO;
    private AdministrativeClassDO administrativeClassDO;
}
