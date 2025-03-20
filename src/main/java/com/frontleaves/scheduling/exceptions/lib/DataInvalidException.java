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

package com.frontleaves.scheduling.exceptions.lib;

import org.jetbrains.annotations.NotNull;

/**
 * 数据无效异常
 * <p>
 * 该异常用于在数据无效时抛出，主要用于区分于 BusinessException，用于表格输出避免被 Transactional 捕获。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public class DataInvalidException extends RuntimeException {
    private final TypeEnum typeEnum;

    public DataInvalidException(@NotNull TypeEnum typeEnum) {
        super(typeEnum.reason);
        this.typeEnum = typeEnum;
    }

    /**
     * 获取异常原因
     *
     * @return 异常原因
     */
    public String getReason() {
        return typeEnum.reason;
    }

    /**
     * 获取异常类型
     *
     * @return 异常类型
     */
    public String getType() {
        return typeEnum.type;
    }

    /**
     * 异常类型枚举
     */
    public enum TypeEnum {
        GENDER_ERROR("Gender", "性别填写错误"),
        NAME_EMPTY_ERROR("Name", "姓名不能为空"),
        STUDENT_ID_EMPTY_ERROR("Student", "学号不能为空");

        private final String type;
        private final String reason;

        TypeEnum(String type, String reason) {
            this.type = type;
            this.reason = reason;
        }
    }
}
