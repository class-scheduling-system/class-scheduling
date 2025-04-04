package com.frontleaves.scheduling.models.dto.base;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 学时类型数据传输对象
 *
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class CreditHourTypeDTO {
    /**
     * 学时类型主键
     */
    private String creditHourTypeUuid;
    /**
     * 学时类型名称
     */
    private String name;
    /**
     * 学时类型描述
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