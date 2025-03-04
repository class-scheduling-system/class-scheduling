package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.BuildingDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.UnitCategoryDAO;
import com.frontleaves.scheduling.daos.UnitTypeDAO;
import com.frontleaves.scheduling.models.dto.DepartmentDTO;
import com.frontleaves.scheduling.models.entity.BuildingDO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.UnitCategoryDO;
import com.frontleaves.scheduling.models.entity.UnitTypeDO;
import com.frontleaves.scheduling.models.vo.DepartmentAddVO;
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

    @Override
    public DepartmentDTO addDepartment(@NotNull DepartmentAddVO departmentAddVO) {
        // 数据检查
        //检查单位类型
        UnitCategoryDO getUnitCategory = unitCategoryDAO.getUnitCategoryByUuid(departmentAddVO.getUnitCategory());
        if (getUnitCategory == null) {
            // 抛出异常
            throw new BusinessException("单位类别不存在", ErrorCode.NOT_EXIST);
        }

        //检查单位办别
        UnitTypeDO getUnitType = unitTypeDAO.getUnitTypeByUuid(departmentAddVO.getUnitType());
        if (getUnitType == null) {
            // 抛出异常
            throw new BusinessException("单位办别不存在", ErrorCode.NOT_EXIST);
        }

        //检查教学楼
        BuildingDO getBuilding = buildingDAO.getBuildingByUuid(departmentAddVO.getAssignedTeachingBuilding());
        if (getBuilding == null) {
            // 抛出异常
            throw new BusinessException("教学楼不存在", ErrorCode.NOT_EXIST);
        }

        //检查上级部门
        DepartmentDO getParentDepartment = departmentDAO.getDepartmentByUuid(departmentAddVO.getParentDepartment());
        if (getParentDepartment == null) {
            // 抛出异常
            throw new BusinessException("上级部门不存在", ErrorCode.NOT_EXIST);
        }

        // 数据拷贝
        DepartmentDO departmentDO = new DepartmentDO();
        // 数据拷贝
        BeanUtil.copyProperties(departmentAddVO, departmentDO);
        departmentDO.setDepartmentUuid(UuidUtil.generateUuidNoDash());

        // 保存数据
        departmentDAO.save(departmentDO);

        // 取出数据
        DepartmentDO getNewDepartment = departmentDAO.getDepartmentByUuid(departmentDO.getDepartmentUuid());
        return BeanUtil.toBean(getNewDepartment, DepartmentDTO.class);
    }

    @Override
    public DepartmentDTO getDepartment(@NotNull String departmentUuid) {
       DepartmentDO departmentDTO = null;
       if (departmentUuid.matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION)) {
           departmentDTO = departmentDAO.getDepartmentByUuid(departmentUuid);
       }
        if (departmentDTO == null) {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }
         return BeanUtil.toBean(departmentDTO, DepartmentDTO.class);
    }

    @Override
    public void deleteDepartment(String departmentUuid) throws BusinessException {
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);
        if (departmentDO != null) {
            departmentDAO.deleteDepartment(departmentDO);
        } else {
            throw new BusinessException("部门不存在", ErrorCode.NOT_EXIST);
        }
    }

    @Override
    public DepartmentDTO updateDepartment(String departmentUuid, DepartmentAddVO departmentAddVO) {
        DepartmentDO departmentDO = departmentDAO.getDepartmentByUuid(departmentUuid);
        if (departmentDO != null) {
            BeanUtil.copyProperties(departmentAddVO, departmentDO);
            departmentDAO.updateDepartment(departmentDO);
            return BeanUtil.toBean(departmentDO, DepartmentDTO.class);
        }
        return null;
    }

    @Override
    public PageDTO<DepartmentDTO> getDepartmentList(int page, int size, boolean isDesc,String name) {
        Page<DepartmentDO> departmentList = departmentDAO.getDepartmentList(page,size,isDesc,name);
        if (departmentList.getTotal() == 0) {
            return new PageDTO<>();
        }else {
            return ProjectUtil.convertPageToPageDTO(departmentList, DepartmentDTO.class);
        }

    }
}
