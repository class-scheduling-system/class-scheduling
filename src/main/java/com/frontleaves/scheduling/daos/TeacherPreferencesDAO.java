package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.TeacherPreferencesMapper;
import com.frontleaves.scheduling.models.entity.TeacherPreferencesDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 教师课程时间偏好DAO
 *
 * @author FLASHLACK
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherPreferencesDAO extends ServiceImpl<TeacherPreferencesMapper, TeacherPreferencesDO> {


}