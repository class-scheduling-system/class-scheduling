package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.TeacherMapper;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 教师数据访问对象
 * @author FLASHLACK
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TeacherDAO extends ServiceImpl<TeacherMapper, TeacherDO> implements IService<TeacherDO> {

    public TeacherDO getTeacherById(String id){
        return this.lambdaQuery().eq(TeacherDO::getId,id).one();
    }
}
