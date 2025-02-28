package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.entity.*;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
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
    private RoleDAO roleDAO;
    @Resource
    private UserDAO userDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private MajorDAO majorDAO;
    @Resource
    private RedissonClient redisson;
    private StudentDO setUpStudent;
    private UserDO setUpUser;

    /**
     * 通过部门 名称获取部门数据
     *
     * @return 部门数据
     */
    private DepartmentDO getDepartmentByName() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().eq(DepartmentDO::getDepartmentName,
                "信息智能工程学院").one();
        if (departmentDO == null) {
            throw new BusinessException("[dao.StudentTest]单元测试通过部门名称找不到部门数据", ErrorCode.OPERATION_ERROR);
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
            throw new BusinessException("[dao.StudentTest]单元测试通过找不到专业数据", ErrorCode.OPERATION_ERROR);
        }
        return majorDO;
    }

    /**
     * 根据角色名称获取角色对象
     * <p>
     * 此方法使用lambda表达式和链式调用从数据库中查询角色名称匹配的角色对象如果找不到对应的角色，
     * 则抛出一个自定义的BusinessException异常，指示操作失败这主要是为了处理角色数据不存在的情况，
     * 确保调用此方法时能够得到明确的错误提示
     *
     * @return RoleDO 如果找到匹配的角色名称，则返回对应的角色对象
     * @throws BusinessException 如果数据库中不存在指定角色名称的角色，则抛出此异常
     */
    private @NotNull RoleDO getRoleByName() {
        RoleDO roleDO = roleDAO.lambdaQuery().eq(RoleDO::getRoleName, "学生").one();
        if (roleDO == null) {
            throw new BusinessException(
                    "[dao.StudentTest]单元测试通过角色名称找不到角色数据", ErrorCode.OPERATION_ERROR);
        }
        return roleDO;
    }

    @BeforeEach
    void setUp() {
        log.debug("StudentDAO单元测试初始化");
        setUpUser = new UserDO();
        setUpUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("studentDAOTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("studentDAOTest@qwer.com")
                .setPhone("14452873800")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(getRoleByName().getRoleUuid())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, setUpUser.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, setUpUser.getName()).remove();
        }
        userDAO.save(setUpUser);
        setUpStudent = new StudentDO();
        setUpStudent.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("ZhangSan1314")
                .setGender(1)
                .setGrade("2022")
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz("1班")
                .setUserUuid(setUpUser.getUserUuid());
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, setUpStudent.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, setUpStudent.getName()).remove();
        }
        studentDAO.save(setUpStudent);
        //添加缓存
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid());
        map.putAll(ConvertUtil.convertObjectToMapString(setUpStudent));
        map.expire(Duration.ofSeconds(86400));
        RBucket<String> bucketId = redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId());
        bucketId.set(setUpStudent.getStudentUuid());
        bucketId.expire(Duration.ofSeconds(86400));
        RBucket<String> bucketUserUuid = redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID +
                setUpStudent.getUserUuid());
        bucketUserUuid.set(setUpStudent.getStudentUuid());
        bucketUserUuid.expire(Duration.ofSeconds(86400));
    }

    @AfterEach
    void tearDown() {
        log.debug("学生信息清理");
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, setUpStudent.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, setUpStudent.getStudentUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid()).delete();
    }

    @Test
    void testGetStudentById() {
        log.debug("测试通过学生 ID 获取学生信息");
        StudentDO studentById = studentDAO.getStudentById(setUpStudent.getId());
        Assertions.assertNotNull(studentById);
        redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId()).delete();
        StudentDO studentById1 = studentDAO.getStudentById(setUpStudent.getId());
        Assertions.assertNotNull(studentById1);
    }

    @Test
    void testGetStudentByUuid() {
        log.debug("测试通过学生 UUID 获取学生信息");
        StudentDO studentByUuid = studentDAO.getStudentByUuid(setUpStudent.getStudentUuid());
        Assertions.assertNotNull(studentByUuid);
        redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid()).delete();
        StudentDO studentByUuid1 = studentDAO.getStudentByUuid(setUpStudent.getStudentUuid());
        Assertions.assertNotNull(studentByUuid1);
    }

    @Test
    void testUpdateUserUuid(){
        log.debug("测试更新学生信息中的用户 UUID");
        //创建一个新的教师用户
        UserDO newTestUserDO = new UserDO();
        newTestUserDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("studentDAONewTestUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("studentDAONewTestUser@qwer.com")
                .setPhone("15859273800")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(getRoleByName().getRoleUuid())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, newTestUserDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, newTestUserDO.getName()).remove();
        }
        userDAO.save(newTestUserDO);
        studentDAO.updateUserUuid(newTestUserDO.getUserUuid(), setUpStudent.getId());
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getId, setUpStudent.getId()).one();
        Assertions.assertEquals(newTestUserDO.getUserUuid(), studentDO1.getUserUuid());
        //删除学生和新增用户数据
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, studentDO1.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, studentDO1.getStudentUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid,newTestUserDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, newTestUserDO.getUserUuid()).remove();
        }
    }

    @Test
    void testDeleteStudent() {
        log.debug("测试删除学生信息");
        studentDAO.deleteStudent(setUpStudent);
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getId, setUpStudent.getId()).one();
        log.debug("检查缓存是否删除");
        RMap<String, String> getMapById = redisson.getMap(StringConstant.Redis.STUDENT_ID + setUpStudent.getId());
        RMap<String, String> getMapByUuid = redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid());
        RMap<String, String> getMapByUserUuid = redisson.getMap(StringConstant.Redis.STUDENT_USER_UUID
                + setUpStudent.getUserUuid());
        Assertions.assertNull(studentDO1);
        Assertions.assertFalse(getMapById.isExists());
        Assertions.assertFalse(getMapByUuid.isExists());
        Assertions.assertFalse(getMapByUserUuid.isExists());
    }
    @Test
    void testGetStudentByUserUuid() {
        log.debug("测试通过用户 UUID 获取学生信息");
        StudentDO studentByUserUuid = studentDAO.getStudentByUserUuid(setUpStudent.getUserUuid());
        Assertions.assertNotNull(studentByUserUuid);
        redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid()).delete();
        StudentDO studentByUserUuid1 = studentDAO.getStudentByUserUuid(setUpStudent.getUserUuid());
        Assertions.assertNotNull(studentByUserUuid1);
    }
}
