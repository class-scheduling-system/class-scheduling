package com.frontleaves.scheduling.models.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 后台用户信息数据传输对象
 * @author FLASHLACK
 */
@Data
@Accessors(chain = true)
public class BackProfileDTO {
    private String name;
    private String email;
    private String phone;
}
