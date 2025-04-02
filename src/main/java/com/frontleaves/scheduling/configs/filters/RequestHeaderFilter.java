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

package com.frontleaves.scheduling.configs.filters;

import com.frontleaves.scheduling.constants.StringConstant;
import com.google.gson.Gson;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.ResultUtil;
import com.xlf.utility.exception.library.RequestHeaderNotMatchException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;


/**
 * 请求头过滤器
 * <p>
 * 该类用于定义请求头过滤器;
 * 用于检查请求头是否符合规范。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
public class RequestHeaderFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            @NotNull HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        String[] noMatchUrls = {"/favicon.ico", "/ws"};
        boolean isMatch = false;
        for (String noMatchUrl : noMatchUrls) {
            if (request.getRequestURI().startsWith(noMatchUrl)) {
                isMatch = true;
                break;
            }
        }
        try {
            if (!isMatch) {
                // 检查是否是空 Referer
                if (request.getHeader("Referer") == null || request.getHeader("Referer").isEmpty()) {
                    log.warn("[FILT] 缺少 Referer 头，操作 「IP:{} | Agent:{}」", request.getRemoteAddr(), request.getHeader(StringConstant.Common.USER_AGENT));
                    throw new RequestHeaderNotMatchException("请求缺少 Referer 头");
                }
                // 检查请求头是否包含正确的 Content-Type
                if (request.getContentType() == null) {
                    log.warn("[FILT] Content-Type 为空，操作 「IP:{} | Agent:{}」", request.getRemoteAddr(), request.getHeader(StringConstant.Common.USER_AGENT));
                    throw new RequestHeaderNotMatchException("Content-Type 不能为空");
                }
                // 检查请求头是否包含正确的 User-Agent
                if (request.getHeader(StringConstant.Common.USER_AGENT) == null || request.getHeader(StringConstant.Common.USER_AGENT).isEmpty()) {
                    log.warn("[FILT] 缺少 User-Agent，操作 「IP:{}」", request.getRemoteAddr());
                    throw new RequestHeaderNotMatchException("请求头中缺少 User-Agent");
                }
            }
            filterChain.doFilter(request, response);
        } catch (RequestHeaderNotMatchException e) {
            Gson gson = new Gson();
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    gson.toJson(
                            ResultUtil.error(ErrorCode.METHOD_NOT_ALLOWED, e.getMessage(), null).getBody()
                    )
            );
        }
    }
}

