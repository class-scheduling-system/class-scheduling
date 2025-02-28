package com.frontleaves.scheduling.dao;

import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.RoleDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.RoleDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.UserDO;
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
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

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
    private RoleDAO roleDAO;
    @Resource
    private Redisson redisson;
    //初始化后查询出来的DO对象
    private TeacherDO teacherDO;
    //初始化后查询出来的DO对象
    private UserDO userDO;

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
     * 根据角色名称获取角色对象
     * <p>
     * 此方法使用lambda表达式和链式调用从数据库中查询角色名称匹配的角色对象如果找不到对应的角色，
     * 则抛出一个自定义的BusinessException异常，指示操作失败这主要是为了处理角色数据不存在的情况，
     * 确保调用此方法时能够得到明确的错误提示
     *
     * @return RoleDO 如果找到匹配的角色名称，则返回对应的角色对象
     * @throws BusinessException 如果数据库中不存在指定角色名称的角色，则抛出此异常
     */
    private RoleDO getRoleByName() {
        RoleDO roleDO = roleDAO.lambdaQuery().eq(RoleDO::getRoleName, "老师").one();
        if (roleDO == null) {
            throw new BusinessException(
                    "[dao.StudentTest]单元测试通过角色名称找不到角色数据", ErrorCode.OPERATION_ERROR);
        }
        return roleDO;
    }

    @BeforeEach
    void setUp() {
        log.debug("TeacherDAO单元测试初始化");
        TeacherDO setUpTeacher = new TeacherDO();
        teacherDO = new TeacherDO();
        UserDO setUpUser = new UserDO();
        userDO = new UserDO();
        setUpUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("teacherDAOTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("teacherDAOTest@qwer.com")
                .setPhone("14452873800")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(getRoleByName().getRoleUuid())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, setUpUser.getName()).one() == null) {
            userDAO.save(setUpUser);
        }
        userDO = userDAO.lambdaQuery().eq(UserDO::getName, setUpUser.getName()).one();
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
            teacherDAO.save(setUpTeacher);
        }
        teacherDO = teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one();
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
    void tearDown() {
        log.debug("TeacherDAO单元测试结束");
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, teacherDO.getId()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, teacherDO.getId()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, userDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, userDO.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.TEACHER_UUID + teacherDO.getTeacherUuid()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_ID + teacherDO.getId()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_USER_UUID + teacherDO.getUserUuid()).delete();
    }

    @Test
    void testGetTeacherById() {
        log.debug("测试通过教师ID获取教师信息");
        TeacherDO teacherById = teacherDAO.getTeacherById(teacherDO.getId());
        assert teacherById != null;
        Assertions.assertNotNull(teacherById);
        log.debug("删除教师ID缓存信息");
        redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + teacherDO.getId()).delete();
        TeacherDO teacherDO1 = teacherDAO.getTeacherById(teacherDO.getId());
        assert teacherDO1 != null;
        Assertions.assertNotNull(teacherDO1);
    }

    @Test
    void testGetTeacherByUuid() {
        log.debug("测试通过教师UUID获取教师信息");
        TeacherDO teacherByUuid = teacherDAO.getTeacherByUuid(teacherDO.getTeacherUuid());
        assert teacherByUuid != null;
        Assertions.assertNotNull(teacherByUuid);
        log.debug("删除教师UUID缓存信息");
        redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + teacherDO.getTeacherUuid()).delete();
        TeacherDO teacherDO1 = teacherDAO.getTeacherByUuid(teacherDO.getTeacherUuid());
        assert teacherDO1 != null;
        Assertions.assertNotNull(teacherDO1);
    }

    @Test
    void testGetTeacherByUserUuid() {
        log.debug("测试通过用户UUID获取教师信息");
        TeacherDO teacherByUserUuid = teacherDAO.getTeacherByUserUuid(userDO.getUserUuid());
        Assertions.assertNotNull(teacherByUserUuid);
        log.debug("删除教师用户UUID缓存信息");
        redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + userDO.getUserUuid()).delete();
        TeacherDO teacherDO1 = teacherDAO.getTeacherByUserUuid(userDO.getUserUuid());
        Assertions.assertNotNull(teacherDO1);
    }

    @Test
    void testDeleteTeacher() {
        log.debug("测试删除教师信息");
        teacherDAO.deleteTeacher(teacherDO);
        TeacherDO teacherDO1 = teacherDAO.lambdaQuery().eq(
                TeacherDO::getTeacherUuid,teacherDO.getTeacherUuid()).one();
        Assertions.assertNull(teacherDO1);
        log.debug("检查缓存是否删除成功");
        RMap<String, String> uuid = redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + teacherDO.getTeacherUuid());
        RBucket<String> id = redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + teacherDO.getId());
        RBucket<String> userUuid = redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + teacherDO.getUserUuid());
        Assertions.assertFalse(uuid.isExists());
        Assertions.assertFalse(id.isExists());
        Assertions.assertFalse(userUuid.isExists());
    }
    @Test
    void testUpdateUserUuid (){
        log.debug("测试更新教师的用户uuid");
        //创建一个新的教师用户
        UserDO newTestUserDO = new UserDO();
        newTestUserDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("teacherDAONewTestUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("teacherDAONewTestUser@qwer.com")
                .setPhone("15859273800")
                .setStatus(1)
                .setBan(0)
                .setRoleUuid(getRoleByName().getRoleUuid())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, newTestUserDO.getName()).one() == null) {
            userDAO.save(newTestUserDO);
        }
        UserDO newUser = userDAO.lambdaQuery().eq(UserDO::getName, newTestUserDO.getName()).one();
        teacherDAO.updateUserUuid(newUser.getUserUuid(),teacherDO.getId());
        TeacherDO teacherDO1 = teacherDAO.lambdaQuery().eq(TeacherDO::getId, teacherDO.getId()).one();
        Assertions.assertEquals(newUser.getUserUuid(),teacherDO1.getUserUuid());
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getTeacherUuid,teacherDO1.getTeacherUuid()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getTeacherUuid,teacherDO1.getTeacherUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid,newUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, newUser.getUserUuid()).remove();
        }
    }


}
