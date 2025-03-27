package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Date;
import java.sql.Timestamp;


/**
 * 学期DTO
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class SemesterDTO {
    /**
     * 学期主键
     */
    private String semesterUuid;

    /**
     * 学期名称
     */
    private String name;

    /**
     * 学期描述
     */
    private String description;

    /**
     * 学期开始日期
     */
    private Date startDate;

    /**
     * 学期结束日期
     */
    private Date endDate;

    /**
     * 是否当前学期
     */
    private Boolean isCurrent;

    /**
     * 是否启用
     */
    private Boolean isEnabled;

    /**
     * 创建时间
     */

    private Timestamp createdAt;

    /**
     * 更新时间
     */

    private Timestamp updatedAt;

}
