package com.frontleaves.scheduling.logic;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.MajorDAO;
import com.frontleaves.scheduling.models.dto.base.MajorDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.entity.base.MajorDO;
import com.frontleaves.scheduling.services.MajorService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.frontleaves.scheduling.constants.StringConstant.Major.MAJOR_NOT_FOUND;

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
     */
    @Override
    public MajorDO updateMajor(String majorUuid, MajorDO majorDTO) {
        // 先从缓存中获取数据;如果没有,再从数据库中获取
        MajorDO majorDO = majorDAO.getMajorByUuid(majorUuid);
        // 验证专业存在
        if (majorDO == null) {
            throw new BusinessException(MAJOR_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        // 把 DTO 里的变动字段覆盖上去,排除主键、创建时间、更新时间字段
        BeanUtils.copyProperties(majorDTO, majorDO, "majorUuid", "createAt", "updatedAt");

        // 调用 DAO 更新数据
        boolean updatedSuccess = majorDAO.updateById(majorDO);
        if (!updatedSuccess) {
            throw new BusinessException("专业更新失败", ErrorCode.OPERATION_FAILED);
        }

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
     */
    @Override
    public void deleteMajor(String majorUuid) {
        // 检查专业是否存在
        MajorDO majorDO = majorDAO.getMajorByUuid(majorUuid);
        if (majorDO == null) {
            throw new BusinessException(MAJOR_NOT_FOUND, ErrorCode.NOT_EXIST);
        }

        majorDAO.isReferenced(majorUuid);

        // 执行删除
        boolean removed = majorDAO.removeById(majorUuid);
        if (!removed) {
            throw new IllegalStateException("专业删除失败");
        }
    }

    /**
     * 获取专业信息
     *
     * @param majorUuid 专业UUID，用于唯一标识一个专业
     * @return MajorDTO 专业信息的DTO对象，包含专业的主要信息
     */
    @Override
    public MajorDTO getMajor(String majorUuid) {
        MajorDO majorDO = majorDAO.getMajorByUuid(majorUuid);
        if (majorDO == null) {
            throw new BusinessException(MAJOR_NOT_FOUND, ErrorCode.NOT_EXIST);
        }
        // 将实体转换为 DTO
        MajorDTO majorDTO = new MajorDTO();
        BeanUtils.copyProperties(majorDO, majorDTO);
        return majorDTO;
    }

    /**
     * 管理员端获取专业列表
     * 该方法允许管理员根据分页参数、排序方式、所属院系和专业名称来获取专业信息列表
     *
     * @param page 页码,从1开始
     * @param size 每页记录数
     * @param isDesc 是否降序排序的标志,true表示降序,false表示升序
     * @param department 所属院系名称,可用于筛选
     * @param name 专业名称,可用于筛选
     * @return 返回一个PageDTO对象, 其中包含分页的专业信息
     */
    @Override
    public PageDTO<MajorDTO> listMajorsForAdmin(int page, int size, Boolean isDesc, String department, String name) {
        return getPageDTO(page, size, isDesc, department, name);
    }

    /**
     * 获取学术专业列表的分页信息
     * 此方法用于根据指定的分页参数、排序方式、院系和专业名称来获取专业信息的分页列表
     *
     * @param page     页码,从1开始,用于指定获取哪一页的数据
     * @param size     每页大小,用于指定每页包含的专业数量
     * @param isDesc   是否降序排列,用于指定专业列表的排序方式
     * @param department 院系名称,用于筛选属于特定院系的专业
     * @param name     专业名称,用于筛选名称中包含特定关键字的专业
     * @return 返回一个PageDTO对象, 其中包含根据给定参数筛选和排序后的专业信息列表
     */
    @Override
    public PageDTO<MajorDTO> listMajorsForAcademic(int page, int size, Boolean isDesc, String department, String name) {
        return getPageDTO(page, size, isDesc, department, name);
    }

    /**
     * 获取学生可选专业的分页列表
     * 此方法允许根据部门和专业名称筛选结果,并支持排序和分页
     *
     * @param page       页码,从1开始
     * @param size       每页的记录数
     * @param isDesc     是否降序排序的标志
     * @param department 部门名称,用于筛选专业
     * @param name       专业名称,用于进一步筛选结果
     * @return 返回一个包含专业信息的PageDTO对象
     */
    @Override
    public PageDTO<MajorDTO> listMajorsForStudent(int page, int size, Boolean isDesc, String department, String name) {
        return getPageDTO(page, size, isDesc, department, name);
    }

    /**
     * 获取专业信息的分页DTO
     *
     * @param page       页码,表示请求的是第几页数据
     * @param size       每页大小,即每页包含的专业信息数量
     * @param isDesc     是否降序排列,用于指定查询结果的排序方式
     * @param department 部门名称,用于筛选属于特定部门的专业
     * @param name       专业名称,用于筛选名称中包含特定关键字的专业
     * @return 返回一个PageDTO对象, 其中包含查询到的专业信息列表和分页相关数据
     */
    private PageDTO<MajorDTO> getPageDTO(int page, int size, Boolean isDesc, String department, String name) {
        Page<MajorDO> resultPage = majorDAO.listMajors(page, size, isDesc, department, name);
        List<MajorDTO> dtoList = resultPage.getRecords().stream().map(majorDO -> {
            MajorDTO majorDTO = new MajorDTO();
            BeanUtils.copyProperties(majorDO, majorDTO);
            return majorDTO;
        }).toList();
        return new PageDTO<>(dtoList, resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
    }
}
