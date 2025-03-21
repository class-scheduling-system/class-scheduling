package com.frontleaves.scheduling.models.dto;

import lombok.Data;

/**
 * 忘记密码响应DTO
 * @author FLASHLACK
 */
@Data
public class ForgetPasswordResponseDTO {
    private Long tokenExpireTime;
}
