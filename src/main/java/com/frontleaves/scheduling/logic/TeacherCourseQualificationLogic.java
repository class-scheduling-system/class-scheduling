package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.daos.TeacherCourseQualificationDAO;
import com.frontleaves.scheduling.models.dto.CourseLibraryAndTeacherCourseQualificationListDTO;
import com.frontleaves.scheduling.models.dto.CourseLibraryDTO;
import com.frontleaves.scheduling.models.dto.TeacherCoursePreferencesDTO;
import com.frontleaves.scheduling.models.dto.TeacherCourseQualificationDTO;
import com.frontleaves.scheduling.models.entity.TeacherCourseQualificationDO;
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

    /**
     * 获取课程库和教师课程资格的关联信息
     *
     * @param courseLibraryDOList 课程库数据对象列表
     * @return 包含课程库和教师课程资格信息的DTO列表
     * @throws BusinessException 当课程没有分配教师时抛出异常
     */
    @Override
    public List<CourseLibraryAndTeacherCourseQualificationListDTO>
    getCourseLibraryAndTeacherCourseQualificationList(@NotNull List<CourseLibraryDTO> courseLibraryDOList
    , Boolean isTeacherPreferences) {
        // 创建返回结果列表
        List<CourseLibraryAndTeacherCourseQualificationListDTO> courseLibraryAndTeacherCourseQualificationListDTO
                = new ArrayList<>();
        // 遍历课程库列表，获取每个课程的教师资格信息
        for (CourseLibraryDTO courseLibraryDTO : courseLibraryDOList) {
            // 根据课程库UUID获取教师课程资格信息
            List<TeacherCourseQualificationDO> teacherCourseQualificationList =
                    teacherCourseQualificationDAO.getTeacherCourseQualificationByCourseLibraryUuid(
                            courseLibraryDTO.getCourseLibraryUuid());
            // 检查是否已分配教师，未分配则抛出异常
            if (teacherCourseQualificationList.isEmpty()) {
                throw new BusinessException("此" + courseLibraryDTO.getName() + "课程没有分配老师教学",
                        ErrorCode.BODY_ERROR);
            }
            // 创建返回数据的最终关联DTO对象
            CourseLibraryAndTeacherCourseQualificationListDTO dto =
                    new CourseLibraryAndTeacherCourseQualificationListDTO();
            // 创建教师课程资格DTO列表
            List<TeacherCoursePreferencesDTO> coursePreferencesDTOList = new ArrayList<>();
            for (TeacherCourseQualificationDO courseQualificationDO : teacherCourseQualificationList){
                TeacherCourseQualificationDTO courseQualificationDTO = BeanUtil.toBean(
                        courseQualificationDO, TeacherCourseQualificationDTO.class);
                TeacherCoursePreferencesDTO coursePreferences = new TeacherCoursePreferencesDTO();
                coursePreferences.setCourseQualification(courseQualificationDTO);
                coursePreferencesDTOList.add(coursePreferences);
            }
            // 将数据转换为DTO
            dto.setCourseLibraryDTO(BeanUtil.toBean(
                    courseLibraryDTO, CourseLibraryDTO.class))
                            .setTeacherCoursePreferencesDTOList(coursePreferencesDTOList);
            // 将关联DTO添加到返回结果列表中
            courseLibraryAndTeacherCourseQualificationListDTO
                    .add(dto);
        }
        return courseLibraryAndTeacherCourseQualificationListDTO;
    }
}