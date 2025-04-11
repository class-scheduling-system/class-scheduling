package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.TeacherPreferencesDAO;
import com.frontleaves.scheduling.models.dto.base.PageDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCourseQualificationDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherPreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.entity.base.CourseLibraryDO;
import com.frontleaves.scheduling.models.entity.base.TeacherCourseQualificationDO;
import com.frontleaves.scheduling.models.entity.base.TeacherDO;
import com.frontleaves.scheduling.models.entity.base.TeacherPreferencesDO;
import com.frontleaves.scheduling.models.vo.TeacherCourseQualificationQueryVO;
import com.frontleaves.scheduling.models.vo.TeacherCourseQualificationVO;
import com.frontleaves.scheduling.services.TeacherCourseQualificationService;
import com.frontleaves.scheduling.utils.ProjectOption;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教师课程资格业务逻辑实现类
 * <p>
 * 该类实现了TeacherCourseQualificationService接口，提供教师课程资格相关的业务逻辑处理。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @since v1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherCourseQualificationLogic implements TeacherCourseQualificationService {
    private final TeacherCourseQualificationDAO teacherCourseQualificationDAO;
    private final TeacherDAO teacherDAO;
    private final TeacherPreferencesDAO teacherPreferencesDAO;
    private final CourseLibraryDAO courseLibraryDAO;

    /**
     * 获取课程库和教师课程资格列表
     *
     * @param courseLibraryDOList  课程库DTO列表，不能为空
     * @param isTeacherPreferences 是否是教师偏好查询，用于决定是否加载教师偏好信息
     * @return 返回一个包含课程库和教师课程资格信息的DTO列表
     * @throws BusinessException 当课程没有分配教师或系统错误时抛出业务异常
     */
    @Override
    public List<CourseLibraryAndTeacherCourseQualificationListDTO>
    getCourseLibraryAndTeacherCourseQualificationList(@NotNull List<CourseLibraryAndTeacherCourseQualificationListDTO> courseLibraryDOList
            , Boolean isTeacherPreferences) {
        // 遍历课程库列表，获取每个课程的教师资格信息
        for (CourseLibraryAndTeacherCourseQualificationListDTO libraryAndClassDTO : courseLibraryDOList) {
            // 根据课程库UUID获取教师课程资格信息
            List<TeacherCourseQualificationDO> teacherCourseQualificationList =
                    teacherCourseQualificationDAO.getTeacherCourseQualificationStatusByCourseLibraryUuid(
                            libraryAndClassDTO.getCourse().getCourseLibraryUuid());
            // 检查是否已分配教师，未分配则抛出异常
            if (teacherCourseQualificationList.isEmpty()) {
                throw new BusinessException("此" + libraryAndClassDTO.getCourse().getName() + "课程没有分配老师教学",
                        ErrorCode.BODY_ERROR);
            }

            // 创建教师课程资格DTO列表
            List<TeacherCoursePreferencesDTO> coursePreferencesDTOList = new ArrayList<>();
            for (TeacherCourseQualificationDO courseQualificationDO : teacherCourseQualificationList) {
                TeacherCourseQualificationDTO courseQualificationDTO = BeanUtil.toBean(
                        courseQualificationDO, TeacherCourseQualificationDTO.class);
                TeacherCoursePreferencesDTO coursePreferences = new TeacherCoursePreferencesDTO();
                //获取老师的DTO
                TeacherDO teacherDO = teacherDAO.getTeacherByUuid(courseQualificationDO.getTeacherUuid());
                if (teacherDO == null) {
                    throw new BusinessException("系统错误，老师不存在", ErrorCode.SERVER_INTERNAL_ERROR);
                }
                // 根据isTeacherPreferences参数决定是否加载教师偏好信息
                if (Boolean.TRUE.equals(isTeacherPreferences)) {
                    List<TeacherPreferencesDO> preferences = teacherPreferencesDAO.getTeacherPreferencesByTeacherUuid(courseQualificationDO.getTeacherUuid());
                    if (preferences != null) {
                        List<TeacherPreferencesDTO> teacherPreferences = BeanUtil.copyToList(preferences, TeacherPreferencesDTO.class);
                        coursePreferences.setPreferenceList(teacherPreferences);
                    }
                }
                TeacherDTO teacherDTO = BeanUtil.toBean(teacherDO, TeacherDTO.class);
                coursePreferences.setQualification(courseQualificationDTO)
                        .setTeacher(teacherDTO);
                coursePreferencesDTOList.add(coursePreferences);
            }
            // 将数据转换为DTO
            libraryAndClassDTO.setCourse(libraryAndClassDTO.getCourse())
                    .setClassList(libraryAndClassDTO.getClassList())
                    .setNumber(libraryAndClassDTO.getNumber())
                    .setTeacherList(coursePreferencesDTOList);
        }
        // 返回最终的DTO列表
        return courseLibraryDOList;
    }
    
    /**
     * 分页获取教师课程资格列表
     *
     * @param page 页码
     * @param size 每页大小
     * @param isDesc 是否降序排序
     * @param queryVO 查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<TeacherCourseQualificationDTO> getTeacherCourseQualificationList(
            Integer page, Integer size, Boolean isDesc, TeacherCourseQualificationQueryVO queryVO) {
        // 参数校验
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1 || size > 100) {
            size = 20;
        }
        if (isDesc == null) {
            isDesc = true;
        }
        
        // 分页查询
        IPage<TeacherCourseQualificationDO> pageResult = teacherCourseQualificationDAO
                .getTeacherCourseQualificationPage(page, size, isDesc, queryVO);
        
        // 转换为DTO
        List<TeacherCourseQualificationDTO> records = pageResult.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        // 构建分页DTO
        return new PageDTO<TeacherCourseQualificationDTO>()
                .setRecords(records)
                .setCurrent(pageResult.getCurrent())
                .setSize(pageResult.getSize())
                .setTotal(pageResult.getTotal());
    }
    
    /**
     * 根据条件获取教师课程资格列表（不分页）
     *
     * @param queryVO 查询条件
     * @return 教师课程资格列表
     */
    @Override
    public List<TeacherCourseQualificationDTO> getTeacherCourseQualificationSimpleList(
            TeacherCourseQualificationQueryVO queryVO) {
        // 查询列表
        List<TeacherCourseQualificationDO> list = teacherCourseQualificationDAO
                .getTeacherCourseQualificationList(queryVO);
        
        // 转换为DTO并返回
        return list.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据资格UUID获取教师课程资格详情
     *
     * @param qualificationUuid 资格UUID
     * @return 教师课程资格详情
     */
    @Override
    public TeacherCourseQualificationDTO getTeacherCourseQualification(String qualificationUuid) {
        // 参数校验
        if (qualificationUuid == null || qualificationUuid.isBlank()) {
            throw new BusinessException("资格UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        
        // 查询资格信息
        TeacherCourseQualificationDO entity = teacherCourseQualificationDAO
                .getTeacherCourseQualificationByUuid(qualificationUuid);
        if (entity == null) {
            throw new BusinessException("教师课程资格不存在", ErrorCode.NOT_EXIST);
        }
        
        // 转换为DTO并返回
        return convertToDTO(entity);
    }
    
    /**
     * 添加教师课程资格
     *
     * @param vo 教师课程资格信息
     * @return 添加成功的教师课程资格UUID
     */
    @Override
    @Transactional
    public String addTeacherCourseQualification(TeacherCourseQualificationVO vo) {
        // 参数校验
        validateTeacherCourseQualification(vo);
        
        // 创建实体
        TeacherCourseQualificationDO entity = new TeacherCourseQualificationDO();
        BeanUtil.copyProperties(vo, entity, ProjectOption.stringBlankToNull());
        
        // 设置默认值
        entity.setStatus(1); // 默认状态为已审核
        entity.setApprovedAt(Timestamp.from(Instant.now())); // 设置审核时间
        entity.setCreatedAt(Timestamp.from(Instant.now()));
        entity.setUpdatedAt(Timestamp.from(Instant.now()));
        
        // 保存到数据库
        boolean success = teacherCourseQualificationDAO.saveTeacherCourseQualification(entity);
        if (!success) {
            throw new BusinessException("添加教师课程资格失败", ErrorCode.OPERATION_ERROR);
        }
        
        return entity.getQualificationUuid();
    }
    
    /**
     * 更新教师课程资格
     *
     * @param qualificationUuid 资格UUID
     * @param vo 教师课程资格信息
     */
    @Override
    @Transactional
    public void updateTeacherCourseQualification(String qualificationUuid, TeacherCourseQualificationVO vo) {
        // 参数校验
        if (qualificationUuid == null || qualificationUuid.isBlank()) {
            throw new BusinessException("资格UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        validateTeacherCourseQualification(vo);
        
        // 查询资格信息
        TeacherCourseQualificationDO entity = teacherCourseQualificationDAO
                .getTeacherCourseQualificationByUuid(qualificationUuid);
        if (entity == null) {
            throw new BusinessException("教师课程资格不存在", ErrorCode.NOT_EXIST);
        }
        
        // 更新实体属性
        BeanUtil.copyProperties(vo, entity, ProjectOption.stringBlankToNull());
        entity.setUpdatedAt(Timestamp.from(Instant.now()));
        
        // 保存到数据库
        boolean success = teacherCourseQualificationDAO.updateTeacherCourseQualification(entity);
        if (!success) {
            throw new BusinessException("更新教师课程资格失败", ErrorCode.OPERATION_ERROR);
        }
    }
    
    /**
     * 删除教师课程资格
     *
     * @param qualificationUuid 资格UUID
     */
    @Override
    @Transactional
    public void deleteTeacherCourseQualification(String qualificationUuid) {
        // 参数校验
        if (qualificationUuid == null || qualificationUuid.isBlank()) {
            throw new BusinessException("资格UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        
        // 查询资格信息确认存在
        TeacherCourseQualificationDO entity = teacherCourseQualificationDAO
                .getTeacherCourseQualificationByUuid(qualificationUuid);
        if (entity == null) {
            throw new BusinessException("教师课程资格不存在", ErrorCode.NOT_EXIST);
        }
        
        // 删除资格
        boolean success = teacherCourseQualificationDAO.removeTeacherCourseQualification(qualificationUuid);
        if (!success) {
            throw new BusinessException("删除教师课程资格失败", ErrorCode.OPERATION_ERROR);
        }
    }
    
    /**
     * 审核教师课程资格
     *
     * @param qualificationUuid 资格UUID
     * @param status 审核状态（1:通过 2:驳回）
     * @param remarks 审核备注
     * @param approvedBy 审核人
     */
    @Override
    @Transactional
    public void approveTeacherCourseQualification(
            String qualificationUuid, Integer status, String remarks, String approvedBy) {
        // 参数校验
        if (qualificationUuid == null || qualificationUuid.isBlank()) {
            throw new BusinessException("资格UUID不能为空", ErrorCode.PARAMETER_ERROR);
        }
        if (status == null || (status != 1 && status != 2)) {
            throw new BusinessException("审核状态无效，必须为1(通过)或2(驳回)", ErrorCode.PARAMETER_ERROR);
        }
        if (approvedBy == null || approvedBy.isBlank()) {
            throw new BusinessException("审核人不能为空", ErrorCode.PARAMETER_ERROR);
        }
        
        // 查询资格信息
        TeacherCourseQualificationDO entity = teacherCourseQualificationDAO
                .getTeacherCourseQualificationByUuid(qualificationUuid);
        if (entity == null) {
            throw new BusinessException("教师课程资格不存在", ErrorCode.NOT_EXIST);
        }
        
        // 检查当前状态
        if (entity.getStatus() != 0) {
            throw new BusinessException("只能审核待审核状态的资格", ErrorCode.OPERATION_ERROR);
        }
        
        // 更新审核信息
        entity.setStatus(status);
        entity.setRemarks(remarks);
        entity.setApprovedBy(approvedBy);
        entity.setApprovedAt(Timestamp.from(Instant.now()));
        entity.setUpdatedAt(Timestamp.from(Instant.now()));
        
        // 保存到数据库
        boolean success = teacherCourseQualificationDAO.updateTeacherCourseQualification(entity);
        if (!success) {
            throw new BusinessException("审核教师课程资格失败", ErrorCode.OPERATION_ERROR);
        }
    }
    
    /**
     * 验证教师课程资格信息
     *
     * @param vo 教师课程资格信息
     */
    private void validateTeacherCourseQualification(TeacherCourseQualificationVO vo) {
        if (vo == null) {
            throw new BusinessException("教师课程资格信息不能为空", ErrorCode.PARAMETER_ERROR);
        }
        
        // 验证教师存在
        TeacherDO teacher = teacherDAO.getTeacherByUuid(vo.getTeacherUuid());
        if (teacher == null) {
            throw new BusinessException("教师不存在", ErrorCode.NOT_EXIST);
        }
        
        // 验证课程存在
        CourseLibraryDO course = courseLibraryDAO.getCourseLibraryByUuid(vo.getCourseUuid());
        if (course == null) {
            throw new BusinessException("课程不存在", ErrorCode.NOT_EXIST);
        }
        
        // 验证资格等级
        if (vo.getQualificationLevel() < 1 || vo.getQualificationLevel() > 3) {
            throw new BusinessException("资格等级无效，必须为1(初级)、2(中级)或3(高级)", ErrorCode.PARAMETER_ERROR);
        }
        
        // 验证教授年限
        if (vo.getTeachYears() < 0) {
            throw new BusinessException("教授年限不能为负数", ErrorCode.PARAMETER_ERROR);
        }
    }
    
    /**
     * 将实体转换为DTO，并附加相关信息
     *
     * @param entity 教师课程资格实体
     * @return 教师课程资格DTO
     */
    private TeacherCourseQualificationDTO convertToDTO(TeacherCourseQualificationDO entity) {
        TeacherCourseQualificationDTO dto = BeanUtil.toBean(entity, TeacherCourseQualificationDTO.class);
        
        // 附加教师信息
        TeacherDO teacher = teacherDAO.getTeacherByUuid(entity.getTeacherUuid());
        if (teacher != null) {
            dto.setTeacherName(teacher.getName());
        }
        
        // 附加课程信息
        CourseLibraryDO course = courseLibraryDAO.getCourseLibraryByUuid(entity.getCourseUuid());
        if (course != null) {
            dto.setCourseName(course.getName());
        }
        
        return dto;
    }
    
    /**
     * 申请教师课程资格
     * <p>
     * 与添加教师课程资格不同，申请的资格状态为待审核(0)
     * </p>
     *
     * @param vo 教师课程资格信息
     * @return 申请成功的教师课程资格UUID
     */
    @Override
    @Transactional
    public String applyTeacherCourseQualification(TeacherCourseQualificationVO vo) {
        // 参数校验
        validateTeacherCourseQualification(vo);
        
        // 创建实体
        TeacherCourseQualificationDO entity = new TeacherCourseQualificationDO();
        BeanUtil.copyProperties(vo, entity, ProjectOption.stringBlankToNull());
        
        // 设置默认值
        entity.setStatus(0); // 状态为待审核
        entity.setCreatedAt(Timestamp.from(Instant.now()));
        entity.setUpdatedAt(Timestamp.from(Instant.now()));
        
        // 保存到数据库
        boolean success = teacherCourseQualificationDAO.saveTeacherCourseQualification(entity);
        if (!success) {
            throw new BusinessException("申请教师课程资格失败", ErrorCode.OPERATION_ERROR);
        }
        
        return entity.getQualificationUuid();
    }
}
