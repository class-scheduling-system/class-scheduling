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

package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.SchedulingConflictMapper;
import com.frontleaves.scheduling.models.dto.base.SchedulingConflictDTO;
import com.frontleaves.scheduling.models.entity.base.SchedulingConflictDO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 排课冲突数据访问对象
 * 使用MybatisPlus处理排课冲突的数据库操作
 *
 * @author xiao_lfeng
 */
@Slf4j
@Repository
public class SchedulingConflictDAO extends ServiceImpl<SchedulingConflictMapper, SchedulingConflictDO> {

    /**
     * 批量保存排课冲突信息
     *
     * @param conflictDTOList 排课冲突DTO列表
     * @param semesterUuid 学期UUID
     * @return 保存成功的数量
     */
    @Transactional
    public int batchSaveConflicts(List<SchedulingConflictDTO> conflictDTOList, String semesterUuid) {
        if (conflictDTOList == null || conflictDTOList.isEmpty()) {
            log.info("没有排课冲突需要保存");
            return 0;
        }

        log.info("开始保存{}条排课冲突信息到数据库", conflictDTOList.size());

        List<SchedulingConflictDO> entityList = new ArrayList<>(conflictDTOList.size());
        for (SchedulingConflictDTO dto : conflictDTOList) {
            // 使用 BeanUtil 直接转换
            SchedulingConflictDO conflictDO = BeanUtil.toBean(dto, SchedulingConflictDO.class);
            entityList.add(conflictDO);
        }

        // 使用MybatisPlus批量保存
        boolean success = this.saveBatch(entityList);
        if (success) {
            log.info("批量保存排课冲突信息成功, 共{}条", entityList.size());
            return entityList.size();
        } else {
            log.error("批量保存排课冲突信息失败");
            return 0;
        }
    }
}
