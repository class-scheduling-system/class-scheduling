package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontleaves.scheduling.models.entity.base.CourseCategoryDO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 课程类别数据传输对象
 * <p>
 * 该类是 {@code CourseCategoryDO} 实体类的 DTO，用于在数据传输过程中传递课程类别信息。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @see CourseCategoryDO
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CourseCategoryDTO {

    /**
     * 课程类别主键
     */
    private String courseCategoryUuid;

    /**
     * 课程类别名称
     */
    private String name;

    /**
     * 课程类别描述
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
