package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.models.dto.SiteDTO;
import com.frontleaves.scheduling.services.PublicService;
import com.xlf.utility.BaseResponse;
import com.xlf.utility.ResultUtil;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共控制器
 * <p>
 * 该类包含获取系统基本信息的接口;
 * 用于输出系统的基本信息。
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@RestController
@RequestMapping("/api/v1/web")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    /**
     * 获取站点基础信息接口
     * <p>
     * 该接口用于获取站点基本信息
     *
     * @return 包含站点数据的响应
     */
    @GetMapping("/info")
    @Transactional
    public @NotNull ResponseEntity<BaseResponse<SiteDTO>> getSiteInfo() {
        SiteDTO siteDTO = publicService.getSiteInfo();
        return ResultUtil.success("成功", siteDTO);
    }
}

