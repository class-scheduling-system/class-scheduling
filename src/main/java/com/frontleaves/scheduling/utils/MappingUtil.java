package com.frontleaves.scheduling.utils;

import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.models.dto.ClassMappingDTO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author fanfan187
 * @version v1.0.0
 * @since v1.0.0
 */
@Component
public class MappingUtil {
    private final AdministrativeClassDAO administrativeClassDAO;

    @Autowired
    public MappingUtil(AdministrativeClassDAO administrativeClassDAO) {
        this.administrativeClassDAO = administrativeClassDAO;
    }


    /**
     * 根据班级UUID获取班级映射信息
     * <p>
     * 此方法通过查询行政班级表来获取与给定班级UUID对应的班级信息,并将相关信息映射到ClassMappingDTO对象中
     * 如果没有找到对应的班级信息,则抛出业务异常
     * </p>
     *
     * @param clazz 班级UUID,用于唯一标识一个班级
     * @return ClassMappingDTO 包含班级映射信息的对象,包括年级UUID、院系UUID和专业UUID
     * @throws BusinessException 当未找到对应班级信息时抛出
     */
    public ClassMappingDTO getClassMappingByClazz(String clazz) {
        // 查询行政班级表,寻找与给定班级UUID匹配的记录
        AdministrativeClassDO adminClass = administrativeClassDAO.lambdaQuery()
                .eq(AdministrativeClassDO::getAdministrativeClassUuid, clazz)
                .one();

        // 如果没有找到对应的班级信息,抛出业务异常
        if (adminClass == null) {
            throw new BusinessException("未找到对应班级信息", ErrorCode.NOT_EXIST);
        }

        // 创建一个班级映射信息对象,并设置其属性
        ClassMappingDTO classMappingDTO = new ClassMappingDTO();
        classMappingDTO.setGradeUuid(adminClass.getGradeUuid())
                .setDepartmentUuid(adminClass.getDepartmentUuid())
                .setMajorUuid(adminClass.getMajorUuid());

        return classMappingDTO;
    }
}
