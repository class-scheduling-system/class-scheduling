package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.List;

/**
 * 角色数据传输对象
 * <p>
 * 用于返回角色数据相关信息，传输的是角色的基本信息;
 * 包含角色名、角色状态、角色权限、创建时间、更新时间等信息。
 * </p>
 *
 * @version v1.0.0
 * @since v1.0.0
 * @author xiao_lfeng
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RoleDTO {

    /**
     * 角色主键，采用 UUID 自动生成
     */
    private String roleUuid;

    /**
     * 角色名
     */
    private String roleName;

    /**
     * 角色状态 0: 禁用 1: 启用
     */
    private Boolean roleStatus;

    /**
     * 角色权限，JSON 格式
     */
    private List<String> permission;

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
