package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.lite.ClassLiteDTO;
import com.frontleaves.scheduling.models.entity.base.AdministrativeClassDO;
import com.frontleaves.scheduling.services.AdministrativeClassService;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 行政班级逻辑实现类
 * <p>
 * 实现了行政班级服务接口，提供创建、修改、删除、查询等功能
 * </p>
 *
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
     * 创建行政班级
     * <p>
     * 该方法用于创建新的行政班级记录，会生成UUID并设置创建时间
     * </p>
     *
     * @param administrativeClassDTO 行政班级信息DTO
     * @return 创建成功的行政班级DTO
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdministrativeClassDTO createAdministrativeClass(AdministrativeClassDTO administrativeClassDTO) {
        log.debug("创建行政班级: {}", administrativeClassDTO);

        // 参数校验
        validateAdministrativeClassDTO(administrativeClassDTO);

        // 检查班级编号唯一性
        checkClassCodeUnique(administrativeClassDTO.getClassCode(), null);

        // 检查班级名称唯一性
        checkClassNameUnique(administrativeClassDTO.getClassName(), null);

        // DTO -> DO
        AdministrativeClassDO administrativeClassDO = new AdministrativeClassDO();
        BeanUtil.copyProperties(administrativeClassDTO, administrativeClassDO);

        // 设置UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        administrativeClassDO.setAdministrativeClassUuid(uuid);

        // 保存到数据库
        boolean success = administrativeClassDAO.save(administrativeClassDO);
        if (!success) {
            throw new BusinessException("行政班级创建失败", ErrorCode.OPERATION_FAILED);
        }

        // DO -> DTO
        AdministrativeClassDTO resultDTO = new AdministrativeClassDTO();
        BeanUtil.copyProperties(administrativeClassDO, resultDTO);

        return resultDTO;
    }

    /**
     * 修改行政班级
     * <p>
     * 更新指定UUID的行政班级信息
     * </p>
     *
     * @param administrativeClassUuid 行政班级UUID
     * @param administrativeClassDTO 行政班级新信息
     * @return 更新后的行政班级DTO
     * @throws BusinessException 当行政班级不存在或操作失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdministrativeClassDTO updateAdministrativeClass(String administrativeClassUuid, AdministrativeClassDTO administrativeClassDTO) throws BusinessException {
        log.debug("修改行政班级: uuid={}, dto={}", administrativeClassUuid, administrativeClassDTO);

        // 查询现有班级信息
        AdministrativeClassDO existingClass = administrativeClassDAO.getAdministrativeClassByUuid(administrativeClassUuid);
        if (existingClass == null) {
            throw new BusinessException("行政班级不存在", ErrorCode.NOT_EXIST);
        }

        // 参数校验
        validateAdministrativeClassDTO(administrativeClassDTO);

        // 检查班级编号唯一性（排除自身）
        if (!StrUtil.equals(existingClass.getClassCode(), administrativeClassDTO.getClassCode())) {
            checkClassCodeUnique(administrativeClassDTO.getClassCode(), administrativeClassUuid);
        }

        // 检查班级名称唯一性（排除自身）
        if (!StrUtil.equals(existingClass.getClassName(), administrativeClassDTO.getClassName())) {
            checkClassNameUnique(administrativeClassDTO.getClassName(), administrativeClassUuid);
        }

        // 更新属性
        AdministrativeClassDO updateDO = new AdministrativeClassDO();
        BeanUtil.copyProperties(administrativeClassDTO, updateDO);
        updateDO.setAdministrativeClassUuid(administrativeClassUuid);

        // 执行更新
        boolean success = administrativeClassDAO.updateById(updateDO);
        if (!success) {
            throw new BusinessException("行政班级更新失败", ErrorCode.OPERATION_FAILED);
        }

        // 获取更新后的数据
        AdministrativeClassDO updatedClass = administrativeClassDAO.getById(administrativeClassUuid);

        // DO -> DTO
        AdministrativeClassDTO resultDTO = new AdministrativeClassDTO();
        BeanUtil.copyProperties(updatedClass, resultDTO);

        return resultDTO;
    }

    /**
     * 删除行政班级
     * <p>
     * 根据UUID删除行政班级记录
     * </p>
     *
     * @param administrativeClassUuid 行政班级UUID
     * @throws BusinessException 当行政班级不存在或删除失败时抛出
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdministrativeClass(String administrativeClassUuid) throws BusinessException {
        log.debug("删除行政班级: {}", administrativeClassUuid);

        // 查询是否存在
        AdministrativeClassDO existingClass = administrativeClassDAO.getById(administrativeClassUuid);
        if (existingClass == null) {
            throw new BusinessException("行政班级不存在", ErrorCode.NOT_EXIST);
        }

        // TODO: 检查是否有关联的学生、排课等数据，如有则不允许删除

        // 执行删除
        boolean success = administrativeClassDAO.removeById(administrativeClassUuid);
        if (!success) {
            throw new BusinessException("行政班级删除失败", ErrorCode.OPERATION_FAILED);
        }
    }

    /**
     * 获取行政班级详情
     * <p>
     * 根据UUID获取行政班级的详细信息
     * </p>
     *
     * @param administrativeClassUuid 行政班级UUID
     * @return 行政班级DTO
     * @throws BusinessException 当行政班级不存在时抛出
     */
    @Override
    public AdministrativeClassDTO getAdministrativeClass(String administrativeClassUuid) throws BusinessException {
        log.debug("查询行政班级详情: {}", administrativeClassUuid);

        // 查询行政班级
        AdministrativeClassDO administrativeClassDO = administrativeClassDAO.getAdministrativeClassByUuid(administrativeClassUuid);
        if (administrativeClassDO == null) {
            throw new BusinessException("行政班级不存在", ErrorCode.NOT_EXIST);
        }

        // DO -> DTO
        AdministrativeClassDTO administrativeClassDTO = new AdministrativeClassDTO();
        BeanUtil.copyProperties(administrativeClassDO, administrativeClassDTO);

        return administrativeClassDTO;
    }

    /**
     * 管理员查询行政班级列表（分页）
     * <p>
     * 提供给管理员使用的行政班级分页查询，支持按部门、专业和名称筛选
     * </p>
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序
     * @param departmentUuid 部门UUID筛选条件
     * @param majorUuid 专业UUID筛选条件
     * @param name 班级名称筛选条件
     * @return 分页后的行政班级DTO列表
     */
    @Override
    public PageDTO<AdministrativeClassDTO> listAdministrativeClassForAdmin(int page, int size, Boolean isDesc,
                                                 String departmentUuid, String majorUuid, String name) {
        log.debug("管理员查询行政班级列表: page={}, size={}, isDesc={}, departmentUuid={}, majorUuid={}, name={}",
                page, size, isDesc, departmentUuid, majorUuid, name);

        // 构建查询条件
        LambdaQueryWrapper<AdministrativeClassDO> queryWrapper = buildBaseQuery(departmentUuid, majorUuid, name);

        // 设置排序
        if (isDesc != null && isDesc) {
            queryWrapper.orderByDesc(AdministrativeClassDO::getCreatedAt);
        } else {
            queryWrapper.orderByAsc(AdministrativeClassDO::getCreatedAt);
        }

        // 执行分页查询
        Page<AdministrativeClassDO> pageResult = administrativeClassDAO.page(new Page<>(page, size), queryWrapper);

        // 转换结果
        return ProjectUtil.convertPageToPageDTO(pageResult, AdministrativeClassDTO.class);
    }

    /**
     * 教务人员查询行政班级列表（分页）
     * <p>
     * 提供给教务人员使用的行政班级分页查询，支持按部门、专业和名称筛选
     * </p>
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序
     * @param departmentUuid 部门UUID筛选条件
     * @param majorUuid 专业UUID筛选条件
     * @param name 班级名称筛选条件
     * @return 分页后的行政班级DTO列表
     */
    @Override
    public PageDTO<AdministrativeClassDTO> listAdministrativeClassForAcademic(int page, int size, Boolean isDesc,
                                                   String departmentUuid, String majorUuid, String name) {
        log.debug("教务查询行政班级列表: page={}, size={}, isDesc={}, departmentUuid={}, majorUuid={}, name={}",
                page, size, isDesc, departmentUuid, majorUuid, name);

        // 构建查询条件
        LambdaQueryWrapper<AdministrativeClassDO> queryWrapper = buildBaseQuery(departmentUuid, majorUuid, name);

        // 设置排序
        if (isDesc != null && isDesc) {
            queryWrapper.orderByDesc(AdministrativeClassDO::getCreatedAt);
        } else {
            queryWrapper.orderByAsc(AdministrativeClassDO::getCreatedAt);
        }

        // 执行分页查询
        Page<AdministrativeClassDO> pageResult = administrativeClassDAO.page(new Page<>(page, size), queryWrapper);

        // 转换结果
        return ProjectUtil.convertPageToPageDTO(pageResult, AdministrativeClassDTO.class);
    }

    /**
     * 学生查询行政班级列表（分页）
     * <p>
     * 提供给学生使用的行政班级分页查询，支持按部门、专业和名称筛选
     * </p>
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序
     * @param departmentUuid 部门UUID筛选条件
     * @param majorUuid 专业UUID筛选条件
     * @param name 班级名称筛选条件
     * @return 分页后的行政班级DTO列表
     */
    @Override
    public PageDTO<AdministrativeClassDTO> listAdministrativeClassForStudent(int page, int size, Boolean isDesc,
                                                   String departmentUuid, String majorUuid, String name) {
        log.debug("学生查询行政班级列表: page={}, size={}, isDesc={}, departmentUuid={}, majorUuid={}, name={}",
                page, size, isDesc, departmentUuid, majorUuid, name);

        // 构建查询条件
        LambdaQueryWrapper<AdministrativeClassDO> queryWrapper = buildBaseQuery(departmentUuid, majorUuid, name);

        // 只显示启用的记录
        queryWrapper.eq(AdministrativeClassDO::getIsEnabled, true);

        // 设置排序
        if (isDesc != null && isDesc) {
            queryWrapper.orderByDesc(AdministrativeClassDO::getCreatedAt);
        } else {
            queryWrapper.orderByAsc(AdministrativeClassDO::getCreatedAt);
        }

        // 执行分页查询
        Page<AdministrativeClassDO> pageResult = administrativeClassDAO.page(new Page<>(page, size), queryWrapper);

        // 转换结果
        return ProjectUtil.convertPageToPageDTO(pageResult, AdministrativeClassDTO.class);
    }

    /**
     * 获取所有行政班级列表(不分页)
     * <p>
     * 获取所有行政班级的简单列表，常用于下拉选择框等场景
     * </p>
     *
     * @return 所有行政班级DTO列表
     */
    @Override
    public List<AdministrativeClassDTO> listAllAdministrativeClass() {
        log.debug("获取所有行政班级列表");

        // 获取所有启用的行政班级
        List<AdministrativeClassDO> classList = administrativeClassDAO.lambdaQuery()
                .eq(AdministrativeClassDO::getIsEnabled, true)
                .orderByAsc(AdministrativeClassDO::getClassName)
                .list();

        // 转换为DTO列表
        return classList.stream()
                .map(classDO -> {
                    AdministrativeClassDTO dto = new AdministrativeClassDTO();
                    BeanUtil.copyProperties(classDO, dto);
                    return dto;
                })
                .toList();
    }

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
        List<String> result = new ArrayList<>();

        for (AdministrativeClassDTO administrativeClassDTO : classGroup) {
            String uuid = administrativeClassDTO.getAdministrativeClassUuid();
            if (uuid == null) {
                throw new BusinessException("行政班级信息格式错误", ErrorCode.NOT_EXIST);
            }
            if (!isValidUuid(uuid)) {
                result.add(courseLibraryService.getCourseLibraryByUuid(uuid).getName());
            } else {
                // 如果是UUID格式，则查询对应的班级名称
                AdministrativeClassDO administrativeClassDO = administrativeClassDAO.getAdministrativeClassByUuid(uuid);
                if (administrativeClassDO == null) {
                    throw new BusinessException("获取班级用户名时，行政班级信息不存在", ErrorCode.NOT_EXIST);
                }
                result.add(administrativeClassDO.getClassName());
            }
        }
        return result;
    }

    @Override
    public @NotNull AdministrativeClassDTO getClassByUuid(String uuid) {
        AdministrativeClassDO administrativeClassDO = administrativeClassDAO.getAdministrativeClassByUuid(uuid);
        if (administrativeClassDO == null) {
            throw new BusinessException("获取班级用户名时，行政班级信息不存在", ErrorCode.NOT_EXIST);
        }
        AdministrativeClassDTO dto = new AdministrativeClassDTO();
        BeanUtil.copyProperties(administrativeClassDO, dto);
        return dto;
    }

    // 私有辅助方法

    /**
     * 校验行政班级DTO的必填字段
     *
     * @param administrativeClassDTO 行政班级DTO
     * @throws BusinessException 当必填字段缺失时抛出
     */
    private void validateAdministrativeClassDTO(AdministrativeClassDTO administrativeClassDTO) {
        if (administrativeClassDTO == null) {
            throw new BusinessException("行政班级信息不能为空", ErrorCode.PARAMETER_ERROR);
        }

        if (StrUtil.isBlank(administrativeClassDTO.getDepartmentUuid())) {
            throw new BusinessException("所属部门不能为空", ErrorCode.PARAMETER_ERROR);
        }

        if (StrUtil.isBlank(administrativeClassDTO.getMajorUuid())) {
            throw new BusinessException("所属专业不能为空", ErrorCode.PARAMETER_ERROR);
        }

        if (StrUtil.isBlank(administrativeClassDTO.getClassCode())) {
            throw new BusinessException("班级编号不能为空", ErrorCode.PARAMETER_ERROR);
        }

        if (StrUtil.isBlank(administrativeClassDTO.getClassName())) {
            throw new BusinessException("班级名称不能为空", ErrorCode.PARAMETER_ERROR);
        }

        if (StrUtil.isBlank(administrativeClassDTO.getGradeUuid())) {
            throw new BusinessException("年级不能为空", ErrorCode.PARAMETER_ERROR);
        }
    }

    /**
     * 检查班级编号是否唯一
     *
     * @param classCode 班级编号
     * @param excludeUuid 排除的UUID（用于更新时排除自身）
     * @throws BusinessException 当班级编号已存在时抛出
     */
    private void checkClassCodeUnique(String classCode, String excludeUuid) {
        LambdaQueryWrapper<AdministrativeClassDO> queryWrapper = new LambdaQueryWrapper<AdministrativeClassDO>()
                .eq(AdministrativeClassDO::getClassCode, classCode);

        if (StrUtil.isNotBlank(excludeUuid)) {
            queryWrapper.ne(AdministrativeClassDO::getAdministrativeClassUuid, excludeUuid);
        }

        long count = administrativeClassDAO.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("班级编号已存在", ErrorCode.EXISTED);
        }
    }

    /**
     * 检查班级名称是否唯一
     *
     * @param className 班级名称
     * @param excludeUuid 排除的UUID（用于更新时排除自身）
     * @throws BusinessException 当班级名称已存在时抛出
     */
    private void checkClassNameUnique(String className, String excludeUuid) {
        LambdaQueryWrapper<AdministrativeClassDO> queryWrapper = new LambdaQueryWrapper<AdministrativeClassDO>()
                .eq(AdministrativeClassDO::getClassName, className);

        if (StrUtil.isNotBlank(excludeUuid)) {
            queryWrapper.ne(AdministrativeClassDO::getAdministrativeClassUuid, excludeUuid);
        }

        long count = administrativeClassDAO.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("班级名称已存在", ErrorCode.EXISTED);
        }
    }

    /**
     * 构建基础查询条件
     *
     * @param departmentUuid 部门UUID
     * @param majorUuid 专业UUID
     * @param name 班级名称
     * @return 查询条件
     */
    private LambdaQueryWrapper<AdministrativeClassDO> buildBaseQuery(String departmentUuid, String majorUuid, String name) {
        LambdaQueryWrapper<AdministrativeClassDO> queryWrapper = new LambdaQueryWrapper<>();

        // 部门筛选
        if (CharSequenceUtil.isNotBlank(departmentUuid)) {
            queryWrapper.eq(AdministrativeClassDO::getDepartmentUuid, departmentUuid);
        }

        // 专业筛选
        if (CharSequenceUtil.isNotBlank(majorUuid)) {
            queryWrapper.eq(AdministrativeClassDO::getMajorUuid, majorUuid);
        }

        // 名称模糊查询
        if (CharSequenceUtil.isNotBlank(name)) {
            queryWrapper.like(AdministrativeClassDO::getClassName, name);
        }

        return queryWrapper;
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
