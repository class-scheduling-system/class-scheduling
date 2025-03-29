package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.AcademicAffairsPermissionDTO;

/**
 * 教务权限服务
 *
 * @author FLASHLACK
 */
public interface AcademicAffairsPermissionService {
    /**
     * 获取教务权限
     *
     * @param userUuid 用户UUID
     * @return 教务权限实体
     */
    AcademicAffairsPermissionDTO getAcademicAffairsPermission(String userUuid);
}
