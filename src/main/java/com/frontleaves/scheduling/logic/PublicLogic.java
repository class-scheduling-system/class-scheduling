/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * Author: 锋楪技术团队 (https://www.frontleaves.com)
 *
 * 本文件包含锋楪技术团队项目的源代码，项目的所有源代码均遵循 MIT 开源许可证协议。
 * --------------------------------------------------------------------------------
 * 许可证声明：
 *
 * 版权所有 (c) 2022-2025 锋楪技术团队。保留所有权利。
 *
 * 本软件是“按原样”提供的，没有任何形式的明示或暗示的保证，包括但不限于
 * 对适销性、特定用途的适用性和非侵权性的暗示保证。在任何情况下，
 * 作者或版权持有人均不承担因软件或软件的使用或其他交易而产生的、
 * 由此引起的或以任何方式与此软件有关的任何索赔、损害或其他责任。
 *
 * 使用本软件即表示您了解此声明并同意其条款。
 *
 * 有关 MIT 许可证的更多信息，请查看项目根目录下的 LICENSE 文件或访问：
 * https://opensource.org/licenses/MIT
 * --------------------------------------------------------------------------------
 * 免责声明：
 *
 * 使用本软件的风险由用户自担。作者或版权持有人在法律允许的最大范围内，
 * 对因使用本软件内容而导致的任何直接或间接的损失不承担任何责任。
 * --------------------------------------------------------------------------------
 */

package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.models.dto.SiteDTO;
import com.frontleaves.scheduling.services.PublicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 提供公共逻辑处理的类，实现了 {@code PublicService} 接口。
 * <p>
 * 该类主要用于处理与系统相关的公共逻辑，例如获取网站的基本信息。通过依赖注入的方式，
 * 使用 {@code SystemDAO} 来从数据库中获取所需的数据，并将其封装到相应的数据传输对象（DTO）中返回。
 * <p>
 * 该类使用了 Spring 的 {@code @Service} 注解来标识其为一个服务层组件，并且使用了 Lombok 的
 * {@code @Slf4j} 和 {@code @RequiredArgsConstructor} 注解来简化日志记录和构造函数的编写。
 *
 * @author fanfan187
 * @version v1.0.0
 * @see PublicService
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PublicLogic implements PublicService {

    private final SystemDAO systemDAO;

    /**
     * 获取网站的基本信息。
     * <p>
     * 该方法从系统数据库中获取网站的详细信息，并将其封装到一个 {@code SiteDTO} 对象中返回。
     * 返回的信息包括网站名称、标题、副标题、描述、关键词、图标URL、Logo URL、ICP备案号、
     * ICP备案链接、公安备案号、公安备案链接、版权状态、开源许可证、联系邮箱、联系电话、办公地址、
     * 微博URL、微信公众号、所有者、创始人、上线日期和技术栈等。
     *
     * @return 包含网站详细信息的 {@code SiteDTO} 对象
     */
    @Override
    public SiteDTO getSiteInfo() {
        SiteDTO siteDTO = new SiteDTO();
        Map<String, String> systemInfoList = systemDAO.getSystemInfoList();
        siteDTO.setName(systemInfoList.get("web_name"))
                .setTitle(systemInfoList.get("web_title"))
                .setSubTitle(systemInfoList.get("web_subtitle"))
                .setDescription(systemInfoList.get("web_description"))
                .setKeywords(systemInfoList.get("web_keywords"))
                .setIconUrl(systemInfoList.get("web_icon_url"))
                .setLogoUrl(systemInfoList.get("web_logo"))
                .setIcpNumber(systemInfoList.get("web_icp"))
                .setIcpLink(systemInfoList.get("web_icp_link"))
                .setSecurityRecord(systemInfoList.get("web_security_record"))
                .setSecurityRecordLink(systemInfoList.get("web_security_record_link"))
                .setCopyrightStatus(systemInfoList.get("web_copyright_status"))
                .setOpenSourceLicense(systemInfoList.get("web_open_source_license"))
                .setContactEmail(systemInfoList.get("web_contact_email"))
                .setContactPhone(systemInfoList.get("web_contact_phone"))
                .setOfficeAddress(systemInfoList.get("web_office_address"))
                .setWeiboUrl(systemInfoList.get("web_weibo_url"))
                .setWechatOfficeAccount(systemInfoList.get("web_wechat_office_account"))
                .setOwner(systemInfoList.get("web_owner"))
                .setFounder(systemInfoList.get("web_founder"))
                .setLaunchDate(systemInfoList.get("web_launch_date"))
                .setTechnologyStack(systemInfoList.get("web_technology_stack"));
        return siteDTO;
    }
}
