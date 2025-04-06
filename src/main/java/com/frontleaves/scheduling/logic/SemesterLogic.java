package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.SemesterDTO;
import com.frontleaves.scheduling.models.entity.base.SemesterDO;
import com.frontleaves.scheduling.models.vo.SemesterVO;
import com.frontleaves.scheduling.services.SemesterService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * 学期逻辑实现类
 * <p>
 * 该类用于实现学期相关的业务逻辑，包括添加、删除、更新和查询学期信息等功能。
 * </p>
 *
 * @author xiaolfeng
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SemesterLogic implements SemesterService {
    private final SemesterDAO semesterDAO;

    @Override
    public void add(SemesterVO vo) {
        // 创建实体对象并保存
        SemesterDO entity = new SemesterDO();
        BeanUtil.copyProperties(vo, entity, ProjectOption.stringBlankToNull());
        semesterDAO.save(entity);
    }

    @Override
    public void delete(String semesterUuid) {
        // 验证学期是否存在
        SemesterDO entity = semesterDAO.getSemesterByUuid(semesterUuid);
        if (entity == null) {
            throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
        }

        // 删除学期
        semesterDAO.removeSemester(semesterUuid);
    }

    @Override
    public void update(String semesterUuid, SemesterVO vo) {
        // 验证学期是否存在
        SemesterDO entity = semesterDAO.getSemesterByUuid(semesterUuid);
        if (entity == null) {
            throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
        }

        // 更新实体对象
        BeanUtil.copyProperties(vo, entity, ProjectOption.stringBlankToNull());
        semesterDAO.updateSemester(entity);
    }

    @Override
    public SemesterDTO getById(String semesterUuid) {
        // 验证学期是否存在
        SemesterDO entity = semesterDAO.getSemesterByUuid(semesterUuid);
        if (entity == null) {
            throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
        }

        // 转换为 DTO 并返回
        return BeanUtil.toBean(entity, SemesterDTO.class);
    }

    @Override
    public PageDTO<SemesterDTO> page(Integer page, Integer size, Boolean isDesc, String keyword) {
        // 获取分页数据
        Page<SemesterDO> pageResult = semesterDAO.getSemesterPage(page, size, isDesc, keyword);
        if (pageResult == null || pageResult.getRecords().isEmpty()) {
            return new PageDTO<SemesterDTO>()
                    .setTotal(0L)
                    .setSize((long) size)
                    .setRecords(List.of())
                    .setCurrent((long) page);
        }

        // 转换为 DTO
        return ProjectUtil.convertPageToPageDTO(pageResult, SemesterDTO.class);
    }

    @Override
    public List<SemesterDTO> list() {
        // 获取启用的学期列表
        return Optional.ofNullable(semesterDAO.getEnabledSemesters())
                .map(list -> list.stream()
                        .map(entity -> BeanUtil.toBean(entity, SemesterDTO.class))
                        .toList())
                .orElse(List.of());
    }

    /**
     * 根据学期的UUID获取学期信息，并检查学期是否启用
     * 如果学期不存在，则抛出异常
     * 如果学期未启用，则抛出异常
     * 否则返回学期信息
     * @param semesterUuid 学期的UUID
     * @return 学期信息
     * @throws BusinessException 如果学期不存在或未启用
     */
    @Override
    public SemesterDTO getSemesterByUuidCheckEnabled(String semesterUuid) {
        SemesterDO semesterDO = semesterDAO.getSemesterByUuid(semesterUuid);
        if (semesterDO == null) {
            throw new BusinessException("学期不存在", ErrorCode.OPERATION_ERROR);
        }
        if (Boolean.FALSE.equals(semesterDO.getIsEnabled())) {
            throw new BusinessException("学期未启用", ErrorCode.OPERATION_ERROR);
        }
        return BeanUtil.toBean(semesterDO, SemesterDTO.class);
    }
}
