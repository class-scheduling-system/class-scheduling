package com.frontleaves.scheduling.models.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.*;

/**
 * @author: fanfan187
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SiteDO {

    //基础站点信息，后期可继续添加
    @TableField("web_name")
    private String name;

    @TableField("web_description")
    private String description;

    @TableField("web_icp")
    private String icp;

    @TableField("web_security_record")
    private String securityRecord;

}


