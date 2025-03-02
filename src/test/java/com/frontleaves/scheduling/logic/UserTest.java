package com.frontleaves.scheduling.logic;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.UserAddInfoDTO;
import com.frontleaves.scheduling.models.dto.UserInfoDTO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.UserAddVO;
import com.frontleaves.scheduling.models.vo.UserEditVO;
import com.frontleaves.scheduling.services.UserService;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
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
    private RoleDAO roleDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private UserDAO userDAO;
    private UserDO setUpUser;


    /**
     * 根据角色名称获取角色信息
     * 此方法特别针对“管理员”角色，用于在系统中查找此角色的详细信息
     * 如果找不到指定的角色，则抛出业务异常，指示操作失败
     *
     * @return RoleDO 返回角色数据对象，包含角色的详细信息
     * @throws BusinessException 当在数据库中找不到指定角色名称的角色时抛出此异常
     */
    private @NotNull RoleDO getRoleByName(String roleName) {
        // 使用Lambda表达式查询“管理员”角色的详细信息
        RoleDO roleDO = roleDAO.lambdaQuery().eq(RoleDO::getRoleName, roleName).one();
        // 如果查询结果为空，则抛出异常
        if (roleDO == null) {
            throw new BusinessException("[dao.StudentTest]单元测试通过部门名称找不到部门数据", ErrorCode.OPERATION_ERROR);
        }
        // 返回查询到的角色信息
        return roleDO;
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
                .setRoleUuid(getRoleByName("管理").getRoleUuid());
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
        UserAddVO addVO = new UserAddVO();
        addVO.setRoleUuid(getRoleByName("管理").getRoleUuid())
                .setName("testAddUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("testAddUser@test.com")
                .setPhone("13800000001")
                .setPermission(List.of("operate"));
        if (userDAO.lambdaQuery().eq(UserDO::getName, addVO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
        }
        UserAddInfoDTO userAddInfoDTO = userService.addUser(addVO);
        Assertions.assertNotNull(userAddInfoDTO);
        // 删除测试用户
        UserDO userDo = userDAO.lambdaQuery().eq(UserDO::getName, addVO.getName()).one();
        if (userDo != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, addVO.getName()).remove();
            redisson.getMap(StringConstant.Redis.USER_UUID + userDo.getUserUuid()).delete();
            redisson.getBucket(StringConstant.Redis.USER_NAME + userDo.getName()).delete();
            redisson.getBucket(StringConstant.Redis.USER_MAIL + userDo.getEmail()).delete();
            redisson.getBucket(StringConstant.Redis.USER_TEL + userDo.getPhone()).delete();
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
    void testUpdateUser() {
        log.debug("测试更新用户信息");
        UserEditVO editVO = new UserEditVO();
        editVO.setName("testUpdateUser")
                .setPhone("13800000001")
                .setEmail("testUpdateUser@test.com")
                .setPermission(List.of("operate"))
                .setBan(1)
                .setStatus(0)
                .setRoleUuid(getRoleByName("管理员").getRoleUuid());
        UserInfoDTO userInfoDTO = userService.updateUser(
                setUpUser.getUserUuid(), editVO, new MockHttpServletRequest());
        UserDO userDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one();
        Assertions.assertNotNull(userInfoDTO);
        //测试返回信息
        Assertions.assertEquals(userInfoDTO.getUser().getEmail(), editVO.getEmail());
        Assertions.assertEquals(userInfoDTO.getUser().getName(), editVO.getName());
        Assertions.assertEquals(userInfoDTO.getUser().getPhone(), editVO.getPhone());
        Assertions.assertEquals(userInfoDTO.getUser().getBan(), editVO.getBan());
        Assertions.assertEquals(userInfoDTO.getUser().getStatus(), editVO.getStatus());
        Assertions.assertEquals(userInfoDTO.getUser().getRole().getRoleUuid(), editVO.getRoleUuid());
        //测试数据库是否一样
        Assertions.assertEquals(editVO.getName(), userDO.getName());
        Assertions.assertEquals(editVO.getPhone(), userDO.getPhone());
        Assertions.assertEquals(editVO.getEmail(), userDO.getEmail());
        Assertions.assertEquals(editVO.getBan(), userDO.getBan());
        Assertions.assertEquals(editVO.getStatus(), userDO.getStatus());
        Assertions.assertEquals(editVO.getRoleUuid(), userDO.getRoleUuid());
    }

    @Test
    void testUpdateUserWithStudent() {
        log.debug("测试更新用户信息改变为学生角色");
        UserEditVO editVO = new UserEditVO();
        editVO.setName("testUpdateUser")
                .setPhone("13800000001")
                .setEmail("testUpdateUser@test.com")
                .setPermission(List.of("operate"))
                .setBan(1)
                .setStatus(0)
                .setRoleUuid(getRoleByName("学生").getRoleUuid());
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
        UserEditVO editVO = new UserEditVO();
        editVO.setName("testUpdateUser")
                .setPhone("13800000001")
                .setEmail("testUpdateUser@test.com")
                .setPermission(List.of("operate"))
                .setBan(1)
                .setStatus(0)
                .setRoleUuid(getRoleByName("老师").getRoleUuid());
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
        PageDTO<UserInfoDTO> userInfoDTOPageDTO = userService.getUserList(1, 10, "test", false, request);
        // 3. 断言返回数据不为空
        Assertions.assertNotNull(userInfoDTOPageDTO, "返回的用户分页数据不应为空");
        List<UserInfoDTO> userList = userInfoDTOPageDTO.getRecords();
        Assertions.assertNotNull(userList, "用户列表不应为空");
        Assertions.assertFalse(userList.isEmpty(), "用户列表应包含至少一个用户");
        // 4. 断言所有用户的 name、email 或 phone 包含关键字 "test"
        for (UserInfoDTO user : userList) {
            boolean containsKeyword = user.getUser().getName().contains("test") ||
                    user.getUser().getEmail().contains("test") ||
                    user.getUser().getPhone().contains("test");

            Assertions.assertTrue(containsKeyword,
                    "用户数据应该包含关键字 'test'，用户信息 - 名称: " + user.getUser().getName() +
                            ", 邮箱: " + user.getUser().getEmail() + ", 电话: " + user.getUser().getPhone());
        }
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