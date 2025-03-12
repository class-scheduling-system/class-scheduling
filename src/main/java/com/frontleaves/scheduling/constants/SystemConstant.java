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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * 系统常量
 * <p>
 * 该类用于定义系统常量;
 * 该类使用 {@link Getter} 和 {@link Setter} 注解标记;
 *
 * @version v1.0.0
 * @since v1.0.0
 * @author xiao_lfeng
 */
@Slf4j
public class SystemConstant {
    // 以下是系统常量
    @Getter
    private static final String SYSTEM_NAME = "ClassScheduling";
    // 以下是系统常量
    @Getter
    @Setter
    private static String isInitMode;
    // 以下是角色常量
    @Getter
    @Setter
    private static String roleAdmin;
    @Getter
    @Setter
    private static String roleTeacher;
    @Getter
    @Setter
    private static String roleStudent;
    @Getter
    @Setter
    private static String roleLeader;
    @Getter
    @Setter
    private static String roleAcademic;
    
    // 以下是教师类型常量
    /**
     * 兼职教师
     */
    @Getter
    @Setter
    private static String teacherTypePartTime;
    /**
     * 助教
     */
    @Getter
    @Setter
    private static String teacherTypeAssistant;
    /**
     * 实习教师
     */
    @Getter
    @Setter
    private static String teacherTypeIntern;
    /**
     * 副教授
     */
    @Getter
    @Setter
    private static String teacherTypeAssociateProf;
    /**
     * 其他
     */
    @Getter
    @Setter
    private static String teacherTypeOther;
    /**
     * 讲师
     */
    @Getter
    @Setter
    private static String teacherTypeLecturer;
    /**
     * 教授
     */
    @Getter
    @Setter
    private static String teacherTypeProfessor;
    /**
     * 全职教师
     */
    @Getter
    @Setter
    private static String teacherTypeFullTime;
    /**
     * 班主任
     */
    @Getter
    @Setter
    private static String teacherTypeCounselor;
    @Getter
    private static final String SYSTEM_VERSION = "v1.0.0";
    @Getter
    private static final String SYSTEM_AUTHOR = "锋楪技术团队";

    private SystemConstant() {
        log.error("SystemConstant 不能被实例化");
    }
}
