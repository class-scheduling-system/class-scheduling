package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.entity.BuildingDO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.DepartmentVO;
import com.frontleaves.scheduling.services.DepartmentService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.UuidUtil;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


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
        BuildingDO getBuilding = buildingDAO.getBuildingByUuid(departmentVO.getAssignedTeachingBuilding());
        if (getBuilding == null) {
            // 抛出异常
            throw new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST);
        }

        //检查上级部门
        DepartmentDO getParentDepartment = departmentDAO.getDepartmentByUuid(departmentVO.getParentDepartment());
        if (getParentDepartment == null) {
            // 抛出异常
            throw new BusinessException("上级部门不存在", ErrorCode.NOT_EXIST);
        }

        // 数据拷贝
        DepartmentDO departmentDO = new DepartmentDO();
        // 数据拷贝
        BeanUtil.copyProperties(departmentVO, departmentDO);
        departmentDO.setDepartmentUuid(UuidUtil.generateUuidNoDash());

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
       DepartmentDO departmentDTO = null;

       // 验证部门UUID格式是否正确，确保UUID没有破折号
       if (departmentUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
           // 如果UUID格式正确，则调用DAO方法获取部门信息
           departmentDTO = departmentDAO.getDepartmentByUuid(departmentUuid);
       }

       // 如果departmentDTO为空，说明没有找到对应的部门信息
       if (departmentDTO == null) {
           // 抛出业务异常，提示部门不存在，并使用预定义的错误码
           throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
       }

       // 将获取到的部门信息转换为DTO对象并返回
       return BeanUtil.toBean(departmentDTO, DepartmentDTO.class);
    }

    /**
     * 删除指定的部门
     *
     * @param departmentUuid 部门的唯一标识符
     * @throws BusinessException 如果部门不存在，则抛出业务异常
     */
    @Override
    public void deleteDepartment(String departmentUuid) throws BusinessException {
        // 根据UUID获取部门对象
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);

        // 判断部门是否存在
        if (departmentDO != null) {
            // 如果部门存在，则调用DAO层删除部门
            departmentDAO.deleteDepartment(departmentDO);
        } else {
            // 如果部门不存在，则抛出业务异常，提示用户错误信息
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
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
     * @param departmentVO 包含更新信息的部门视图对象
     * @return 更新后的部门数据传输对象，如果部门不存在则返回null
     */
    @Override
    public DepartmentDTO updateDepartment(String departmentUuid, DepartmentVO departmentVO) {
        // 根据UUID获取部门数据对象
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);
        if (departmentDO != null) {
            // 验证单位类别是否存在
            if (departmentVO.getUnitCategory() != null) {
                UnitCategoryDO getUnitCategory = unitCategoryDAO.getUnitCategoryByUuid(departmentVO.getUnitCategory());
                if (getUnitCategory == null) {
                    // 抛出异常
                    throw new BusinessException("单位类别不存在", ErrorCode.NOT_EXIST);
                }
            }
            // 验证单位办别是否存在
            if (departmentVO.getUnitType() != null) {
                UnitTypeDO getUnitType = unitTypeDAO.getUnitTypeByUuid(departmentVO.getUnitType());
                if (getUnitType == null) {
                    // 抛出异常
                    throw new BusinessException("单位办别不存在", ErrorCode.NOT_EXIST);
                }
            }
            // 验证教学楼是否存在
            if (departmentVO.getAssignedTeachingBuilding() != null) {
                BuildingDO getBuilding = buildingDAO.getBuildingByUuid(departmentVO.getAssignedTeachingBuilding());
                if (getBuilding == null) {
                    // 抛出异常
                    throw new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST);
                }
            }
            // 验证上级部门是否存在
            if (departmentVO.getParentDepartment() != null) {
                DepartmentDO getParentDepartment = departmentDAO.getDepartmentByUuid(departmentVO.getParentDepartment());
                if (getParentDepartment == null) {
                    // 抛出异常
                    throw new BusinessException("上级部门不存在", ErrorCode.NOT_EXIST);
                }
            }
            // 将视图对象属性复制到数据对象并更新数据库
            BeanUtil.copyProperties(departmentVO, departmentDO);
            departmentDAO.updateDepartment(departmentDO);
            // 返回更新后的部门数据传输对象
            return BeanUtil.toBean(departmentDO, DepartmentDTO.class);
        }
        // 如果部门不存在，返回null
        return null;
    }

    /**
     * 获取部门列表
     *
     * @param page     页码，表示请求的数据页数
     * @param size     每页数量，表示每页包含的部门数量
     * @param isDesc   是否降序，用于指定排序方式
     * @param name     部门名称，用于模糊搜索
     * @return         返回一个包含部门信息的PageDTO对象
     *
     * 此方法调用departmentDAO来获取部门列表，并根据查询参数进行分页和排序
     * 如果查询结果为空，则返回一个空的PageDTO对象；否则，将查询结果转换为DTO形式并返回
     */
    @Override
    public PageDTO<DepartmentDTO> getDepartmentList(int page, int size, boolean isDesc, String name) {
        // 调用DAO层方法获取部门列表
        Page<DepartmentDO> departmentList = departmentDAO.getDepartmentList(page,size,isDesc,name);

        // 检查查询结果是否为空
        if (departmentList.getTotal() == 0) {
            // 如果为空，返回一个新的空PageDTO对象
            return new PageDTO<>();
        }else {
            // 如果不为空，将查询结果转换为DTO形式并返回
            return ProjectUtil.convertPageToPageDTO(departmentList, DepartmentDTO.class);
        }
    }
}
