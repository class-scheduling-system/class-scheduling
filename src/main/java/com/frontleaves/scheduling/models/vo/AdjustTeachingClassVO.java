package com.frontleaves.scheduling.models.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 调整教学班信息
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdjustTeachingClassVO {
    /**
     * 必填调整的教学班UUID
     */
    private String teachingClassUuid;
    /**
     * 可选，调整的教学班编号
     */
    private String teachingClassCode;
    /**
     * 可选，调整的教学班名称
     */
    private String teachingClassName;
    /**
     * 可选，调整的行政班级UUID
     */
    private List<String> administrativeClassUuids;
    /**
     * 可选，教学班人数
     */
    private Integer actualStudentCount;
    /**
     * 可选，教学班描述
     */
    private String description;
}
