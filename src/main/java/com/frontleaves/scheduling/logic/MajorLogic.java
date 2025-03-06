package com.frontleaves.scheduling.logic;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.MajorDAO;
import com.frontleaves.scheduling.mappers.DepartmentMapper;
import com.frontleaves.scheduling.models.dto.MajorDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.services.MajorService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.javassist.NotFoundException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Function;

/**
 * 专业业务逻辑的实现
 * <p>
 * 负责专业的创建、修改、删除、查询等具体实现
 * </p>
 *
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MajorLogic implements MajorService {
    private final MajorDAO majorDAO;
    private final DepartmentMapper departmentMapper;
    private static final String MAJOR_NOT_FOUND = "专业不存在";

    /**
     * 创建专业信息
     * <p>
     * 该方法接收 {@code MajorDTO} 对象作为参数,该对象包含了需要创建的专业信息.
     * </p>
     *
     * @param majorDTO 包含待创建专业信息的数据传输对象
     * @return 返回带有专业UUID的 MajorDTO 对象
     */
    @Override
    public MajorDTO createMajor(MajorDTO majorDTO) {
        MajorDO majorDO = new MajorDO();
        BeanUtils.copyProperties(majorDTO, majorDO);
        // 通过DAO层的save方法将 MajorDO 对象保存到数据库
        majorDAO.save(majorDO);
        // 将保存后的专业 UUID 设置回传入的 MajorDTO 对象中
        majorDTO.setMajorUuid(majorDO.getMajorUuid());
        return majorDTO;
    }

    /**
     * 修改专业信息
     *
     * @param majorUuid 专业唯一标识符
     * @param majorDTO  包含专业更新信息的数据传输对象
     * @return 更新后的专业数据传输对象
     * @throws NotFoundException 如果指定专业的唯一标识符不存在,则抛出此异常
     */
    @Override
    public MajorDTO updateMajor(String majorUuid, MajorDTO majorDTO) throws NotFoundException {
        MajorDO majorDO = majorDAO.getById(majorUuid);
        // 验证专业存在
        if (majorDO == null) {
            throw new NotFoundException(MAJOR_NOT_FOUND);
        }

        // 把 DTO 里的变动字段覆盖上去
        BeanUtils.copyProperties(majorDTO, majorDO, "majorUuid", "createAt", "updatedAt");
        majorDO.setMajorUuid(majorUuid);
        majorDAO.updateById(majorDO);

        // 返回更新后的数据
        MajorDTO updated = new MajorDTO();
        BeanUtils.copyProperties(majorDO, updated);
        return majorDTO;
    }

    /**
     * 删除专业信息
     * <p>
     * 此方法首先验证专业是否存在,如果不存在则抛出NotFoundException异常;
     * 如果专业存在,将执行删除操作;
     * 如果删除失败,同样抛出NotFoundException异常
     * </p>
     *
     * @param majorUuid 专业的唯一标识符
     * @throws NotFoundException 如果专业不存在或删除失败
     */
    @Override
    public void deleteMajor(String majorUuid) throws NotFoundException {
        // 检查专业是否存在
        MajorDO majorDO = majorDAO.getById(majorUuid);
        if (majorDO == null) {
            throw new NotFoundException(MAJOR_NOT_FOUND);
        }

        // 检查专业是否被系统引用
        if (majorDO.getMajorStatus() == 1) {
            throw new BusinessException("该专业正在被系统引用,无法删除", ErrorCode.OPERATION_FAILED);
        }

        // 进行二次确认
        log.warn("即将删除专业[{}],请确认是否继续", majorUuid);

        // 执行删除
        boolean removed = majorDAO.removeById(majorUuid);
        if (!removed) {
            throw new IllegalStateException("专业删除失败");
        }
        // 确认删除成功后，记录日志
        log.info("专业 [{}] 已成功删除", majorUuid);
    }

    /**
     * 获取专业信息
     *
     * @param majorUuid 专业UUID，用于唯一标识一个专业
     * @return MajorDTO 专业信息的DTO对象，包含专业的主要信息
     * @throws NotFoundException 如果找不到对应专业的信息，则抛出NotFoundException异常
     */
    @Override
    public MajorDTO getMajor(String majorUuid) throws NotFoundException {
        MajorDO majorDO = majorDAO.getById(majorUuid);
        if (majorDO == null) {
            throw new NotFoundException(MAJOR_NOT_FOUND);
        }
        // 将实体转换为 DTO
        MajorDTO majorDTO = new MajorDTO();
        BeanUtils.copyProperties(majorDO, majorDTO);
        return majorDTO;
    }

    /**
     * 管理员查询专业列表方法
     * <p>
     * 该方法支持分页查询，并可根据部门UUID和专业名称进行模糊搜索;
     * 还支持根据创建时间进行升序或降序排序
     * </p>
     *
     * @param page       当前页码，从1开始
     * @param size       每页记录数
     * @param isDesc     是否按创建时间降序排序，true为降序，false为升序，null默认为降序
     * @param department 部门UUID，用于模糊搜索
     * @param name       专业名称，用于模糊搜索
     * @return 返回封装了专业信息的分页对象
     */
    @Override
    public PageDTO<MajorDTO> listMajorsForAdmin(int page, int size, Boolean isDesc, String department, String name) {
        return listMajors(page, size, isDesc, department, name, majorDO -> {
            MajorDTO majorAdminDTO = new MajorDTO();
            BeanUtils.copyProperties(majorDO, majorAdminDTO);
            return majorAdminDTO;
        });
    }

    /**
     * 教务查询专业列表方法
     *
     * @param page       页码
     * @param size       每页大小
     * @param isDesc     是否按创建时间降序排序
     * @param department 学院UUID，用于筛选属于特定学院的专业
     * @param name       专业名称，用于模糊搜索
     * @return 返回一个分页对象，包含专业信息
     */
    @Override
    public PageDTO<MajorDTO> listMajorsForAcademic(int page, int size, Boolean isDesc, String department, String name) {
        return listMajors(page, size, isDesc, department, name, majorDO -> {
            MajorDTO majorAcademicDTO = new MajorDTO();
            BeanUtils.copyProperties(majorDO, majorAcademicDTO);
            return majorAcademicDTO;
        });
    }


    /**
     * 学生查询专业列表方法
     *
     * @param page       页码
     * @param size       每页大小
     * @param isDesc     是否按降序排序
     * @param department 学院标识
     * @param name       专业名称
     * @return 分页的专业信息DTO
     */
    @Override
    public PageDTO<MajorDTO> listMajorsForStudent(int page, int size, Boolean isDesc, String department, String name) {
        return listMajors(page, size, isDesc, department, name, majorDO -> {
            MajorDTO majorStudentDTO = new MajorDTO();
            BeanUtils.copyProperties(majorDO, majorStudentDTO);
            return majorStudentDTO;
        });
    }

    /**
     * 分页查询专业信息列表
     *
     * @param <T>        返回结果泛型类型
     * @param page       当前页码（从1开始）
     * @param size       每页记录数
     * @param isDesc    是否按创建时间倒序排序（true=倒序，false=正序，null=默认排序）
     * @param department 学院名称（模糊查询条件）
     * @param name       专业名称（模糊查询条件）
     * @param converter  DO到DTO的转换函数
     * @return           分页结果对象，包含转换后的DTO列表和分页信息
     * @throws BusinessException 当指定学院不存在时抛出
     */
    private <T> PageDTO<T> listMajors(int page, int size, Boolean isDesc, String department, String name, Function<MajorDO, T> converter) {
        // 初始化分页参数和查询构造器
        Page<MajorDO> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<MajorDO> queryWrapper = new LambdaQueryWrapper<>();

        // 学院名称过滤逻辑
        if (CharSequenceUtil.isNotBlank(department)) {
            List<String> departmentUuids = departmentMapper.getDepartmentUuidByName(department);
            if (departmentUuids.isEmpty()) {
                throw new BusinessException("学院不存在", ErrorCode.BODY_ERROR);
            } else {
                queryWrapper.in(MajorDO::getDepartmentUuid, departmentUuids);
            }
        }
        // 专业名称模糊查询
        if (CharSequenceUtil.isNotBlank(name)) {
            queryWrapper.like(MajorDO::getMajorName, name);
        }
        // 排序逻辑（默认按创建时间排序）
        queryWrapper.orderBy(isDesc == null || isDesc, false, MajorDO::getCreatedAt);

        // 执行分页查询并转换结果
        Page<MajorDO> resultPage = majorDAO.page(pageParam, queryWrapper);
        List<T> dtoList = resultPage.getRecords().stream().map(converter).toList();

        return new PageDTO<>(dtoList, resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
    }

}



