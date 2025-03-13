package com.frontleaves.scheduling.dao;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.frontleaves.scheduling.constants.StringConstant;
import com.frontleaves.scheduling.constants.SystemConstant;
import com.frontleaves.scheduling.daos.*;
import com.frontleaves.scheduling.models.entity.DepartmentDO;
import com.frontleaves.scheduling.models.entity.MajorDO;
import com.frontleaves.scheduling.models.entity.StudentDO;
import com.frontleaves.scheduling.models.entity.UserDO;
import com.frontleaves.scheduling.models.vo.StudentVO;
import com.xlf.utility.ErrorCode;
import com.xlf.utility.exception.BusinessException;
import com.xlf.utility.util.ConvertUtil;
import com.xlf.utility.util.PasswordUtil;
import com.xlf.utility.util.UuidUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

@Slf4j
@SpringBootTest
class StudentTest {
    @Resource
    private StudentDAO studentDAO;

    @Resource
    private UserDAO userDAO;
    @Resource
    private DepartmentDAO departmentDAO;
    @Resource
    private MajorDAO majorDAO;
    @Resource
    private RedissonClient redisson;
    @Resource
    private AdministrativeClassDAO administrativeClassDAO;
    @Resource
    private GradeDAO gradeDAO;
    private StudentDO setUpStudent;
    private UserDO setUpUser;

    /**
     * 通过部门 名称获取部门数据
     *
     * @return 部门数据
     */
    private DepartmentDO getDepartmentByName() {
        DepartmentDO departmentDO = departmentDAO.lambdaQuery().list().get(0);
        if (departmentDO == null) {
            throw new BusinessException("[dao.StudentTest]单元测试通过找不到部门数据",
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
            throw new BusinessException("[dao.StudentTest]单元测试通过找不到专业数据",
                    ErrorCode.OPERATION_ERROR);
        }
        return majorDO;
    }


    @BeforeEach
    void setUp() {
        log.debug("StudentDAO单元测试初始化");
        setUpUser = new UserDO();
        setUpUser.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("studentDAOTest")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("studentDAOTest@qwer.com")
                .setPhone("14452873800")
                .setStatus((byte) 1)
                .setBan(false)
                .setRoleUuid(SystemConstant.getRoleStudent())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, setUpUser.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, setUpUser.getName()).remove();
        }
        userDAO.save(setUpUser);
        setUpStudent = new StudentDO();
        log.debug("初始化学生信息");
        setUpStudent.setStudentUuid(UuidUtil.generateUuidNoDash())
                .setId("1")
                .setName("ZhangSan1314")
                .setGender(true)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(0).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(0).getAdministrativeClassUuid())
                .setGraduated(false)
                .setUserUuid(setUpUser.getUserUuid());
        if (studentDAO.lambdaQuery().eq(StudentDO::getName, setUpStudent.getName()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getName, setUpStudent.getName()).remove();
        }
        log.debug("保存数据");
        studentDAO.save(setUpStudent);
        //添加缓存
        RMap<String, String> map = redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid());
        map.putAll(ConvertUtil.convertObjectToMapString(setUpStudent));
        map.expire(Duration.ofSeconds(86400));
        RBucket<String> bucketId = redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId());
        bucketId.set(setUpStudent.getStudentUuid());
        bucketId.expire(Duration.ofSeconds(86400));
        RBucket<String> bucketUserUuid = redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID +
                setUpStudent.getUserUuid());
        bucketUserUuid.set(setUpStudent.getStudentUuid());
        bucketUserUuid.expire(Duration.ofSeconds(86400));
    }

    @AfterEach
    void tearDown() {
        log.debug("学生信息清理");
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, setUpStudent.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, setUpStudent.getStudentUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, setUpUser.getUserUuid()).remove();
        }
        redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId()).delete();
        redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid()).delete();
    }

    @Test
    void testGetStudentById() {
        log.debug("测试通过学生 ID 获取学生信息");
        StudentDO studentById = studentDAO.getStudentById(setUpStudent.getId());
        Assertions.assertNotNull(studentById);
        redisson.getBucket(StringConstant.Redis.STUDENT_ID + setUpStudent.getId()).delete();
        StudentDO studentById1 = studentDAO.getStudentById(setUpStudent.getId());
        Assertions.assertNotNull(studentById1);
    }

    @Test
    void testGetStudentByUuid() {
        log.debug("测试通过学生 UUID 获取学生信息");
        StudentDO studentByUuid = studentDAO.getStudentByUuid(setUpStudent.getStudentUuid());
        Assertions.assertNotNull(studentByUuid);
        redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid()).delete();
        StudentDO studentByUuid1 = studentDAO.getStudentByUuid(setUpStudent.getStudentUuid());
        Assertions.assertNotNull(studentByUuid1);
    }

    @Test
    void testUpdateUserUuid() {
        log.debug("测试更新学生信息中的用户 UUID");
        //创建一个新的教师用户
        UserDO newTestUserDO = new UserDO();
        newTestUserDO.setUserUuid(UuidUtil.generateUuidNoDash())
                .setName("studentDAONewTestUser")
                .setPassword(PasswordUtil.encrypt("123456Aa"))
                .setEmail("studentDAONewTestUser@qwer.com")
                .setPhone("15859273800")
                .setStatus((byte) 1)
                .setBan(false)
                .setRoleUuid(SystemConstant.getRoleStudent())
                .setPermission("[\"user:role:edit\"]");
        if (userDAO.lambdaQuery().eq(UserDO::getName, newTestUserDO.getName()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getName, newTestUserDO.getName()).remove();
        }
        userDAO.save(newTestUserDO);
        studentDAO.updateUserUuid(newTestUserDO.getUserUuid(), setUpStudent.getId());
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getId, setUpStudent.getId()).one();
        Assertions.assertEquals(newTestUserDO.getUserUuid(), studentDO1.getUserUuid());
        //删除学生和新增用户数据
        if (studentDAO.lambdaQuery().eq(StudentDO::getStudentUuid, studentDO1.getStudentUuid()).one() != null) {
            studentDAO.lambdaUpdate().eq(StudentDO::getStudentUuid, studentDO1.getStudentUuid()).remove();
        }
        if (userDAO.lambdaQuery().eq(UserDO::getUserUuid, newTestUserDO.getUserUuid()).one() != null) {
            userDAO.lambdaUpdate().eq(UserDO::getUserUuid, newTestUserDO.getUserUuid()).remove();
        }
    }

    @Test
    void testDeleteStudent() {
        log.debug("测试删除学生信息");
        studentDAO.deleteStudent(setUpStudent);
        StudentDO studentDO1 = studentDAO.lambdaQuery().eq(StudentDO::getId, setUpStudent.getId()).one();
        log.debug("检查缓存是否删除");
        RMap<String, String> getMapById = redisson.getMap(StringConstant.Redis.STUDENT_ID + setUpStudent.getId());
        RMap<String, String> getMapByUuid = redisson.getMap(StringConstant.Redis.STUDENT_UUID + setUpStudent.getStudentUuid());
        RMap<String, String> getMapByUserUuid = redisson.getMap(StringConstant.Redis.STUDENT_USER_UUID
                + setUpStudent.getUserUuid());
        Assertions.assertNull(studentDO1);
        Assertions.assertFalse(getMapById.isExists());
        Assertions.assertFalse(getMapByUuid.isExists());
        Assertions.assertFalse(getMapByUserUuid.isExists());
    }

    @Test
    void testGetStudentByUserUuid() {
        log.debug("测试通过用户 UUID 获取学生信息");
        StudentDO studentByUserUuid = studentDAO.getStudentByUserUuid(setUpStudent.getUserUuid());
        Assertions.assertNotNull(studentByUserUuid);
        redisson.getBucket(StringConstant.Redis.STUDENT_USER_UUID + setUpStudent.getUserUuid()).delete();
        StudentDO studentByUserUuid1 = studentDAO.getStudentByUserUuid(setUpStudent.getUserUuid());
        Assertions.assertNotNull(studentByUserUuid1);
    }

    /**
     * 测试获取学生列表
     */
    @Test
    void testListStudents() {
        log.debug("测试获取学生列表");
        // 传入条件
        Page<StudentDO> pageResult = studentDAO.listStudents(
                1, 10, true,
                setUpStudent.getClazz(), Boolean.valueOf(setUpStudent.getDepartment()),
                setUpStudent.getMajor(), setUpStudent.getGradeUuid());
        Assertions.assertNotNull(pageResult, "listStudents返回值不应为null");
        Assertions.assertFalse(pageResult.getRecords().isEmpty(), "列表查询应至少返回一条记录");

        // 当所有选填字段为空时, 直接返回空页
        Page<StudentDO> emptyPage = studentDAO.listStudents(
                1, 10, null,
                null, null, null, null
        );
        Assertions.assertNotNull(emptyPage, "listStudents返回值不应为null");
        Assertions.assertTrue(emptyPage.getRecords().isEmpty(), "选填字段为空时列表查询应返回空页");
    }

    /**
     * 测试编辑学生
     */
    @Test
    void testEditStudent() {
        String studentUuid = UuidUtil.generateUuidNoDash();
        String studentId = "A951753";

        // 如果存在相同学号的学生记录，先删除
        if (studentDAO.lambdaQuery().eq(StudentDO::getId, studentId).exists()) {
            studentDAO.lambdaUpdate().eq(StudentDO::getId, studentId).remove();
        }

        // 构造初始学生数据并保存
        StudentDO initialStudent = new StudentDO();
        initialStudent.setStudentUuid(studentUuid)
                .setId(studentId)
                .setName("OriginalName")
                .setGender(false)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(1).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(1).getAdministrativeClassUuid())
                .setGraduated(false)
                .setUserUuid(setUpUser.getUserUuid());
        studentDAO.save(initialStudent);

        // 2. 构造更新用的 VO 对象（更新姓名、毕业状态等）
        StudentVO studentTestVO = new StudentVO();
        studentTestVO.setStudentUuid(studentUuid)
                .setId(studentId)
                .setName("LiSi456")
                .setGender(false)
                .setGradeUuid(gradeDAO.lambdaQuery().list().get(1).getGradeUuid())
                .setDepartment(getDepartmentByName().getDepartmentUuid())
                .setMajor(getMajorByName().getMajorUuid())
                .setClazz(administrativeClassDAO.lambdaQuery().list().get(1).getAdministrativeClassUuid())
                .setGraduated(true)
                .setUserUuid(setUpUser.getUserUuid());

        // 3. 执行更新操作
        StudentDO editStudent = null;
        try {
            editStudent = studentDAO.editStudent(studentUuid, studentTestVO);
        } catch (BusinessException e) {
            Assertions.fail("editStudent 异常：" + e.getMessage());
        }

        // 4. 校验更新结果
        Assertions.assertNotNull(editStudent, "更新后的学生不应为null");
        Assertions.assertEquals("A951753", editStudent.getId(), "学号应该被更新");
        Assertions.assertEquals("LiSi456", editStudent.getName(), "姓名应该被更新");
        Assertions.assertTrue(editStudent.getGraduated(), "毕业状态应更新为 true");

        // 5. 验证缓存是否更新
        RMap<String, String> studentCache = redisson.getMap(StringConstant.Redis.STUDENT_UUID + studentUuid);
        Assertions.assertTrue(studentCache.isExists(), "学生缓存应存在");
        String cachedName = studentCache.get("name");
        Assertions.assertEquals(editStudent.getName(), cachedName, "缓存中的姓名应该被更新");
    }













}
