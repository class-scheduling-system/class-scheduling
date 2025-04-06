package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.base.TeacherCourseQualificationDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教师课程资格表映射器
 * <p>
 * 该类用于定义教师课程资格表的数据库操作映射器。
 * 通过继承 {@code BaseMapper} 接口，提供了对 {@code TeacherCourseQualificationDO} 实体类的基本增删改查功能。
 * </p>
 *
 * @author FLASHLACK
 * @version v1.0.0
 * @see TeacherCourseQualificationDO
 * @since v1.0.0
 */
@Mapper
public interface TeacherCourseQualificationMapper extends BaseMapper<TeacherCourseQualificationDO> {
}
