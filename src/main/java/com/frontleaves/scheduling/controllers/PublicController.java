package com.frontleaves.scheduling.controllers;

import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.models.entity.SiteDO;
import com.frontleaves.scheduling.models.vo.ResponseWrapperVO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
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

    //注入SystemDAO，用于获取系统信息
    private final SystemDAO systemDAO;

    @Cacheable(value = "siteInfo", key = "'web_info'")
    @GetMapping("/info")
    public ResponseWrapperVO<SiteDO> getSiteInfo() {
        try {
            // 从 Redis 或数据库中获取系统信息
            String name = systemDAO.getSystemInfo("name");
            String description = systemDAO.getSystemInfo("description");
            String icp = systemDAO.getSystemInfo("icp");
            String securityRecord = systemDAO.getSystemInfo("security-record");

            if (name == null || description == null || icp == null || securityRecord == null) {
                return new ResponseWrapperVO<>("Error", 500, "站点信息获取失败", null);
            }

            SiteDO siteDO = new SiteDO(name, description, icp, securityRecord);
            return new ResponseWrapperVO<>("Success", 200, "站点信息获取成功", siteDO);
        } catch (Exception e) {
            // 错误处理，保持响应结构一致
            return new ResponseWrapperVO<>("Error", 500, "站点信息获取失败", null);
        }
    }
}

