package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.CreditHourTypeDAO;
import com.frontleaves.scheduling.models.dto.scheduling.CreditHourTypeEnuDTO;
import com.frontleaves.scheduling.models.entity.CreditHourTypeDO;
import com.frontleaves.scheduling.services.CreditHourTypeService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

/**
 * 学时类型逻辑
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditHourTypeLogic implements CreditHourTypeService {
    private final CreditHourTypeDAO creditHourTypeDAO;
    @Override
    public @NotNull CreditHourTypeEnuDTO getCreditHourTypeByUuid(String creditHourType) {
        CreditHourTypeDO creditHourTypeDO = creditHourTypeDAO.getCreditHourTypeByUuid(creditHourType);
        if (creditHourTypeDO == null){
            throw new BusinessException("学时类型不存在", ErrorCode.BODY_ERROR);
        }
        return BeanUtil.toBean(creditHourTypeDO, CreditHourTypeEnuDTO.class);
    }
}
