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

import com.frontleaves.scheduling.daos.SystemDAO;
import com.frontleaves.scheduling.daos.TableDAO;
import com.frontleaves.scheduling.models.entity.SystemDO;
import com.frontleaves.scheduling.models.entity.TableDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 初始化方法类
 * <p>
 * 该类用于配置初始化函数;
 * 详细写可以重复执行的初始化方法的最小单元。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@RequiredArgsConstructor
class FunctionInit {
    private final TableDAO tableDAO;
    private final SystemDAO systemDAO;

    /**
     * 检查数据表是否完整
     * <p>
     * 该方法用于检查数据表是否完整，如果数据表不完整则会禁止服务启动。
     */
    public void checkDatabase(String tableName) {
        TableDO tableDO = tableDAO.lambdaQuery().eq(TableDO::getTableName, tableName).one();
        if (tableDO == null) {
            log.error("[INIT] 数据表 {} 不存在", tableName);
            System.exit(0);
        } else {
            log.debug("[INIT] 数据表 {} 存在", tableName);
        }
    }

    /**
     * 检查系统表是否完整
     * <p>
     * 该方法用于检查系统表是否完整，如果系统表不完整将会创建内容。
     */
    public void checkSystemTable(String key, String value) {
        SystemDO systemDO = systemDAO.lambdaQuery().eq(SystemDO::getSystemKey, key).one();
        if (systemDO == null) {
            log.info("[INIT] 系统表 {} 不存在，创建中", key);
            systemDAO.save(new SystemDO().setSystemKey(key).setSystemVal(value));
        } else {
            log.debug("[INIT] 系统表 {} 存在", key);
        }
    }
}
