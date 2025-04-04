package com.frontleaves.scheduling.services;

import com.frontleaves.scheduling.models.dto.base.CreditHourTypeDTO;
import org.jetbrains.annotations.NotNull;

/**
 * 学时类型服务接口
 * @author FLASHLACK
 */
public interface CreditHourTypeService {
    /**
     * 根据学时类型UUID获取学时类型
     * @param creditHourType 学时类型UUID
     * @return 学时类型
     */
    @NotNull
    CreditHourTypeDTO getCreditHourTypeByUuid(String creditHourType);
}
