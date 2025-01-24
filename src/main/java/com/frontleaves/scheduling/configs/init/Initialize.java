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

import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.daos.TableDAO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.Jedis;

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
    private final Jedis jedis;

    private FunctionInit init;

    @PostConstruct
    public void init() {
        log.info("[INIT] 系统初始化开始");
        log.info("========== Start of Initialization ==========");
        // 初始化准备算法
        init = new FunctionInit(tableDAO, systemDAO, jedis);

        // 初始化数据库完整性检查
        this.checkTable();
        this.checkSystemTable();
        this.getSystemIntoConstant();
    }

    @Bean
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
                    \t\t\t\u001B[33m::: {} :::\t\t\t\t ::: {} ::: \t\u001B[32m       /____/  \s
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
     * 检查系统数据表
     * <p>
     * 该方法用于检查系统数据表内数据是否完整，如果不完整将会创建系统表数据。
     */
    private void checkSystemTable() {
        log.info("[INIT] 系统数据表检查开始");

        // 检查 cs_system 表
        init.checkSystemTable("author", SystemConstant.getSYSTEM_AUTHOR());
        init.checkSystemTable("is_init_mode", "true");

        log.info("[INIT] 系统数据表检查完成");
    }

    /**
     * 获取系统信息常量
     * <p>
     * 该方法用于获取系统信息常量，将系统信息常量存入常量类中。
     */
    private void getSystemIntoConstant() {
        // 系统有关信息
        SystemConstant.setIsInitMode(jedis.get("is_init_mode"));
    }
}
