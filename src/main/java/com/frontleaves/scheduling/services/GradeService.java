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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.GradeDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;

import java.util.List;

/**
 * 年级服务接口
 * <p>
 * 定义了年级相关的服务方法，包括增删改查操作。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
public interface GradeService {

    /**
     * 创建年级
     *
     * @param gradeDTO 年级信息
     * @return 创建后的年级信息，包含主键
     */
    GradeDTO createGrade(GradeDTO gradeDTO);

    /**
     * 更新年级信息
     *
     * @param gradeDTO 年级信息
     * @return 更新后的年级信息
     */
    GradeDTO updateGrade(GradeDTO gradeDTO);

    /**
     * 根据UUID删除年级
     *
     * @param gradeUuid 年级UUID
     * @return 是否删除成功
     */
    boolean deleteGrade(String gradeUuid);

    /**
     * 获取年级详情
     *
     * @param gradeUuid 年级UUID
     * @return 年级详情
     */
    GradeDTO getGradeDetail(String gradeUuid);

    /**
     * 分页查询年级列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param name 年级名称，可选，用于模糊查询
     * @param year 入学年份，可选
     * @return 分页数据
     */
    PageDTO<GradeDTO> page(Integer page, Integer size, String name, Short year);

    /**
     * 获取简单年级列表
     *
     * @return 年级列表
     */
    List<GradeDTO> listSimple();
} 
