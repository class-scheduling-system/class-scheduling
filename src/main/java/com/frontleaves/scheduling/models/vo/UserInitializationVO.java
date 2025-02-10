package com.frontleaves.scheduling.models.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 用户初始化VO
 * @author FLASHLACK
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserInitializationVO {
    @NotBlank(message = "用户名不能为空")
    private String name;

}
