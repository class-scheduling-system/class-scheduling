package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 学期数据传输对象
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
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
     * 开始日期
     */
    private Timestamp startDate;

    /**
     * 结束日期
     */
    private Timestamp endDate;

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
