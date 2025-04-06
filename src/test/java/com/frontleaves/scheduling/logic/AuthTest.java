package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.base.ForgetPasswordResponseDTO;
import com.frontleaves.scheduling.models.dto.base.TokenDTO;
import com.frontleaves.scheduling.models.dto.email.EmailVerificationTokenDTO;
import com.frontleaves.scheduling.models.dto.lite.BackProfileDTO;
import com.frontleaves.scheduling.models.dto.merge.UserLoginDTO;
import com.frontleaves.scheduling.models.entity.base.*;
import com.frontleaves.scheduling.models.vo.UserInitializationVO;
import com.frontleaves.scheduling.models.vo.UserLoginVO;
import com.frontleaves.scheduling.services.AuthService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
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
    @Autowired
    private TokenDAO tokenDAO;


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

    @Test
    void testForgetPassword() {
        log.debug("测试忘记密码");
        String email = userDAO.lambdaQuery().list().get(0).getEmail();
        ForgetPasswordResponseDTO forgetPasswordResponseDTO = authService.forgetPassword(email);
        Assertions.assertNotNull(forgetPasswordResponseDTO);
    }

    @Test
    void testForgetPasswordWihError() {
        log.debug("测试忘记密码错误");
        String email = "2903128990321@";
        Assertions.assertThrows(BusinessException.class, () -> authService.forgetPassword(email));
        String email1 = "asdhajihdkjashdkjas@qq.com";
        Assertions.assertThrows(BusinessException.class, () -> authService.forgetPassword(email1));
    }

    private UserDO setUp() {
        UserDO userDO = new UserDO();
        userDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("logicUserTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("logicAuthTest@test.com")
                .setPhone("13800000000")
                .setStatus((byte) 1)
                .setBan(false)
                .setPermission("[\"user:unit:department:tag:category:delete\"]")
                .setRoleUuid(SystemConstant.getRoleAdmin());
        if (userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, userDO.getName()).remove();
        }
        userDAO.save(userDO);
        return userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();
    }

    private void tearDown(UserDO setUpUser) {
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.USER_UUID + setUpUser.getUserUuid()).delete();
        redisson.getBucket(StringConstant.Redis.USER_NAME + setUpUser.getName()).delete();
        redisson.getBucket(StringConstant.Redis.USER_MAIL + setUpUser.getEmail()).delete();
        redisson.getBucket(StringConstant.Redis.USER_TEL + setUpUser.getPhone()).delete();
    }

    @Test
    void testCheckResetPassword() {
        log.debug("测试检查重置密码");
        UserDO userDO = setUp();
        EmailVerificationTokenDTO emailVerificationTokenDTO = tokenDAO.createEmailToken(userDO);
        String token = emailVerificationTokenDTO.getToken();
        UserDO backUserDO = authService.checkResetPassword(token, "654321Aa");
        Assertions.assertNotNull(backUserDO);
        tearDown(userDO);
    }

    @Test
    void testCheckResetPasswordWithError() {
        log.debug("测试检查重置密码报错");
        Assertions.assertThrows(
                BusinessException.class, () ->
                        authService.checkResetPassword(
                                "1213213145241453112", "654321Aa")
        );
    }

    @Test
    void testCheckProfile() {
        log.debug("测试检查个人信息");
        UserDO userDO = setUp();
        TokenDTO tokenDTO = tokenDAO.createToken(userDO);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenDTO.getToken());
        UserDO backUserDO = authService.checkProfile("logicAuthTest", "logicAuthTest@test123.com"
                , "13800000011", request);
        Assertions.assertNotNull(backUserDO);
        tearDown(userDO);
        redisson.getKeys().deleteByPattern(StringConstant.Redis.TOKEN + "*");
    }

    @Test
    void testCheckProfileWithError() {
        log.debug("测试检查个人信息报错");
        UserDO userDO = setUp();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + UuidUtil.generateStringUuid());
        Assertions.assertThrows(
                UserAuthenticationException.class,
                () -> authService.checkProfile("logicAuthTest", "logicAuthTest@test123.com"
                        , "13800000011", request)
        );
        tearDown(userDO);
        redisson.getKeys().deleteByPattern(StringConstant.Redis.TOKEN + "*");
    }

    @Test
    void testProfile() {
        log.debug("测试个人信息更新");
        UserDO userDO = setUp();
        BackProfileDTO backProfileDTO = authService.profile(userDO);
        Assertions.assertNotNull(backProfileDTO);
        tearDown(userDO);
    }


    @Test
    void testChangePassword() {
        log.debug("测试修改密码");
        UserDO userDO = setUp();
        TokenDTO tokenDTO = tokenDAO.createToken(userDO);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenDTO.getToken());
        authService.changePassword("123456Aa", "654321Aa", request);
        //检查密码是否更改成功
        UserDO backUserDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();
        Assertions.assertNotNull(backUserDO);
        Assertions.assertTrue(PasswordUtil.verify("654321Aa", backUserDO.getPassword()));
        tearDown(userDO);
        redisson.getKeys().deleteByPattern(StringConstant.Redis.TOKEN + "*");
    }

    @Test
    void testChangePasswordWithError() {
        log.debug("测试修改密码报错");
        UserDO userDO = setUp();
        TokenDTO tokenDTO = tokenDAO.createToken(userDO);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokenDTO.getToken());
        //当前密码错误
        Assertions.assertThrows(
                BusinessException.class,
                () -> authService.changePassword("",
                        "654321Aa", request)
        );
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + UuidUtil.generateStringUuid());
        //token错误
        Assertions.assertThrows(
                UserAuthenticationException.class,
                () -> authService.changePassword("123456Aa",
                        "654321Aa", request1)
        );
        tearDown(userDO);
        redisson.getKeys().deleteByPattern(StringConstant.Redis.TOKEN + "*");
    }
}
