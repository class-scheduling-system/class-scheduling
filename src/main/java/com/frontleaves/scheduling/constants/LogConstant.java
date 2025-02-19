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

package com.frontleaves.scheduling.constants;

import lombok.extern.slf4j.Slf4j;

/**
 * 提供日志记录中使用的常量字符串，旨在统一和标准化日志输出的前缀。
 * 包含服务、控制器、数据访问层、工具类、异常、不同日志级别等标识，
 * 便于日志分析和系统维护时快速识别日志来源与重要性。
 *
 * @version v1.0.0
 * @since v1.0.0
 * @author xiao_lfeng
 */
@Slf4j
public class LogConstant {
    public static final String SERVICE = "[SERV] ";
    public static final String CONTROLLER = "[CTRL] ";
    public static final String DAO = "[DAO] ";
    public static final String UTIL = "[UTIL] ";
    public static final String EXCEPTION = "[EXCP] ";
    public static final String INFO = "[INFO] ";
    public static final String WARN = "[WARN] ";
    public static final String ERROR = "[ERRO] ";
    public static final String DEBUG = "[DEBG] ";
    public static final String TRACE = "[TRAC] ";

    private LogConstant() {
        log.error("LogConstant 不能被实例化");
    }
}
