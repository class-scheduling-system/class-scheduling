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
                PasswordUtil.encrypt("123456Aa"),
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
    void testAddUserWithAcademic() {
        log.debug("测试添加教务用户");
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
        Assertions.assertFalse(userUuidMap.isExists());
        Assertions.assertFalse(userNameBucket.isExists());
        Assertions.assertFalse(userMailBucket.isExists());
        Assertions.assertFalse(userTelBucket.isExists());
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

}
