package com.frontleaves.scheduling.models.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.frontleaves.scheduling.models.dto.base.RoleDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.sql.Timestamp;
import java.util.List;

/**
 * 用户数据传输对象
 * <p>
 * 用于返回用户数据相关信息，传输的是用户的基本信息;
 * 不包含老师、学生、管理员等信息。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class UserDTO {
    /**
     * 用户主键，采用 UUID 自动生成
     */
    private String userUuid;

    /**
     * 用户名
     */
    private String name;

    /**
     * 用户邮箱
     */
    private String email;

    /**
     * 用户手机号
     */
    private String phone;

    /**
     * 用户状态 0: 禁用 1: 启用
     */
    private Byte status;

    /**
     * 用户是否被封禁 0: 未封禁 1: 已封禁
     */
    private Boolean ban;

    /**
     * 角色名
     */
    private RoleDTO role;

    /**
     * 用户权限，JSON 格式
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
