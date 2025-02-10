package com.frontleaves.scheduling.daos;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.frontleaves.scheduling.mappers.StudentMapper;
import com.frontleaves.scheduling.models.entity.StudentDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

/**
 * 学生 DAO
 *
 * @author FLASHLACK
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StudentDAO extends ServiceImpl<StudentMapper, StudentDO> implements IService<StudentDO> {
    /**
     * 通过学号获取学生信息
     * @param id 学号
     * @return 学生信息
     */
    public StudentDO getStudentById(String id){
        return this.lambdaQuery().eq(StudentDO::getId,id).one();
    }
}

