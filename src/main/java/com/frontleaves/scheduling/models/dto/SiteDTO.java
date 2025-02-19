package com.frontleaves.scheduling.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author fanfan187
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SiteDTO {

    /**
     * 站点基础信息
     */
    private String name;
    private String title;
    private String subTitle;
    private String description;
    private String keywords;
    private String iconUrl;
    private String logoUrl;

    /**
     * 站点备案与版权
     */
    private String icpNumber;
    private String icpLink;
    private String securityRecord;
    private String securityRecordLink;
    private String copyrightStatus;
    private String openSourceLicense;

    /**
     * 站点联系与社交
     */
    private String contactEmail;
    private String contactPhone;
    private String officeAddress;
    private String weiboUrl;
    private String wechatOfficeAccount;

    /**
     * 站点高级元数据
     */
    private String owner;
    private String founder;
    private String launchDate;
    private String technologyStack;

}
















