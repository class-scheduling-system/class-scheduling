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
 * 本软件是"按原样"提供的，没有任何形式的明示或暗示的保证，包括但不限于
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
    public static final String TOKEN_NOT_EXIST = "令牌不存在";
    public static final String TOKEN_EXPIRED = "令牌已过期";
    public static final String REFRESH_TOKEN_ERROR = "刷新令牌错误";
    public static final String REFRESH_TOKEN_EXPIRED = "刷新令牌已过期";
    public static final String DATABASE_OPERATION_FAILED = "数据库操作失败";
    public static final String REDIS_OPERATION_FAILED = "缓存操作失败";
    public static final String USER_DATA_NOT_EXIST = "用户数据不存在";
    public static final String DEPARTMENT_DELETE_FAILED = "删除部门失败";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String TEACHER_NOT_EXIST = "教师不存在";
    public static final String USER_NOT_EXIST = "用户不存在";
    public static final String DEPARTMENT_NOT_EXIST = "部门不存在";

    private StringConstant() {
        log.error("StringConstant 不能被实例化");
    }

    /**
     * Redis 常量
     */
    public static class Redis {
        public static final String SYSTEM = "system:";
        public static final String TOKEN = "token:";
        public static final String PERMISSION = "permission:";
        public static final String PERMISSION_PAGE = "permission:page:";
        public static final String PERMISSION_LIST = "permission:list";
        public static final String ROLE_UUID = "role:uuid:";
        public static final String ROLE_NAME = "role:name:";
        public static final String USER_UUID = "user:uuid:";
        public static final String USER_NAME = "user:name:";
        public static final String USER_MAIL = "user:mail:";
        public static final String USER_TEL = "user:tel:";
        public static final String STUDENT_ID = "stu:id:";
        public static final String STUDENT_UUID = "stu:uuid:";
        public static final String STUDENT_USER_UUID = "stu:user:uuid:";
        public static final String TEACHER_ID = "tea:id:";
        public static final String TEACHER_UUID = "tea:uuid:";
        public static final String TEACHER_USER_UUID = "tea:user:uuid:";
        public static final String BUILDING_LIST = "building:list";
        public static final String BUILDING_UUID = "building:uuid:";
        public static final String BUILDING_NAME = "building:name:";
        public static final String BUILDING_CAMPUS = "building:campus:";
        public static final String CAMPUS_UUID = "campus:uuid:";
        public static final String CAMPUS_NAME = "campus:name:";
        public static final String CAMPUS_CODE = "campus:code:";
        public static final String MAJOR_UUID = "major:uuid:";
        public static final String CAMPUS_LIST = "campus:list";
        public static final String CAMPUS_PAGE_OF_LIST = "campus:page:";
        public static final String DEPARTMENT_UUID = "department:uuid:";
        public static final String DEPARTMENT_LIST = "department:list";
        public static final String UNIT_CATEGORY_UUID = "unit:cate:uuid:";
        public static final String UNIT_CATEGORY_NAME = "unit:cate:name:";
        public static final String UNIT_TYPE_UUID = "unit:type:uuid:";
        public static final String CLASSROOM_TAG_LIST = "classroom:tag:list";
        public static final String CLASSROOM_TAG_UUID = "classroom:tag:uuid:";
        public static final String CLASSROOM_TYPE_LIST = "classroom:type:list";
        public static final String CLASSROOM_TYPE_UUID = "classroom:type:uuid:";
        public static final String CLASSROOM_LIST = "classroom:list:";
        public static final String CLASSROOM_UUID = "classroom:uuid:";
        public static final String CLASSROOM_NUMBER = "classroom:number:";
        public static final String TABLES_CHAIRS_UUID = "tc:uuid:";
        public static final String ROLE_LIST = "role:list";
        public static final String BUILDING_KEY_LIST = "building:key:list";
        public static final String TEACHER_TYPE_UUID = "teacher:type:UUID";

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

    /**
     * 正则表达式常量类。
     */
    public static class Regular {
        public static final String EMAIL_REGULAR_EXPRESSION ="^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
        public static final String PHONE_REGULAR_EXPRESSION ="^1[3456789]\\d{9}$";
        public static final String USER_NAME_REGULAR_EXPRESSION ="^[0-9A-Za-z_-]{4,32}$";
        public static final String PASSWORD_REGULAR_EXPRESSION = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{6,}$";
        public static final String UUID_NO_DASH_REGULAR_EXPRESSION = "^[a-f0-9]{8}[a-f0-9]{4}4[a-f0-9]{3}[89ab][a-f0-9]{3}[a-f0-9]{12}$";
        public static final String UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY = "(|^[a-f0-9]{8}[a-f0-9]{4}4[a-f0-9]{3}[89ab][a-f0-9]{3}[a-f0-9]{12}$)";
        public static final String UUID_REGULAR_EXPRESSION = "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$";
        public static final String EMAIL_REGULAR_EXPRESSION_ABLE_EMPTY ="^(|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})$";
        public static final String USER_NAME_REGULAR_EXPRESSION_ABLE_EMPTY ="(|^[0-9A-Za-z_-]{4,32}$)";
        public static final String PHONE_REGULAR_EXPRESSION_ABLE_EMPTY ="(|^1[3456789]\\d{9}$)";
        public static final String PASSWORD_REGULAR_EXPRESSION_ABLE_EMPTY = "(|^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{6,}$)";
        public static final String STUDENT_ID_REGULAR_EXPRESSION = "^[a-zA-Z0-9]{4,32}$";
        public static final String STUDENT_NAME_REGULAR_EXPRESSION = "^[\u4E00-\u9FA5A-Za-z]{2,20}$";
        public static final String SERIAL_NUMBER_REGULAR_EXPRESSION = "^[A-Za-z0-9]{2,64}$";
        public static final String FIXED_PHONE_REGULAR_EXPRESSION_ABLE_EMPTY = "(|^\\d{3}-\\d{8}|\\d{4}-\\d{7}$)";
        private Regular() {
            log.error("Regular 不能被实例化");
        }
    }

    /**
     * 专业常量
     */
    public static class Major {
        public static final String MAJOR_NOT_FOUND = "专业不存在";
        public static final String MAJOR_UUID_FORMAT_ERROR = "majorUuid格式无效";

        private Major() {
            log.error("不能被实例化");
        }
    }
}
