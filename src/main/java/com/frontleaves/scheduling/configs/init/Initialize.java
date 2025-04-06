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

package com.frontleaves.scheduling.configs.init;

import cn.hutool.core.date.DateUtil;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

/**
 * 初始化方法类
 * <p>
 * 该类用于在启动时执行，用于启动时检查数据表的完整性，以及对其他数据进行初始化操作;
 * 检查完成放行通过，否则将会禁止服务启动。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class Initialize {
    private final TableDAO tableDAO;
    private final SystemDAO systemDAO;
    private final RoleDAO roleDAO;
    private final RedissonClient redissonClient;
    private final UserDAO userDAO;
    private final TeacherTypeDAO teacherTypeDAO;
    private final  ClassroomTypeDAO classroomTypeDAO;

    private FunctionInit init;

    @PostConstruct
    public void init() {
        // 初始化准备算法
        init = new FunctionInit(tableDAO, systemDAO, roleDAO, redissonClient,teacherTypeDAO,classroomTypeDAO);

        // 初始化数据库完整性检查
        this.checkTable();
        this.initClearRedis();
        this.checkSystemTable();
        this.writeRoleInfo();
        this.writeTeacherType();
        this.withClassroomType();
        this.initTestUser();
    }

    @Bean
    @Order(2)
    public CommandLineRunner commandLineRunner() {
        return args -> {
            log.info("========== End of Initialization ==========");
            log.info("""
                    \s
                    \u001B[38;5;111m    ________                \u001B[32m   _____      __             __      ___           \s
                    \u001B[38;5;111m   / ____/ /___ ___________ \u001B[32m  / ___/_____/ /_  ___  ____/ /_  __/ (_)___  ____ _
                    \u001B[38;5;111m  / /   / / __ `/ ___/ ___/ \u001B[32m  \\__ \\/ ___/ __ \\/ _ \\/ __  / / / / / / __ \\/ __ `/
                    \u001B[38;5;111m / /___/ / /_/ (__  |__  )  \u001B[32m ___/ / /__/ / / /  __/ /_/ / /_/ / / / / / / /_/ /\s
                    \u001B[38;5;111m \\____/_/\\__,_/____/____/ \u001B[32m  /____/\\___/_/ /_/\\___/\\__,_/\\__,_/_/_/_/ /_/\\__, / \s
                    \t\t\t\u001B[33m::: {} :::\t\t\t\t ::: {} ::: \t\u001B[32m       /____/  \u001B[0m\s
                    """, SystemConstant.getSYSTEM_AUTHOR(), SystemConstant.getSYSTEM_VERSION()
            );
        };
    }

    /**
     * 检查数据表是否完整
     * <p>
     * 该方法用于检查数据表是否完整，如果数据表不完整则会禁止服务启动。
     */
    private void checkTable() {
        log.info("[INIT] 数据库检查开始");

        // 按照顺序检查数据库表
        init.checkDatabase("cs_system");
        init.checkDatabase("cs_permission");
        init.checkDatabase("cs_role");
        init.checkDatabase("cs_user");
        init.checkDatabase("cs_unit_type");
        init.checkDatabase("cs_unit_category");
        init.checkDatabase("cs_tables_chairs_type");
        init.checkDatabase("cs_semester");
        init.checkDatabase("cs_major");
        init.checkDatabase("cs_credit_hour_type");
        init.checkDatabase("cs_course_type");
        init.checkDatabase("cs_course_property");
        init.checkDatabase("cs_course_nature");
        init.checkDatabase("cs_course_category");
        init.checkDatabase("cs_campus");
        init.checkDatabase("cs_building");
        init.checkDatabase("cs_department");
        init.checkDatabase("cs_classroom_tag");
        init.checkDatabase("cs_classroom_type");
        init.checkDatabase("cs_classroom");
        init.checkDatabase("cs_student");
        init.checkDatabase("cs_teacher");
        init.checkDatabase("cs_course_library");
        init.checkDatabase("cs_academic_affairs_permission");
        init.checkDatabase("cs_class_assignment");

        log.info("[INIT] 数据库检查完成");
    }

    /**
     * 初始化清空 Redis 缓存
     * <p>
     * 该方法用于在系统初始化阶段清空 Redis 中的所有缓存数据。此操作将删除 Redis 中的所有键值对，确保系统从一个干净的状态开始运行。
     * 清空操作前后会记录日志信息，以便于调试和监控。
     * </p>
     */
    private void initClearRedis() {
        log.info("[INIT] 清空 Redis 缓存");
        redissonClient.getKeys().flushall();
        log.debug("[INIT] 清空 Redis 缓存完成");
    }

    /**
     * 检查系统数据表
     * <p>
     * 该方法用于检查系统数据表内数据是否完整，如果不完整将会创建系统表数据。
     */
    private void checkSystemTable() {
        log.info("[INIT] 系统数据表检查开始");

        // 系统有关数据检查
        init.checkSystemTable("system_author", SystemConstant.getSYSTEM_AUTHOR());
        init.checkSystemTable("system_version", SystemConstant.getSYSTEM_VERSION());
        init.checkSystemTable("system_name", SystemConstant.getSYSTEM_NAME());
        SystemConstant.setIsInitMode(init.checkSystemTable("system_init_mode", "true"));
        init.checkSystemTable("system_init_time", DateUtil.now());

        //站点基础信息
        init.checkSystemTable("web_name", SystemConstant.getSYSTEM_NAME());
        init.checkSystemTable("web_title", "智课方舟-智能排课系统");
        init.checkSystemTable("web_sub_title", "基于AI的智能排课系统，适用于学校等教育场地");
        init.checkSystemTable("web_description", "一个用于智能排课的系统");
        init.checkSystemTable("web_keywords", "排课系统，教育管理，人工智能，课程规划");
        init.checkSystemTable("web_icon_url", "https://xxx.com/favicon.ico");
        init.checkSystemTable("web_logo_url", "https://xxx.com/logo.png");
        //站点备案与版权
        init.checkSystemTable("web_icp_number", "京ICP备 2020001234号");
        init.checkSystemTable("web_icp_link", "https://beian.miit.gov.cn");
        init.checkSystemTable("web_security_record", "京公网安备 110101020004221号");
        init.checkSystemTable("web_security_record_link", "https://www.beian.gov.cn/portal/registerSystemInfo");
        init.checkSystemTable("web_copyright_status", "Copyright © 2024 智课方舟. All rights reserved.");
        init.checkSystemTable("web_open_source_license", "MIT License");
        //站点联系与社交
        init.checkSystemTable("web_contact_email", "xxx@xxx.com");
        init.checkSystemTable("web_contact_phone", "123xxxxxxxx");
        init.checkSystemTable("web_office_address", "北京市海淀区中关村大街 1 号");
        init.checkSystemTable("web_weibo_url", "weibo_url");
        init.checkSystemTable("web_wechat_official_account", "FrontLeavesTech");
        //站点高级元数据
        init.checkSystemTable("web_owner", "锋楪技术团队");
        init.checkSystemTable("web_founder", "张三，李四，王五");
        init.checkSystemTable("web_launch_date", "2025-01-01");
        init.checkSystemTable("web_technology_stack", "Spring Boot, React, MySQL, Redis");

        // APIKEY
        init.checkSystemTable("ai_front_api_key", "app-PI6ZLJbaYnwDQBllEI50cP8c");
        init.checkSystemTable("ai_message_api_key", "app-Gd2OjXtXLHc9082P8QuWLfaG");

        log.info("[INIT] 系统数据表检查完成");
    }

    /**
     * 获取角色信息
     * <p>
     * 该方法用于获取角色信息，将角色信息存入常量类中。
     */
    private void writeRoleInfo() {
        SystemConstant.setRoleAdmin(init.loadRoleContent("管理员"));
        SystemConstant.setRoleTeacher(init.loadRoleContent("教师"));
        SystemConstant.setRoleStudent(init.loadRoleContent("学生"));
        SystemConstant.setRoleLeader(init.loadRoleContent("管理"));
        SystemConstant.setRoleAcademic(init.loadRoleContent("教务"));
    }

    /**
     * 获取教师类型信息
     * <p>
     * 该方法用于获取教师类型信息，将教师类型信息存入常量类中。
     */
    private void writeTeacherType(){
        SystemConstant.setTeacherTypeAssistant(init.loadTeacherTypeContent("助教"));
        SystemConstant.setTeacherTypeLecturer(init.loadTeacherTypeContent("讲师"));
        SystemConstant.setTeacherTypePartTime(init.loadTeacherTypeContent("兼职教师"));
        SystemConstant.setTeacherTypeIntern(init.loadTeacherTypeContent("实习教师"));
        SystemConstant.setTeacherTypeAssociateProf(init.loadTeacherTypeContent("副教授"));
        SystemConstant.setTeacherTypeOther(init.loadTeacherTypeContent("其他"));
        SystemConstant.setTeacherTypeProfessor(init.loadTeacherTypeContent("教授"));
        SystemConstant.setTeacherTypeFullTime(init.loadTeacherTypeContent("专职教师"));
        SystemConstant.setTeacherTypeCounselor(init.loadTeacherTypeContent("辅导员"));
    }
    private void withClassroomType(){
        SystemConstant.setClassroomTypeCommon(init.loadClassroomTypeContent("普通教室"));
    }

    /**
     * 初始化测试用户
     * <p>
     * 该方法用于初始化测试用户，用于测试系统是否正常运行。
     */
    private void initTestUser() {
        log.info("[INIT] 初始化测试用户开始");
        userDAO.lambdaQuery().eq(UserDO::getName, "test").oneOpt().ifPresentOrElse(
                userDO -> {
                    log.info("[INIT] 测试用户已存在");
                    log.info("[INIT] 将测试用户信息恢复默认");
                    userDO
                            .setPassword(PasswordUtil.encrypt("123456"))
                            .setRoleUuid(SystemConstant.getRoleAdmin())
                            .setPhone("13388888880")
                            .setEmail("test@x-lf.cn");
                    userDAO.updateById(userDO);
                }, () -> {
                    UserDO userDO = new UserDO();
                    userDO
                            .setUserUuid(UuidUtil.generateUuidNoDash())
                            .setEmail("test@x-lf.cn")
                            .setPhone("13388888880")
                            .setName("test")
                            .setPassword(PasswordUtil.encrypt("123456"))
                            .setRoleUuid(SystemConstant.getRoleAdmin());
                    userDAO.save(userDO);
                    log.info("[INIT] 初始化测试用户完成");
                });
    }
}
