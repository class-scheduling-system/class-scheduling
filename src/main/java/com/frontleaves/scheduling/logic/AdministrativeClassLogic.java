package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.lite.ClassLiteDTO;
import com.frontleaves.scheduling.models.entity.base.AdministrativeClassDO;
import com.frontleaves.scheduling.services.AdministrativeClassService;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

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
    private final CourseLibraryService courseLibraryService;

    /**
     * 根据班级标识获取班级映射信息
     * <p>
     * 此方法用于从给定的班级标识中获取对应的班级映射信息，包括年级、院系和专业UUID
     * 它首先通过调用DAO层的方法获取行政班数据，然后检查数据是否存在
     * 如果不存在，抛出业务异常；如果存在，则构建并返回包含映射信息的DTO对象
     * </p>
     *
     * @param clazz 班级标识，用于查询班级映射信息
     * @return ClassLiteDTO 包含班级映射信息的DTO对象
     * @throws BusinessException 当行政班级信息不存在时抛出的业务异常
     */
    @Override
    public ClassLiteDTO getClassMappingByClazz(String clazz) {
        // 从DAO层获取行政班数据
        AdministrativeClassDO administrativeClassDO = administrativeClassDAO.getAdministrativeClassMappingByClazz(clazz);
        // 判断对应班级信息是否存在
        if (administrativeClassDO == null) {
            throw new BusinessException("行政班级信息不存在", ErrorCode.NOT_EXIST);
        }
        // 构建并返回班级映射信息对象
        return new ClassLiteDTO()
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

    @Override
    public List<String> getClassNameByGroup(@NotNull List<AdministrativeClassDTO> classGroup) {
        //检查内为uuid格式还是DTO格式
        if (classGroup.isEmpty()) {
            throw new BusinessException("行政班级信息不能为空", ErrorCode.NOT_EXIST);
        }
        for (AdministrativeClassDTO administrativeClassDTO : classGroup) {
            String uuid = administrativeClassDTO.getAdministrativeClassUuid();
            if (uuid == null) {
                throw new BusinessException("行政班级信息格式错误", ErrorCode.NOT_EXIST);
            }
            if (!isValidUuid(uuid)) {
                return List.of(courseLibraryService.getCourseLibraryByUuid(uuid).getName()) ;
            }else {
                // 如果是UUID格式，则查询对应的班级名称
                AdministrativeClassDO administrativeClassDO = administrativeClassDAO.getAdministrativeClassByUuid(uuid);
                if (administrativeClassDO == null) {
                    throw new BusinessException("获取班级用户名时，行政班级信息不存在", ErrorCode.NOT_EXIST);
                }
                return List.of(administrativeClassDO.getClassName());
            }
        }
        return List.of();
    }

    @Override
    public AdministrativeClassDTO getClassByUuid(String uuid) {
        AdministrativeClassDO administrativeClassDO = administrativeClassDAO.getAdministrativeClassByUuid(uuid);
        if (administrativeClassDO == null) {
            throw new BusinessException("获取班级用户名时，行政班级信息不存在", ErrorCode.NOT_EXIST);
        }
        return BeanUtil.toBean(administrativeClassDO, AdministrativeClassDTO.class);
    }

    /**
     * 验证给定的字符串是否符合 UUID 的正则表达式格式
     * 此方法使用正则表达式来检查字符串的格式是否与 UUID 标准匹配
     * UUID 标准格式为 8-4-4-4-12 的 32 位十六进制数字
     * @param uuid 待验证的字符串
     * @return 如果字符串是有效的 UUID，则返回 true；否则返回 false
     */
    public static boolean isValidUuid(String uuid) {
        // 检查字符串是否非空，并且匹配 UUID 的正则表达式
        return uuid != null && Pattern.matches(StringConstant.Regular.UUID_REGULAR_EXPRESSION, uuid);
    }
}
