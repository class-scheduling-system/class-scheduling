package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.TeacherPreferencesDAO;
import com.frontleaves.scheduling.models.dto.base.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherCourseQualificationDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherDTO;
import com.frontleaves.scheduling.models.dto.base.TeacherPreferencesDTO;
import com.frontleaves.scheduling.models.dto.merge.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.entity.TeacherCourseQualificationDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.TeacherPreferencesDO;
import com.frontleaves.scheduling.services.TeacherCourseQualificationService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        // 创建返回结果列表
        List<CourseLibraryAndTeacherCourseQualificationListDTO> courseLibraryAndTeacherCourseQualificationListDTO
                = new ArrayList<>();
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
            // 创建返回数据的最终关联DTO对象
            CourseLibraryAndTeacherCourseQualificationListDTO dto =
                    new CourseLibraryAndTeacherCourseQualificationListDTO();
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
            dto.setCourse(libraryAndClassDTO.getCourse())
                    .setClassList(libraryAndClassDTO.getClassList())
                    .setNumber(libraryAndClassDTO.getNumber())
                    .setTeacherList(coursePreferencesDTOList);
            // 将关联DTO添加到返回结果列表中
            courseLibraryAndTeacherCourseQualificationListDTO
                    .add(dto);
        }
        // 返回最终的DTO列表
        return courseLibraryAndTeacherCourseQualificationListDTO;
    }
}
