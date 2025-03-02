/*
 * --------------------------------------------------------------------------------
 * Copyright (c) 2022-NOW(至今) 锋楪技术团队
 * system_author: 锋楪技术团队 (https://www.frontleaves.com)
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

package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.daos.SystemDAO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;

@Slf4j
@SpringBootTest
class SystemTest {
    @Resource
    private SystemDAO systemDAO;

    @Test
    void getSystemInfo() {
        // 存在的结果
        String getSystemAuthor = systemDAO.getSystemInfo("system_author");
        log.debug("getSystemAuthor: {}", getSystemAuthor);
        systemDAO.setSystemInfo("system_author", "锋楪技朋团队");
        String getSystemAuthor2 = systemDAO.getSystemInfo("system_author");
        log.debug("getSystemAuthor2: {}", getSystemAuthor2);
        Assertions.assertEquals("锋楪技朋团队", getSystemAuthor2);
        // 不存在的结果
        String getSystemAuthor3 = systemDAO.getSystemInfo("system_author2");
        log.debug("getSystemAuthor3: {}", getSystemAuthor3);

        // 测试结果
        Assertions.assertNull(getSystemAuthor3);
        Assertions.assertEquals("锋楪技朋团队", getSystemAuthor2);
        // 恢复
        systemDAO.setSystemInfo("system_author", "锋楪技术团队");
    }

    @Test
    void addSystemInfo() {
        String getKey = "system_" + System.currentTimeMillis();
        // 存入随机结果
        systemDAO.addSystemInfo(getKey, "随机存入测试");
        String getRandomAdd = systemDAO.getSystemInfo(getKey);
        log.debug("获取随机存入结果: {}", getRandomAdd);

        Assertions.assertThrows(DuplicateKeyException.class, () -> {
            // 存入已存在的结果
            systemDAO.addSystemInfo("system_author", "测试团队");
            String getSystemAuthor = systemDAO.getSystemInfo("system_author");
            log.debug("获取添加的内容，检查是否会报错: {}", getSystemAuthor);
            systemDAO.setSystemInfo("system_author", "锋楪技术团队");
        });
    }
}
