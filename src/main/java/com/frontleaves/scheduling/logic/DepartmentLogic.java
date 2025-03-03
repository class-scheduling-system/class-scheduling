package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
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
}
