package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CourseLiteDTO {
    /**
     * 课程库UUID
     */
    private String courseLibraryUuid;

    /**
     * 课程名称
     */
    private String name;

    /**
     * 课程类别
     */
    private String category;

    /**
     * 课程属性
     */
    private String property;

    /**
     * 课程类型
     */
    private String type;

    /**
     * 课程性质
     */
    private String nature;

    /**
     * 所属院系
     */
    private String department;

}
