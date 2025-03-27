package com.frontleaves.scheduling.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.frontleaves.scheduling.models.entity.TeacherPreferencesDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教师课程时间偏好Mapper接口
 * @author FLASHLACK
 */
@Mapper
public interface TeacherPreferencesMapper extends BaseMapper<TeacherPreferencesDO> {
}