package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.PasswordUtil;
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

import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@Slf4j
class UserTest {
    @Resource
    private UserDAO userDAO;
    @Resource
    private RedissonClient redisson;

    private UserDO userDO;

    @BeforeEach
    void setUp() {
        log.debug("初始化UserDAOTest用户数据");
        userDO = new UserDO();
        userDO.setUserUuid("2b700c3c14ef4afdbf18c0a89d5311b9")
                .setName("ZhangSan55511164")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("12138@qq.com")
                .setPhone("12345678901")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(SystemConstant.getRoleAdmin())
                .setPermission("[\"user:unit:department:delete\"]");
        // 清理数据库中可能已经存在的相同UUID的用户
        if (userDAO.getUserByUuid(userDO.getUserUuid()) != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, userDO.getUserUuid()).remove();
        }
        // 保存新用户
        userDAO.save(userDO);
        // 缓存用户到 Redis
        RMap<String, String> mapUserUuid = redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        RBucket<String> bucketUserName = redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName());
        RBucket<String> bucketUserEmail = redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail());
        RBucket<String> bucketUserPhone = redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone());
        bucketUserName.set(userDO.getUserUuid());
        bucketUserEmail.set(userDO.getUserUuid());
        bucketUserPhone.set(userDO.getUserUuid());
        mapUserUuid.putAll(ConvertUtil.convertObjectToMapString(userDO));
        bucketUserName.expire(Duration.ofSeconds(86400));
        mapUserUuid.expire(Duration.ofSeconds(86400));
        bucketUserEmail.expire(Duration.ofSeconds(86400));
        bucketUserPhone.expire(Duration.ofSeconds(86400));
    }

    @AfterEach
    void tearDown() {
        log.debug("清理UserDAOTest用户数据");
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, userDO.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
        redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
        redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
        redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
    }

    @Test
    void testGetUserByUuid() {
        log.debug("测试获取用户信息(UUID)");
        UserDO newUserDO = userDAO.getUserByUuid(userDO.getUserUuid());
        // 如果 userDO 在数据库中，应该返回 null
        Assertions.assertNotNull(newUserDO);
        log.debug("删除缓存并再次获取");
        redisson.getBucket(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
        UserDO newUserDO2 = userDAO.getUserByUuid(userDO.getUserUuid());
        Assertions.assertNotNull(newUserDO2);
        redisson.getBucket(StringConstant.Redis.USER_UUID + userDO.getUserUuid()).delete();
    }

    @Test
    void testGetUserByName() {
        log.debug("测试获取用户信息(USERNAME)");
        UserDO newUserDO = userDAO.getUserByName(userDO.getName());
        // 如果 userDO 在数据库中，应该返回 null
        Assertions.assertNotNull(newUserDO);
        redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
        UserDO newUserDO2 = userDAO.getUserByUuid(userDO.getUserUuid());
        Assertions.assertNotNull(newUserDO2);
        redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName()).delete();
    }

    @Test
    void testGetUserByMail() {
        log.debug("测试获取用户信息(EMAIL)");
        UserDO newUserDO = userDAO.getUserByMail(userDO.getEmail());
        // 如果 userDO 在数据库中，应该返回 null
        Assertions.assertNotNull(newUserDO);
        redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
        UserDO newUserDO2 = userDAO.getUserByUuid(userDO.getUserUuid());
        Assertions.assertNotNull(newUserDO2);
        redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail()).delete();
    }

    @Test
    void testGetUserByTel() {
        log.debug("测试获取用户信息(TEL)");
        UserDO newUserDO = userDAO.getUserByTel(userDO.getPhone());
        Assertions.assertNotNull(newUserDO);
        redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
        UserDO newUserDO2 = userDAO.getUserByUuid(userDO.getUserUuid());
        Assertions.assertNotNull(newUserDO2);
        redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone()).delete();
    }

    @Test
    void testDeleteUser() {
        log.debug("测试删除用户信息");
        userDAO.deleteUser(userDO);
        Assertions.assertNull(userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one());
        RMap<String, String> mapUserUuid = redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        RBucket<String> mapUserName = redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName());
        RBucket<String> mapUserEmail = redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail());
        RBucket<String> mapUserPhone = redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone());
        Assertions.assertFalse(mapUserUuid.isExists());
        Assertions.assertFalse(mapUserName.isExists());
        Assertions.assertFalse(mapUserEmail.isExists());
        Assertions.assertFalse(mapUserPhone.isExists());
    }

    @Test
    void testUpdateUser() {
        log.debug("测试更新用户信息");
        UserDO oldUserDO = userDO;
        UserDO newUserDO = new UserDO();
        newUserDO.setUserUuid(oldUserDO.getUserUuid())
                .setName("ZhangSan555")
                .setEmail("22344234@qq.com")
                .setPassword(PasswordUtil.encrypt("123456qwerQWER"))
                .setStatus(0)
                .setBan(1)
                .setPermission("[\"user:unit\"]");
        userDAO.updateUser(oldUserDO, newUserDO);
        UserDO updateNewUserDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, oldUserDO.getUserUuid()).one();
        Assertions.assertEquals(newUserDO.getName(), updateNewUserDO.getName());
        Assertions.assertEquals(newUserDO.getEmail(), updateNewUserDO.getEmail());
        Assertions.assertEquals(newUserDO.getPassword(), updateNewUserDO.getPassword());
        Assertions.assertEquals(newUserDO.getStatus(), updateNewUserDO.getStatus());
        Assertions.assertEquals(newUserDO.getBan(), updateNewUserDO.getBan());
        Assertions.assertEquals(newUserDO.getPermission(), updateNewUserDO.getPermission());
        log.debug("检测更新后缓存是否删除");
        RMap<String, String> mapUserUuid = redisson.getMap(StringConstant.Redis.USER_UUID + oldUserDO.getUserUuid());
        RBucket<String> mapUserName = redisson.getBucket(StringConstant.Redis.USER_NAME + oldUserDO.getName());
        RBucket<String> mapUserEmail = redisson.getBucket(StringConstant.Redis.USER_MAIL + oldUserDO.getEmail());
        RBucket<String> mapUserPhone = redisson.getBucket(StringConstant.Redis.USER_TEL + oldUserDO.getPhone());
        Assertions.assertFalse(mapUserUuid.isExists());
        Assertions.assertFalse(mapUserName.isExists());
        Assertions.assertFalse(mapUserEmail.isExists());
        Assertions.assertFalse(mapUserPhone.isExists());
        //删除更新后的用户
        userDAO.lambdaUpdate().eq(UserDO::getUserUuid, updateNewUserDO.getUserUuid()).remove();
    }

    @Test
    void testGetUserDoAsc() {
        log.debug("测试获取用户列表");
        // 获取 userDAO 返回的 Page 对象
        Page<UserDO> pageAsc = userDAO.getUserDoPage(1, 10, "", false);
        // 获取总页数
        int totalPages = (int) pageAsc.getTotal();
        // 判断页数是否大于 1
        Assertions.assertTrue(totalPages >= 1, "总页数应该大于等于 1");
        // 判断是否为正序（根据 createdAt 字段）
        List<UserDO> content = pageAsc.getRecords();
        for (int i = 0; i < content.size() - 1; i++) {
            Timestamp currentTimestamp = content.get(i).getCreatedAt();
            Timestamp nextTimestamp = content.get(i + 1).getCreatedAt();
            // 将 Timestamp 转换为 LocalDateTime
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            // 检查当前项的 createdAt 是否严格大于下一项的 createdAt
            Assertions.assertFalse(currentCreatedAt.isAfter(nextCreatedAt),
                    "数据应该按正序排列，当前数据顺序错误");
        }
    }

    @Test
    void getUserDoPageDesc() {
        // 获取 userDAO 返回的 Page 对象（降序）
        Page<UserDO> pageDesc = userDAO.getUserDoPage(1, 10, "", true);
        int totalPagesDesc = (int) pageDesc.getTotal();
        Assertions.assertTrue(totalPagesDesc > 1, "总页数应该大于 1");
        List<UserDO> contentDesc = pageDesc.getRecords();
        for (int i = 0; i < contentDesc.size() - 1; i++) {
            Timestamp currentTimestamp = contentDesc.get(i).getCreatedAt();
            Timestamp nextTimestamp = contentDesc.get(i + 1).getCreatedAt();
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            Assertions.assertFalse(currentCreatedAt.isBefore(nextCreatedAt),
                    "数据应该按降序排列，当前数据顺序错误");
        }
    }

    @Test
    void getUserDoPageAscByKeyWordDesc() {
        Page<UserDO> pageKeyWord = userDAO.getUserDoPage(1, 10, "test", false);
        List<UserDO> contentKeyWord = pageKeyWord.getRecords();
        for (int i = 0; i < contentKeyWord.size() - 1; i++) {
            Timestamp currentTimestamp = contentKeyWord.get(i).getCreatedAt();
            Timestamp nextTimestamp = contentKeyWord.get(i + 1).getCreatedAt();
            // 将 Timestamp 转换为 LocalDateTime
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            // 检查当前项的 createdAt 是否严格大于下一项的 createdAt
            Assertions.assertFalse(currentCreatedAt.isAfter(nextCreatedAt),
                    "数据应该按正序排列，当前数据顺序错误");
        }
    }

    @Test
    void getUserDoPageKeyDescByKeyWord() {
        Page<UserDO> pageKeyWord = userDAO.getUserDoPage(1, 10, "test", true);
        List<UserDO> contentKeyWord = pageKeyWord.getRecords();
        for (int i = 0; i < contentKeyWord.size() - 1; i++) {
            Timestamp currentTimestamp = contentKeyWord.get(i).getCreatedAt();
            Timestamp nextTimestamp = contentKeyWord.get(i + 1).getCreatedAt();
            LocalDateTime currentCreatedAt = currentTimestamp.toLocalDateTime();
            LocalDateTime nextCreatedAt = nextTimestamp.toLocalDateTime();
            Assertions.assertFalse(currentCreatedAt.isBefore(nextCreatedAt),
                    "数据应该按降序排列，当前数据顺序错误");
        }
    }
}

