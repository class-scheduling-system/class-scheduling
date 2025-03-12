package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.UserLoginDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.frontleaves.scheduling.services.AuthService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest
class AuthTest {
    @Resource
    private AuthService authService;
    @Resource
    private StudentDAO studentDAO;
    @Resource
    private MajorDAO majorDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private TeacherDAO teacherDAO;
    @Resource
    private GradeDAO gradeDAO;
    @Resource
    private UserDAO userDAO;
    @Resource
    private AdministrativeClassDAO administrativeClassDAO;


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

    @Test
    void testCheckLoginForUser() {
        log.info("测试用户登录数据检查");
        UserLoginVO userLoginVO = new UserLoginVO("test", "123456");
        UserLoginDTO userLoginDTO = authService.checkLoginForUser(userLoginVO, new MockHttpServletRequest());
        log.debug("userLoginDTO: {}", userLoginDTO);
        Assertions.assertNotNull(userLoginDTO);
    }

    @Test
    void testCheckLoginFail() {
        log.info("测试用户登录数据检查失败");
        UserLoginVO userLoginVO = new UserLoginVO("test", "1234567");
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(UserAuthenticationException.class
                , () -> authService.checkLoginForUser(userLoginVO, request));
    }

    @Test
    @Transactional
    void testCheckLoginForNewUserByStu() {
        log.info("测试学生或教师登录数据检查");
        //创建学生数据
        StudentDO studentDO = new StudentDO();
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("22344")
                .setName("testCheckLoginForNewUser")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, studentDO.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, studentDO.getName()).remove();
        }
        studentDAO.save(studentDO);
        UserLoginVO userLoginVO = new UserLoginVO("22344", "stu22344");
        UserLoginDTO userLoginDTO = authService.checkLoginForNewUser(userLoginVO, new MockHttpServletRequest());
        Assertions.assertNotNull(userLoginDTO);
        Assertions.assertTrue(userLoginDTO.getInitialization());
        //删除学生数据
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).remove();
            redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid()).delete();
            redisson.getBucket(StringConstant.Redis.STUDENT_ID + studentDO.getId()).delete();
            redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + studentDO.getUserUuid()).delete();
        }
    }

    @Test
    @Transactional
    void testCheckLoginForNewUserByTea() {
        TeacherDO teacherDO = new TeacherDO();
        teacherDO.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(getDepartmentByName().getDepartmentUuid())
                .setId("123456")
                .setName("teacherDAOTest")
                .setEnglishName("ZhangSeng")
                .setEthnic("汉族")
                .setSex(true)
                .setType(SystemConstant.getTeacherTypeLecturer())
                .setPhone("14452873800")
                .setEmail("qwerasdfzxcv@qwer.com")
                .setJobTitle("教授")
                .setDesc("这是一个教授");
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, teacherDO.getId()).one() == null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, teacherDO.getId()).remove();
        }
        teacherDAO.save(teacherDO);
        UserLoginVO userLoginVO = new UserLoginVO("123456", "te123456");
        UserLoginDTO userLoginDTO = authService.checkLoginForNewUser(userLoginVO, new MockHttpServletRequest());
        Assertions.assertNotNull(userLoginDTO);
        Assertions.assertTrue(userLoginDTO.getInitialization());
        //删除教师数据
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getTeacherUuid, teacherDO.getTeacherUuid()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getTeacherUuid, teacherDO.getTeacherUuid()).remove();
            redisson.getMap(StringConstant.Redis.TEACHER_UUID + teacherDO.getTeacherUuid()).delete();
            redisson.getBucket(StringConstant.Redis.TEACHER_ID + teacherDO.getId()).delete();
            redisson.getBucket(StringConstant.Redis.TEACHER_USER_UUID + teacherDO.getUserUuid()).delete();
        }
    }

    @Test
    @Transactional
    void testUserRegisteredByStu() {
        log.debug("测试学生初始化");
        //创建学生数据
        StudentDO studentDO = new StudentDO();
        studentDO.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("22344")
                .setName("testCheckLoginForNewUser")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false);
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, studentDO.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, studentDO.getName()).remove();
        }
        studentDAO.save(studentDO);
        UserInitializationVO userInitializationVO = new UserInitializationVO(true, "22344",
                "testCheckLoginForNewUser", "123456Aa",
                "testCheckLoginForNewUser@test.com", "13888888888");
        authService.userRegistered(userInitializationVO, new MockHttpServletRequest());
        //检查数据库是否存在
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getName, userInitializationVO.getName()).one();
        Assertions.assertNotNull(userDO);
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getUserUuid, userDO.getUserUuid()).one();
        Assertions.assertNotNull(studentDO1);
        //删除数据
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, studentDO.getStudentUuid()).remove();
            redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentDO.getStudentUuid()).delete();
            redisson.getBucket(StringConstant.Redis.STUDENT_ID + studentDO.getId()).delete();
            redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + studentDO.getUserUuid()).delete();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, userDO.getUserUuid()).remove();
            redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
            redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
            redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
            redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
        }
    }

    @Test
    @Transactional
    void testUserRegisteredByTea() {
        log.debug("测试老师初始化");
        TeacherDO teacherDO = new TeacherDO();
        teacherDO.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(getDepartmentByName().getDepartmentUuid())
                .setId("654321")
                .setName("AuthTest")
                .setEnglishName("ZhangSeng")
                .setEthnic("汉族")
                .setSex(true)
                .setType(SystemConstant.getTeacherTypeLecturer())
                .setPhone("14452873800")
                .setEmail("AuthTest@text.com")
                .setJobTitle("教授")
                .setDesc("这是一个教授");
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, teacherDO.getId()).one() == null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, teacherDO.getId()).remove();
        }
        teacherDAO.save(teacherDO);
        UserInitializationVO userInitializationVO = new UserInitializationVO(false, "654321",
                "AuthTest", "123456Aa", "AuthTest@text.com", "13888888888");
        authService.userRegistered(userInitializationVO, new MockHttpServletRequest());
        //检查数据库是否存在
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getName, userInitializationVO.getName()).one();
        Assertions.assertNotNull(userDO);
        TeacherDO teacherDO1 = teacherDAO.lambdaQuery().eq(TeacherDO::getUserUuid, userDO.getUserUuid()).one();
        Assertions.assertNotNull(teacherDO1);
        //删除数据
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getTeacherUuid, teacherDO.getTeacherUuid()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getTeacherUuid, teacherDO.getTeacherUuid()).remove();
            redisson.getMap(StringConstant.Redis.TEACHER_UUID + teacherDO.getTeacherUuid()).delete();
            redisson.getBucket(StringConstant.Redis.TEACHER_ID + teacherDO.getId()).delete();
            redisson.getBucket(StringConstant.Redis.TEACHER_USER_UUID + teacherDO.getUserUuid()).delete();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, userDO.getUserUuid()).remove();
            redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
            redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
            redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
            redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
        }
    }
}
