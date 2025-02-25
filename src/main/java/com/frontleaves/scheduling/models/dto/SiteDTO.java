package com.frontleaves.scheduling.models.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 站点信息传输对象
 * <p>
 * 该类用于封装站点的基本信息，包括名称、标题、描述等。它主要用于在不同层之间传递站点信息，
 * 例如从前端到后端或从数据库到前端。通过使用该类，可以方便地管理和传递站点的详细信息。
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SiteDTO {

    /**
     * 站点名称
     * <p>
     * 该变量用于存储站点的名称，是站点的基本信息之一。
     * </p>
     */
    @NotNull
    private String name;
    /**
     * 站点标题
     * <p>
     * 该字段用于存储站点的标题信息。站点标题通常出现在浏览器标签页中，是用户对网站的第一印象。
     * 它应当简洁明了地描述网站的主要内容或功能。
     * </p>
     */
    @NotNull
    private String title;
    /**
     * 站点副标题
     * <p>
     * 该变量用于存储站点的副标题，通常用于补充主标题 {@code title} 的信息。
     * 副标题可以提供更详细的描述或额外的信息，帮助用户更好地理解站点的主题和内容。
     * </p>
     */
    @NotNull
    private String subTitle;
    /**
     * 站点描述
     * <p>
     * 该变量用于存储站点的详细描述信息。站点描述通常用于搜索引擎优化（SEO），帮助用户更好地理解站点的内容和功能。
     * </p>
     */
    @NotNull
    private String description;
    /**
     * 站点关键词
     * <p>
     * 该变量用于存储站点的关键词，通常用于搜索引擎优化（SEO）。关键词是描述站点内容或主题的一系列词语，
     * 帮助搜索引擎更好地理解和索引站点。这些关键词应与站点的实际内容相关，以提高搜索结果的相关性和排名。
     * </p>
     */
    @NotNull
    private String keywords;
    /**
     * 站点图标 URL
     * <p>
     * 该变量用于存储站点图标的 URL 地址。站点图标通常是一个小图标，用于在浏览器标签页、书签栏或其他位置显示，
     * 以便用户能够快速识别和区分不同的网站。常见的站点图标格式包括 ICO、PNG 和 SVG。
     * </p>
     */
    @NotNull
    private String iconUrl;
    /**
     * 站点 Logo URL
     * <p>
     * 该变量用于存储站点的 Logo 图片的 URL。Logo 是站点的品牌标识，通常出现在网站的头部或导航栏中，
     * 有助于用户识别和记忆站点。URL 应指向一个有效的图片资源。
     * </p>
     */
    private String logoUrl;

    /**
     * ICP 备案号
     * <p>
     * 该变量用于存储站点的 ICP 备案号。ICP 备案号是中国大陆对互联网信息服务进行管理的一种方式，
     * 所有在中国大陆提供互联网信息服务的网站都需要进行 ICP 备案，并在网站上展示备案号。
     * 备案号通常由一串数字和字母组成，用户可以通过备案号查询网站的相关信息。
     * </p>
     */
    private String icpNumber;
    /**
     * ICP 备案链接
     * <p>
     * 该变量用于存储站点的 ICP 备案链接。ICP 备案是中国大陆对互联网信息服务进行管理的一种制度，
     * 任何在中国大陆提供互联网信息服务的网站都需要进行备案，并获得一个唯一的备案号。
     * 该链接通常指向工信部的备案查询页面，用户可以通过该链接验证站点的备案信息是否真实有效。
     * </p>
     */
    private String icpLink;
    /**
     * 公安备案号
     * <p>
     * 该变量用于存储站点的公安备案号。公安备案号是中国大陆对互联网信息服务进行管理的一种方式，
     * 所有在中国大陆提供互联网信息服务的网站都需要进行公安备案，并在网站上展示备案号。
     */
    private String securityRecord;
    /**
     * 公安备案链接
     * <p>
     * 该变量用于存储站点的公安备案链接。公安备案是中国大陆对互联网信息服务进行管理的一种制度，
     * 任何在中国大陆提供互联网信息服务的网站都需要进行备案，并获得一个唯一的备案号。
     */
    private String securityRecordLink;
    /**
     * 版权状态
     * <p>
     * 该变量用于存储站点的版权状态信息。版权状态描述了站点内容的版权归属和使用许可情况，
     * 包括但不限于著作权声明、许可协议（如 Creative Commons 许可）、是否允许转载等。
     * 这对于保护原创内容和明确使用权限非常重要。
     * </p>
     */
    private String copyrightStatus;
    /**
     * 开源许可证
     * <p>
     * 该变量用于存储站点所使用的开源许可证信息。开源许可证是软件分发时附带的一种法律文本，它规定了软件的使用、复制、修改和分发等权利和义务。
     * 通过明确指定开源许可证，可以帮助用户了解站点所使用的技术栈中的开源组件的许可情况，确保符合相关法律法规要求。
     * </p>
     */
    private String openSourceLicense;

    /**
     * 联系邮箱
     * <p>
     * 该变量用于存储站点的联系邮箱地址。联系邮箱是用户与站点管理员或运营团队进行沟通的重要方式之一。
     * 用户可以通过此邮箱发送咨询、反馈或其他相关问题。确保该邮箱的有效性和及时响应对于提升用户体验和维护良好的客户关系至关重要。
     * </p>
     */
    private String contactEmail;
    /**
     * 联系电话
     * <p>
     * 该变量用于存储站点的联系电话。联系电话是用户与站点管理员或运营团队进行沟通的重要方式之一。
     * 通过提供有效的联系电话，用户可以在需要时直接联系到站点的相关人员，以便解决疑问、反馈问题或获取帮助。
     * </p>
     */
    private String contactPhone;
    /**
     * 办公地址
     * <p>
     * 该变量用于存储站点的办公地址。办公地址是站点运营团队或公司的实际办公地点，通常用于提供给用户联系或访问的实际位置信息。
     * 它可以帮助用户在需要时找到站点的实体办公地点，进行面对面的沟通或其他相关事务处理。
     * </p>
     */
    private String officeAddress;
    /**
     * 微博链接
     * <p>
     * 该变量用于存储站点的官方微博链接。通过此链接，用户可以访问到与 {@code SiteDTO} 对象相关的官方微博页面。
     * </p>
     */
    private String weiboUrl;
    /**
     * 企业微信公众号账号
     * <p>
     * 该字段用于存储企业的官方微信公众号账号。此信息通常用于对外展示或提供联系方式，方便用户通过微信平台关注企业官方账号，获取更多信息或进行互动。
     * </p>
     */
    private String wechatOfficeAccount;

    /**
     * 站点所有者
     * <p>
     * 该字段用于存储站点的所有者信息。通常包含所有者的名称或标识，以便在需要时能够识别和联系到站点的所有者。
     * </p>
     */
    private String owner;
    /**
     * 创始人
     * <p>
     * 该变量用于存储站点的创始人信息。创始人是指创建并拥有该站点的主要负责人。
     * </p>
     */
    private String founder;
    /**
     * 站点启动日期
     * <p>
     * 该字段表示站点正式上线的日期。通常用于记录和展示站点的成立时间。
     * </p>
     */
    private String launchDate;
    /**
     * 技术栈
     * <p>
     * 该字段用于存储和表示网站所使用的技术栈信息。技术栈通常包括后端语言、前端框架、数据库、中间件等。
     * 通过此字段可以了解网站的技术实现细节，便于维护和技术交流。
     * </p>
     */
    private String technologyStack;

}
















