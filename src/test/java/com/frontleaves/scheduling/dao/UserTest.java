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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
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
                .setStatus((byte) 1)
                .setBan(false)
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
                .setStatus((byte) 0)
                .setBan(true)
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

    /**
     * 测试更新用户密码功能
     * <p>
     * 该方法测试 UserDAO 中的 updateUserPassword 方法，验证以下功能：
     * 1. 密码是否成功更新到数据库
     * 2. Redis 缓存是否被正确清除以保持数据一致性
     * </p>
     */
    @Test
    void testUpdateUserPassword() {
        log.debug("测试更新用户密码");

        // 验证初始缓存状态
        RMap<String, String> userUuidMap = redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        Assertions.assertTrue(userUuidMap.isExists(), "测试前用户UUID缓存应该存在");

        // 保存原始密码以便验证
        String originalPassword = userDO.getPassword();

        // 创建包含新密码的用户对象
        String newPassword = PasswordUtil.encrypt("NewPassword789");
        UserDO updatePasswordDO = new UserDO();
        updatePasswordDO.setUserUuid(userDO.getUserUuid())
                .setPassword(newPassword)
                .setName(userDO.getName())
                .setEmail(userDO.getEmail())
                .setPhone(userDO.getPhone());

        // 执行密码更新操作
        userDAO.updateUserPassword(updatePasswordDO);

        // 从数据库获取更新后的用户数据
        UserDO updatedUserDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();

        // 验证密码是否已更新
        Assertions.assertNotNull(updatedUserDO, "更新后应能从数据库获取用户");
        Assertions.assertNotEquals(originalPassword, updatedUserDO.getPassword(), "密码应该已被更改");
        Assertions.assertEquals(newPassword, updatedUserDO.getPassword(), "新密码应该已正确保存");

        // 验证其他字段没有被修改
        Assertions.assertEquals(userDO.getName(), updatedUserDO.getName(), "用户名不应被修改");
        Assertions.assertEquals(userDO.getEmail(), updatedUserDO.getEmail(), "邮箱不应被修改");
        Assertions.assertEquals(userDO.getPhone(), updatedUserDO.getPhone(), "电话不应被修改");

        // 验证Redis缓存是否已被清除
        userUuidMap = redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        RBucket<String> userNameBucket = redisson.getBucket(StringConstant.Redis.USER_NAME + userDO.getName());
        RBucket<String> userMailBucket = redisson.getBucket(StringConstant.Redis.USER_MAIL + userDO.getEmail());
        RBucket<String> userTelBucket = redisson.getBucket(StringConstant.Redis.USER_TEL + userDO.getPhone());

        // 检查所有缓存是否已被删除
        Assertions.assertFalse(userUuidMap.isExists(), "更新密码后用户UUID缓存应被删除");
        Assertions.assertFalse(userNameBucket.isExists(), "更新密码后用户名缓存应被删除");
        Assertions.assertFalse(userMailBucket.isExists(), "更新密码后用户邮箱缓存应被删除");
        Assertions.assertFalse(userTelBucket.isExists(), "更新密码后用户电话缓存应被删除");

        // 更新用户对象以反映数据库中的新状态（为了tearDown正常工作）
        userDO = updatedUserDO;
    }

    /**
     * 测试更新用户资料功能
     * <p>
     * 该方法测试 UserDAO 中的 updateUserProfile 方法，验证以下功能：
     * 1. 只有非null字段被更新到数据库
     * 2. Redis 缓存是否被正确清除以保持数据一致性
     * </p>
     */
    @Test
    void testUpdateUserProfile() {
        log.debug("测试更新用户资料");

        // 验证初始缓存状态
        RMap<String, String> userUuidMap = redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        Assertions.assertTrue(userUuidMap.isExists(), "测试前用户UUID缓存应该存在");

        // 保存原始数据以便验证
        String originalName = userDO.getName();
        String originalEmail = userDO.getEmail();
        String originalPhone = userDO.getPhone();

        // 创建包含需要更新字段的用户对象 - 故意只更新部分字段
        UserDO updateProfileDO = new UserDO();
        updateProfileDO.setUserUuid(userDO.getUserUuid())
                .setName("UpdatedProfileName")
                .setEmail("updated.profile@example.com")
                // 故意将电话设为null，验证selective更新
                .setPhone(null);

        // 执行资料更新操作
        userDAO.updateUserProfile(updateProfileDO, userDO);

        // 从数据库获取更新后的用户数据
        UserDO updatedUserDO = userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one();

        // 验证资料是否已正确更新
        Assertions.assertNotNull(updatedUserDO, "更新后应能从数据库获取用户");

        // 验证已提供值的字段是否已更新
        Assertions.assertNotEquals(originalName, updatedUserDO.getName(), "用户名应该已被更改");
        Assertions.assertEquals(updateProfileDO.getName(), updatedUserDO.getName(), "新用户名应该已正确保存");

        Assertions.assertNotEquals(originalEmail, updatedUserDO.getEmail(), "邮箱应该已被更改");
        Assertions.assertEquals(updateProfileDO.getEmail(), updatedUserDO.getEmail(), "新邮箱应该已正确保存");

        // 验证未提供值的字段是否保持不变（因为是null）
        Assertions.assertEquals(originalPhone, updatedUserDO.getPhone(), "未提供值的电话号码不应被更新为null");

        // 验证密码等其他字段没有被修改
        Assertions.assertEquals(userDO.getPassword(), updatedUserDO.getPassword(), "密码不应被修改");
        Assertions.assertEquals(userDO.getStatus(), updatedUserDO.getStatus(), "状态不应被修改");

        // 验证Redis缓存是否已被清除
        userUuidMap = redisson.getMap(StringConstant.Redis.USER_UUID + userDO.getUserUuid());
        RBucket<String> oldUserNameBucket = redisson.getBucket(StringConstant.Redis.USER_NAME + originalName);
        RBucket<String> newUserNameBucket = redisson.getBucket(StringConstant.Redis.USER_NAME + updateProfileDO.getName());
        RBucket<String> oldUserMailBucket = redisson.getBucket(StringConstant.Redis.USER_MAIL + originalEmail);
        RBucket<String> newUserMailBucket = redisson.getBucket(StringConstant.Redis.USER_MAIL + updateProfileDO.getEmail());
        RBucket<String> userTelBucket = redisson.getBucket(StringConstant.Redis.USER_TEL + originalPhone);

        // 检查所有缓存是否已被删除
        Assertions.assertFalse(userUuidMap.isExists(), "更新资料后用户UUID缓存应被删除");
        Assertions.assertFalse(oldUserNameBucket.isExists(), "更新资料后原用户名缓存应被删除");
        Assertions.assertFalse(newUserNameBucket.isExists(), "更新后的用户名不应有缓存");
        Assertions.assertFalse(oldUserMailBucket.isExists(), "更新资料后原邮箱缓存应被删除");
        Assertions.assertFalse(newUserMailBucket.isExists(), "更新后的邮箱不应有缓存");
        Assertions.assertFalse(userTelBucket.isExists(), "更新资料后用户电话缓存应被删除");

        // 更新用户对象以反映数据库中的新状态（为了tearDown正常工作）
        userDO = updatedUserDO;
    }

}
