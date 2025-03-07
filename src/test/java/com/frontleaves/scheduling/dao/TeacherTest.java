package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.entity.multiple.UserAndTeacherDO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@SpringBootTest
@Slf4j
class TeacherTest {
    @Resource
    private TeacherDAO teacherDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private UserDAO userDAO;
    @Resource
    private RedissonClient redisson;
    private TeacherDO setUpTeacher;
    private UserDO setUpUser;


    /**
     * 通过部门 名称获取部门数据
     *
     * @return 部门数据
     */
    private DepartmentDO getDepartmentByName() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        if (departmentDO == null) {
            throw new BusinessException("[dao.TeacherTest]单元测试通过找不到部门数据",
                    ErrorCode.OPERATION_ERROR);
        }
        return departmentDO;
    }


    @BeforeEach
    @Transactional
    void setUp() {
        log.debug("TeacherDAO单元测试初始化");
        setUpTeacher = new TeacherDO();
        setUpUser = new UserDO();
        setUpUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("teacherDAOTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("teacherDAOTest@qwer.com")
                .setPhone("14452873800")
                .setStatus((short) 1)
                .setBan(0)
                .setRoleUuid(SystemConstant.getRoleTeacher())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, setUpUser.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, setUpUser.getName()).remove();
        }
        userDAO.save(setUpUser);
        setUpTeacher.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(getDepartmentByName().getDepartmentUuid())
                .setUserUuid(setUpUser.getUserUuid())
                .setId("123456")
                .setName("teacherDAOTest")
                .setEnglishName("ZhangSeng")
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
        RMap<String, String> teacherMap = redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid());
        teacherMap.putAll(ConvertUtil.convertObjectToMapString(setUpTeacher));
        teacherMap.expire(Duration.ofSeconds(86400));
        RBucket<String> teacherId = redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + setUpTeacher.getId());
        teacherId.set(setUpTeacher.getTeacherUuid());
        teacherId.expire(Duration.ofSeconds(86400));
        RBucket<String> teacherUserUuid = redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + setUpTeacher.getUserUuid());
        teacherUserUuid.set(setUpTeacher.getTeacherUuid());
        teacherUserUuid.expire(Duration.ofSeconds(86400));
    }

    @AfterEach
    @Transactional
    void tearDown() {
        log.debug("TeacherDAO单元测试结束");
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, setUpTeacher.getId()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_ID + setUpTeacher.getId()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_USER_UUID + setUpTeacher.getUserUuid()).delete();
    }

    @Test
    void testGetTeacherById() {
        log.debug("测试通过教师ID获取教师信息");
        TeacherDO teacherById = teacherDAO.getTeacherById(setUpTeacher.getId());
        assert teacherById != null;
        Assertions.assertNotNull(teacherById);
        log.debug("删除教师ID缓存信息");
        redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + setUpTeacher.getId()).delete();
        TeacherDO teacherDO1 = teacherDAO.getTeacherById(setUpTeacher.getId());
        assert teacherDO1 != null;
        Assertions.assertNotNull(teacherDO1);
    }

    @Test
    void testGetTeacherByUuid() {
        log.debug("测试通过教师UUID获取教师信息");
        TeacherDO teacherByUuid = teacherDAO.getTeacherByUuid(setUpTeacher.getTeacherUuid());
        assert teacherByUuid != null;
        Assertions.assertNotNull(teacherByUuid);
        log.debug("删除教师UUID缓存信息");
        redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid()).delete();
        TeacherDO teacherDO1 = teacherDAO.getTeacherByUuid(setUpTeacher.getTeacherUuid());
        assert teacherDO1 != null;
        Assertions.assertNotNull(teacherDO1);
    }

    @Test
    void testGetTeacherByUserUuid() {
        log.debug("测试通过用户UUID获取教师信息");
        TeacherDO teacherByUserUuid = teacherDAO.getTeacherByUserUuid(setUpUser.getUserUuid());
        Assertions.assertNotNull(teacherByUserUuid);
        log.debug("删除教师用户UUID缓存信息");
        redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + setUpUser.getUserUuid()).delete();
        TeacherDO teacherDO1 = teacherDAO.getTeacherByUserUuid(setUpUser.getUserUuid());
        Assertions.assertNotNull(teacherDO1);
    }

    @Test
    void testDeleteTeacher() {
        log.debug("测试删除教师信息");
        teacherDAO.deleteTeacher(setUpTeacher);
        TeacherDO teacherDO1 = teacherDAO.lambdaQuery().eq(
                TeacherDO::getTeacherUuid, setUpTeacher.getTeacherUuid()).one();
        Assertions.assertNull(teacherDO1);
        log.debug("检查缓存是否删除成功");
        RMap<String, String> uuid = redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid());
        RBucket<String> id = redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + setUpTeacher.getId());
        RBucket<String> userUuid = redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + setUpTeacher.getUserUuid());
        Assertions.assertFalse(uuid.isExists());
        Assertions.assertFalse(id.isExists());
        Assertions.assertFalse(userUuid.isExists());
    }

    @Test
    void testUpdateUserUuid() {
        log.debug("测试更新教师的用户uuid");
        //创建一个新的教师用户
        UserDO newTestUserDO = new UserDO();
        newTestUserDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("teacherDAONewTestUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("teacherDAONewTestUser@qwer.com")
                .setPhone("15859273800")
                .setStatus((short) 1)
                .setBan(0)
                .setRoleUuid(SystemConstant.getRoleTeacher())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, newTestUserDO.getName()).one() == null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, newTestUserDO.getName()).remove();
        }
        userDAO.save(newTestUserDO);
        teacherDAO.updateUserUuid(newTestUserDO.getUserUuid(), setUpTeacher.getId());
        TeacherDO teacherDO1 = teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one();
        Assertions.assertEquals(newTestUserDO.getUserUuid(), teacherDO1.getUserUuid());
        //删除老师和用户数据
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getTeacherUuid, teacherDO1.getTeacherUuid()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getTeacherUuid, teacherDO1.getTeacherUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, newTestUserDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, newTestUserDO.getUserUuid()).remove();
        }
    }

    @Test
    void testUpdateTeacher() {
        log.debug("测试更新教师信息");
        UserDO userDO = new UserDO();
        userDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("logicUserTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("logicUserTest@test.com")
                .setPhone("13800000000")
                .setStatus((short) 1)
                .setBan(0)
                .setPermission("[\"user:unit:department:tag:category:delete\"]")
                .setRoleUuid(SystemConstant.getRoleTeacher());
        if (userDAO.lambdaQuery().eq(UserDO::getName, userDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, userDO.getName()).remove();
        }
        userDAO.save(userDO);
        TeacherDO newTeacherDO = new TeacherDO();
        newTeacherDO.setTeacherUuid(setUpTeacher.getTeacherUuid())
                .setId("123456")
                .setUserUuid(userDO.getUserUuid())
                .setName("newTeacherDAOTest")
                .setEnglishName("ZhangSeng123456")
                .setEthnic("水族")
                .setSex(0)
                .setPhone("14452873811")
                .setEmail("newTeacherDAOTest@qwer.com")
                .setJobTitle("老师")
                .setDesc("这是一个老师");
        teacherDAO.updateTeacher(newTeacherDO);
        TeacherDO teacherDO1 = teacherDAO.lambdaQuery()
                .eq(TeacherDO::getTeacherUuid, newTeacherDO.getTeacherUuid())
                .one();
        Assertions.assertEquals(newTeacherDO.getId(), teacherDO1.getId());
        Assertions.assertEquals(newTeacherDO.getName(), teacherDO1.getName());
        Assertions.assertEquals(newTeacherDO.getEnglishName(), teacherDO1.getEnglishName());
        Assertions.assertEquals(newTeacherDO.getEthnic(), teacherDO1.getEthnic());
        Assertions.assertEquals(newTeacherDO.getSex(), teacherDO1.getSex());
        Assertions.assertEquals(newTeacherDO.getPhone(), teacherDO1.getPhone());
        Assertions.assertEquals(newTeacherDO.getEmail(), teacherDO1.getEmail());
        Assertions.assertEquals(newTeacherDO.getJobTitle(), teacherDO1.getJobTitle());
        Assertions.assertEquals(newTeacherDO.getDesc(), teacherDO1.getDesc());
        //检查更新后缓存是否删除
        RMap<String, String> uuid = redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + teacherDO1.getTeacherUuid());
        RBucket<String> id = redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + teacherDO1.getId());
        RBucket<String> userUuid = redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + teacherDO1.getUserUuid());
        Assertions.assertFalse(uuid.isExists());
        Assertions.assertFalse(id.isExists());
        Assertions.assertFalse(userUuid.isExists());
        teacherDAO.removeById(setUpTeacher);
        userDAO.removeById(userDO);
    }

    @Test
    void testGetTeacherList() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        List<UserAndTeacherDO> teacherList = teacherDAO.getTeacherList(1, 20, true, null, null, null);
        log.debug("{}", teacherList);
        Assertions.assertNotNull(teacherList);
    }

}
