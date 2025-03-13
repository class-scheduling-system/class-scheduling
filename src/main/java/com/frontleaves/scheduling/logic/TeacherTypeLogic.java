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
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.TeacherTypeDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherTypeDTO;
import com.frontleaves.scheduling.models.entity.TeacherTypeDO;
import com.frontleaves.scheduling.services.TeacherTypeService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 教师类型业务逻辑实现类
 * <p>
 * 该类实现了TeacherTypeService接口，提供教师类型相关的业务逻辑处理，
 * 包括获取单个教师类型、获取教师类型分页列表、获取教师类型简洁列表等功能。
 * </p>
 *
 * @author xiao_lfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherTypeLogic implements TeacherTypeService {

    private final TeacherTypeDAO teacherTypeDAO;

    /**
     * 根据UUID获取教师类型信息
     *
     * @param teacherTypeUuid 教师类型UUID
     * @return 教师类型DTO对象
     */
    @Override
    public TeacherTypeDTO getTeacherType(String teacherTypeUuid) {
        // 验证教师类型UUID是否符合无连字符的UUID正则表达式
        String getType = Optional.ofNullable(teacherTypeUuid)
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException("教师类型UUID格式不正确", ErrorCode.PARAMETER_ERROR));

        TeacherTypeDO teacherTypeDO = Optional.ofNullable(teacherTypeDAO.getTeacherTypeByUuid(getType))
                .orElseThrow(() -> new BusinessException("教师类型不存在", ErrorCode.NOT_EXIST));

        return BeanUtil.toBean(teacherTypeDO, TeacherTypeDTO.class);
    }

    /**
     * 获取教师类型分页列表
     *
     * @param page   页码
     * @param size   每页大小
     * @param isDesc 是否降序排序
     * @param name   教师类型名称（可选，用于筛选）
     * @return 教师类型分页数据
     */
    @Override
    public PageDTO<TeacherTypeDTO> getTeacherTypePage(Integer page, Integer size, Boolean isDesc, String name) {
        // 调用DAO获取分页数据
        Page<TeacherTypeDO> teacherTypePage = Optional
                .ofNullable(teacherTypeDAO.getTeacherTypePage(page, size, isDesc, name))
                .orElse(new Page<>());
        return ProjectUtil.convertPageToPageDTO(teacherTypePage, TeacherTypeDTO.class);
    }

    /**
     * 获取所有教师类型简洁列表
     *
     * @return 教师类型DTO列表
     */
    @Override
    public List<TeacherTypeDTO> getTeacherTypeList() {
        // 调用DAO获取所有教师类型
        List<TeacherTypeDO> teacherTypes = teacherTypeDAO.getAllTeacherTypes();

        // 如果没有找到任何教师类型，返回空列表
        if (teacherTypes.isEmpty()) {
            return new ArrayList<>();
        }

        // 转换为DTO列表并返回
        return teacherTypes.stream()
                .map(teacherType -> BeanUtil.toBean(teacherType, TeacherTypeDTO.class))
                .toList();
    }

    /**
     * 添加教师类型
     *
     * @param typeName        类型名称
     * @param typeEnglishName 类型英文名称
     * @param typeDesc        类型描述
     * @return 添加成功的教师类型DTO
     */
    @Override
    public TeacherTypeDTO addTeacherType(String typeName, String typeEnglishName, String typeDesc) {
        // 参数校验
        if (typeName == null || typeName.isBlank()) {
            throw new BusinessException("类型名称不能为空", ErrorCode.PARAMETER_ERROR);
        }

        if (typeEnglishName == null || typeEnglishName.isBlank()) {
            throw new BusinessException("类型英文名称不能为空", ErrorCode.PARAMETER_ERROR);
        }

        // 检查名称和英文名称是否重复
        TeacherTypeDO existType = teacherTypeDAO.getTeacherTypeByName(typeName);
        if (existType != null) {
            throw new BusinessException("类型名称已存在", ErrorCode.EXISTED);
        }

        existType = teacherTypeDAO.getTeacherTypeByEnglishName(typeEnglishName);
        if (existType != null) {
            throw new BusinessException("类型英文名称已存在", ErrorCode.EXISTED);
        }

        // 创建教师类型对象
        TeacherTypeDO teacherType = new TeacherTypeDO()
                .setTeacherTypeUuid(UuidUtil.generateUuidNoDash())
                .setTypeName(typeName)
                .setTypeEnglishName(typeEnglishName)
                .setTypeDesc(typeDesc);

        // 添加到数据库
        teacherTypeDAO.addTeacherType(teacherType);

        // 返回创建的DTO
        return BeanUtil.toBean(teacherType, TeacherTypeDTO.class);
    }

    /**
     * 更新教师类型
     *
     * @param teacherTypeUuid 教师类型UUID
     * @param typeName        类型名称
     * @param typeEnglishName 类型英文名称
     * @param typeDesc        类型描述
     * @return 更新后的教师类型DTO
     */
    @Override
    public TeacherTypeDTO updateTeacherType(String teacherTypeUuid, String typeName, String typeEnglishName, String typeDesc) {
        Optional.ofNullable(teacherTypeUuid)
                .filter(uuid -> !uuid.isBlank())
                .filter(uuid -> uuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION))
                .orElseThrow(() -> new BusinessException("教师类型UUID不能为空", ErrorCode.PARAMETER_ERROR));
        TeacherTypeDO teacherType = Optional.of(teacherTypeUuid)
                .map(teacherTypeDAO::getTeacherTypeByUuid)
                .orElseThrow(() -> new BusinessException("教师类型不存在", ErrorCode.NOT_EXIST));

        Optional.ofNullable(typeName)
                .filter(name -> !name.isBlank())
                .filter(name -> !name.equals(teacherType.getTypeName()))
                .ifPresent(name -> {
                    TeacherTypeDO existType = teacherTypeDAO.getTeacherTypeByName(name);
                    if (existType != null) {
                        throw new BusinessException("类型名称已存在", ErrorCode.EXISTED);
                    }
                });
        Optional.ofNullable(typeEnglishName)
                .filter(name -> !name.isBlank())
                .filter(name -> !name.equals(teacherType.getTypeEnglishName()))
                .ifPresent(name -> {
                    TeacherTypeDO existType = teacherTypeDAO.getTeacherTypeByEnglishName(name);
                    if (existType != null) {
                        throw new BusinessException("类型英文名称已存在", ErrorCode.EXISTED);
                    }
                });

        teacherType
                .setTypeName(typeName)
                .setTypeEnglishName(typeEnglishName)
                .setTypeDesc(typeDesc);

        if (!teacherTypeDAO.updateTeacherType(teacherType)) {
            throw new BusinessException("更新教师类型失败", ErrorCode.OPERATION_FAILED);
        }

        // 返回更新后的教师类型
        TeacherTypeDO updatedType = teacherTypeDAO.getTeacherTypeByUuid(teacherType.getTeacherTypeUuid());
        return BeanUtil.toBean(updatedType, TeacherTypeDTO.class);
    }

    /**
     * 删除教师类型
     *
     * @param teacherTypeUuid 教师类型UUID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteTeacherType(String teacherTypeUuid) {
        // 检查UUID是否正确
        if (!teacherTypeUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
            throw new BusinessException("教师类型UUID格式不正确", ErrorCode.PARAMETER_ERROR);
        }

        // 调用DAO删除教师类型
        boolean deleted = teacherTypeDAO.deleteTeacherType(teacherTypeUuid);

        if (!deleted) {
            throw new BusinessException("删除教师类型失败，可能不存在或被引用", ErrorCode.OPERATION_FAILED);
        }

        return true;
    }
}
