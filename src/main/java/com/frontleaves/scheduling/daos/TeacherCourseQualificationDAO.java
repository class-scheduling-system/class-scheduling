package com.frontleaves.scheduling.daos;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.mappers.TeacherCourseQualificationMapper;
import com.frontleaves.scheduling.models.entity.base.TeacherCourseQualificationDO;
import com.xlf.utility.util.ConvertUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.List;

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
    private final RedissonClient redisson;

    /**
     * 根据教师课程资格UUID获取教师课程资格信息
     * @param teacherCourseQualificationUuid 教师课程资格UUID
     * @return 教师课程资格信息
     */
    public TeacherCourseQualificationDO getTeacherCourseQualificationByUuid(
            String teacherCourseQualificationUuid) {
        RMap<String, String> rMap = redisson.getMap(
                StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_UUID + teacherCourseQualificationUuid);
        if (!rMap.isExists()){
            TeacherCourseQualificationDO teacherCourseQualificationDO = this.getById(teacherCourseQualificationUuid);
            if (teacherCourseQualificationDO != null) {
                rMap.putAll(ConvertUtil.convertObjectToMapString(teacherCourseQualificationDO));
                rMap.expire(Duration.ofSeconds(3600));
                return teacherCourseQualificationDO;
            }
            return null;
        }
        return BeanUtil.toBean(rMap, TeacherCourseQualificationDO.class);
    }

    /**
     * 根据课程库UUID获取已通过审核教师课程资格信息,并将查询结果存入Redis缓存
     * <p>
     * 该方法首先尝试从Redis缓存中获取教师课程资格信息。如果缓存中不存在，则从数据库中查询，
     * 并将查询结果存入Redis缓存中。缓存的有效期为1小时。
     * </p>
     * @param courseLibraryUuid 课程库UUID，用于查询对应的教师课程资格信息
     * @return 教师课程资格信息，如果未找到则返回null
     * @see TeacherCourseQualificationDO
     */
    public List<TeacherCourseQualificationDO> getTeacherCourseQualificationStatusByCourseLibraryUuid(
            String courseLibraryUuid) {
        RList<TeacherCourseQualificationDO> rList = redisson.getList(
                StringConstant.Redis.TEACHER_COURSE_QUALIFICATION_COURSE_LIBRARY_UUID + courseLibraryUuid);
        if (!rList.isExists()){
            List<TeacherCourseQualificationDO> teacherCourseQualificationDOList = this.lambdaQuery()
                    .eq(TeacherCourseQualificationDO::getCourseUuid, courseLibraryUuid)
                    .eq(TeacherCourseQualificationDO::getStatus,1).list();
            if (teacherCourseQualificationDOList != null) {
                rList.addAll(teacherCourseQualificationDOList);
                rList.expire(Duration.ofSeconds(3600));
                return teacherCourseQualificationDOList;
            }
            return list();
        }
        return rList.readAll();
    }
}
