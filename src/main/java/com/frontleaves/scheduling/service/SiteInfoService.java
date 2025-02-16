package com.frontleaves.scheduling.service;

import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.models.dto.SiteDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author fanfan187
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class SiteInfoService {

    private final SystemDAO systemDAO;

    /**
     * 获取站点信息
     * 使用 Redis 缓存，首先从缓存中获取数据
     *
     * @return SiteDTO 站点信息
     */

    public SiteDTO getSiteInfo() {
        return new SiteDTO()
                .setName(systemDAO.getSystemInfo("web_name"))
                .setTitle(systemDAO.getSystemInfo("web_title"))
                .setSubTitle(systemDAO.getSystemInfo("web_subtitle"))
                .setDescription(systemDAO.getSystemInfo("web_description"))
                .setKeywords(systemDAO.getSystemInfo("web_keywords"))
                .setIconUrl(systemDAO.getSystemInfo("web_icon_url"))
                .setLogoUrl(systemDAO.getSystemInfo("web_logo"))
                .setIcpNumber(systemDAO.getSystemInfo("web_icp"))
                .setIcpLink(systemDAO.getSystemInfo("web_icp_link"))
                .setSecurityRecord(systemDAO.getSystemInfo("web_security_record"))
                .setSecurityRecordLink(systemDAO.getSystemInfo("web_security_record_link"))
                .setCopyrightStatus(systemDAO.getSystemInfo("web_copyright_status"))
                .setOpenSourceLicense(systemDAO.getSystemInfo("web_open_source_license"))
                .setContactEmail(systemDAO.getSystemInfo("web_contact_email"))
                .setContactPhone(systemDAO.getSystemInfo("web_contact_phone"))
                .setOfficeAddress(systemDAO.getSystemInfo("web_office_address"))
                .setWeiboUrl(systemDAO.getSystemInfo("web_weibo_url"))
                .setWechatOfficeAccount(systemDAO.getSystemInfo("web_wechat_office_account"))
                .setOwner(systemDAO.getSystemInfo("web_owner"))
                .setFounder(systemDAO.getSystemInfo("web_founder"))
                .setLaunchDate(systemDAO.getSystemInfo("web_launch_date"))
                .setTechnologyStack(systemDAO.getSystemInfo("web_technology_stack"));
    }
}
