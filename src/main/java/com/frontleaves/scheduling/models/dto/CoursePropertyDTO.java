package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontleaves.scheduling.models.entity.CoursePropertyDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 课程属性数据传输对象
 * <p>
 * 该类是 {@code CoursePropertyDO} 实体类的 DTO，用于在数据传输过程中传递课程属性信息。
 * </p>
 *
 * @author Claude AI
 * @version v1.0.0
 * @see CoursePropertyDO
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CoursePropertyDTO {

    /**
     * 课程属性主键
     */
    private String coursePropertyUuid;

    /**
     * 课程属性名称
     */
    private String name;

    /**
     * 课程属性描述
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