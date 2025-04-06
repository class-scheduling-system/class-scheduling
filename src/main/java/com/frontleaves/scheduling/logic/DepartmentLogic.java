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

package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.base.DepartmentDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.lite.DepartmentLiteDTO;
import com.frontleaves.scheduling.models.entity.base.DepartmentDO;
import com.frontleaves.scheduling.models.entity.base.UnitCategoryDO;
import com.frontleaves.scheduling.models.entity.base.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 部门逻辑处理类，实现了 {@link DepartmentService} 接口，提供了部门管理的具体实现。
 * <p>
 * 该类负责处理与部门相关的业务逻辑，包括查询、添加、删除和更新部门信息等。通过依赖注入的方式获取所需的其他服务或组件。
 * </p>
 *
 * @author xiao_lfeng | qiyu
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentLogic implements DepartmentService {

    private final DepartmentDAO departmentDAO;
    private final UnitCategoryDAO unitCategoryDAO;
    private final UnitTypeDAO unitTypeDAO;
    private final BuildingDAO buildingDAO;

    /**
     * 添加新部门
     * <p>
     * 此方法负责处理新部门的添加操作它首先验证部门关联的单位类别、单位办别、教学楼和上级部门是否存在，
     * 如果存在，则将部门信息保存到数据库中，并返回新创建的部门信息
     *
     * @param departmentVO 部门的详细信息，包括单位类别、单位办别、教学楼、上级部门等
     * @return 返回新添加部门的详细信息
     * @throws BusinessException 如果单位类别、单位办别、教学楼或上级部门不存在，则抛出业务异常
     */
    @Override
    public DepartmentDTO addDepartment(@NotNull DepartmentVO departmentVO) {
        // 数据检查
        //检查单位类别
        UnitCategoryDO getUnitCategory = unitCategoryDAO.getUnitCategoryByUuid(departmentVO.getUnitCategory());
        if (getUnitCategory == null) {
            // 抛出异常
            throw new BusinessException("单位类别不存在", ErrorCode.NOT_EXIST);
        }

        //检查单位办别
        UnitTypeDO getUnitType = unitTypeDAO.getUnitTypeByUuid(departmentVO.getUnitType());
        if (getUnitType == null) {
            // 抛出异常
            throw new BusinessException("单位办别不存在", ErrorCode.NOT_EXIST);
        }

        //检查教学楼
        Optional.ofNullable(departmentVO.getAssignedTeachingBuilding())
                .filter(data -> !data.isEmpty())
                .ifPresent(departmentList ->
                        departmentList.forEach(department ->
                                Optional.ofNullable(buildingDAO.getBuildingByUuid(department))
                                        .orElseThrow(() -> new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST))
                        )
                );

        //检查上级部门
        if (departmentVO.getParentDepartment() != null && !departmentVO.getParentDepartment().isEmpty()) {
            // 检查上级部门是否存在

            DepartmentDO getParentDepartment = departmentDAO.getDepartmentByUuid(departmentVO.getParentDepartment());
            if (getParentDepartment == null) {
                // 抛出异常
                throw new BusinessException("上级部门不存在", ErrorCode.NOT_EXIST);
            }
        }
        // 数据拷贝
        DepartmentDO departmentDO = new DepartmentDO();
        // 数据拷贝
        BeanUtil.copyProperties(departmentVO, departmentDO, StringConstant.Ignore.ASSIGNED_TEACHING_BUILDING);
        departmentDO.setAssignedTeachingBuilding(JSONUtil.toJsonStr(departmentVO.getAssignedTeachingBuilding()));

        // 保存数据
        departmentDAO.save(departmentDO);

        // 取出数据
        DepartmentDO getNewDepartment = departmentDAO.getDepartmentByUuid(departmentDO.getDepartmentUuid());
        return BeanUtil.toBean(getNewDepartment, DepartmentDTO.class);
    }

    /**
     * 根据部门UUID获取部门详情
     *
     * @param departmentUuid 部门的UUID，用于唯一标识一个部门，不允许为空
     * @return DepartmentDTO 部门的数据传输对象，包含部门的相关信息
     * @throws BusinessException 当部门不存在时，抛出业务异常
     */
    @Override
    public DepartmentDTO getDepartment(@NotNull String departmentUuid) {
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);
        if (departmentDO == null) {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }
        DepartmentDTO getDepartmentDTO = new DepartmentDTO();
        BeanUtil.copyProperties(departmentDO, getDepartmentDTO, StringConstant.Ignore.ASSIGNED_TEACHING_BUILDING);
        Optional.ofNullable(departmentDO.getAssignedTeachingBuilding())
                .filter(data -> !data.isBlank())
                .ifPresentOrElse(data -> getDepartmentDTO.setAssignedTeachingBuilding(
                                Optional.ofNullable(JSONUtil.toList(data, String.class))
                                        .orElse(List.of())),
                        () -> getDepartmentDTO.setAssignedTeachingBuilding(List.of()));
        return getDepartmentDTO;
    }

    /**
     * 删除指定的部门
     *
     * @param departmentUuid 部门的唯一标识符
     * @throws BusinessException 如果部门不存在或有依赖关系，则抛出业务异常
     */
    @Override
    public void deleteDepartment(String departmentUuid) throws BusinessException {
        // 根据UUID获取部门对象
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);

        // 判断部门是否存在
        if (departmentDO == null) {
            // 如果部门不存在，则抛出业务异常，提示用户错误信息
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }

        // 检查是否有子部门依赖于此部门
        boolean childCount = departmentDAO.lambdaQuery()
                .eq(DepartmentDO::getParentDepartment, departmentUuid)
                .exists();
        if (childCount) {
            // 如果有子部门依赖，抛出业务异常
            throw new BusinessException("删除部门失败，该部门存在子部门", ErrorCode.EXISTED);
        }

        try {
            departmentDAO.deleteDepartment(departmentDO);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            // 处理其他可能的异常
            log.error("删除部门时发生异常", e);
            throw new BusinessException("删除部门失败", ErrorCode.OPERATION_FAILED);
        }
    }

    /**
     * 更新部门信息
     * <p>
     * 此方法首先根据部门UUID获取部门数据对象，然后分别验证单位类别、单位办别、教学楼和上级部门的存在性
     * 如果不存在，则抛出业务异常如果所有验证通过，则将视图对象的属性复制到数据对象中，
     * 更新数据库中的部门信息，并返回更新后的部门数据传输对象
     *
     * @param departmentUuid 部门唯一标识符
     * @param departmentVO   包含更新信息的部门视图对象
     * @return 更新后的部门数据传输对象，如果部门不存在则返回null
     */
    @Override
    public DepartmentDTO updateDepartment(String departmentUuid, DepartmentVO departmentVO) {
        // 根据UUID获取部门数据对象
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);
        if (departmentDO != null) {
            // 验证单位类别是否存在
            Optional.ofNullable(departmentVO.getUnitCategory())
                    .map(unitCategoryDAO::getUnitCategoryByUuid)
                    .orElseThrow(() -> new BusinessException("单位类别不存在", ErrorCode.NOT_EXIST));
            Optional.ofNullable(departmentVO.getUnitType())
                    .map(unitTypeDAO::getUnitTypeByUuid)
                    .orElseThrow(() -> new BusinessException("单位办别不存在", ErrorCode.NOT_EXIST));
            if (departmentVO.getAssignedTeachingBuilding() != null && !departmentVO.getAssignedTeachingBuilding().isEmpty()) {
                Optional.of(departmentVO.getAssignedTeachingBuilding())
                        .ifPresent(departmentList ->
                                departmentList.forEach(department ->
                                        Optional.ofNullable(buildingDAO.getBuildingByUuid(department))
                                                .orElseThrow(() -> new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST))
                                )
                        );
            }
            if (departmentVO.getParentDepartment() != null && !departmentVO.getParentDepartment().isEmpty()) {
                Optional.of(departmentVO.getParentDepartment())
                        .map(departmentDAO::getDepartmentByUuid)
                        .orElseThrow(() -> new BusinessException("上级部门不存在", ErrorCode.NOT_EXIST));
            }

            // 将视图对象属性复制到数据对象并更新数据库
            BeanUtil.copyProperties(departmentVO, departmentDO, ProjectOption.stringBlankToNull());
            if (departmentVO.getAssignedTeachingBuilding() != null && !departmentVO.getAssignedTeachingBuilding().isEmpty()) {
                departmentDO.setAssignedTeachingBuilding(JSONUtil.toJsonStr(departmentVO.getAssignedTeachingBuilding()));
            }
            departmentDAO.updateDepartment(departmentDO);
            // 返回更新后的部门数据传输对象
            DepartmentDTO departmentDTO = new DepartmentDTO();
            BeanUtil.copyProperties(departmentDO, departmentDTO,  StringConstant.Ignore.ASSIGNED_TEACHING_BUILDING);
            departmentDTO.setAssignedTeachingBuilding(
                    Optional.ofNullable(departmentDO.getAssignedTeachingBuilding())
                            .filter(data -> !data.isBlank())
                            .map(data -> JSONUtil.toList(data, String.class))
                            .orElse(List.of())
            );
            return departmentDTO;
        }
        // 如果部门不存在，返回null
        return null;
    }

    /**
     * 获取部门列表
     *
     * @param page   页码，表示请求的数据页数
     * @param size   每页数量，表示每页包含的部门数量
     * @param isDesc 是否降序，用于指定排序方式
     * @param name   部门名称，用于模糊搜索
     * @return 返回一个包含部门信息的PageDTO对象
     * <p>
     * 此方法调用departmentDAO来获取部门列表，并根据查询参数进行分页和排序
     * 如果查询结果为空，则返回一个空的PageDTO对象；否则，将查询结果转换为DTO形式并返回
     */
    @Override
    public PageDTO<DepartmentDTO> getDepartmentPage(int page, int size, boolean isDesc, String name) {
        Page<DepartmentDO> departmentList = departmentDAO.getDepartmentPage(page, size, isDesc, name);

        if (departmentList.getTotal() == 0) {
            return new PageDTO<>();
        } else {
            PageDTO<DepartmentDTO> pageDTO = new PageDTO<>(departmentList.getTotal(), departmentList.getSize());
            pageDTO.setCurrent(departmentList.getCurrent());
            pageDTO.setRecords(
                    departmentList.getRecords().stream()
                            .map(departmentDO -> {
                                DepartmentDTO departmentDTO = new DepartmentDTO();
                                BeanUtil.copyProperties(departmentDO, departmentDTO, StringConstant.Ignore.ASSIGNED_TEACHING_BUILDING);
                                JSONArray jsonArray = JSONUtil.parseArray(departmentDO.getAssignedTeachingBuilding());
                                departmentDTO.setAssignedTeachingBuilding(
                                        Optional.ofNullable(jsonArray.toList(String.class))
                                                .orElse(List.of())
                                );
                                return departmentDTO;
                            }).toList()
            );
            return pageDTO;
        }
    }

    /**
     * 获取部门列表
     * <p>
     * 该方法用于获取所有部门的列表信息，返回部门的简要信息列表。
     * </p>
     *
     * @return 返回部门的简要信息列表
     */
    @Override
    public List<DepartmentLiteDTO> getDepartmentList() {
        return Optional.ofNullable(departmentDAO.getDepartmentList())
                .map(data -> BeanUtil.copyToList(data, DepartmentLiteDTO.class))
                .orElse(List.of());
    }

    /**
     *  根据部门唯一标识获取部门信息
     * @param departmentUuid 部门的唯一标识
     * @return 部门信息
     */
    @Override
    public DepartmentDTO getDepartmentByUuidWithThrows(@NotBlank String departmentUuid) {
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);
        if (departmentDO == null) {
            throw new BusinessException("通过部门ID查询，部门不存在", ErrorCode.NOT_EXIST);
        }
        return BeanUtil.toBean(departmentDO,DepartmentDTO.class);
    }

    /**
     * 根据部门唯一标识获取部门信息
     * <p>
     * 该方法通过传入的部门唯一标识 {@code departmentUuid} 查询对应的部门信息。如果查询到的部门信息存在，则将其转换为 {@link DepartmentDTO} 对象并返回；如果未找到对应部门，则返回 {@code null}。
     * </p>
     *
     * @param departmentUuid 部门的唯一标识
     * @return 如果找到对应的部门信息，则返回 {@link DepartmentDTO} 对象；否则返回 {@code null}
     */
    @Override
    @Nullable
    public DepartmentDTO getDepartmentByUuid(String departmentUuid) {
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);
        if (departmentDO == null) {
            return null;
        }
        return BeanUtil.toBean(departmentDO, DepartmentDTO.class);
    }
}
