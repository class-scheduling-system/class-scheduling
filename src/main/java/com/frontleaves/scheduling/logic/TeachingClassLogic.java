package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.SemesterDAO;
import com.frontleaves.scheduling.daos.TeachingClassDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeachingClassDTO;
import com.frontleaves.scheduling.models.dto.lite.TeachingClassLiteDTO;
import com.frontleaves.scheduling.models.entity.base.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.base.DepartmentDO;
import com.frontleaves.scheduling.models.entity.base.SemesterDO;
import com.frontleaves.scheduling.models.entity.base.TeachingClassDO;
import com.frontleaves.scheduling.services.TeachingClassService;
import com.frontleaves.scheduling.utils.ProjectUtil;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 教学班服务实现类
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingClassLogic implements TeachingClassService {
    private final TeachingClassDAO teachingClassDAO;
    private final CourseLibraryDAO courseLibraryDAO;
    private final DepartmentDAO departmentDAO;
    private final SemesterDAO semesterDAO;

    @Override
    public List<TeachingClassDTO> getTeachingClassListBySemester(String semesterUuid) {
        List<TeachingClassDO> list = teachingClassDAO.getTeachingClassBySemester(semesterUuid);
        return list.stream()
                .map(teachingClassDO -> BeanUtil.toBean(teachingClassDO, TeachingClassDTO.class))
                .toList();
    }

    @Override
    public @NotNull TeachingClassDTO getTeachingClassByUuid(String teachingClassUuid) {
        TeachingClassDO teachingClassDO = teachingClassDAO.getTeachingClassByUuid(teachingClassUuid);
        if (teachingClassDO == null) {
            throw new BusinessException("教学班不存在", ErrorCode.NOT_EXIST);
        }
        return BeanUtil.toBean(teachingClassDO, TeachingClassDTO.class);
    }

    @Override
    public void save(TeachingClassDO teachingClassDO) {
        teachingClassDAO.save(teachingClassDO);
    }

    @Override
    public @Nullable TeachingClassDTO getTeachingClassByUuidNoError(String teachingClassUuid) {
        TeachingClassDO teachingClassDO = teachingClassDAO.getTeachingClassByUuid(teachingClassUuid);
        return BeanUtil.toBean(teachingClassDO, TeachingClassDTO.class);
    }

    @Override
    public PageDTO<TeachingClassLiteDTO> getTeachingClassList(int page, int size, String keyword, 
                                                          String departmentUuid, String semesterUuid, boolean isDesc) {
        log.debug("获取教学班列表: page={}, size={}, keyword={}, departmentUuid={}, semesterUuid={}, isDesc={}", 
                page, size, keyword, departmentUuid, semesterUuid, isDesc);
                
        // 构建查询条件
        LambdaQueryWrapper<TeachingClassDO> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加条件：按关键字查询（教学班名称或编号）
        if (StringUtils.hasText(keyword)) {
            queryWrapper.like(TeachingClassDO::getTeachingClassName, keyword)
                    .or()
                    .like(TeachingClassDO::getTeachingClassCode, keyword);
        }
        
        // 添加条件：按部门查询
        if (StringUtils.hasText(departmentUuid)) {
            queryWrapper.eq(TeachingClassDO::getCourseDepartmentUuid, departmentUuid);
        }
        
        // 添加条件：按学期查询
        if (StringUtils.hasText(semesterUuid)) {
            queryWrapper.eq(TeachingClassDO::getSemesterUuid, semesterUuid);
        }
        
        // 添加排序
        if (isDesc) {
            queryWrapper.orderByDesc(TeachingClassDO::getCreatedAt);
        } else {
            queryWrapper.orderByAsc(TeachingClassDO::getCreatedAt);
        }
        
        // 执行分页查询
        Page<TeachingClassDO> pageResult = teachingClassDAO.page(new Page<>(page, size), queryWrapper);
        
        // 判断是否有数据
        if (pageResult.getTotal() == 0) {
            return new PageDTO<>();
        }
        
        // 获取相关的课程、学期和部门信息以丰富结果
        List<String> courseUuids = pageResult.getRecords().stream()
                .map(TeachingClassDO::getCourseUuid)
                .distinct()
                .collect(Collectors.toList());
                
        List<String> semesterUuids = pageResult.getRecords().stream()
                .map(TeachingClassDO::getSemesterUuid)
                .distinct()
                .collect(Collectors.toList());
                
        List<String> departmentUuids = pageResult.getRecords().stream()
                .map(TeachingClassDO::getCourseDepartmentUuid)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询相关信息
        Map<String, CourseLibraryDO> courseMap = courseLibraryDAO.lambdaQuery()
                .in(CourseLibraryDO::getCourseLibraryUuid, courseUuids)
                .list()
                .stream()
                .collect(Collectors.toMap(CourseLibraryDO::getCourseLibraryUuid, Function.identity(), (k1, k2) -> k1));
                
        Map<String, SemesterDO> semesterMap = semesterDAO.lambdaQuery()
                .in(SemesterDO::getSemesterUuid, semesterUuids)
                .list()
                .stream()
                .collect(Collectors.toMap(SemesterDO::getSemesterUuid, Function.identity(), (k1, k2) -> k1));
                
        Map<String, DepartmentDO> departmentMap = departmentDAO.lambdaQuery()
                .in(DepartmentDO::getDepartmentUuid, departmentUuids)
                .list()
                .stream()
                .collect(Collectors.toMap(DepartmentDO::getDepartmentUuid, Function.identity(), (k1, k2) -> k1));
        
        // 转换为DTO
        List<TeachingClassLiteDTO> teachingClassLiteDTOs = pageResult.getRecords().stream()
                .map(teachingClassDO -> {
                    TeachingClassLiteDTO dto = new TeachingClassLiteDTO();
                    BeanUtil.copyProperties(teachingClassDO, dto);
                    
                    // 设置关联信息
                    CourseLibraryDO course = courseMap.get(teachingClassDO.getCourseUuid());
                    if (course != null) {
                        dto.setCourseName(course.getName());
                    }
                    
                    SemesterDO semester = semesterMap.get(teachingClassDO.getSemesterUuid());
                    if (semester != null) {
                        dto.setSemesterName(semester.getName());
                    }
                    
                    DepartmentDO department = departmentMap.get(teachingClassDO.getCourseDepartmentUuid());
                    if (department != null) {
                        dto.setCourseDepartmentName(department.getDepartmentName());
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
        
        // 修改为正确的PageDTO转换方法
        return new PageDTO<TeachingClassLiteDTO>()
                .setTotal(pageResult.getTotal())
                .setSize(pageResult.getSize())
                .setCurrent(pageResult.getCurrent())
                .setRecords(teachingClassLiteDTOs);
    }

    @Override
    public List<TeachingClassLiteDTO> getTeachingClassListByDepartment(String departmentUuid) {
        log.debug("根据部门UUID获取教学班列表: departmentUuid={}", departmentUuid);
        
        if (!StringUtils.hasText(departmentUuid)) {
            throw new BusinessException("部门UUID不能为空", ErrorCode.BODY_ERROR);
        }
        
        // 查询该部门的所有教学班
        List<TeachingClassDO> teachingClasses = teachingClassDAO.lambdaQuery()
                .eq(TeachingClassDO::getCourseDepartmentUuid, departmentUuid)
                .eq(TeachingClassDO::getIsEnabled, true)
                .list();
                
        if (teachingClasses.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取相关信息
        List<String> courseUuids = teachingClasses.stream()
                .map(TeachingClassDO::getCourseUuid)
                .distinct()
                .collect(Collectors.toList());
                
        List<String> semesterUuids = teachingClasses.stream()
                .map(TeachingClassDO::getSemesterUuid)
                .distinct()
                .collect(Collectors.toList());
        
        // 批量查询相关信息
        Map<String, CourseLibraryDO> courseMap = courseLibraryDAO.lambdaQuery()
                .in(CourseLibraryDO::getCourseLibraryUuid, courseUuids)
                .list()
                .stream()
                .collect(Collectors.toMap(CourseLibraryDO::getCourseLibraryUuid, Function.identity(), (k1, k2) -> k1));
                
        Map<String, SemesterDO> semesterMap = semesterDAO.lambdaQuery()
                .in(SemesterDO::getSemesterUuid, semesterUuids)
                .list()
                .stream()
                .collect(Collectors.toMap(SemesterDO::getSemesterUuid, Function.identity(), (k1, k2) -> k1));
                
        // 获取部门信息
        DepartmentDO department = departmentDAO.getById(departmentUuid);
        
        // 转换为DTO
        return teachingClasses.stream()
                .map(teachingClassDO -> {
                    TeachingClassLiteDTO dto = new TeachingClassLiteDTO();
                    BeanUtil.copyProperties(teachingClassDO, dto);
                    
                    // 设置关联信息
                    CourseLibraryDO course = courseMap.get(teachingClassDO.getCourseUuid());
                    if (course != null) {
                        dto.setCourseName(course.getName());
                    }
                    
                    SemesterDO semester = semesterMap.get(teachingClassDO.getSemesterUuid());
                    if (semester != null) {
                        dto.setSemesterName(semester.getName());
                    }
                    
                    if (department != null) {
                        dto.setCourseDepartmentName(department.getDepartmentName());
                    }
                    
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingClassDTO createTeachingClass(TeachingClassDTO teachingClassDTO) {
        log.debug("创建教学班: {}", teachingClassDTO);
        
        // 参数校验
        validateTeachingClassDTO(teachingClassDTO);
        
        // 检查编号唯一性
        checkTeachingClassCodeUnique(teachingClassDTO.getTeachingClassCode(), null);
        
        // 检查课程存在性
        CourseLibraryDO course = courseLibraryDAO.getById(teachingClassDTO.getCourseUuid());
        if (course == null) {
            throw new BusinessException("课程不存在", ErrorCode.NOT_EXIST);
        }
        
        // 检查学期存在性
        SemesterDO semester = semesterDAO.getById(teachingClassDTO.getSemesterUuid());
        if (semester == null) {
            throw new BusinessException("学期不存在", ErrorCode.NOT_EXIST);
        }
        
        // DTO转DO
        TeachingClassDO teachingClassDO = new TeachingClassDO();
        BeanUtil.copyProperties(teachingClassDTO, teachingClassDO);
        
        // 设置UUID和默认值
        String uuid = UUID.randomUUID().toString().replace("-", "");
        teachingClassDO.setTeachingClassUuid(uuid);
        
        if (teachingClassDO.getIsEnabled() == null) {
            teachingClassDO.setIsEnabled(true);
        }
        
        // 保存到数据库
        boolean success = teachingClassDAO.save(teachingClassDO);
        if (!success) {
            throw new BusinessException("教学班创建失败", ErrorCode.OPERATION_FAILED);
        }
        
        // 返回创建后的对象
        return getTeachingClassByUuid(uuid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingClassDTO updateTeachingClass(String teachingClassUuid, TeachingClassDTO teachingClassDTO) {
        log.debug("更新教学班: uuid={}, dto={}", teachingClassUuid, teachingClassDTO);
        
        // 查询教学班是否存在
        TeachingClassDO existingClass = teachingClassDAO.getById(teachingClassUuid);
        if (existingClass == null) {
            throw new BusinessException("教学班不存在", ErrorCode.NOT_EXIST);
        }
        
        // 参数校验
        validateTeachingClassDTO(teachingClassDTO);
        
        // 检查编号唯一性（排除自身）
        if (!existingClass.getTeachingClassCode().equals(teachingClassDTO.getTeachingClassCode())) {
            checkTeachingClassCodeUnique(teachingClassDTO.getTeachingClassCode(), teachingClassUuid);
        }
        
        // 更新属性
        TeachingClassDO updateDO = new TeachingClassDO();
        BeanUtil.copyProperties(teachingClassDTO, updateDO);
        updateDO.setTeachingClassUuid(teachingClassUuid);
        
        // 执行更新
        boolean success = teachingClassDAO.updateById(updateDO);
        if (!success) {
            throw new BusinessException("教学班更新失败", ErrorCode.OPERATION_FAILED);
        }
        
        // 返回更新后的对象
        return getTeachingClassByUuid(teachingClassUuid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteTeachingClass(String teachingClassUuid) {
        log.debug("删除教学班: {}", teachingClassUuid);
        
        // 查询教学班是否存在
        TeachingClassDO existingClass = teachingClassDAO.getById(teachingClassUuid);
        if (existingClass == null) {
            throw new BusinessException("教学班不存在", ErrorCode.NOT_EXIST);
        }
        
        // TODO: 检查是否有关联的排课、选课等数据，如有则不允许删除
        
        // 执行删除
        teachingClassDAO.deleteTeachingClass(teachingClassUuid);
    }
    
    /**
     * 校验教学班DTO
     */
    private void validateTeachingClassDTO(TeachingClassDTO teachingClassDTO) {
        // 检查必填字段
        if (teachingClassDTO == null) {
            throw new BusinessException("教学班信息不能为空", ErrorCode.BODY_ERROR);
        }
        
        if (!StringUtils.hasText(teachingClassDTO.getCourseUuid())) {
            throw new BusinessException("课程UUID不能为空", ErrorCode.BODY_ERROR);
        }
        
        if (!StringUtils.hasText(teachingClassDTO.getSemesterUuid())) {
            throw new BusinessException("学期UUID不能为空", ErrorCode.BODY_ERROR);
        }
        
        if (!StringUtils.hasText(teachingClassDTO.getTeachingClassCode())) {
            throw new BusinessException("教学班编号不能为空", ErrorCode.BODY_ERROR);
        }
        
        if (!StringUtils.hasText(teachingClassDTO.getTeachingClassName())) {
            throw new BusinessException("教学班名称不能为空", ErrorCode.BODY_ERROR);
        }
        
        if (!StringUtils.hasText(teachingClassDTO.getCourseDepartmentUuid())) {
            throw new BusinessException("开课院系不能为空", ErrorCode.BODY_ERROR);
        }
    }
    
    /**
     * 检查教学班编号唯一性
     */
    private void checkTeachingClassCodeUnique(String code, String excludeUuid) {
        LambdaQueryWrapper<TeachingClassDO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TeachingClassDO::getTeachingClassCode, code);
        
        // 如果是更新操作，需要排除自身
        if (StringUtils.hasText(excludeUuid)) {
            queryWrapper.ne(TeachingClassDO::getTeachingClassUuid, excludeUuid);
        }
        
        long count = teachingClassDAO.count(queryWrapper);
        if (count > 0) {
            throw new BusinessException("教学班编号已存在", ErrorCode.BODY_ERROR);
        }
    }
}
