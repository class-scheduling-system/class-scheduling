package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.models.dto.ClassMappingDTO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.frontleaves.scheduling.services.AdministrativeClassService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdministrativeClassLogic implements AdministrativeClassService {

    private final AdministrativeClassDAO administrativeClassDAO;

    /**
     * 根据班级标识获取班级映射信息
     * <p>
     * 此方法用于从给定的班级标识中获取对应的班级映射信息，包括年级、院系和专业UUID
     * 它首先通过调用DAO层的方法获取行政班数据，然后检查数据是否存在
     * 如果不存在，抛出业务异常；如果存在，则构建并返回包含映射信息的DTO对象
     * </p>
     *
     * @param clazz 班级标识，用于查询班级映射信息
     * @return ClassMappingDTO 包含班级映射信息的DTO对象
     * @throws BusinessException 当行政班级信息不存在时抛出的业务异常
     */
    @Override
    public ClassMappingDTO getClassMappingByClazz(String clazz) {
        // 从DAO层获取行政班数据
        AdministrativeClassDO administrativeClassDO = administrativeClassDAO.getAdministrativeClassMappingByClazz(clazz);
        // 判断对应班级信息是否存在
        if (administrativeClassDO == null) {
            throw new BusinessException("行政班级信息不存在", ErrorCode.NOT_EXIST);
        }
        // 构建并返回班级映射信息对象
        return new ClassMappingDTO()
                .setGradeUuid(administrativeClassDO.getGradeUuid())
                .setDepartmentUuid(administrativeClassDO.getDepartmentUuid())
                .setMajorUuid(administrativeClassDO.getMajorUuid());
    }

    /**
     * 获取行政班级列表
     * <p>
     * 本方法通过调用DAO层的行政班级列表获取方法来获取所有行政班级的信息，并进行非空校验
     * 如果列表为空，则抛出业务异常，指示行政班级信息不存在
     * </p>
     *
     * @return 行政班级列表，包含所有行政班级的信息
     * @throws BusinessException 如果行政班级信息不存在，则抛出此异常
     */
    @Override
    public List<AdministrativeClassDO> getAdministrativeClassList() {
        // 从DAO层获取行政班数据
        List<AdministrativeClassDO> classList = administrativeClassDAO.getAdministrativeClassList();
        // 判断对应班级信息是否存在
        if (classList == null) {
            throw new BusinessException("行政班级信息不存在", ErrorCode.NOT_EXIST);
        }
        return classList;
    }
}
