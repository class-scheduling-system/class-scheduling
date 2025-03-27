package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.models.dto.SemesterDTO;
import com.frontleaves.scheduling.models.entity.SemesterDO;
import com.frontleaves.scheduling.services.SemesterService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

/**
 * 学期逻辑
 * @author FLASHLACK
 */
@Slf4j
@Service
@Repository
@RequiredArgsConstructor
public class SemesterLogic implements SemesterService {
    private final SemesterDAO semesterDAO;
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
