package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.MajorDAO;
import com.frontleaves.scheduling.daos.StudentDAO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@Slf4j
@SpringBootTest
class StudentTest {
    @Resource
    private StudentDAO studentDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private MajorDAO majorDAO;
    @Resource
    private RedissonClient redisson;

    /**
     * 通过部门 名称获取部门数据
     *
     * @return 部门数据
     */
    private DepartmentDO getDepartmentByName() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().eq(DepartmentDO::getDepartmentName,
                "信息智能工程学院").one();
        if (departmentDO == null) {
            throw new BusinessException("单元测试通过部门名称找不到部门数据", ErrorCode.OPERATION_ERROR);
        }
        return departmentDO;
    }

    /**
     * 通过专业名称获取专业数据
     *
     * @return 专业数据
     */
    private MajorDO getMajorByName() {
        MajorDO majorDO = majorDAO.lambdaQuery().eq(MajorDO::getMajorName, "软件技术").one();
        if (majorDO == null) {
            throw new BusinessException("单元测试通过找不到专业数据", ErrorCode.OPERATION_ERROR);
        }
        return majorDO;
    }

    @Test
    void testDeleteStudent() {
        log.debug("测试删除学生信息");
        StudentDO studentDO = new StudentDO();
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("ZhangSan1314")
                .setGender(1)
                .setGrade("2022")
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz("1班");
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, studentDO.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, studentDO.getName()).remove();
        }
        studentDAO.save(studentDO);
        RMap<String, String> mapId = redisson.getMap(StringConstant.Redis.STUDENT_ID + studentDO.getId());
        mapId.putAll(ConvertUtil.convertObjectToMapString(studentDO));
        mapId.expire(Duration.ofSeconds(86400));
        RMap<String, String> mapUuid = redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid());
        mapUuid.putAll(ConvertUtil.convertObjectToMapString(studentDO));
        mapUuid.expire(Duration.ofSeconds(86400));
        studentDAO.deleteStudent(studentDO);
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getId, studentDO.getId()).one();
        RMap<String, String> getMapById = redisson.getMap(StringConstant.Redis.STUDENT_ID + studentDO.getId());
        RMap<String, String> getMapByUuid = redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid());
        Assertions.assertNull(studentDO1);
        Assertions.assertFalse(getMapById.isExists());
        Assertions.assertFalse(getMapByUuid.isExists());
    }
}
