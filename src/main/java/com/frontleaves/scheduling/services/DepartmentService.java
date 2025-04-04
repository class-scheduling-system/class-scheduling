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

package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.DepartmentDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.lite.DepartmentLiteDTO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门服务接口，定义了部门相关的操作。
 * <p>
 * 该接口提供了部门管理相关的基础方法，包括添加、删除、更新和查询部门信息等。具体实现细节由实现类决定。
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Service
public interface DepartmentService {

    /**
     * 根据部门唯一标识获取部门信息
     * <p>
     * 该方法通过传入的部门唯一标识 {@code departmentUuid} 查询对应的部门信息。如果查询到的部门信息存在，则将其转换为 {@link DepartmentDTO} 对象并返回；如果未找到对应部门，则返回 {@code null}。
     * </p>
     *
     * @param departmentUuid 部门的唯一标识
     * @return 如果找到对应的部门信息，则返回 {@link DepartmentDTO} 对象；否则返回 {@code null}
     */
    @Nullable
    DepartmentDTO getDepartmentByUuid(String departmentUuid);

    DepartmentDTO addDepartment(DepartmentVO departmentVO);

    DepartmentDTO getDepartment(String departmentUuid);

    void deleteDepartment(String departmentUuid);

    DepartmentDTO updateDepartment(String departmentUuid, DepartmentVO departmentVO);

    PageDTO<DepartmentDTO> getDepartmentPage(int page, int size, boolean isDesc, String name);

    /**
     * 获取部门列表
     * <p>
     * 该方法用于获取所有部门的列表信息，返回部门的简要信息列表。
     * </p>
     *
     * @return 返回部门的简要信息列表
     */
    List<DepartmentLiteDTO> getDepartmentList();


    /**
     * 获取部门信息伴随报错
     *
     * @param departmentUuid 部门的唯一标识
     * @return 部门信息
     */
    @NotNull
    DepartmentDTO getDepartmentByUuidWithThrows(
            @NotBlank String departmentUuid);
}
