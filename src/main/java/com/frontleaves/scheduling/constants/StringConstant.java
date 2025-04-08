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
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String TEACHER_NOT_EXIST = "教师不存在";
    public static final String USER_NOT_EXIST = "用户不存在";
    public static final String TEACHER_TYPE_UUID_NOT_EMPTY = "教师类型UUID不能为空";
    public static final String TEACHER_TYPE_UUID_FORMAT_ERROR = "教师类型UUID格式不正确";
    public static final String TEACHER_UUID_FORMAT_ERROR = "教师UUID格式不正确";
    public static final String TEACHER_PREFERENCES_NOT_EXIST = "教师偏好不存在";
    public static final String TEACHER_PREFERENCES_UUID_FORMAT_ERROR = "教师偏好UUID格式不正确";
    public static final String TEACHER_PREFERENCES_SAVE_FAILED = "教师偏好保存失败";
    public static final String TEACHER_PREFERENCES_UPDATE_FAILED = "教师偏好更新失败";
    public static final String TEACHER_PREFERENCES_DELETE_FAILED = "教师偏好删除失败";
    public static final String TEACHER_PREFERENCES_UUID_ILLEGAL = "教师课程偏好主键不合法";
    public static final String SEMESTER_UUID_FORMAT_ERROR = "学期UUID格式不正确";
    public static final String SEMESTER_NOT_EXIST = "学期不存在";
    public static final String DEPARTMENT_DELETE_FAILED = "删除部门失败";
    public static final String DEPARTMENT_NOT_EXIST = "部门不存在";
    public static final String DEPARTMENT_UUID_NOT_EMPTY = "部门主键不能为空";
    public static final String DEPARTMENT_UUID_FORMAT_ERROR = "部门UUID格式不正确";
    public static final String EMAIL_VERIFICATION_TOKEN_EXPIRED = "邮箱验证令牌已过期";
    public static final String EMAIL_VERIFICATION_TOKEN_INVALID = "邮箱验证令牌无效";
    public static final String EMAIL_VERIFICATION_TOKEN_MISMATCH = "邮箱地址与验证令牌不匹配";
    public static final String UNIT_TYPE_NOT_FOUND = "未找到该单位办别";
    public static final String UNIT_CATEGORY_NOT_FOUND = "未找到该单位类别";
    public static final String STUDENT_NOT_EXIST = "学生不存在";
    public static final String STUDENT_SAVE_FAILED = "学生信息保存失败";
    public static final String REQUEST_LOG_CACHE = "request:log:cache";
    public static final String OPERATE_SUCCESS = "操作成功";
    public static final String TABLES_CHAIRS_NOT_EXIST = "桌椅类型不存在";
    public static final String TABLES_CHAIRS_NAME_EXISTS = "桌椅类型名称已存在";
    public static final String TABLES_CHAIRS_UUID_FORMAT_ERROR = "桌椅类型UUID格式不正确";
    public static final String COURSE_UUID_FORMAT_ERROR = " 课程UUID格式不正确";


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
        public static final String STUDENT_LIST = "stu:list";
        public static final String STUDENT_LIST_BY_DEPARTMENT_AND_CLASS = "stu:list:department:class:";
        public static final String STUDENT_USER_UUID = "stu:user:uuid:";
        public static final String TEACHER_ID = "tea:id:";
        public static final String TEACHER_UUID = "tea:uuid:";
        public static final String TEACHER_USER_UUID = "tea:user:uuid:";
        public static final String TEACHER_PREFERENCES_UUID = "tea:pref:uuid:";
        public static final String TEACHER_PREFERENCES_LIST = "tea:pref:list:";
        public static final String TEACHER_PREFERENCES_PAGE = "tea:pref:page:";
        public static final String TEACHER_PREFERENCES_TEACHER = "tea:pref:teacher:";
        public static final String TEACHER_PREFERENCES_SEMESTER = "tea:pref:semester:";
        public static final String BUILDING_LIST = "building:list";
        public static final String BUILDING_UUID = "building:uuid:";
        public static final String BUILDING_NAME = "building:name:";
        public static final String BUILDING_CAMPUS = "building:campus:";
        public static final String BUILDING_KEY_LIST = "building:key:list:";
        public static final String CAMPUS_UUID = "campus:uuid:";
        public static final String CAMPUS_NAME = "campus:name:";
        public static final String CAMPUS_CODE = "campus:code:";
        public static final String MAJOR_UUID = "major:uuid:";
        public static final String CAMPUS_LIST = "campus:list";
        public static final String CAMPUS_FULL_LIST = "campus:full:list";
        public static final String CAMPUS_PAGE_OF_LIST = "campus:page:";
        public static final String DEPARTMENT_UUID = "department:uuid:";
        public static final String DEPARTMENT_LIST = "department:list";
        public static final String COURSE_LIBRARY_UUID = "course:library:uuid";
        public static final String COURSE_CATEGORY_UUID = "course:category:uuid:";
        public static final String COURSE_NATURE_UUID = "course:nature:uuid:";
        public static final String COURSE_PROPERTY_UUID = "course:property:uuid:";
        public static final String COURSE_TYPE_UUID = "course:type:uuid:";
        public static final String UNIT_CATEGORY_UUID = "unit:cate:uuid:";
        public static final String UNIT_CATEGORY_NAME = "unit:cate:name:";
        public static final String UNIT_TYPE_UUID = "unit:type:uuid:";
        public static final String CLASSROOM_TAG_LIST = "classroom:tag:list";
        public static final String COURSE_LIBRARY_LITE_LIST = "course:library:lite:list";
        public static final String CLASSROOM_TAG_UUID = "classroom:tag:uuid:";
        public static final String CLASSROOM_TYPE_LIST = "classroom:type:list";
        public static final String CLASSROOM_TYPE_UUID = "classroom:type:uuid:";
        public static final String CLASSROOM_PAGE = "classroom:page:";
        public static final String CLASSROOM_UUID = "classroom:uuid:";
        public static final String CLASSROOM_NUMBER = "classroom:number:";
        public static final String CLASSROOM_STATUS = "classroom:status:";
        public static final String TABLES_CHAIRS_UUID = "tc:type:uuid:";
        public static final String TABLES_CHAIRS_NAME = "tc:type:name:";
        public static final String TABLES_CHAIRS_LIST = "tc:type:list";
        public static final String TABLES_CHAIRS_PAGE = "tc:type:page:";
        public static final String ROLE_LIST = "role:list";
        public static final String ADMINISTRATIVE_CLASS_UUID = "administrative:class:uuid:";
        public static final String GRADE_UUID = "grade:uuid:";
        public static final String MAJOR_LIST = "major:list";
        public static final String GRADE_LIST = "grade:list";
        public static final String ADMINISTRATIVE_CLASS_LIST = "ait:class:list";
        public static final String ADMINISTRATIVE_CLASS_LIST_BY_DEPARTMENT = "ait:class:list:department:";
        public static final String ADMINISTRATIVE_CLASS_LIST_BY_MAJOR = "ait:class:list:major:";
        public static final String ADMINISTRATIVE_CLASS_MAPPING_BY_CALZZ = "ait:class:mapping:class:";
        public static final String ACADEMIC_AFFAIRS_PERMISSION_USER_UUID = "aca:affairs:per:user:uuid:";
        public static final String ACADEMIC_AFFAIRS_PERMISSION_UUID = "aca:affairs:per:uuid:";
        public static final String MAJOR_LIST_BY_DEPARTMENT_UUID = "major:list:department:uuid:";
        public static final String DEPARTMENT_NAME = "department:name:";
        public static final String GRADE_NAME = "grade:name:";
        public static final String TEACHER_TYPE_UUID = "teacher:type:uuid:";
        public static final String TEACHER_TYPE_LIST = "teacher:type:list";
        public static final String TEACHER_TYPE_PAGE = "teacher:type:page:";
        public static final String TEACHER_LITE_LIST = "tea:lite:list:";
        public static final String EMAIL_TOKEN = "email:token:";
        public static final String EMAIL_TO_TOKEN = "email:to:token:";
        public static final String UNIT_TYPE_NAME = "unit:type:name:";
        public static final String UNIT_TYPE_LIST = "unit:type:list";
        public static final String UNIT_TYPE_PAGE_OF_LIST = "unit:type:page:";
        public static final String UNIT_CATEGORY_LIST = "unit:cate:list";
        public static final String UNIT_CATEGORY_PAGE = "unit:cate:page:";
        public static final String CLASS_ASSIGNMENT_UUID = "class:assignment:uuid:";
        public static final String CLASS_ASSIGNMENT_LIST = "class:assignment:list:";
        public static final String CLASS_ASSIGNMENT_PAGE = "class:assignment:page:";
        public static final String SEMESTER_UUID = "semester:uuid:";
        public static final String SEMESTER_ENABLED = "semester:enabled:";
        public static final String SEMESTER_LIST = "semester:list";
        public static final String SEMESTER_PAGE = "semester:page:";
        public static final String TEACHER_COURSE_QUALIFICATION_UUID = "teacher:course:qualification:uuid:";
        public static final String TEACHER_COURSE_QUALIFICATION_COURSE_LIBRARY_UUID = "teacher:course:qualification:course:library:uuid:";
        public static final String COURSE_TYPE_LIST = "course:type:list";
        public static final String CLASSROOM_BUILDING = "classroom:building:";
        public static final String SCHEDULE_LESSONS = "schedule:lessons:";
        public static final String COURSE_LIBRARY_LIST = "course:library:list";
        public static final String COURSE_LIBRARY_PAGE_DEPARTMENT = "course:library:list:department:";
        public static final String SCHEDULE_EXECUTE_STATUS = "schedule:execute:status:";
        public static final String SCHEDULE_EXECUTE_PROGRESS = "schedule:execute:progress:";
        public static final String CREDIT_HOUR_TYPE_UUID = "credit:hour:type:uuid:";
        public static final String TEACHING_CLASS_LIST_SEMESTER = "teaching:class:list:semester:";
        public static final String CLASS_ASSIGNMENT_LIST_SEMESTER = "class:assignment:list:semester:";
        public static final String COURSE_CATEGORY_LIST = "course:category:list";
        public static final String COURSE_PROPERTY_LIST = "course:property:list";
        public static final String COURSE_NATURE_LIST = "course:nature:list";
        public static final String COURSE_LIBRARY_ID = "course:library:id:";
        public static final String CLASSROOM_LIST = "classroom:list";
        public static final String TEACHING_CLASS_UUID = "teaching:class:uuid:";
        public static final String CREDIT_HOUR_TYPE_LIST = "credit:hour:type:list";
        public static final String SCHEDULE_RESULT = "schedule:result:";
        public static final String SCHEDULING_TASK = "scheduling:task:";

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
            public static final String UNKNOWN = "unknown";

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
        public static final String PASSWORD_REGULAR_EXPRESSION_ABLE_EMPTY = "(|^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{6,}$)";
        public static final String UUID_NO_DASH_REGULAR_EXPRESSION = "^[0-9a-f]{32}$";
        public static final String UUID_NO_DASH_REGULAR_EXPRESSION_ABLE_EMPTY = "(|^[0-9a-f]{32}$)";
        public static final String UUID_REGULAR_EXPRESSION = "^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$";
        public static final String EMAIL_REGULAR_EXPRESSION_ABLE_EMPTY ="^(|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6})$";
        public static final String USER_NAME_REGULAR_EXPRESSION_ABLE_EMPTY ="(|^[0-9A-Za-z_-]{4,32}$)";
        public static final String PHONE_REGULAR_EXPRESSION_ABLE_EMPTY ="(|^1[3456789]\\d{9}$)";
        public static final String STUDENT_ID_REGULAR_EXPRESSION = "^[a-zA-Z0-9]{4,32}$";
        public static final String STUDENT_NAME_REGULAR_EXPRESSION = "^[一-龥A-Za-z]{2,20}$";
        public static final String SERIAL_NUMBER_REGULAR_EXPRESSION = "^[A-Za-z0-9]{2,64}$";
        public static final String FIXED_PHONE_REGULAR_EXPRESSION_ABLE_EMPTY = "(|^\\d{3}-\\d{8}|\\d{4}-\\d{7}$)";
        private Regular() {
            log.error("Regular 不能被实例化");
        }
    }

    /**
     * 忽略常量类
     */
    public static class Ignore {
        public static final String ASSIGNED_TEACHING_BUILDING = "assignedTeachingBuilding";

        private Ignore() {
            log.error("Ignore 不能被实例化");
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

    /**
     * 行政班级常量
     */
    public static class AdministrativeClass {
        public static final String ADMINISTRATIVE_CLASS_NOT_FOUND = "行政班级不存在";
        public static final String ADMINISTRATIVE_CLASS_UUID_FORMAT_ERROR = "行政班级UUID格式无效";
        public static final String ADMINISTRATIVE_CLASS_CODE_EXISTS = "班级编号已存在";
        public static final String ADMINISTRATIVE_CLASS_NAME_EXISTS = "班级名称已存在";

        private AdministrativeClass() {
            log.error("AdministrativeClass 不能被实例化");
        }
    }

    /**
     * 错误消息相关常量
     */
    public static class ErrorMessage {
        public static final String SEMESTER_UUID_FORMAT_ERROR = "学期UUID格式错误";
        public static final String CLASS_ASSIGNMENT_UUID_FORMAT_ERROR = "排课分配UUID格式错误";
        public static final String CLASS_ASSIGNMENT_NOT_FOUND = "排课分配不存在";
        public static final String COURSE_UUID_FORMAT_ERROR = "课程UUID格式错误";
        public static final String TEACHER_UUID_FORMAT_ERROR = "教师UUID格式错误";
        public static final String PAGE_SIZE_TOO_LARGE = "单页查询不允许超过 200";
        public static final String TABLES_CHAIRS_TYPE_UUID_FORMAT_ERROR = "桌椅类型UUID格式错误";
        public static final String TABLES_CHAIRS_TYPE_NOT_FOUND = "桌椅类型不存在";
        public static final String TABLES_CHAIRS_TYPE_NAME_EXISTS = "桌椅类型名称已存在";

        private ErrorMessage() {
            log.error("ErrorMessage 不能被实例化");
        }
    }
}
