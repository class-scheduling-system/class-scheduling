package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;

/**
 * 校区数据传输对象
 * <p>
 * 该类用于传输校区相关的信息，包括校区的唯一标识符、名称、编码、描述、状态、地址以及创建和更新时间。
 * 通过此 DTO 可以方便地在不同层之间传递校区信息。
 * </p>
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author xiao_lfeng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CampusDTO {

    /**
     * 校区主键
     */
    private String campusUuid;

    /**
     * 校区名称
     */
    private String campusName;

    /**
     * 校区编码
     */
    private String campusCode;

    /**
     * 校区描述
     */
    private String campusDesc;

    /**
     * 校区状态 0:禁用 1:启用
     */
    private Boolean campusStatus;

    /**
     * 校区地址
     */
    private String campusAddress;

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
