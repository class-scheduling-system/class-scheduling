package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.UserAddInfoDTO;
import com.frontleaves.scheduling.models.dto.UserInfoDTO;
import com.frontleaves.scheduling.models.entity.*;
import com.frontleaves.scheduling.models.vo.UserAddVO;
import com.frontleaves.scheduling.models.vo.UserEditVO;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.exception.library.UserAuthenticationException;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@Slf4j
class UserTest {
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redisson;
    @Resource
    private UserDAO userDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private AcademicAffairsPermissionDAO academicAffairsPermissionDAO;
    @Resource
    private MajorDAO majorDAO;
    @Resource
    private StudentDAO studentDAO;
    @Resource
    private TeacherDAO teacherDAO;
    private UserDO setUpUser;

    /**
     * 通过部门 名称获取部门数据
     *
     * @return 部门数据
     */
    private DepartmentDO getDepartmentByName() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        if (departmentDO == null) {
            throw new BusinessException("[dao.UserTest]单元测试通过找不到部门数据",
                    ErrorCode.OPERATION_ERROR);
        }
        return departmentDO;
    }

    /**
     * 通过专业名称获取专业数据
     *
     * @return 专业数据
     */
    private MajorDO getMajorByName() {
        MajorDO majorDO = majorDAO.lambdaQuery().list().get(0);
        if (majorDO == null) {
            throw new BusinessException("[dao.UserTest]单元测试通过找不到专业数据",
                    ErrorCode.OPERATION_ERROR);
        }
        return majorDO;
    }


    @BeforeEach
    void setUp() {
        UserDO userDO = new UserDO();
        userDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("logicUserTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("logicUserTest@test.com")
                .setPhone("13800000000")
                .setStatus(1)
                .setBan(0)
                .setPermission("[\"user:unit:department:tag:category:delete\"]")
                .setRoleUuid(SystemConstant.getRoleAdmin());
        if (userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, userDO.getName()).remove();
        }
        userDAO.save(userDO);
        setUpUser = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();
    }

    @AfterEach
    void tearDown() {
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.USER_UUID + setUpUser.getUserUuid()).delete();
        redisson.getBucket(StringConstant.Redis.USER_NAME + setUpUser.getName()).delete();
        redisson.getBucket(StringConstant.Redis.USER_MAIL + setUpUser.getEmail()).delete();
        redisson.getBucket(StringConstant.Redis.USER_TEL + setUpUser.getPhone()).delete();
    }

    @Test
    void tessGetUserInfo() {
        log.debug("测试获取用户信息");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");
        UserInfoDTO userInfoDTO = userService.getUserInfo(setUpUser.getUserUuid(), request);
        Assertions.assertNotNull(userInfoDTO);
    }

    @Test
    void testAddUser() {
        log.debug("测试添加用户");
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleAdmin(),
                "testAddUser",
                "123456Aa",
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                "",
                0
        );
        if (userDAO.lambdaQuery().eq(UserDO::getName, addVO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
        }
        UserAddInfoDTO userAddInfoDTO = userService.addUser(addVO, false);
        Assertions.assertNotNull(userAddInfoDTO);
        // 删除测试用户
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getName, addVO.getName()).one();
        if (userDO != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
            redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
            redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
            redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
            redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
        }
    }

    @Test
    void testCheckAddUser() {
        log.debug("测试添加用户检查");
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleLeader(),
                "testAddUser",
                "123456Aa",
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                "",
                0
        );
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkAddUser(addVO));
    }

    @Test
    void testAddUserWithAcademic() {
        log.debug("测试添加教务用户");
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleAcademic(),
                "testAddUser",
                "123456Aa",
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                departmentDAO.lambdaQuery().list().get(0).getDepartmentUuid(),
                0
        );
        if (userDAO.lambdaQuery().eq(UserDO::getName, addVO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
        }
        UserAddInfoDTO userAddInfoDTO = userService.addUser(addVO, true);
        Assertions.assertNotNull(userAddInfoDTO);
        //检查是否存到数据库
        Assertions.assertNotNull(
                userDAO.lambdaQuery().eq(UserDO::getUserUuid, userAddInfoDTO.getUser().getUserUuid()).one());
        Assertions.assertNotNull(academicAffairsPermissionDAO.lambdaQuery().eq(
                AcademicAffairsPermissionDO::getAuthorizedUser, userAddInfoDTO.getUser().getUserUuid()).one());
        academicAffairsPermissionDAO.lambdaUpdate().eq(
                AcademicAffairsPermissionDO::getAuthorizedUser, userAddInfoDTO.getUser().getUserUuid()).remove();
        // 删除测试用户
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userAddInfoDTO.getUser().getUserUuid()).one();
        if (userDO != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
            redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
            redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
            redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
            redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
        }
    }

    @Test
    void testCheckAddUserStu() {
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleStudent(),
                "testAddUser",
                "123456Aa",
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                departmentDAO.lambdaQuery().list().get(0).getDepartmentUuid(),
                0
        );
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkAddUser(addVO));
    }

    @Test
    void testCheckAddUserTea() {
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleTeacher(),
                "testAddUser",
                "123456Aa",
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                departmentDAO.lambdaQuery().list().get(0).getDepartmentUuid(),
                0
        );
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkAddUser(addVO));
    }

    @Test
    void testDeleteUser() {
        log.debug("测试删除用户");
        userService.deleteUser(setUpUser.getUserUuid(), new MockHttpServletRequest());
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one();
        Assertions.assertNull(userDO);
        RMap<String, String> userUuidMap = redisson.getMap(
                StringConstant.Redis.USER_UUID + setUpUser.getUserUuid());
        RBucket<String> userNameBucket = redisson.getBucket(
                StringConstant.Redis.USER_NAME + setUpUser.getName());
        RBucket<String> userMailBucket = redisson.getBucket(
                StringConstant.Redis.USER_MAIL + setUpUser.getEmail());
        RBucket<String> userTelBucket = redisson.getBucket(
                StringConstant.Redis.USER_TEL + setUpUser.getPhone());
        Assertions.assertFalse(userUuidMap.isExists());
        Assertions.assertFalse(userNameBucket.isExists());
        Assertions.assertFalse(userMailBucket.isExists());
        Assertions.assertFalse(userTelBucket.isExists());
    }

    @Test
    void testDeleteUserWithStudent() {
        UserDO userDO = new UserDO();
        userDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("deleteUserWithStudent")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("deleteUserWithStudent@test.com")
                .setPhone("13800000111")
                .setStatus(1)
                .setBan(0)
                .setPermission("[\"user:unit:department:tag:category:delete\"]")
                .setRoleUuid(SystemConstant.getRoleStudent());
        if (userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, userDO.getName()).remove();
        }
        userDAO.save(userDO);
        StudentDO setUpStudent = new StudentDO();
        setUpStudent.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("testDeleteUserWithStudent")
                .setGender(1)
                .setGrade("2022")
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz("1班")
                .setUserUuid(userDO.getUserUuid());
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, setUpStudent.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, setUpStudent.getName()).remove();
        }
        studentDAO.save(setUpStudent);
        userService.deleteUser(userDO.getUserUuid(), new MockHttpServletRequest());
        UserDO userDODeleted = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();
        StudentDO studentDO = studentDAO.lambdaQuery().eq(
                StudentDO::getStudentUuid, setUpStudent.getStudentUuid()).one();
        Assertions.assertNull(userDODeleted);
        Assertions.assertNull(studentDO);
        RMap<String, String> userUuidMap = redisson.getMap(
                StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        RBucket<String> userNameBucket = redisson.getBucket(
                StringConstant.Redis.USER_NAME + userDO.getName());
        RBucket<String> userMailBucket = redisson.getBucket(
                StringConstant.Redis.USER_MAIL + userDO.getEmail());
        RBucket<String> userTelBucket = redisson.getBucket(
                StringConstant.Redis.USER_TEL + userDO.getPhone());
        RMap<String, String> studentUuidMap = redisson.getMap(
                StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid());
        RBucket<String> studentIdBucket = redisson.getBucket(
                StringConstant.Redis.STUDENT_ID + setUpStudent.getId());
        RBucket<String> studentUserUuidBucket = redisson.getBucket(
                StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid());
        Assertions.assertFalse(userUuidMap.isExists());
        Assertions.assertFalse(userNameBucket.isExists());
        Assertions.assertFalse(userMailBucket.isExists());
        Assertions.assertFalse(userTelBucket.isExists());
        Assertions.assertFalse(studentUuidMap.isExists());
        Assertions.assertFalse(studentIdBucket.isExists());
        Assertions.assertFalse(studentUserUuidBucket.isExists());

    }

    @Test
    void testDeleteUserWithTeacher() {
        UserDO userDO = new UserDO();
        userDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("testDeleteUserWithTeacher")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("testDeleteUserWithTeacher@test.com")
                .setPhone("13800000111")
                .setStatus(1)
                .setBan(0)
                .setPermission("[\"user:unit:department:tag:category:delete\"]")
                .setRoleUuid(SystemConstant.getRoleTeacher());
        if (userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, userDO.getName()).remove();
        }
        userDAO.save(userDO);
        TeacherDO setUpTeacher = new TeacherDO();
        setUpTeacher.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(getDepartmentByName().getDepartmentUuid())
                .setUserUuid(userDO.getUserUuid())
                .setId("123456")
                .setName("testDeleteUserWithTeacher")
                .setEnglishName("testDeleteUserWithTeacher")
                .setEthnic("汉族")
                .setSex(1)
                .setPhone("14452873800")
                .setEmail("qwerasdfzxcv@qwer.com")
                .setJobTitle("教授")
                .setDesc("这是一个教授");
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one() == null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, setUpTeacher.getId()).remove();
        }
        teacherDAO.save(setUpTeacher);
        userService.deleteUser(userDO.getUserUuid(), new MockHttpServletRequest());
        UserDO userDODeleted = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();
        TeacherDO teacherDO = teacherDAO.lambdaQuery().eq(
                TeacherDO::getTeacherUuid, setUpTeacher.getTeacherUuid()).one();
        Assertions.assertNull(userDODeleted);
        Assertions.assertNull(teacherDO);
        RMap<String, String> userUuidMap = redisson.getMap(
                StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        RBucket<String> userNameBucket = redisson.getBucket(
                StringConstant.Redis.USER_NAME + userDO.getName());
        RBucket<String> userMailBucket = redisson.getBucket(
                StringConstant.Redis.USER_MAIL + userDO.getEmail());
        RBucket<String> userTelBucket = redisson.getBucket(
                StringConstant.Redis.USER_TEL + userDO.getPhone());
        RMap<String, String> teacherUuidMap = redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid());
        RBucket<String> teacherIdBucket = redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + setUpTeacher.getId());
        RBucket<String> teacherUserUuidBucket = redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + setUpTeacher.getUserUuid());
        Assertions.assertFalse(userUuidMap.isExists());
        Assertions.assertFalse(userNameBucket.isExists());
        Assertions.assertFalse(userMailBucket.isExists());
        Assertions.assertFalse(userTelBucket.isExists());
        Assertions.assertFalse(teacherUuidMap.isExists());
        Assertions.assertFalse(teacherIdBucket.isExists());
        Assertions.assertFalse(teacherUserUuidBucket.isExists());
    }

    @Test
    void testDeleteUserWithOtherRole() {
        UserDO userDO = new UserDO();
        userDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("testDeleteUserWithOtherRole")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("testDeleteUserWithOtherRole@test.com")
                .setPhone("13800000222")
                .setStatus(1)
                .setBan(0)
                .setPermission("[\"user:unit:department:tag:category:delete\"]")
                .setRoleUuid(SystemConstant.getRoleLeader());
        if (userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, userDO.getName()).remove();
        }
        userDAO.save(userDO);
        userService.deleteUser(userDO.getUserUuid(), new MockHttpServletRequest());
        UserDO userDODeleted = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();
        Assertions.assertNull(userDODeleted);
        RMap<String, String> userUuidMap = redisson.getMap(
                StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        RBucket<String> userNameBucket = redisson.getBucket(
                StringConstant.Redis.USER_NAME + userDO.getName());
        RBucket<String> userMailBucket = redisson.getBucket(
                StringConstant.Redis.USER_MAIL + userDO.getEmail());
        RBucket<String> userTelBucket = redisson.getBucket(
                StringConstant.Redis.USER_TEL + userDO.getPhone());
        Assertions.assertFalse(userUuidMap.isExists());
        Assertions.assertFalse(userNameBucket.isExists());
        Assertions.assertFalse(userMailBucket.isExists());
        Assertions.assertFalse(userTelBucket.isExists());
    }

    @Test
    void testUpdateUser() {
        log.debug("测试更新用户信息");
        UserEditVO editVO = new UserEditVO("testUpdateUser", "", "testUpdateUser@test.com",
                "13800000001", null, 1,
                SystemConstant.getRoleAdmin(),
                List.of("operate"));
        UserInfoDTO userInfoDTO = userService.updateUser(
                setUpUser.getUserUuid(), editVO, new MockHttpServletRequest());
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one();
        Assertions.assertNotNull(userInfoDTO);
        //测试返回信息
        Assertions.assertEquals(userInfoDTO.getUser().getEmail(), editVO.getEmail());
        Assertions.assertEquals(userInfoDTO.getUser().getName(), editVO.getName());
        Assertions.assertEquals(userInfoDTO.getUser().getPhone(), editVO.getPhone());
        Assertions.assertEquals(userInfoDTO.getUser().getBan(), editVO.getBan());
        Assertions.assertEquals(userInfoDTO.getUser().getRole().getRoleUuid(), editVO.getRoleUuid());
        //测试数据库是否一样
        Assertions.assertEquals(editVO.getName(), userDO.getName());
        Assertions.assertEquals(editVO.getPhone(), userDO.getPhone());
        Assertions.assertEquals(editVO.getEmail(), userDO.getEmail());
        Assertions.assertEquals(editVO.getBan(), userDO.getBan());
        Assertions.assertEquals(editVO.getRoleUuid(), userDO.getRoleUuid());
    }

    @Test
    void testUpdateUserWithStudent() {
        log.debug("测试更新用户信息改变为学生角色");
        UserEditVO editVO = new UserEditVO("testUpdateUser", "", "testUpdateUser@test.com",
                "13800000001", 0, 1,
                SystemConstant.getRoleStudent(),
                List.of("operate"));
        String userUuid = setUpUser.getUserUuid();
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(BusinessException.class, () ->
                userService.updateUser(
                        userUuid, editVO, request)
        );
    }

    @Test
    void testUpdateUserWithTeacher() {
        log.debug("测试更新用户信息改变为教师角色");
        UserEditVO editVO = new UserEditVO("testUpdateUser", "", "testUpdateUser@test.com",
                "13800000001", 0, 1,
                SystemConstant.getRoleTeacher(),
                List.of("operate"));
        String userUuid = setUpUser.getUserUuid();
        MockHttpServletRequest request = new MockHttpServletRequest();
        log.debug("editVO的角色uuid:{}", editVO.getRoleUuid());
        Assertions.assertThrows(BusinessException.class, () ->
                userService.updateUser(
                        userUuid, editVO, request)
        );
    }

    @Test
    void testGetUserList() {
        log.debug("测试获取用户列表");
        // 1. 创建 MockHttpServletRequest
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 2. 调用 userService.getUserList 获取数据
        PageDTO<UserInfoDTO> userInfoDTOPageDTO = userService.getUserList(1, 10, "", false, request);
        // 3. 断言返回结果不为空
        Assertions.assertNotNull(userInfoDTOPageDTO);
        List<UserInfoDTO> userList = userInfoDTOPageDTO.getRecords();
        Assertions.assertNotNull(userList, "用户列表不应为空");
        // 4. 断言数据按创建时间升序排列
        for (int i = 0; i < userList.size() - 1; i++) {
            Timestamp currentTimestamp = userList.get(i).getUser().getCreatedAt();
            Timestamp nextTimestamp = userList.get(i + 1).getUser().getCreatedAt();
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            // 断言当前时间戳不晚于下一个时间戳 (升序：时间越来越新)
            Assertions.assertFalse(currentCreatedAt.isAfter(nextCreatedAt),
                    "数据应该按升序排列，当前数据顺序错误");
        }
    }

    @Test
    void testGetUserListWithKeyWord() {
        log.debug("测试获取用户列表通过关键字");
        // 1. 创建 MockHttpServletRequest
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 2. 调用 userService.getUserList 获取包含 "test" 关键字的用户列表
        PageDTO<UserInfoDTO> userInfoDTOPageDTO = userService.getUserList(
                1, 10, "test", false, request);
        // 3. 断言返回数据不为空
        Assertions.assertNotNull(userInfoDTOPageDTO, "返回的用户分页数据不应为空");
        List<UserInfoDTO> userList = userInfoDTOPageDTO.getRecords();
        Assertions.assertNotNull(userList, "用户列表不应为空");
        Assertions.assertFalse(userList.isEmpty(), "用户列表应包含至少一个用户");
        // 5. 断言数据按 createdAt **升序** 排列
        for (int i = 0; i < userList.size() - 1; i++) {
            Timestamp currentTimestamp = userList.get(i).getUser().getCreatedAt();
            Timestamp nextTimestamp = userList.get(i + 1).getUser().getCreatedAt();
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            // 断言当前时间戳不晚于下一个时间戳 (升序)
            Assertions.assertFalse(currentCreatedAt.isAfter(nextCreatedAt),
                    "数据应该按升序排列，当前数据顺序错误");
        }
    }


    @Test
    void testGetUserInfoWithStudent() {
        log.debug("测试获取用户信息包含学生信息");
        setUpUser.setRoleUuid(SystemConstant.getRoleStudent());
        log.debug("更新用户角色为学生");
        userDAO.updateById(setUpUser);
        StudentDO setUpStudent = new StudentDO();
        setUpStudent.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("testDeleteUserWithStudent")
                .setGender(1)
                .setGrade("2022")
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz("1班")
                .setUserUuid(setUpUser.getUserUuid());
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, setUpStudent.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, setUpStudent.getName()).remove();
        }
        log.debug("保存测试学生数据");
        studentDAO.save(setUpStudent);
        UserInfoDTO userInfoDTO = userService.getUserInfo(setUpUser.getUserUuid(), new MockHttpServletRequest());
        Assertions.assertNotNull(userInfoDTO);
        Assertions.assertNotNull(userInfoDTO.getStudent());
        studentDAO.removeById(setUpStudent);
    }

    @Test
    void testGetUserInfoWithTeacher() {
        log.debug("测试获取用户信息包含老师信息");
        setUpUser.setRoleUuid(SystemConstant.getRoleTeacher());
        log.debug("更新用户角色为老师");
        userDAO.updateById(setUpUser);
        TeacherDO setUpTeacher = new TeacherDO();
        setUpTeacher.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(getDepartmentByName().getDepartmentUuid())
                .setUserUuid(setUpUser.getUserUuid())
                .setId("123456")
                .setName("testDeleteUserWithTeacher")
                .setEnglishName("testDeleteUserWithTeacher")
                .setEthnic("汉族")
                .setSex(1)
                .setPhone("14452873800")
                .setEmail("qwerasdfzxcv@qwer.com")
                .setJobTitle("教授")
                .setDesc("这是一个教授");
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one() == null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, setUpTeacher.getId()).remove();
        }
        teacherDAO.save(setUpTeacher);
        UserInfoDTO userInfoDTO = userService.getUserInfo(setUpUser.getUserUuid(), new MockHttpServletRequest());
        Assertions.assertNotNull(userInfoDTO);
        Assertions.assertNotNull(userInfoDTO.getTeacher());
        teacherDAO.removeById(setUpTeacher);
    }

    @Test
    void testCheckAddUserReturnTure() {
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleAcademic(),
                "testAddUser",
                PasswordUtil.encrypt("123456Aa"),
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                departmentDAO.lambdaQuery().list().get(0).getDepartmentUuid(),
                0
        );
        Assertions.assertTrue(userService.checkAddUser(addVO));
    }

    @Test
    void testCheckAddUserReturnFalse() {
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleAdmin(),
                "testAddUser",
                PasswordUtil.encrypt("123456Aa"),
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                null,
                null
        );
        Assertions.assertFalse(userService.checkAddUser(addVO));
    }

    @Test
    void testCheckAddUserThrow() {
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleAcademic(),
                "testAddUser",
                PasswordUtil.encrypt("123456Aa"),
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                null,
                0
        );
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkAddUser(addVO));
    }

    @Test
    void testAddUserWithNoPassword() {
        log.debug("测试添加用户让系统生成密码");
        UserAddVO addVO = new UserAddVO(
                SystemConstant.getRoleAdmin(),
                "testAddUser",
                "",
                "testAddUser@test.com",
                "13800000001",
                List.of("operate"),
                "",
                0
        );
        if (userDAO.lambdaQuery().eq(UserDO::getName, addVO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
        }
        UserAddInfoDTO userAddInfoDTO = userService.addUser(addVO, false);
        Assertions.assertNotNull(userAddInfoDTO);
        Assertions.assertNotNull(userAddInfoDTO.getNewPassword());
        Assertions.assertFalse(userAddInfoDTO.getNewPassword().isEmpty());
        userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
    }

    @Test
    void testCheckUuidNoUuid() {
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkUuid(""));
    }

    @Test
    void testDeleteUserWithFalseUuid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uuid  = UuidUtil.generateUuidNoDash();
        Assertions.assertThrows(UserAuthenticationException.class, () ->
                userService.deleteUser(uuid,request));
    }


    // 用户不存在，抛出 UserAuthenticationException
    @Test
    void testCheckUpdateDate_UserNotExist() {
        UserEditVO editVO = new UserEditVO(
                "testUpdateUser", "", "testUpdateUser@test.com",
                "13800000001", null, 1,
                SystemConstant.getRoleAdmin(),
                List.of("operate"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uuid = UuidUtil.generateUuidNoDash();
        Assertions.assertThrows(UserAuthenticationException.class, () ->
            userService.updateUser(uuid, editVO, request)
        );
    }

    //用户名已存在，抛出 BusinessException
    @Test
    void testCheckUpdateDate_UserNameExists() {
        UserDO existUser = new UserDO();
        existUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("existUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("existUser@test.com")
                .setPhone("13800000011")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(SystemConstant.getRoleAdmin());
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getName, existUser.getName()).one();
        if (userDO != null) {
           userDAO.removeById(userDO);
        }
        userDAO.save(existUser);
        UserEditVO editVO = new UserEditVO(
                "existUser", "", "",
                "", null, 1,
                SystemConstant.getRoleAdmin(),
                List.of("operate"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uuid = setUpUser.getUserUuid();
        Assertions.assertThrows(BusinessException.class, () ->
            userService.updateUser(uuid, editVO, request)
        );
        userDAO.removeById(existUser);
        redisson.getMap(StringConstant.Redis.USER_UUID + existUser.getUserUuid());
        redisson.getBucket(StringConstant.Redis.USER_NAME + existUser.getName());
        redisson.getBucket(StringConstant.Redis.USER_MAIL + existUser.getEmail());
        redisson.getBucket(StringConstant.Redis.USER_TEL + existUser.getPhone());
    }

    // 3. 邮箱已存在，抛出 BusinessException
    @Test
    void testCheckUpdateDate_EmailExists() {
        UserDO existUser = new UserDO();
        existUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("existUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("existUser@test.com")
                .setPhone("13800000011")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(SystemConstant.getRoleAdmin());
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getName, existUser.getName()).one();
        if (userDO != null) {
            userDAO.removeById(userDO);
        }
        userDAO.save(existUser);
        UserEditVO editVO = new UserEditVO(
                "testUpdateUser", "", "existUser@test.com",
                "13800000001", null, 1,
                SystemConstant.getRoleAdmin(),
                List.of("operate"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uuid = setUpUser.getUserUuid();
        Assertions.assertThrows(BusinessException.class, () ->
            userService.updateUser(uuid, editVO, request)
        );
        userDAO.removeById(existUser);
        redisson.getMap(StringConstant.Redis.USER_UUID + existUser.getUserUuid());
        redisson.getBucket(StringConstant.Redis.USER_NAME + existUser.getName());
        redisson.getBucket(StringConstant.Redis.USER_MAIL + existUser.getEmail());
        redisson.getBucket(StringConstant.Redis.USER_TEL + existUser.getPhone());
    }

    // 4. 手机号已存在，抛出 BusinessException
    @Test
    void testCheckUpdateDate_PhoneExists() {
        UserDO existUser = new UserDO();
        existUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("existUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("existUser@test.com")
                .setPhone("13800000011")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(SystemConstant.getRoleAdmin());
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getName, existUser.getName()).one();
        if (userDO != null) {
            userDAO.removeById(userDO);
        }
        userDAO.save(existUser);
        UserEditVO editVO = new UserEditVO(
                "testUpdateUser", "", "testUpdateUser@test.com",
                "13800000011", null, 1,
                SystemConstant.getRoleAdmin(),
                List.of("operate"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uuid = setUpUser.getUserUuid();
        Assertions.assertThrows(BusinessException.class, () ->
            userService.updateUser(uuid, editVO, request)
        );
        userDAO.removeById(existUser);
        redisson.getMap(StringConstant.Redis.USER_UUID + existUser.getUserUuid());
        redisson.getBucket(StringConstant.Redis.USER_NAME + existUser.getName());
        redisson.getBucket(StringConstant.Redis.USER_MAIL + existUser.getEmail());
        redisson.getBucket(StringConstant.Redis.USER_TEL + existUser.getPhone());
    }

    // 5. 角色不存在，抛出 ServerInternalErrorException
    @Test
    void testCheckUpdateDate_RoleNotExist() {
        UserEditVO editVO = new UserEditVO(
                "testUpdateUser", "", "testUpdateUser@test.com",
                "13800000001", null, 1,
                UuidUtil.generateUuidNoDash(),
                List.of("operate"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        String uuid = setUpUser.getUserUuid();
        Assertions.assertThrows(BusinessException.class, () ->
            userService.updateUser(uuid, editVO, request)
        );
    }
    @Test
    void testGetUserListWithStudent(){
        log.debug("测试获取用户列表中包含学生信息");
        setUpUser.setRoleUuid(SystemConstant.getRoleStudent());
        userDAO.updateById(setUpUser);
        StudentDO setUpStudent = new StudentDO();
        setUpStudent.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("testGetUserListWithStudent")
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
        PageDTO<UserInfoDTO> userInfoDTOPageDTO  = userService.getUserList(
                1, 10, "logicUserTest", false, new MockHttpServletRequest());
        Assertions.assertNotNull(userInfoDTOPageDTO.getRecords());
        for (UserInfoDTO  userInfoDTO: userInfoDTOPageDTO.getRecords()) {
            Assertions.assertNotNull(userInfoDTO.getStudent());
        }
        studentDAO.removeById(setUpStudent);
        redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getStudentUuid()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid()).delete();
    }
    @Test
    void testGetUserListWithTeacher(){
        log.debug("测试获取用户列表中包含老师信息");
        setUpUser.setRoleUuid(SystemConstant.getRoleTeacher());
        userDAO.updateById(setUpUser);
        TeacherDO setUpTeacher = new TeacherDO();
        setUpTeacher.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(getDepartmentByName().getDepartmentUuid())
                .setUserUuid(setUpUser.getUserUuid())
                .setId("123456")
                .setName("testGetUserListWithTeacher")
                .setEnglishName("testGetUserListWithTeacher")
                .setEthnic("汉族")
                .setSex(1)
                .setPhone("14452873800")
                .setEmail("testGetUserListWithTeacher@qwer.com")
                .setJobTitle("教授")
                .setDesc("这是一个教授");
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one() == null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, setUpTeacher.getId()).remove();
        }
        teacherDAO.save(setUpTeacher);
        PageDTO<UserInfoDTO> userInfoDTOPageDTO  = userService.getUserList(
                1, 10, "logicUserTest", false, new MockHttpServletRequest());
        Assertions.assertNotNull(userInfoDTOPageDTO.getRecords());
        for (UserInfoDTO  userInfoDTO: userInfoDTOPageDTO.getRecords()) {
            Assertions.assertNotNull(userInfoDTO.getTeacher());
        }
        teacherDAO.removeById(setUpTeacher);
        redisson.getMap(StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_ID + setUpTeacher.getId()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_USER_UUID + setUpTeacher.getUserUuid()).delete();
    }

    @Test
    void testCheckPageAndSize (){
        log.debug("测试检查页码和页面大小");
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkPageAndSize(0, 10));
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkPageAndSize(1, 0));
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkPageAndSize(null, 10));
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkPageAndSize(1, null));
    }
    @Test
    void testUpdateStudent (){
        log.debug("测试更新学生to其他角色");
        setUpUser.setRoleUuid(SystemConstant.getRoleStudent());
        userDAO.updateById(setUpUser);
        UserEditVO editVO = new UserEditVO("testUpdateUser", "", "testUpdateUser@test.com",
                "13800000001", null, 1,
                SystemConstant.getRoleAdmin(),
                List.of("operate"));
        String uuid = setUpUser.getUserUuid();
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(BusinessException.class, () ->
                userService.updateUser(
                        uuid, editVO, request)
        );
    }
    @Test
    void testUpdateTeacher (){
        log.debug("测试更新老师to其他角色");
        setUpUser.setRoleUuid(SystemConstant.getRoleTeacher());
        userDAO.updateById(setUpUser);
        UserEditVO editVO = new UserEditVO("testUpdateUser", "", "testUpdateUser@test.com",
                "13800000001", null, 1,
                SystemConstant.getRoleAdmin(),
                List.of("operate"));
        String uuid = setUpUser.getUserUuid();
        MockHttpServletRequest request = new MockHttpServletRequest();
        Assertions.assertThrows(BusinessException.class, () ->
                userService.updateUser(
                        uuid, editVO, request)
        );
    }
    @Test
    void testCheckUserExist (){
        log.debug("测试检查用户是否存在");
        String name = setUpUser.getName();
        String email = setUpUser.getEmail();
        String phone = setUpUser.getPhone();
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkUserExist(name, email, phone));
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkUserExist("testCheckUserExist", email, phone));
        Assertions.assertThrows(BusinessException.class, () ->
                userService.checkUserExist("testCheckUserExist", "testCheckUserExist@test.com",
                        phone));
    }
}
