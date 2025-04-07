package com.frontleaves.scheduling.models.dto.lite;

import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 学生轻量级DTO
 * <p>
 * 用于返回学生简要信息列表，不包含详细信息
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StudentLiteDTO {
    
    /**
     * 学生UUID
     */
    private String studentUuid;
    
    /**
     * 学生学号
     */
    private String id;
    
    /**
     * 学生姓名
     */
    private String name;
    
    /**
     * 学生性别（true-男，false-女）
     */
    private Boolean gender;
    
    /**
     * 所属部门（院系）UUID
     */
    private String department;
    
    /**
     * 所属部门（院系）名称
     */
    @Nullable
    private String departmentName;
    
    /**
     * 行政班UUID
     */
    private String clazz;
    
    /**
     * 行政班名称
     */
    @Nullable
    private String className;
} 