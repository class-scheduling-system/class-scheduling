package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.AcademicAffairsPermissionDAO;
import com.frontleaves.scheduling.models.entity.AcademicAffairsPermissionDO;
import com.frontleaves.scheduling.services.AcademicAffairsPermissionService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 教务权限逻辑
 * @author FLASHLACK
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AcademicAffairsPermissionLogic implements AcademicAffairsPermissionService {
    private final AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    /**
     * 根据用户UUID获取教务权限信息
     * 当通过用户的UUID无法找到对应的教务权限信息时，抛出业务异常，指示权限未设置
     * 这个方法主要用于验证用户是否有教务管理的权限
     * @param userUuid 用户的唯一标识符，用于查询教务权限
     * @return 返回用户的教务权限信息对象，包含具体的权限详情
     * @throws BusinessException 当用户没有设置教务权限时抛出此异常
     */
    @Override
    public AcademicAffairsPermissionDO getAcademicAffairsPermission(String userUuid) {
        // 根据用户UUID从数据库中获取教务权限信息
        AcademicAffairsPermissionDO academicAffairsPermissionDO =
                academicAffairsPermissionDAO.getAcademicAffairsPermissionByUserUuid(userUuid);

        // 检查获取的教务权限信息是否为空，如果为空则抛出异常
        if (academicAffairsPermissionDO == null) {
            throw new BusinessException("此用户教务权限并未设置", ErrorCode.OPERATION_ERROR);
        }

        // 返回获取的教务权限信息
        return academicAffairsPermissionDO;
    }
}
