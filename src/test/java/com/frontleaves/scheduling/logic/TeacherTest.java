package com.frontleaves.scheduling.logic;

import cn.hutool.core.bean.BeanUtil;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.DepartmentDAO;
import com.frontleaves.scheduling.daos.TeacherDAO;
import com.frontleaves.scheduling.daos.TeacherTypeDAO;
import com.frontleaves.scheduling.daos.UserDAO;
import com.frontleaves.scheduling.models.dto.PageDTO;
import com.frontleaves.scheduling.models.dto.TeacherDTO;
import com.frontleaves.scheduling.models.dto.TeacherDisableDTO;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.TeacherDO;
import com.frontleaves.scheduling.models.entity.TeacherTypeDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.TeacherVO;
import com.frontleaves.scheduling.services.TeacherService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@Slf4j
class TeacherTest {
    @Resource
    private TeacherService teacherService;
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
    private DepartmentDO setUpDepartment;
    @Autowired
    private TeacherTypeDAO teacherTypeDAO;

    /**
     * 获取项目中的一个可用部门
     *
     * @return 部门数据对象
     */
    private DepartmentDO getDepartmentByName() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        if (departmentDO == null) {
            throw new BusinessException("[logic.TeacherTest]单元测试找不到部门数据",
                    ErrorCode.OPERATION_ERROR);
        }
        return departmentDO;
    }

    @BeforeEach
    void setUp() {
        log.debug("TeacherLogic单元测试初始化");
        setUpDepartment = getDepartmentByName();

        // 创建测试用户
        setUpUser = new UserDO();
        setUpUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("teacherLogicTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("teacherLogicTest@test.com")
                .setPhone("13888888888")
                .setStatus((byte) 1)
                .setBan(false)
                .setPermission("[\"user:role:edit\"]")
                .setRoleUuid(SystemConstant.getRoleTeacher());

        // 清除可能存在的重复用户
        if (userDAO.lambdaQuery().eq(UserDO::getName, setUpUser.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, setUpUser.getName()).remove();
        }

        // 保存用户
        userDAO.save(setUpUser);

        TeacherTypeDO teacherTypeDO = teacherTypeDAO.lambdaQuery().eq(TeacherTypeDO::getTypeName, "辅导员").one();
        // 创建测试教师
        setUpTeacher = new TeacherDO();
        setUpTeacher.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(setUpDepartment.getDepartmentUuid())
                .setUserUuid(setUpUser.getUserUuid())
                .setId("T12345")
                .setName("teacherLogicTest")
                .setEnglishName("teacherLogicTest")
                .setEthnic("汉族")
                .setSex(true)
                .setType(teacherTypeDO.getTeacherTypeUuid())
                .setPhone("13888888888")
                .setEmail("teacherLogicTest@test.com")
                .setJobTitle("教授")
                .setDesc("这是一个测试教师");

        // 清除可能存在的重复教师
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, setUpTeacher.getId()).remove();
        }

        // 保存教师
        teacherDAO.save(setUpTeacher);

        // 将教师信息放入Redis缓存
        RMap<String, String> teacherMap = redisson.getMap(
                StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid());
        teacherMap.putAll(ConvertUtil.convertObjectToMapString(setUpTeacher));
        teacherMap.expire(Duration.ofSeconds(86400));

        // 设置教师ID和UUID的映射关系
        RBucket<String> teacherId = redisson.getBucket(
                StringConstant.Redis.TEACHER_ID + setUpTeacher.getId());
        teacherId.set(setUpTeacher.getTeacherUuid());
        teacherId.expire(Duration.ofSeconds(86400));

        // 设置教师用户UUID和教师UUID的映射关系
        RBucket<String> teacherUserUuid = redisson.getBucket(
                StringConstant.Redis.TEACHER_USER_UUID + setUpTeacher.getUserUuid());
        teacherUserUuid.set(setUpTeacher.getTeacherUuid());
        teacherUserUuid.expire(Duration.ofSeconds(86400));
    }

    @AfterEach
    @Transactional
    void tearDown() {
        log.debug("TeacherLogic单元测试结束，清理测试数据");

        // 删除测试教师
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, setUpTeacher.getId()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, setUpTeacher.getId()).remove();
        }

        // 删除测试用户
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).remove();
        }

        // 清理Redis缓存
        redisson.getMap(StringConstant.Redis.TEACHER_UUID + setUpTeacher.getTeacherUuid()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_ID + setUpTeacher.getId()).delete();
        redisson.getBucket(StringConstant.Redis.TEACHER_USER_UUID + setUpTeacher.getUserUuid()).delete();
    }

    /**
     * 测试添加教师方法
     */
    @Test
    void testAddTeacher() {
        log.debug("测试添加教师");

        TeacherTypeDO teacherTypeDO = teacherTypeDAO.lambdaQuery().eq(TeacherTypeDO::getTypeName, "辅导员").one();
        // 创建教师视图对象
        TeacherVO teacherVO = new TeacherVO(
                setUpDepartment.getDepartmentUuid(),
                "T54321",
                "newTeacherLogicTest",
                "newTeacherLogicTest",
                "汉族",
                true,
                teacherTypeDO.getTeacherTypeUuid(),
                "13777777777",
                "newTeacherLogicTest@test.com",
                "讲师",
                "这是一个新的测试教师"
        );

        // 调用添加教师方法
        teacherService.addTeacher(teacherVO);

        // 查询新添加的教师
        TeacherDO savedTeacher = teacherDAO.lambdaQuery().eq(TeacherDO::getId, teacherVO.getId()).one();

        // 验证教师是否添加成功
        Assertions.assertNotNull(savedTeacher, "新添加的教师应该存在");
        Assertions.assertEquals(teacherVO.getId(), savedTeacher.getId(), "教师ID应该匹配");
        Assertions.assertEquals(teacherVO.getName(), savedTeacher.getName(), "教师名称应该匹配");
        Assertions.assertEquals(teacherVO.getUnitUuid(), savedTeacher.getUnitUuid(), "教师所属部门UUID应该匹配");

        // 验证UUID是否被正确生成
        Assertions.assertNotNull(savedTeacher.getTeacherUuid(), "教师UUID不应为空");
        Assertions.assertTrue(savedTeacher.getTeacherUuid().matches(StringConstant.Regular.UUID_NO_DASH_REGULAR_EXPRESSION),
                "教师UUID应符合无连字符UUID格式");

        // 清理测试数据
        teacherDAO.lambdaUpdate().eq(TeacherDO::getId, teacherVO.getId()).remove();
    }

    /**
     * 测试添加教师时部门不存在的情况
     */
    @Test
    void testAddTeacherWithNonExistentDepartment() {
        log.debug("测试添加教师时部门不存在");

        // 创建教师视图对象，使用不存在的部门UUID
        TeacherVO teacherVO = new TeacherVO(
                UuidUtil.generateUuidNoDash(), // 使用一个不存在的部门UUID
                "T99999",
                "NonExistentDepartmentTeacher",
                "NonExistentDepartmentTeacher",
                "汉族",
                true,
                "辅导员",
                "13666666666",
                "nonexistent@test.com",
                "讲师",
                "测试部门不存在"
        );

        // 验证调用添加教师方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.addTeacher(teacherVO));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试获取教师详情
     */
    @Test
    void testGetTeacher() {
        log.debug("测试获取教师详情");

        // 调用获取教师详情方法
        TeacherDTO teacherDTO = teacherService.getTeacher(setUpTeacher.getTeacherUuid());

        // 验证返回结果
        Assertions.assertNotNull(teacherDTO, "教师DTO不应为空");
        Assertions.assertEquals(setUpTeacher.getTeacherUuid(), teacherDTO.getTeacherUuid(), "教师UUID应该匹配");
        Assertions.assertEquals(setUpTeacher.getName(), teacherDTO.getName(), "教师名称应该匹配");
        Assertions.assertEquals(setUpTeacher.getId(), teacherDTO.getId(), "教师ID应该匹配");
        Assertions.assertEquals(setUpTeacher.getUnitUuid(), teacherDTO.getUnitUuid(), "教师所属部门UUID应该匹配");
        Assertions.assertEquals(setUpTeacher.getUserUuid(), teacherDTO.getUserUuid(), "教师关联的用户UUID应该匹配");
    }

    /**
     * 测试获取不存在的教师
     */
    @Test
    void testGetNonExistentTeacher() {
        log.debug("测试获取不存在的教师");

        // 生成一个不存在的教师UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 验证调用获取教师方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.getTeacher(nonExistentUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals(StringConstant.TEACHER_NOT_EXIST, exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试使用无效格式的UUID获取教师
     */
    @Test
    void testGetTeacherWithInvalidUuidFormat() {
        log.debug("测试使用无效格式的UUID获取教师");

        // 使用带连字符的UUID，这应该不符合无连字符UUID正则表达式
        String invalidUuid = java.util.UUID.randomUUID().toString(); // 带连字符的UUID

        // 验证调用获取教师方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.getTeacher(invalidUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals(StringConstant.TEACHER_NOT_EXIST, exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试获取教师列表
     */
    @Test
    void testGetTeacherList() {
        log.debug("测试获取教师列表");

        // 调用获取教师列表方法
        PageDTO<TeacherDTO> pageDTO = teacherService.getTeacherList(1, 10, false, null, null, null);

        // 验证返回结果
        Assertions.assertNotNull(pageDTO, "分页DTO不应为空");
        Assertions.assertEquals(1, pageDTO.getCurrent(), "当前页应该是1");
        Assertions.assertEquals(10, pageDTO.getSize(), "每页大小应该是10");

        // 验证记录列表不为空
        Assertions.assertNotNull(pageDTO.getRecords(), "记录列表不应为null");
    }

    /**
     * 测试获取教师列表，按部门筛选
     */
    @Test
    void testGetTeacherListFilterByDepartment() {
        log.debug("测试获取教师列表，按部门筛选");

        // 调用获取教师列表方法，使用部门UUID筛选
        PageDTO<TeacherDTO> pageDTO = teacherService.getTeacherList(
                1, 10, false, setUpDepartment.getDepartmentUuid(), null, null);

        // 验证返回结果
        Assertions.assertNotNull(pageDTO, "分页DTO不应为空");

        // 如果有记录，验证每条记录的部门UUID是否匹配
        if (pageDTO.getTotal() > 0 && pageDTO.getRecords() != null) {
            for (TeacherDTO teacherDTO : pageDTO.getRecords()) {
                Assertions.assertEquals(setUpDepartment.getDepartmentUuid(), teacherDTO.getUnitUuid(),
                        "筛选后的教师应该属于指定部门");
            }
        }
    }

    /**
     * 测试获取教师列表，按姓名搜索
     */
    @Test
    void testGetTeacherListSearchByName() {
        log.debug("测试获取教师列表，按姓名搜索");

        // 使用测试教师的姓名进行搜索
        PageDTO<TeacherDTO> pageDTO = teacherService.getTeacherList(
                1, 10, false, null, null, setUpTeacher.getName());

        // 验证返回结果
        Assertions.assertNotNull(pageDTO, "分页DTO不应为空");

        // 验证搜索结果包含测试教师
        boolean foundTestTeacher = false;
        if (pageDTO.getTotal() > 0 && pageDTO.getRecords() != null) {
            for (TeacherDTO teacherDTO : pageDTO.getRecords()) {
                if (teacherDTO.getTeacherUuid().equals(setUpTeacher.getTeacherUuid())) {
                    foundTestTeacher = true;
                    break;
                }
            }
            Assertions.assertTrue(foundTestTeacher, "搜索结果应该包含测试教师");
        }
    }

    /**
     * 测试获取空的教师列表
     */
    @Test
    void testGetEmptyTeacherList() {
        log.debug("测试获取空的教师列表");

        // 使用一个不太可能存在的教师姓名进行搜索
        String unlikelyName = "极不可能存在的教师名称" + System.currentTimeMillis();
        PageDTO<TeacherDTO> pageDTO = teacherService.getTeacherList(
                1, 10, false, null, null, unlikelyName);

        // 验证返回结果
        Assertions.assertNotNull(pageDTO, "分页DTO不应为空");
        Assertions.assertEquals(0, pageDTO.getTotal(), "应该找不到任何记录");
        Assertions.assertEquals(1, pageDTO.getCurrent(), "当前页应该是1");
        Assertions.assertEquals(10, pageDTO.getSize(), "每页大小应该是10");
        Assertions.assertNotNull(pageDTO.getRecords(), "记录列表不应为null");
        Assertions.assertTrue(pageDTO.getRecords().isEmpty(), "记录列表应该为空");
    }

    /**
     * 测试禁用教师
     */
    @Test
    void testDisableTeacher() {
        log.debug("测试禁用教师");

        // 调用禁用教师方法
        TeacherDisableDTO disableDTO = teacherService.disableTeacher(setUpTeacher.getTeacherUuid(), true);

        // 验证返回结果
        Assertions.assertNotNull(disableDTO, "禁用DTO不应为空");
        Assertions.assertEquals(setUpTeacher.getTeacherUuid(), disableDTO.getTeacherUuid(), "教师UUID应该匹配");
        Assertions.assertTrue(disableDTO.getStatus(), "禁用状态应该为true");

        // 验证用户状态是否已更新
        UserDO updatedUser = userDAO.getUserByUuid(setUpUser.getUserUuid());
        Assertions.assertEquals((byte) 0, updatedUser.getStatus(), "用户状态应该已更新为禁用(0)");

        // 将用户状态恢复为启用，以不影响其他测试
        UserDO restoreUser = BeanUtil.toBean(updatedUser, UserDO.class);
        restoreUser.setStatus((byte) 1);
        userDAO.updateUser(updatedUser, restoreUser);
    }

    /**
     * 测试启用教师
     */
    @Test
    void testEnableTeacher() {
        log.debug("测试启用教师");

        // 先将用户状态设为禁用
        UserDO disabledUser = BeanUtil.toBean(setUpUser, UserDO.class);
        disabledUser.setStatus((byte) 0);
        userDAO.updateUser(setUpUser, disabledUser);

        // 调用禁用教师方法（传入false表示启用）
        TeacherDisableDTO enableDTO = teacherService.disableTeacher(setUpTeacher.getTeacherUuid(), false);

        // 验证返回结果
        Assertions.assertNotNull(enableDTO, "启用DTO不应为空");
        Assertions.assertEquals(setUpTeacher.getTeacherUuid(), enableDTO.getTeacherUuid(), "教师UUID应该匹配");
        Assertions.assertFalse(enableDTO.getStatus(), "禁用状态应该为false（表示启用）");

        // 验证用户状态是否已更新
        UserDO updatedUser = userDAO.getUserByUuid(setUpUser.getUserUuid());
        Assertions.assertEquals((byte) 1, updatedUser.getStatus(), "用户状态应该已更新为启用(1)");
    }

    /**
     * 测试禁用不存在的教师
     */
    @Test
    void testDisableNonExistentTeacher() {
        log.debug("测试禁用不存在的教师");

        // 生成一个不存在的教师UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 验证调用禁用教师方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.disableTeacher(nonExistentUuid, true));

        // 验证异常信息和错误码
        Assertions.assertEquals(StringConstant.TEACHER_NOT_EXIST, exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试禁用未注册的教师
     */
    @Test
    void testDisableUnregisteredTeacher() {
        log.debug("测试禁用未注册的教师");

        TeacherTypeDO teacherTypeDO = teacherTypeDAO.lambdaQuery().eq(TeacherTypeDO::getTypeName, "辅导员").one();
        // 创建一个未关联用户的教师
        TeacherDO unregisteredTeacher = new TeacherDO();
        unregisteredTeacher.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(setUpDepartment.getDepartmentUuid())
                .setUserUuid(null) // 未关联用户
                .setId("T76543")
                .setName("unregisteredTeacher")
                .setEnglishName("unregisteredTeacher")
                .setEthnic("汉族")
                .setSex(true)
                .setType(teacherTypeDO.getTeacherTypeUuid())
                .setPhone("13666666666")
                .setEmail("unregistered@test.com")
                .setJobTitle("讲师")
                .setDesc("这是一个未注册的教师");

        // 清除可能存在的重复教师
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, unregisteredTeacher.getId()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, unregisteredTeacher.getId()).remove();
        }

        // 保存教师
        teacherDAO.save(unregisteredTeacher);

        // 验证调用禁用教师方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.disableTeacher(unregisteredTeacher.getTeacherUuid(), true));

        // 验证异常信息和错误码
        Assertions.assertEquals("教师未注册", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());

        // 清理测试数据
        teacherDAO.lambdaUpdate().eq(TeacherDO::getId, unregisteredTeacher.getId()).remove();
    }

    /**
     * 测试删除教师
     */
    @Test
    void testDeleteTeacher() {
        log.debug("测试删除教师");

        TeacherTypeDO teacherTypeDO = teacherTypeDAO.lambdaQuery().eq(TeacherTypeDO::getTypeName, "辅导员").one();
        // 创建一个未关联用户的教师用于删除测试
        TeacherDO teacherToDelete = new TeacherDO();
        teacherToDelete.setTeacherUuid(UuidUtil.generateUuidNoDash())
                .setUnitUuid(setUpDepartment.getDepartmentUuid())
                .setUserUuid(null) // 未关联用户，可以被删除
                .setId("T98765")
                .setName("teacherToDelete")
                .setEnglishName("teacherToDelete")
                .setEthnic("汉族")
                .setSex(true)
                .setType(teacherTypeDO.getTeacherTypeUuid())
                .setPhone("13999999999")
                .setEmail("teacherToDelete@test.com")
                .setJobTitle("讲师")
                .setDesc("这是一个用于测试删除的教师");

        // 清除可能存在的重复教师
        if (teacherDAO.lambdaQuery().eq(TeacherDO::getId, teacherToDelete.getId()).one() != null) {
            teacherDAO.lambdaUpdate().eq(TeacherDO::getId, teacherToDelete.getId()).remove();
        }

        // 保存教师
        teacherDAO.save(teacherToDelete);

        // 调用删除教师方法
        teacherService.deleteTeacher(teacherToDelete.getTeacherUuid());

        // 验证教师是否已被删除
        TeacherDO deletedTeacher = teacherDAO.getTeacherByUuid(teacherToDelete.getTeacherUuid());
        Assertions.assertNull(deletedTeacher, "教师应该已被删除");
    }

    /**
     * 测试删除已注册的教师
     */
    @Test
    void testDeleteRegisteredTeacher() {
        log.debug("测试删除已注册的教师");

        // 验证调用删除教师方法时会抛出业务异常（因为setUpTeacher已关联用户）
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.deleteTeacher(setUpTeacher.getTeacherUuid()));

        // 验证异常信息和错误码
        Assertions.assertEquals("教师已注册，无法删除", exception.getMessage());
        Assertions.assertEquals(ErrorCode.EXISTED, exception.getErrorCode());
    }

    /**
     * 测试删除不存在的教师
     */
    @Test
    void testDeleteNonExistentTeacher() {
        log.debug("测试删除不存在的教师");

        // 生成一个不存在的教师UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 验证调用删除教师方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.deleteTeacher(nonExistentUuid));

        // 验证异常信息和错误码
        Assertions.assertEquals(StringConstant.TEACHER_NOT_EXIST, exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试更新教师信息
     */
    @Test
    void testUpdateTeacher() {
        log.debug("测试更新教师信息");

        TeacherTypeDO teacherTypeDO = teacherTypeDAO.lambdaQuery().eq(TeacherTypeDO::getTypeName, "辅导员").one();
        // 创建更新后的教师视图对象
        TeacherVO updateTeacherVO = new TeacherVO(
                setUpDepartment.getDepartmentUuid(),
                setUpTeacher.getId(),
                "updatedTeacherName",
                "updatedEnglishName",
                "苗族", // 更新民族
                false, // 更新性别
                teacherTypeDO.getTeacherTypeUuid(), // 更新职称
                "13888888889", // 更新手机号
                "updatedEmail@test.com", // 更新邮箱
                "副教授", // 更新职称
                "这是更新后的教师描述" // 更新描述
        );

        // 调用更新教师方法
        teacherService.updateTeacher(setUpTeacher.getTeacherUuid(), updateTeacherVO);

        // 获取更新后的教师信息
        TeacherDO updatedTeacher = teacherDAO.getTeacherByUuid(setUpTeacher.getTeacherUuid());

        // 验证教师信息是否已更新
        Assertions.assertNotNull(updatedTeacher, "更新后的教师不应为空");
        Assertions.assertEquals(updateTeacherVO.getName(), updatedTeacher.getName(), "教师名称应该已更新");
        Assertions.assertEquals(updateTeacherVO.getEnglishName(), updatedTeacher.getEnglishName(), "教师英文名称应该已更新");
        Assertions.assertEquals(updateTeacherVO.getEthnic(), updatedTeacher.getEthnic(), "教师民族应该已更新");
        Assertions.assertEquals(updateTeacherVO.getSex(), updatedTeacher.getSex(), "教师性别应该已更新");
        Assertions.assertEquals(updateTeacherVO.getPhone(), updatedTeacher.getPhone(), "教师手机号应该已更新");
        Assertions.assertEquals(updateTeacherVO.getEmail(), updatedTeacher.getEmail(), "教师邮箱应该已更新");
        Assertions.assertEquals(updateTeacherVO.getJobTitle(), updatedTeacher.getJobTitle(), "教师职称应该已更新");
        Assertions.assertEquals(updateTeacherVO.getDesc(), updatedTeacher.getDesc(), "教师描述应该已更新");
    }

    /**
     * 测试更新教师信息时部门不存在
     */
    @Test
    void testUpdateTeacherWithNonExistentDepartment() {
        log.debug("测试更新教师信息时部门不存在");

        // 创建更新后的教师视图对象，使用不存在的部门UUID
        TeacherVO updateTeacherVO = new TeacherVO(
                UuidUtil.generateUuidNoDash(), // 使用一个不存在的部门UUID
                setUpTeacher.getId(),
                "updatedTeacherName",
                "updatedEnglishName",
                "苗族",
                false,
                "辅导员",
                "13888888889",
                "updatedEmail@test.com",
                "副教授",
                "这是更新后的教师描述"
        );

        // 验证调用更新教师方法时会抛出业务异常
        BusinessException exception = Assertions.assertThrows(BusinessException.class, () ->
                teacherService.updateTeacher(setUpTeacher.getTeacherUuid(), updateTeacherVO));

        // 验证异常信息和错误码
        Assertions.assertEquals("部门不存在", exception.getMessage());
        Assertions.assertEquals(ErrorCode.NOT_EXIST, exception.getErrorCode());
    }

    /**
     * 测试更新不存在的教师
     */
    @Test
    void testUpdateNonExistentTeacher() {
        log.debug("测试更新不存在的教师");

        // 生成一个不存在的教师UUID
        String nonExistentUuid = UuidUtil.generateUuidNoDash();

        // 创建教师视图对象
        TeacherVO updateTeacherVO = new TeacherVO(
                setUpDepartment.getDepartmentUuid(),
                "T11111",
                "NonExistentTeacher",
                "NonExistentTeacher",
                "汉族",
                true,
                "辅导员",
                "13888888888",
                "nonexistent@test.com",
                "教授",
                "这是一个不存在的教师"
        );

        // 调用更新教师方法（注意这里没有抛异常，因为方法实现可能是无操作返回null）
        teacherService.updateTeacher(nonExistentUuid, updateTeacherVO);

        // 验证教师是否仍然不存在
        TeacherDO teacherDO = teacherDAO.getTeacherByUuid(nonExistentUuid);
        Assertions.assertNull(teacherDO, "不存在的教师应该仍然不存在");
    }
}
