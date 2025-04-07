package com.frontleaves.scheduling.models.dto.lite;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 专业简要信息DTO
 * <p>
 * 该DTO用于存储专业的简要信息,包括专业UUID、专业名称和所属院系名称
 * </p>
 * 
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MajorLiteDTO {
    /**
     * 专业UUID
     */
    private String majorUuid;
    /**
     * 专业名称
     */
    private String majorName;
    /**
     * 专业代码
     */
    private String majorCode;
    /**
     * 专业状态
     */
    private Integer majorStatus;
    /**
     * 学制(年)
     */
    private Integer educationYears;
    /**
     * 培养层次
     */
    private String trainingLevel;
}
