package com.frontleaves.scheduling.models.dto;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BuildingImportDTO {

    private String campusName; // 校区名称

    private String buildingName; // 教学楼名称

    @TableField(value = "is_status")
    private Boolean status; // 状态（"启用"或"停用"）
}
