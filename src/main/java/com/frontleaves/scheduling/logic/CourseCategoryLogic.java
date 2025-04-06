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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.CourseCategoryDAO;
import com.frontleaves.scheduling.models.dto.base.CourseCategoryDTO;
import com.frontleaves.scheduling.models.entity.base.CourseCategoryDO;
import com.frontleaves.scheduling.services.CourseCategoryService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 课程类别逻辑层实现
 * <p>
 * 该类实现了 {@link CourseCategoryService} 接口，提供课程类别相关的业务逻辑处理。
 * </p>
 *
 * @author Claude AI
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseCategoryLogic implements CourseCategoryService {

    /**
     * 课程类别数据访问对象
     */
    private final CourseCategoryDAO courseCategoryDAO;

    /**
     * {@inheritDoc}
     */
    @Override
    public CourseCategoryDTO getCourseCategoryByUuidWithError(String uuid) {
        CourseCategoryDO courseCategoryDO = courseCategoryDAO.getCourseCategoryByUuid(uuid);
        if (courseCategoryDO == null) {
            throw new BusinessException("课程类别不存在", ErrorCode.BODY_ERROR);
        }
        return BeanUtil.toBean(courseCategoryDO, CourseCategoryDTO.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CourseCategoryDTO> getCourseCategoryList() {
        List<CourseCategoryDO> courseCategoryDOList = courseCategoryDAO.getCourseCategoryList();
        return BeanUtil.copyToList(courseCategoryDOList, CourseCategoryDTO.class);
    }
}