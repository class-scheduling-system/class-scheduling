package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.TeacherCourseQualificationMapper;
import com.frontleaves.scheduling.models.entity.TeacherCourseQualificationDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 教师课程资格数据访问对象
 * <p>
 * 该类是 {@code TeacherCourseQualificationDO} 实体类的数据访问对象，通过继承 {@code ServiceImpl<TeacherCourseQualificationMapper, TeacherCourseQualificationDO>}，
 * 实现了对教师课程资格表的基本 CRUD 操作。此类提供了与数据库交互的方法，用于管理教师课程资格的相关信息。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @see TeacherCourseQualificationDO
 * @see TeacherCourseQualificationMapper
 * @since v1.0.0
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherCourseQualificationDAO extends
        ServiceImpl<TeacherCourseQualificationMapper, TeacherCourseQualificationDO> {
}