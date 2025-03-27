package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 课程类型数据传输对象
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CourseTypeDTO {
    /**
     * 课程类型主键
     */
    private String courseTypeUuid;

    /**
     * 课程类型名称
     */
    private String name;

    /**
     * 课程类型描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    private Timestamp updatedAt;
}
