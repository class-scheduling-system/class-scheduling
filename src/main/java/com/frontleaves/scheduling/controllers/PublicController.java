package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.models.dto.SiteDTO;
import com.frontleaves.scheduling.services.PublicService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共控制器
 * <p>
 * 该控制器提供了对外的公共接口，主要用于获取站点的基本信息。通过调用 {@code PublicService} 接口的方法，从系统数据库中获取详细的站点信息，并将其封装到一个 {@code SiteDTO} 对象中返回。
 *
 * @see PublicService
 * @see SiteDTO
 *
 * @since v1.0.0
 * @version v1.0.0
 * @author fanfan187
 */
@RestController
@RequestMapping("/api/v1/web")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    /**
     * 获取站点信息
     * <p>
     * 该方法用于获取系统的站点基本信息。通过调用 {@code publicService.getSiteInfo()} 方法从系统数据库中获取详细的站点信息，并将其封装到一个 {@code SiteDTO} 对象中返回。
     * 返回的信息包括站点名称、标题、副标题、描述、关键词、图标URL、Logo URL、ICP备案号、ICP备案链接、公安备案号、公安备案链接、版权状态、开源许可证、联系邮箱、联系电话、办公地址、微博URL、微信公众号、所有者、创始人、上线日期和技术栈等。
     *
     * @return 包含站点详细信息的响应实体，其中数据部分为 {@code BaseResponse<SiteDTO>} 类型
     */
    @GetMapping("/info")
    public ResponseEntity<BaseResponse<SiteDTO>> getSiteInfo() {
        SiteDTO siteDTO = publicService.getSiteInfo();
        return ResultUtil.success("成功", siteDTO);
    }
}

