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

package com.frontleaves.scheduling.utils;

import cn.hutool.core.bean.copier.CopyOptions;
import lombok.extern.slf4j.Slf4j;

/**
 * 项目选项工具类
 * <p>
 * 该类提供了与项目配置相关的静态方法，主要用于创建和修改 {@code CopyOptions} 实例。
 * 通过这些方法可以方便地设置字段值编辑器等选项，以满足特定的项目需求。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
public class ProjectOption {

    private ProjectOption() {
        log.error("ProjectOption 不能被实例化");
    }

    /**
     * 将空白字符串替换为 null
     * <p>
     * 该方法返回一个 {@code CopyOptions} 实例，其中定义了一个字段值编辑器。
     * 该编辑器会检查每个字段的值，如果字段值是一个空白字符串（仅包含空格、制表符或换行符），则将其替换为 {@code null}。
     * 对于非字符串类型的字段值或非空白字符串，其值保持不变。
     *
     * @return 返回配置好的 {@code CopyOptions} 实例
     */
    public static CopyOptions replaceBlankToNull() {
        return CopyOptions.create()
                .setFieldValueEditor((fieldName, fieldValue) -> {
                    if (fieldValue instanceof String str && str.isBlank()) {
                        return null;
                    }
                    return fieldValue;
                });
    }
}
