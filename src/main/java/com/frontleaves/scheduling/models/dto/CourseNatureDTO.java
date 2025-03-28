package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontleaves.scheduling.models.entity.CourseNatureDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 课程性质数据传输对象
 * <p>
 * 该类是 {@code CourseNatureDO} 实体类的 DTO，用于在数据传输过程中传递课程性质信息。
 * </p>
 *
 * @author Claude AI
 * @version v1.0.0
 * @see CourseNatureDO
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CourseNatureDTO {

    /**
     * 课程性质主键
     */
    private String courseNatureUuid;

    /**
     * 课程性质名称
     */
    private String name;

    /**
     * 课程性质描述
     */
    private String description;

    /**
     * 创建时间，时间戳以数字格式返回
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间，时间戳以数字格式返回
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}