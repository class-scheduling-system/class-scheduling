package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.AdministrativeClassDAO;
import com.frontleaves.scheduling.daos.CourseLibraryDAO;
import com.frontleaves.scheduling.models.dto.base.AdministrativeClassDTO;
import com.frontleaves.scheduling.models.dto.base.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.entity.AdministrativeClassDO;
import com.frontleaves.scheduling.models.entity.CourseLibraryDO;
import com.frontleaves.scheduling.models.vo.SpecificCourseIdVO;
import com.frontleaves.scheduling.services.CourseLibraryService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 课程库逻辑层
 *
 * @author FLASHLACK
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseLibraryLogic implements CourseLibraryService {
    private final CourseLibraryDAO courseLibraryDAO;
    private final AdministrativeClassDAO administrativeClassDAO;

    /**
     * 设置课程信息到CourseLibraryAndClassDTO对象中
     *
     * @param courseLibraryAndTeacherCourseQualificationListDTO 课程库和班级DTO对象，用于存储课程信息
     * @param courseMap                                         包含课程ID和课程库对象的映射，用于查找特定课程
     * @param specificCourseIdVO                                包含特定课程ID的VO对象，用于指定需要查找的课程
     *                                                          本方法首先根据特定课程ID从课程映射中获取课程库对象如果未找到对应的课程，
     *                                                          则抛出业务异常表示未找到匹配的课程如果找到了课程，则将其转换为课程库DTO对象
     *                                                          并设置到CourseLibraryAndClassDTO对象中
     */
    private void setCourse(
            CourseLibraryAndTeacherCourseQualificationListDTO courseLibraryAndTeacherCourseQualificationListDTO,
            @NotNull Map<String, CourseLibraryDO> courseMap,
            @NotNull SpecificCourseIdVO specificCourseIdVO) {
        // 根据特定课程ID从课程映射中获取课程库对象
        CourseLibraryDO courseLibraryDO = courseMap.get(specificCourseIdVO.getCourseId());
        // 如果未找到对应的课程，则抛出业务异常
        if (courseLibraryDO == null) {
            throw new BusinessException("未找到与 courseId 匹配的课程： " + specificCourseIdVO.getCourseId(), ErrorCode.BODY_ERROR);
        }
        // 将找到的课程库对象转换为课程库DTO对象并设置到CourseLibraryAndClassDTO对象中
        courseLibraryAndTeacherCourseQualificationListDTO.setCourse(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
    }



    /**
     * 计算选课学生的总人数
     *
     * @param courselibraryandclassdto 课程库和班级信息的DTO对象，用于存储班级DTO列表和总学生数
     * @param classMap                 班级ID与班级信息的映射，用于快速获取班级信息
     * @param specificCourseIdVO       包含特定课程ID信息的对象，用于指定需要计算学生数的班级ID列表
     */
    private void calculateStudentCount(
            CourseLibraryAndTeacherCourseQualificationListDTO courselibraryandclassdto,
            Map<String, AdministrativeClassDO> classMap, @NotNull SpecificCourseIdVO specificCourseIdVO) {
        // 初始化总学生数为0
        int totalStudentCount = 0;
        // 遍历特定课程VO中的班级ID列表
        for (String classId : specificCourseIdVO.getClassId()) {
            // 从班级映射中获取当前班级ID对应的班级信息
            AdministrativeClassDO administrativeClassDO = classMap.get(classId);
            // 如果找到了对应的班级信息
            if (administrativeClassDO != null) {
                // 将班级信息转换为DTO对象
                AdministrativeClassDTO classDTO = BeanUtil.toBean(administrativeClassDO, AdministrativeClassDTO.class);
                // 将转换后的班级DTO添加到课程库和班级信息DTO的班级DTO列表中
                courselibraryandclassdto.getClassList().add(classDTO);
                // 累加当前班级的学生数到总学生数中
                totalStudentCount += administrativeClassDO.getStudentCount();
            }
        }
        // 将计算得到的总学生数设置到课程库和班级信息DTO中
        courselibraryandclassdto.setNumber(totalStudentCount);
    }


    /**
     * 根据部门UUID、特定课程ID列表和排除课程ID列表获取课程库列表
     * 如果查询结果为空，则抛出业务异常
     *
     * @param departmentUuid    部门UUID，用于查询课程库
     * @param specificCourseIds 特定课程ID列表，用于过滤课程库
     * @return 课程库列表，如果列表为空则抛出异常
     * @throws BusinessException 当课程库列表为空时抛出的业务异常
     */
    @Override
    public List<CourseLibraryDTO> listCourseLibraryByDepartmentAndSpecifyWithThrow(
            @NotBlank String departmentUuid, List<String> specificCourseIds) {
        // 调用DAO层方法获取课程库列表
        List<CourseLibraryDO> listCourseLibraryByDepartmentAndSpecify = courseLibraryDAO.getListCourseLibraryByDepartmentAndSpecify(
                departmentUuid, specificCourseIds);
        // 检查获取的课程库列表是否为空，如果为空则抛出业务异常
        if (listCourseLibraryByDepartmentAndSpecify != null && listCourseLibraryByDepartmentAndSpecify.isEmpty()) {
            throw new BusinessException("课程库列表为空", ErrorCode.BODY_ERROR);
        }
        // 将获取的课程库列表转换为DTO对象
        List<CourseLibraryDTO> courseLibraryDTOList = new ArrayList<>();
        if (listCourseLibraryByDepartmentAndSpecify != null) {
            for (CourseLibraryDO courseLibraryDO : listCourseLibraryByDepartmentAndSpecify) {
                courseLibraryDTOList.add(BeanUtil.toBean(courseLibraryDO, CourseLibraryDTO.class));
            }
        }
        // 返回获取的课程库列表
        return courseLibraryDTOList;
    }


    /**
     * 获取特定课程的列表和班级信息DTO
     * 该方法用于根据特定课程ID列表获取相应的课程和班级信息，并计算学生人数
     *
     * @param specificCourseIds 包含特定课程ID的列表，用于查询课程和班级信息，必须不为空
     * @return 返回一个包含课程库和班级信息的DTO列表
     */
    @Override
    public List<CourseLibraryAndTeacherCourseQualificationListDTO> getCourseListAndClassDTO(
            @NotNull List<SpecificCourseIdVO> specificCourseIds, String departmentUuid) {
        List<CourseLibraryAndTeacherCourseQualificationListDTO> lists = new ArrayList<>();
        // 获取所有课程并构建映射
        Map<String, CourseLibraryDO> courseMap = courseLibraryDAO.getCourseListByDepart(departmentUuid).stream()
                .collect(Collectors.toMap(CourseLibraryDO::getCourseLibraryUuid, course -> course));
        // 获取所有班级并构建映射
        Map<String, AdministrativeClassDO> classMap = administrativeClassDAO.getAdministrativeClassListByDepartment(departmentUuid)
                .stream()
                .collect(Collectors.toMap(AdministrativeClassDO::getAdministrativeClassUuid, clazz -> clazz));
        // 遍历特定课程ID列表，为每个课程构建课程库和班级信息DTO
        for (SpecificCourseIdVO specificCourseIdVO : specificCourseIds) {
            CourseLibraryAndTeacherCourseQualificationListDTO libraryAndClassDTO =
                    new CourseLibraryAndTeacherCourseQualificationListDTO();
            // 设置课程
            setCourse(libraryAndClassDTO, courseMap, specificCourseIdVO);
            log.debug("设置人数:{}",specificCourseIdVO.getNumber());
            // 计算学生人数（如果外部没有提供）
            if (specificCourseIdVO.getNumber() != null) {
                libraryAndClassDTO.setNumber(specificCourseIdVO.getNumber());
            } else {
                // 计算学生数，同时添加班级信息
                calculateStudentCount(libraryAndClassDTO, classMap, specificCourseIdVO);
            }
            log.debug("设置后人数为:{}", libraryAndClassDTO.getNumber());
            lists.add(libraryAndClassDTO);
        }
        return lists;
    }

    /**
     * 根据课程UUID获取课程信息
     * 此方法用于通过课程的唯一标识符（UUID）来检索课程信息它首先调用课程库DAO中的方法来获取课程对象如果未找到对应的课程，
     * 则抛出一个商业异常，指示课程不存在这样做的目的是确保当请求特定课程时，能够提供明确的错误信息而不是返回null，
     * 从而提高系统的健壮性和用户体验
     * @param courseUuid 课程的唯一标识符（UUID）
     * @return 返回找到的CourseLibraryDO对象
     * @throws BusinessException 如果课程不存在，则抛出此异常
     */
    @Override
    public @NotNull CourseLibraryDO getCourseByUuid(String courseUuid) {
        CourseLibraryDO courseLibraryDO = courseLibraryDAO.getCourseByUuid(courseUuid);
        if (courseLibraryDO == null) {
            throw new BusinessException("课程不存在", ErrorCode.NOT_EXIST);
        }
        return courseLibraryDO;
    }
}
