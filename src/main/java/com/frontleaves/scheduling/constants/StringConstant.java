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
 * 提供字符串常量的类，包括系统错误信息、Redis相关常量及通用常量等。
 * <p>
 * 本类中的常量旨在作为应用内共享的不可变值，避免硬编码字符串，提高代码可维护性。
 * 所有常量均为静态最终成员，且构造方法私有化，防止外部实例化。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
public class StringConstant {

    public static final String TOKEN_ATTRIBUTION_ERROR = "令牌归属错误";

    private StringConstant() {
        log.error("StringConstant 不能被实例化");
    }

    /**
     * Redis 常量
     */
    public static class Redis {
        public static final String SYSTEM = "system:";
        public static final String TOKEN = "token:";
        public static final String ROLE_UUID = "role:uuid:";
        public static final String ROLE_NAME = "user:name:";

        private Redis() {
            log.error("Redis 不能被实例化");
        }
    }

    /**
     * 常量
     */
    public static class Common {

        public static final String USER_AGENT = "User-Agent";

        private Common() {
            log.error("Common 不能被实例化");
        }

        /**
         * 驼峰命名常量
         */
        public static class Hump {
            public static final String USER_UUID = "userUuid";
            public static final String TOKEN = "token";
            public static final String REFRESH_TOKEN = "refreshToken";
            public static final String EXPIRE_TIME = "expireTime";
            public static final String REFRESH_EXPIRE_TIME = "refreshExpireTime";
            public static final String CREATED_AT = "createdAt";
            public static final String UPDATED_AT = "updatedAt";

            private Hump() {
                log.error("Hump 不能被实例化");
            }
        }
    }
}
