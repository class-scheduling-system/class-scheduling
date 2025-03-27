package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 教室和教室类型DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class ClassroomAndTypeDTO {
    private ClassroomDTO classroom;
    private ClassroomTypeDTO classroomType;
}
