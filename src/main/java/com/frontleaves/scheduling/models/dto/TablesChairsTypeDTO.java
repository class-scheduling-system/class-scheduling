package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 桌椅类型数据传输对象
 * <p>
 * 用于传输和表示桌椅类型的相关信息。该类包含了桌椅类型的主键、名称、描述、图片（以 Base64 字符串形式存储）、创建时间和更新时间等属性。
 * </p>
 *
 * @version v1.0.0
 * @author  xiao_lfeng
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class TablesChairsTypeDTO {

    /**
     * 桌椅类型主键
     */
    private String tablesChairsTypeUuid;

    /**
     * 桌椅类型名称
     */
    private String name;

    /**
     * 桌椅类型描述
     */
    private String description;

    /**
     * 桌椅类型图片，存储为 Base64 字符串
     */
    private String base64Img;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Timestamp updatedAt;
}
